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
import ru.beeline.cxbackend.controller.RequestContext;
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
import ru.beeline.cxbackend.utils.BpmnOrderCalculator;
import ru.beeline.cxbackend.utils.Utils;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.*;
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

    public void importFromBpmnCreate(Long id) {
        CJ cj = cjRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new NotFoundException("Сj id " + id + " does not exist"));
        ProcessCJ processCJ = extractModel(importFromBpmn(id));
        saveElements(processCJ, id, cj);
        cj.setBpmn(true);
        cjRepository.save(cj);
    }

    public void importFromBpmnUpdate(Long id) {
        CJ cj = cjRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new NotFoundException("Сj id " + id + " does not exist"));
        ProcessCJ processCJ = extractModel(importFromBpmn(id));
        saveOrUpdateElements(processCJ, id, cj);
        cj.setBpmn(true);
        cjRepository.save(cj);
    }

    public byte[] importFromBpmn(Long id) {
        List<DocumentationTypeDTO> documentationTypeDTO = documentClient.getDocumentationType("CJ");
        ResponseEntity<byte[]> document = documentClient.getDocument(id, documentationTypeDTO.get(0).getId());
        checkFileExtension(document);
        return document.getBody();
    }

    private void checkFileExtension(ResponseEntity<byte[]> document) {
        String header = document.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (header == null) {
            throw new BadRequestException("Missing Content-Disposition");
        }
        ContentDisposition disposition = ContentDisposition.parse(header);
        String filename = disposition.getFilename();
        if (filename != null && filename.toLowerCase().endsWith(".bpmn")) {
            return;
        }
        throw new BadRequestException("File extension is not .bpmn");
    }

    public ProcessCJ extractModel(byte[] content) {
        Element processElement = prepareExtract(content);
        ProcessCJ processCJ = new ProcessCJ();
        processCJ.id = processElement.getAttribute("id");

        processCJ.sequenceFlows = extractDirectSequenceFlows(processElement);

        processCJ.collapsedSubProcesses = new ArrayList<>();
        for (Element stageEl : filterDirectSubProcessChildren(processElement)) {
            CollapsedSubProcess stage = new CollapsedSubProcess();
            stage.id = stageEl.getAttribute("id");
            stage.name = stageEl.getAttribute("name");
            stage.sequenceFlows = extractDirectSequenceFlows(stageEl);
            findBiElements(stageEl, stage.biElements);
            processCJ.collapsedSubProcesses.add(stage);
        }

        sortModel(processCJ);
        return processCJ;
    }

    private void findBiElements(Element stageEl, List<BIElement> biElements) {
        for (int i = 0; i < stageEl.getChildNodes().getLength(); i++) {
            Node node = stageEl.getChildNodes().item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element el = (Element) node;
            String localName = el.getLocalName();

            if (!"subProcess".equals(localName) && !"callActivity".equals(localName)) continue;

            String name = el.getAttribute("name");
            if (name == null || !name.startsWith("BI")) continue;

            BIElement bi = new BIElement();
            bi.type = localName;
            bi.id = el.getAttribute("id");
            bi.name = name;

            if ("callActivity".equals(localName)) {
                NodeList calledEls = el.getElementsByTagNameNS("*", "calledElement");
                if (calledEls.getLength() > 0) {
                    Element calledElement = (Element) calledEls.item(0);
                    bi.processId = calledElement.getAttribute("processId");
                    BI existing = biRepository.findByUniqueIdentAndDeletedDateIsNull(bi.processId);
                    if (existing == null) {
                        throw new BadRequestException("unique_ident is " + bi.processId + " not found");
                    }
                }
            }

            bi.sequenceFlows = extractDirectSequenceFlows(el);

            findBiSteps(el, bi.biSteps);

            biElements.add(bi);
        }
    }

    private void findBiSteps(Element parent, List<BiStep> steps) {
        List<BiStep> directChildren = new ArrayList<>();
        Map<String, Element> nestedSubProcesses = new LinkedHashMap<>();

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element el = (Element) node;
            String localName = el.getLocalName();

            if ("subProcess".equals(localName) || "task".equals(localName)
                    || "serviceTask".equals(localName) || "userTask".equals(localName)) {

                BiStep step = BiStep.builder()
                        .type(localName)
                        .id(el.getAttribute("id"))
                        .name(el.getAttribute("name"))
                        .build();
                directChildren.add(step);

                if ("subProcess".equals(localName)) {
                    nestedSubProcesses.put(step.id, el);
                }
            }
        }

        if (directChildren.isEmpty()) return;

        List<String> ids = directChildren.stream().map(s -> s.id).collect(Collectors.toList());
        List<SequenceFlow> directFlows = extractDirectSequenceFlows(parent);
        Map<String, BigDecimal> orderMap = BpmnOrderCalculator.computeOrders(ids, directFlows);

        for (BiStep step : directChildren) {
            step.order = orderMap.getOrDefault(step.id, BigDecimal.ONE);
        }
        directChildren.sort(Comparator.comparing(s -> s.order));

        for (BiStep step : directChildren) {
            steps.add(step);
            if (nestedSubProcesses.containsKey(step.id)) {
                findBiSteps(nestedSubProcesses.get(step.id), steps);
            }
        }
    }

    private void sortModel(ProcessCJ processCJ) {
        List<CollapsedSubProcess> stages = processCJ.getCollapsedSubProcesses();

        List<String> stageIds = stages.stream().map(s -> s.id).collect(Collectors.toList());
        Map<String, BigDecimal> stageOrders = BpmnOrderCalculator.computeOrders(stageIds, processCJ.sequenceFlows);
        for (CollapsedSubProcess stage : stages) {
            stage.order = stageOrders.getOrDefault(stage.id, BigDecimal.ONE);
        }
        stages.sort(Comparator.comparing(s -> s.order));
        processCJ.setCollapsedSubProcesses(stages);

        for (CollapsedSubProcess stage : processCJ.getCollapsedSubProcesses()) {
            List<BIElement> biElements = stage.getBiElements();
            List<String> biIds = biElements.stream().map(b -> b.id).collect(Collectors.toList());
            Map<String, BigDecimal> biOrders = BpmnOrderCalculator.computeOrders(biIds, stage.sequenceFlows);
            for (BIElement bi : biElements) {
                bi.order = biOrders.getOrDefault(bi.id, BigDecimal.ONE);
            }
            biElements.sort(Comparator.comparing(b -> b.order));
        }
    }

    private void saveElements(ProcessCJ processCJ, long id, CJ cj) {
        List<BiStepTypeEnum> biStepTypeEnums = biStepTypeEnumRepository.findAll();
        for (CollapsedSubProcess stage : processCJ.getCollapsedSubProcesses()) {
            CJStep cjStep = cjStepRepository.findFirstByCjIdAndIdBpmn(id, stage.id);
            cjStep = cjStep != null ? cjStep : saveCjStep(stage.getOrder(), stage, id);
            log.info("name = {}", cjStep.getName());

            for (BIElement bi : stage.getBiElements()) {
                BI biOptional = null;

                if ("callActivity".equals(bi.type)) {
                    biOptional = biRepository.findByUniqueIdentAndDeletedDateIsNull(bi.getProcessId());
                    if (biOptional != null) {
                        log.info("add biInCJStep cjStep.getId() = {}", cjStep.getId());
                        BIInCJStep existing = biInCJStepRepository.findByCjStepIdAndBiId(cjStep.getId(), biOptional.getId());
                        if (existing == null) {
                            saveBIInCJStep(cjStep, biOptional, bi.getOrder());
                        }
                    }
                } else if ("subProcess".equals(bi.type)) {
                    biOptional = biRepository.findByIdBpmnAndDeletedDateIsNull(bi.getId());
                    if (biOptional == null) {
                        biOptional = saveSubProcess(bi, cj);
                    }
                    log.info("add biInCJStep cjStep.getId() = {}", cjStep.getId());
                    BIInCJStep existing = biInCJStepRepository.findByCjStepIdAndBiId(cjStep.getId(), biOptional.getId());
                    if (existing == null) {
                        saveBIInCJStep(cjStep, biOptional, bi.getOrder());
                    }
                }

                stepProcessPost(bi, biOptional, biStepTypeEnums);
            }
        }
    }

    private void stepProcessPost(BIElement bi, BI biOptional, List<BiStepTypeEnum> biStepTypeEnums) {
        if (biOptional == null) return;
        for (BiStep step : bi.getBiSteps()) {
            Optional<BiStepTypeEnum> typeOpt = biStepTypeEnums.stream()
                    .filter(t -> t.getName().equalsIgnoreCase(step.getType()))
                    .findFirst();
            if (typeOpt.isEmpty()) continue;

            Optional<ru.beeline.cxbackend.domain.bi.BiStep> existing =
                    biStepRepository.findByBiAndBpmnIdAndStepType(biOptional, step.getId(), typeOpt.get());
            if (existing.isEmpty()) {
                log.info("add STEP name = {}", step.getName());
                ru.beeline.cxbackend.domain.bi.BiStep saved =
                        biStepRepository.saveAndFlush(ru.beeline.cxbackend.domain.bi.BiStep.builder()
                                .name(step.getName())
                                .bi(biOptional)
                                .uniqueIdent("temp")
                                .stepType(typeOpt.get())
                                .bpmnId(step.getId())
                                .order(step.getOrder())
                                .build());
                saved.setUniqueIdent(Utils.createUniqueIdent("Step", saved.getId().longValue()));
                biStepRepository.saveAndFlush(saved);
            }
        }
    }

    private void saveOrUpdateElements(ProcessCJ processCJ, long id, CJ cj) {
        log.info("start method saveOrUpdateElements");
        List<BiStepTypeEnum> biStepTypeEnums = biStepTypeEnumRepository.findAll();
        cleanCjSteps(processCJ, id);

        for (CollapsedSubProcess stage : processCJ.getCollapsedSubProcesses()) {
            log.info("Обработка collapsedSubProcesses: {}", stage.name);
            CJStep cjStep = cjStepRepository.findFirstByCjIdAndIdBpmn(id, stage.id);
            cjStep = cjStep != null
                    ? updateCjStep(cjStep, stage.name, stage.getOrder())
                    : saveCjStep(stage.getOrder(), stage, id);
            log.info("cjStep id = {}", cjStep.getId());

            List<BIInCJStep> existingBiInCjSteps = biInCJStepRepository.findAllByCjStepId(cjStep.getId());
            Map<Long, BIInCJStep> biInCJStepMap = existingBiInCjSteps.stream()
                    .collect(Collectors.toMap(BIInCJStep::getBiId, s -> s, (a, b) -> a));
            log.info("Количество BiElements в CollapsedSubProcess = {}", stage.getBiElements().size());

            for (BIElement bi : stage.getBiElements()) {
                log.info("Создание, обновление bi, cj: {}", stage.name);
                BI biOptional = null;

                if ("callActivity".equals(bi.type)) {
                    log.info("bi.type: callActivity");
                    biOptional = biRepository.findByUniqueIdentAndDeletedDateIsNull(bi.getProcessId());
                    if (biOptional != null) {
                        callActivityProcess(biOptional, cjStep, bi.getOrder(), biInCJStepMap);
                    } else {
                        continue;
                    }
                } else if ("subProcess".equals(bi.type)) {
                    log.info("bi.type: subProcess. Поиск bi с id: {}", bi.getId());
                    biOptional = biRepository.findByIdBpmnAndDeletedDateIsNull(bi.getId());
                    if (biOptional == null) {
                        log.info("bi не найден, сохранение нового bi");
                        biOptional = saveSubProcess(bi, cj);
                    } else {
                        updateBi(biOptional, bi);
                    }
                    log.info("add biInCJStep cjStep.getId() = {}", cjStep.getId());
                    BIInCJStep biInCJStep = biInCJStepMap.remove(biOptional.getId());
                    if (biInCJStep != null) {
                        biInCJStep.setOrder(bi.getOrder());
                        biInCJStepRepository.save(biInCJStep);
                    } else {
                        saveBIInCJStep(cjStep, biOptional, bi.getOrder());
                    }
                } else {
                    log.info("Unknown bi.type: {}", bi.type);
                    continue;
                }

                List<ru.beeline.cxbackend.domain.bi.BiStep> allBiSteps = biStepRepository.findByBi(biOptional);
                List<ru.beeline.cxbackend.domain.bi.BiStep> presentSteps = new ArrayList<>();
                stepProcess(bi, biOptional, biStepTypeEnums, presentSteps);

                Set<Integer> presentIds = presentSteps.stream()
                        .map(ru.beeline.cxbackend.domain.bi.BiStep::getId)
                        .collect(Collectors.toSet());
                allBiSteps.removeIf(s -> presentIds.contains(s.getId()));

                log.info("delete BiSteps");
                if (!allBiSteps.isEmpty()) {
                    biStepRelationRepository.deleteAllByBiStepIn(allBiSteps);
                }
                biStepRepository.deleteAll(allBiSteps);
            }

            biInCJStepRepository.deleteAll(biInCJStepMap.values());
        }
    }

    private void stepProcess(BIElement bi, BI biOptional, List<BiStepTypeEnum> biStepTypeEnums,
                             List<ru.beeline.cxbackend.domain.bi.BiStep> biStepIsPresent) {
        log.info("start step process method");
        for (BiStep step : bi.getBiSteps()) {
            Optional<BiStepTypeEnum> typeOpt = biStepTypeEnums.stream()
                    .filter(t -> t.getName().equalsIgnoreCase(step.getType()))
                    .findFirst();
            if (typeOpt.isEmpty()) {
                log.info("bi step type: {} не соответствует списку допустимых типов",
                        step.getType() != null ? step.getType() : "null");
                continue;
            }

            Optional<ru.beeline.cxbackend.domain.bi.BiStep> stepOpt =
                    biStepRepository.findByBiAndBpmnIdAndStepType(biOptional, step.getId(), typeOpt.get());

            if (stepOpt.isEmpty()) {
                log.info("add STEP name = {}", step.getName());
                ru.beeline.cxbackend.domain.bi.BiStep saved =
                        biStepRepository.saveAndFlush(ru.beeline.cxbackend.domain.bi.BiStep.builder()
                                .name(step.getName())
                                .bi(biOptional)
                                .stepType(typeOpt.get())
                                .uniqueIdent("temp")
                                .bpmnId(step.getId())
                                .order(step.getOrder())
                                .build());
                saved.setUniqueIdent(Utils.createUniqueIdent("Step", saved.getId().longValue()));
                biStepRepository.saveAndFlush(saved);
                biStepIsPresent.add(saved);
            } else {
                log.info("Обновляем bi step");
                ru.beeline.cxbackend.domain.bi.BiStep existing = stepOpt.get();
                boolean changed = false;
                if (!existing.getName().equals(step.getName())) {
                    existing.setName(step.getName());
                    changed = true;
                }
                if (!Objects.equals(existing.getOrder(), step.getOrder())) {
                    existing.setOrder(step.getOrder());
                    changed = true;
                }
                if (changed) biStepRepository.save(existing);
                biStepIsPresent.add(existing);
            }
        }
        log.info("step process method complete");
    }

    private CJStep saveCjStep(BigDecimal order, CollapsedSubProcess stage, long id) {
        log.info("Создание нового cj step с name: {}", stage.name);
        return cjStepRepository.save(CJStep.builder()
                .order(order)
                .name(stage.name)
                .cjId(id)
                .idBpmn(stage.getId())
                .build());
    }

    private CJStep updateCjStep(CJStep cjStep, String name, BigDecimal order) {
        log.info("найден cj step с именем: {}", cjStep.getName());
        boolean changed = false;
        if (!Objects.equals(cjStep.getName(), name)) {
            cjStep.setName(name);
            log.info("Обновление cj step name: {}", name);
            changed = true;
        }
        if (!Objects.equals(cjStep.getOrder(), order)) {
            cjStep.setOrder(order);
            changed = true;
        }
        if (changed) cjStepRepository.save(cjStep);
        log.info("Сохранение обновлённого cj step");
        return cjStep;
    }

    private BIInCJStep saveBIInCJStep(CJStep cjStep, BI bi, BigDecimal order) {
        return biInCJStepRepository.save(BIInCJStep.builder()
                .cjStepId(cjStep.getId())
                .buisnessIteraction(bi)
                .order(order)
                .build());
    }

    private void callActivityProcess(BI biOptional, CJStep cjStep, BigDecimal order,
                                     Map<Long, BIInCJStep> biInCJStepMap) {
        log.info("add biInCJStep cjStep.getId() = {}", cjStep.getId());
        BIInCJStep biInCJStep = biInCJStepMap.remove(biOptional.getId());
        if (biInCJStep != null) {
            biInCJStep.setOrder(order);
            biInCJStepRepository.save(biInCJStep);
        } else {
            saveBIInCJStep(cjStep, biOptional, order);
        }
    }

    private void cleanCjSteps(ProcessCJ processCJ, long id) {
        List<String> stageIds = processCJ.getCollapsedSubProcesses().stream()
                .map(s -> s.id)
                .collect(Collectors.toList());
        if (!stageIds.isEmpty()) {
            cjStepRepository.deleteByCjIdAndIdBpmnNotIn(id, stageIds);
        } else {
            cjStepRepository.deleteByCjId(id);
        }
    }

    private void updateBi(BI biOptional, BIElement bi) {
        if (!biOptional.getName().equals(bi.name)) {
            log.info("bi найден, обновляем bi");
            biOptional.setName(bi.name);
            biOptional.setLastModifiedDate(new java.sql.Date(new Date().getTime()));
            biRepository.save(biOptional);
        }
    }

    private BI saveSubProcess(BIElement bi, CJ cj) {
        BI saved = biRepository.save(BI.builder()
                .name(bi.name)
                .lastModifiedDate(new java.sql.Date(new Date().getTime()))
                .createdDate(new java.sql.Date(new Date().getTime()))
                .uniqueIdent("1")
                .authorId(RequestContext.getUserId())
                .status(bIStatusRepository.findById(2L).get())
                .productId(cj.getIdProductExt())
                .idBpmn(bi.getId())
                .build());
        log.info("add BI name = {}", saved.getName());
        saved.setUniqueIdent(Utils.createUniqueIdent("BI", saved.getId()));
        return biRepository.save(saved);
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
            return (Element) processList.item(0);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private static List<SequenceFlow> extractDirectSequenceFlows(Element parent) {
        List<SequenceFlow> flows = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element el = (Element) node;
            if (!"sequenceFlow".equals(el.getLocalName())) continue;
            String id = el.getAttribute("id");
            String sourceRef = el.getAttribute("sourceRef");
            String targetRef = el.getAttribute("targetRef");
            if (id != null && !id.isEmpty() && sourceRef != null && targetRef != null) {
                flows.add(new SequenceFlow(id, sourceRef, targetRef));
            }
        }
        return flows;
    }

    private static List<Element> filterDirectSubProcessChildren(Element processElement) {
        List<Element> result = new ArrayList<>();
        NodeList children = processElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) node;
                if ("subProcess".equals(el.getLocalName())) {
                    result.add(el);
                }
            }
        }
        return result;
    }
}
