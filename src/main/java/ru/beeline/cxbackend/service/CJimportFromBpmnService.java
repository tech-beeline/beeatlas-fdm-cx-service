/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.beeline.cxbackend.client.DocumentClient;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.bi.BIInCJStep;
import ru.beeline.cxbackend.domain.bi.BiStepTypeEnum;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.domain.cj.CJStep;
import ru.beeline.cxbackend.dto.CJFullDtoV2;
import ru.beeline.cxbackend.dto.DocumentationTypeDTO;
import ru.beeline.cxbackend.exception.BadRequestException;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.model.*;
import ru.beeline.cxbackend.repository.*;
import ru.beeline.cxbackend.service.bpmn.BpmnOrderAssignment;
import ru.beeline.cxbackend.service.bpmn.BpmnOrderCalculator;
import ru.beeline.cxbackend.service.bpmn.BpmnOrderUtils;
import ru.beeline.cxbackend.utils.Utils;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class CJimportFromBpmnService {

    @Autowired
    private CJRepository cjRepository;
    @Autowired
    private BIStatusRepository bIStatusRepository;

    @Autowired
    private DocumentClient documentClient;

    @Autowired
    private BusinessInteractionRepository biRepository;

    @Autowired
    private CJStepRepository cjStepRepository;

    @Autowired
    private BIInCJStepRepository biInCJStepRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private BiStepRelationRepository biStepRelationRepository;

    @Autowired
    private BiStepTypeEnumRepository biStepTypeEnumRepository;

    @Autowired
    private BiStepRepository biStepRepository;

    @PostConstruct
    public void initModelMapperMapping() {
        modelMapper.typeMap(CJ.class, CJFullDtoV2.class).addMapping(CJ::getIdProductExt, CJFullDtoV2::setProductId);
    }

    public void importFromBpmnCreate(Long id, Long userId) {
        CJ cj = cjRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new NotFoundException("Сj id " + id + " does not exist"));
        ProcessCJ processCJ = extractModel(importFromBpmn(id, userId));
        saveElements(processCJ, id, cj, userId);
        cj.setBpmn(true);
        cjRepository.save(cj);
    }

    public void importFromBpmnUpdate(Long id, Long userId) {
        CJ cj = cjRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new NotFoundException("Сj id " + id + " does not exist"));
        ProcessCJ processCJ = extractModel(importFromBpmn(id, userId));
        saveOrUpdateElements(processCJ, id, cj, userId);
        cj.setBpmn(true);
        cjRepository.save(cj);
    }

    /**
     * Same import pipeline as {@link #importFromBpmnCreate}/{@link #importFromBpmnUpdate}, but
     * takes the already-parsed {@link ProcessCJ} model directly instead of a BPMN file fetched
     * from document-service. {@code extractModel} only exists to turn a BPMN XML document into
     * this model in the first place — a caller that already has the data in its own shape (e.g.
     * the staging pipeline, building it straight from its canonical model) can skip generating
     * and re-parsing BPMN XML entirely and just post the model. Dispatches to create vs. update
     * based on whether this CJ has already been bpmn-imported, mirroring the two REST entry
     * points above.
     */
    public void importFromModel(Long id, ProcessCJ processCJ, Long userId) {
        CJ cj = cjRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new NotFoundException("Сj id " + id + " does not exist"));
        sortModel(processCJ);
        if (Boolean.TRUE.equals(cj.getBpmn())) {
            saveOrUpdateElements(processCJ, id, cj, userId);
        } else {
            saveElements(processCJ, id, cj, userId);
        }
        cj.setBpmn(true);
        cjRepository.save(cj);
    }

    public byte[] importFromBpmn(Long id, Long userId) {
        List<DocumentationTypeDTO> documentationTypeDTO = documentClient.getDocumentationType("CJ");
        ResponseEntity<byte[]> document = documentClient.getDocument(id, documentationTypeDTO.get(0).getId(), userId);
        checkFileExtension(document);
        return document.getBody();
    }

    private void checkFileExtension(ResponseEntity<byte[]> document) {

        String header = document.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION);

        if (header == null) {
            throw new BadRequestException("Missing Content-Disposition");
        }

        ContentDisposition disposition = ContentDisposition.parse(header);

        String filename = disposition.getFilename();

        if (filename != null &&
                filename.toLowerCase().endsWith(".bpmn")) {
            return;
        }

        throw new BadRequestException("File extension is not .bpmn");
    }

    public ProcessCJ extractModel(byte[] content) {
        Element processElement = prepareExtract(content);
        ProcessCJ processCJ = new ProcessCJ();
        processCJ.id = processElement.getAttribute("id");

        List<SequenceFlow> processSequenceFlows = extractSequenceFlowsFromElement(processElement);
        List<Element> topLevelSubProcesses = filterChildren(processElement);
        processCJ.collapsedSubProcesses = new ArrayList<>();
        for (Element topLevelSubProcess : topLevelSubProcesses) {
            CollapsedSubProcess stage = new CollapsedSubProcess();
            stage.id = topLevelSubProcess.getAttribute("id");
            stage.name = topLevelSubProcess.getAttribute("name");
            findBiElements(topLevelSubProcess, stage.biElements);
            processCJ.collapsedSubProcesses.add(stage);
        }
        applyOrder(processCJ.getCollapsedSubProcesses(), processSequenceFlows, stage -> stage.id, this::assignOrderToStage);
        processCJ.getCollapsedSubProcesses().sort(Comparator.comparing(
                CollapsedSubProcess::sortKey,
                Comparator.nullsLast(BpmnOrderUtils.comparator())));
        return processCJ;
    }

    private CJStep updateCjStep(CJStep cjStep, String name, Integer order, String orderTree) {
        log.info("найден cj step с именем: " + cjStep.getName());
        if (!Objects.equals(cjStep.getName(), name)) {
            cjStep.setName(name);
            log.info("Обновление cj step name: {}", name);
        }
        if (!Objects.equals(cjStep.getOrder(), order)) {
            cjStep.setOrder(order);
        }
        if (!Objects.equals(cjStep.getOrderTree(), orderTree)) {
            cjStep.setOrderTree(orderTree);
        }
        cjStepRepository.save(cjStep);
        log.info("Сохранение обновленого cj step");
        return cjStep;
    }

    private void cleanCjSteps(ProcessCJ processCJ, long id) {
        List<String> stageIds = new ArrayList<>();
        for (int stageIter = 0; stageIter < processCJ.getCollapsedSubProcesses().size(); stageIter++) {
            CollapsedSubProcess stage = processCJ.getCollapsedSubProcesses().get(stageIter);
            stageIds.add(stage.id);
        }
        if (!stageIds.isEmpty()) {
            cjStepRepository.deleteByCjIdAndIdBpmnNotIn(id, stageIds);
        } else {
            cjStepRepository.deleteByCjId(id);
        }
    }

    private CJStep saveCjStep(CollapsedSubProcess stage, long id) {
        log.info("Создание нового cj step с name: {}", stage.name);
        return cjStepRepository.save(CJStep.builder()
                .order(stage.order)
                .orderTree(stage.sortKey())
                .name(stage.name)
                .cjId(id)
                .idBpmn(stage.getId())
                .build());
    }

    private BIInCJStep saveBIInCJStep(CJStep cjStep, BI biOptional, Integer order, String orderTree) {
        return biInCJStepRepository.save(BIInCJStep.builder()
                .cjStepId(cjStep.getId())
                .buisnessIteraction(biOptional)
                .order(order != null ? order.longValue() : null)
                .orderTree(orderTree)
                .build());
    }

    private void saveElements(ProcessCJ processCJ, long id, CJ cj, Long userId) {
        List<BiStepTypeEnum> biStepTypeEnums = biStepTypeEnumRepository.findAll();
        for (CollapsedSubProcess stage : processCJ.getCollapsedSubProcesses()) {
            CJStep cjStep = cjStepRepository.findFirstByCjIdAndIdBpmn(id, stage.id);
            cjStep = cjStep != null ? cjStep : saveCjStep(stage, id);
            log.info("name = " + cjStep.getName());
            for (BIElement bi : stage.getBiElements()) {
                BI biOptional = null;
                if ("callActivity".equals(bi.type)) {
                    biOptional = biRepository.findByUniqueIdentAndDeletedDateIsNull(bi.getProcessId());
                    if (biOptional != null) {
                        log.info("add biInCJStep cjStep.getId() = " + cjStep.getId());
                        BIInCJStep biInCJStep = biInCJStepRepository.findByCjStepIdAndBiId(cjStep.getId(), biOptional.getId());
                        biInCJStep = biInCJStep != null ? biInCJStep : saveBIInCJStep(cjStep, biOptional, bi.order, bi.sortKey());
                    }
                }
                if ("subProcess".equals(bi.type)) {
                    biOptional = biRepository.findByIdBpmnAndDeletedDateIsNull(bi.getId());
                    if (biOptional == null) {
                        biOptional = saveSubProcess(bi, cj, userId);
                    }
                    log.info("add biInCJStep cjStep.getId() = " + cjStep.getId());
                    BIInCJStep biInCJStep = biInCJStepRepository.findByCjStepIdAndBiId(cjStep.getId(), biOptional.getId());
                    biInCJStep = biInCJStep != null ? biInCJStep : saveBIInCJStep(cjStep, biOptional, bi.order, bi.sortKey());
                }
                stepProcessPost(bi, biOptional, biStepTypeEnums);
            }
        }
    }

    private void stepProcessPost(BIElement bi, BI biOptional, List<BiStepTypeEnum> biStepTypeEnums) {
        for (BiStep step : bi.getBiSteps()) {
            Optional<BiStepTypeEnum> biStepTypeEnum = biStepTypeEnums.stream()
                    .filter(stepTypeEnum -> stepTypeEnum.getName().equalsIgnoreCase(step.getType()))
                    .findFirst();
            if (biStepTypeEnum.isPresent()) {
                Optional<ru.beeline.cxbackend.domain.bi.BiStep> stepOptional = biStepRepository.findByBiAndBpmnIdAndStepType(
                        biOptional,
                        step.getId(),
                        biStepTypeEnum.get());
                if (stepOptional.isEmpty()) {
                    log.info("add STEP name = " + step.getName());
                    ru.beeline.cxbackend.domain.bi.BiStep biStep = biStepRepository.saveAndFlush(ru.beeline.cxbackend.domain.bi.BiStep.builder()
                            .name(step.getName())
                            .bi(biOptional)
                            .uniqueIdent("temp")
                            .stepType(biStepTypeEnum.get())
                            .bpmnId(step.getId())
                            .orderTree(step.sortKey())
                            .build());
                    biStep.setUniqueIdent(Utils.createUniqueIdent("Step", biStep.getId().longValue()));
                    biStepRepository.saveAndFlush(biStep);
                }
            }
        }
    }

    private void saveOrUpdateElements(ProcessCJ processCJ, long id, CJ cj, Long userId) {
        log.info("start method saveOrUpdateElements");
        List<BiStepTypeEnum> biStepTypeEnums = biStepTypeEnumRepository.findAll();
        cleanCjSteps(processCJ, id);
        for (CollapsedSubProcess stage : processCJ.getCollapsedSubProcesses()) {
            log.info("Обработка collapsedSubProcesses: {}", stage.name);
            CJStep cjStep = cjStepRepository.findFirstByCjIdAndIdBpmn(id, stage.id);
            cjStep = cjStep != null ? updateCjStep(cjStep, stage.name, stage.order, stage.sortKey()) : saveCjStep(stage, id);
            log.info("cjStep id = {}", cjStep.getId());
            List<BIInCJStep> biInCJStepList = biInCJStepRepository.findAllByCjStepId(cjStep.getId());
            log.info("BIInCJStepList size = {}", biInCJStepList.size());
            Map<Long, BIInCJStep> biInCJStepMap = biInCJStepList.stream().collect(Collectors.toMap(
                    BIInCJStep::getBiId,
                    biInCJStep -> biInCJStep,
                    (existing, replacement) -> existing
            ));
            log.info("Количество BiElements в CollapsedSubProcess = {}", stage.getBiElements().size());
            for (BIElement bi : stage.getBiElements()) {
                log.info("Создание, обновление bi , cj: {}", stage.name);
                BI biOptional = null;
                if ("callActivity".equals(bi.type)) {
                    log.info("bi.type: callActivity");
                    if (bi.getProcessId() != null && !bi.getProcessId().isEmpty()) {
                        biOptional = biRepository.findByUniqueIdentAndDeletedDateIsNull(bi.getProcessId());
                        if (biOptional != null) {
                            callActivityProcess(biOptional, cjStep, bi.order, bi.sortKey(), biInCJStepMap);
                        }
                    }
                    continue;
                } else if ("subProcess".equals(bi.type)) {
                    log.info("bi.type: subProcess. Поиск bi с id: {}", bi.getId());
                    biOptional = biRepository.findByIdBpmnAndDeletedDateIsNull(bi.getId());
                    log.info("Найден bi с bpmnId: {}", bi.getId());
                    if (biOptional == null) {
                        log.info("bi не найден сохранение нового bi");
                        biOptional = saveSubProcess(bi, cj, userId);
                    } else {
                        updateBi(biOptional, bi);
                    }
                    log.info("add biInCJStep cjStep.getId() = " + cjStep.getId());
                    BIInCJStep biInCJStep = biInCJStepMap.get(biOptional.getId());
                    biInCJStepMap.remove(biOptional.getId());
                    biInCJStep = biInCJStep != null ? updateBiInCjStepOrder(biInCJStep, bi.order, bi.sortKey())
                            : saveBIInCJStep(cjStep, biOptional, bi.order, bi.sortKey());
                } else {
                    log.info("Unknown bi.type: {}", bi.type);
                    continue;
                }
                List<ru.beeline.cxbackend.domain.bi.BiStep> allBiSteps = biStepRepository.findByBi(biOptional);
                List<ru.beeline.cxbackend.domain.bi.BiStep> biStepIsPresent = new ArrayList<>();
                stepProcess(bi, biOptional, biStepTypeEnums, biStepIsPresent);
                Set<Integer> idsToRemove = biStepIsPresent.stream()
                        .map(ru.beeline.cxbackend.domain.bi.BiStep::getId)
                        .collect(Collectors.toSet());
                allBiSteps.removeIf(item -> idsToRemove.contains(item.getId()));
                log.info("delete BiSteps");
                if (!allBiSteps.isEmpty()) {
                    biStepRelationRepository.deleteAllByBiStepIn(allBiSteps);
                }
                biStepRepository.deleteAll(allBiSteps);
            }
            biInCJStepRepository.deleteAll(biInCJStepMap.values());
        }
    }

    private void updateBi(BI biOptional, BIElement bi) {
        if (!biOptional.getName().equals(bi.name)) {
            log.info("bi найден, Обновляем bi");
            biOptional.setName(bi.name);
            biOptional.setLastModifiedDate(new java.sql.Date((new Date()).getTime()));
            biRepository.save(biOptional);
        }
    }

    private BIInCJStep updateBiInCjStepOrder(BIInCJStep biInCJStep, Integer order, String orderTree) {
        if (!Objects.equals(biInCJStep.getOrder(), order != null ? order.longValue() : null)) {
            biInCJStep.setOrder(order != null ? order.longValue() : null);
        }
        if (!Objects.equals(biInCJStep.getOrderTree(), orderTree)) {
            biInCJStep.setOrderTree(orderTree);
        }
        return biInCJStepRepository.save(biInCJStep);
    }

    private void callActivityProcess(BI biOptional, CJStep cjStep, Integer order, String orderTree,
                                     Map<Long, BIInCJStep> biInCJStepMap) {
        log.info("add biInCJStep cjStep.getId() = " + cjStep.getId());
        BIInCJStep biInCJStep = biInCJStepMap.get(biOptional.getId());
        biInCJStepMap.remove(biOptional.getId());
        if (biInCJStep != null) {
            biInCJStep.setOrder(order != null ? order.longValue() : null);
            biInCJStep.setOrderTree(orderTree);
            biInCJStepRepository.save(biInCJStep);
        } else {
            saveBIInCJStep(cjStep, biOptional, order, orderTree);
        }
    }

    private BI saveSubProcess(BIElement bi, CJ cj, Long userId) {
        BI biOptional = biRepository.save(BI.builder()
                .name(bi.name)
                .lastModifiedDate(new java.sql.Date((new Date()).getTime()))
                .createdDate(new java.sql.Date((new Date()).getTime()))
                .uniqueIdent("1")
                .authorId(userId)
                .status(bIStatusRepository.findById(2L).get())
                .productId(cj.getIdProductExt())
                .idBpmn(bi.getId())
                .build());
        log.info("add BI name = " + biOptional.getName());
        biOptional.setUniqueIdent(Utils.createUniqueIdent("BI", biOptional.getId()));
        biOptional = biRepository.save(biOptional);
        return biOptional;
    }

    private void stepProcess(BIElement bi, BI biOptional, List<BiStepTypeEnum> biStepTypeEnums,
                             List<ru.beeline.cxbackend.domain.bi.BiStep> biStepIsPresent) {
        log.info("start step process method");
        for (BiStep step : bi.getBiSteps()) {
            Optional<BiStepTypeEnum> biStepTypeEnum = biStepTypeEnums.stream()
                    .filter(stepTypeEnum -> stepTypeEnum.getName().equalsIgnoreCase(step.getType()))
                    .findFirst();
            if (biStepTypeEnum.isPresent()) {
                Optional<ru.beeline.cxbackend.domain.bi.BiStep> stepOptional = biStepRepository.findByBiAndBpmnIdAndStepType(
                        biOptional, step.getId(), biStepTypeEnum.get());
                if (stepOptional.isEmpty()) {
                    log.info("add STEP name = " + step.getName());
                    ru.beeline.cxbackend.domain.bi.BiStep biStep =
                            biStepRepository.saveAndFlush(                            ru.beeline.cxbackend.domain.bi.BiStep.builder()
                            .name(step.getName())
                            .bi(biOptional)
                            .stepType(biStepTypeEnum.get())
                            .uniqueIdent("temp")
                            .bpmnId(step.getId())
                            .orderTree(step.sortKey())
                            .build());
                    biStep.setUniqueIdent(Utils.createUniqueIdent("Step", biStep.getId().longValue()));
                    biStepRepository.saveAndFlush(biStep);
                } else {
                    log.info("Обновляем bi step");
                    ru.beeline.cxbackend.domain.bi.BiStep biStep = stepOptional.get();
                    if (!biStep.getName().equals(step.getName())) {
                        biStep.setName(step.getName());
                    }
                    if (!Objects.equals(biStep.getOrderTree(), step.sortKey())) {
                        biStep.setOrderTree(step.sortKey());
                    }
                    biStepRepository.save(biStep);
                    biStepIsPresent.add(stepOptional.get());
                }
            } else {
                log.info("bi step type: {} не соотвествует списку допустимых типов", step.getType() != null ? step.getType() : "null");
            }
        }
        log.info("step process method complete");
    }

    private void findBiElements(Element parent, List<BIElement> biElements) {
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            Node node = parent.getChildNodes().item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element el = (Element) node;
            String localName = el.getLocalName();
            if ("subProcess".equals(localName) || "callActivity".equals(localName)) {
                String name = el.getAttribute("name");
                if (name != null && name.startsWith("BI")) {
                    BIElement bi = new BIElement();
                    bi.type = localName;
                    bi.id = el.getAttribute("id");
                    bi.name = name;
                    if ("callActivity".equals(bi.type)) {
                        NodeList extElements = el.getElementsByTagNameNS("*", "calledElement");
                        if (extElements.getLength() > 0) {
                            Element calledElement = (Element) extElements.item(0);
                            bi.processId = calledElement.getAttribute("processId");
                            BI biOptional = biRepository.findByUniqueIdentAndDeletedDateIsNull(bi.getProcessId());
                            if (biOptional == null) {
                                throw new BadRequestException("unique_ident is " + bi.getProcessId() + " not found");
                            }

                        }
                    }
                    findBiSteps(el, bi.biSteps);
                    biElements.add(bi);
                }
            }
        }
        applyOrder(biElements, extractSequenceFlowsFromElement(parent), bi -> bi.id, this::assignOrderToBi);
        biElements.sort(Comparator.comparing(BIElement::sortKey, Comparator.nullsLast(BpmnOrderUtils.comparator())));
    }

    private void findBiSteps(Element parent, List<BiStep> steps) {
        collectBiSteps(parent, steps);
        applyOrder(steps, extractSequenceFlowsFromElement(parent), step -> step.id, this::assignOrderToBiStep);
        steps.sort(Comparator.comparing(BiStep::sortKey, Comparator.nullsLast(BpmnOrderUtils.comparator())));
    }

    private void collectBiSteps(Element parent, List<BiStep> steps) {
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            Node node = parent.getChildNodes().item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element el = (Element) node;
            String localName = el.getLocalName();

            if ("subProcess".equals(localName) || "task".equals(localName) || "serviceTask".equals(localName) || "userTask".equals(
                    localName)) {

                steps.add(BiStep.builder()
                        .type(localName)
                        .id(el.getAttribute("id"))
                        .name(el.getAttribute("name"))
                        .build());

                if ("subProcess".equals(localName)) {
                    collectBiSteps(el, steps);
                }
            }
        }
    }

    private List<SequenceFlow> extractSequenceFlowsFromElement(Element parent) {
        List<SequenceFlow> sequenceFlows = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element el = (Element) node;
            if ("sequenceFlow".equals(el.getLocalName())) {
                String id = el.getAttribute("id");
                String sourceRef = el.getAttribute("sourceRef");
                String targetRef = el.getAttribute("targetRef");
                if (id != null && sourceRef != null && targetRef != null) {
                    sequenceFlows.add(new SequenceFlow(id, sourceRef, targetRef));
                }
            }
        }
        return sequenceFlows;
    }

    private void assignOrderToStage(CollapsedSubProcess stage, BpmnOrderAssignment assignment) {
        stage.order = assignment.getOrder();
        stage.orderTree = assignment.getOrderTree();
    }

    private void assignOrderToBi(BIElement bi, BpmnOrderAssignment assignment) {
        bi.order = assignment.getOrder();
        bi.orderTree = assignment.getOrderTree();
    }

    private void assignOrderToBiStep(BiStep step, BpmnOrderAssignment assignment) {
        step.order = assignment.getOrder();
        step.orderTree = assignment.getOrderTree();
    }

    private <T> void applyOrder(List<T> elements,
                                List<SequenceFlow> sequenceFlows,
                                Function<T, String> getIdFunc,
                                java.util.function.BiConsumer<T, BpmnOrderAssignment> setOrderFunc) {
        if (elements == null || elements.isEmpty()) {
            return;
        }
        List<String> elementIds = elements.stream().map(getIdFunc).collect(Collectors.toList());
        Map<String, BpmnOrderAssignment> orders = BpmnOrderCalculator.calculateOrder(elementIds, sequenceFlows);
        for (T element : elements) {
            setOrderFunc.accept(element, orders.get(getIdFunc.apply(element)));
        }
    }

    private static Element prepareExtract(byte[] content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            Element definitions = builder.parse(new ByteArrayInputStream(content)).getDocumentElement();
            NodeList processList = definitions.getElementsByTagNameNS("*", "process");
            if (processList.getLength() == 0) {
                throw new IllegalArgumentException("No bpmn:process element found in BPMN XML");
            }
            if (processList.getLength() > 1) {
                throw new BadRequestException("BPMN XML should contain exactly one process element");
            }
            Element processElement = (Element) processList.item(0);
            return processElement;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new IllegalArgumentException(e.getMessage());
        }

    }


    private static List<Element> filterChildren(Element processElement) {
        List<Element> topLevelSubProcesses = new ArrayList<>();
        NodeList children = processElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                if ("subProcess".equals(el.getLocalName())) {
                    topLevelSubProcesses.add(el);
                }
            }
        }
        return topLevelSubProcesses;
    }

}
