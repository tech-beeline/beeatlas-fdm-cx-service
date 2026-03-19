/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
        log.info("checkFileExtension");
        String contentDisposition = document.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);

        String filename = null;
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            int index = contentDisposition.indexOf("filename=");
            filename = contentDisposition.substring(index + 9).trim();
            if (filename.startsWith("\"") && filename.endsWith("\"") && filename.length() > 1) {
                filename = filename.substring(1, filename.length() - 1);
                if (filename != null && filename.toLowerCase().endsWith(".bpmn")) {
                    return;
                }
            }
        }
        throw new BadRequestException("File extension is not .bpmn");
    }

    public ProcessCJ extractModel(byte[] content) {
        Element processElement = prepareExtract(content);
        ProcessCJ processCJ = new ProcessCJ();
        processCJ.id = processElement.getAttribute("id");

        extractSequenceFlows(processElement, processCJ);
        List<Element> topLevelSubProcesses = filterChildren(processElement);
        processCJ.collapsedSubProcesses = new ArrayList<>();
        for (int i = 0; i < topLevelSubProcesses.size(); i++) {

            CollapsedSubProcess stage = new CollapsedSubProcess();
            stage.id = topLevelSubProcesses.get(i).getAttribute("id");
            stage.name = topLevelSubProcesses.get(i).getAttribute("name");
            findBiElements(topLevelSubProcesses.get(i), stage.biElements);
            processCJ.collapsedSubProcesses.add(stage);

        }
        sortModel(processCJ);
        return processCJ;
    }

    private CJStep updateCjStep(CJStep cjStep, String name, Integer order) {
        log.info("найден cj step с именем: " + cjStep.getName());
        if (!Objects.equals(cjStep.getName(), name)) {
            cjStep.setName(name);
            log.info("Обновление cj step name: {}", name);
        }
        if (!Objects.equals(cjStep.getOrder(), order)) {
            cjStep.setOrder(order);
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

    private CJStep saveCjStep(int stageIter, CollapsedSubProcess stage, long id) {
        log.info("Создание нового cj step с name: {}", stage.name);
        return cjStepRepository.save(CJStep.builder()
                .order(stageIter)
                .name(stage.name)
                .cjId(id)
                .idBpmn(stage.getId())
                .build());
    }

    private BIInCJStep saveBIInCJStep(CJStep cjStep, BI biOptional, Integer biIter) {
        return biInCJStepRepository.save(BIInCJStep.builder()
                .cjStepId(cjStep.getId())
                .buisnessIteraction(biOptional)
                .order(biIter.longValue())
                .build());
    }

    private void saveElements(ProcessCJ processCJ, long id, CJ cj) {
        List<BiStepTypeEnum> biStepTypeEnums = biStepTypeEnumRepository.findAll();
        for (int stageIter = 0; stageIter < processCJ.getCollapsedSubProcesses().size(); stageIter++) {
            CollapsedSubProcess stage = processCJ.getCollapsedSubProcesses().get(stageIter);
            CJStep cjStep = cjStepRepository.findFirstByCjIdAndIdBpmn(id, stage.id);
            cjStep = cjStep != null ? cjStep : saveCjStep(stageIter, stage, id);
            log.info("name = " + cjStep.getName());
            for (Integer biIter = 0; biIter < stage.getBiElements().size(); biIter++) {
                BIElement bi = stage.getBiElements().get(biIter);
                BI biOptional = null;
                if ("callActivity".equals(bi.type)) {
                    biOptional = biRepository.findByUniqueIdentAndDeletedDateIsNull(bi.getProcessId());
                    if (biOptional != null) {
                        log.info("add biInCJStep cjStep.getId() = " + cjStep.getId());
                        BIInCJStep biInCJStep = biInCJStepRepository.findByCjStepIdAndBiId(cjStep.getId(), biOptional.getId());
                        biInCJStep = biInCJStep != null ? biInCJStep : saveBIInCJStep(cjStep, biOptional, biIter);
                    }
                }
                if ("subProcess".equals(bi.type)) {
                    biOptional = biRepository.findByIdBpmnAndDeletedDateIsNull(bi.getId());
                    if (biOptional == null) {
                        biOptional = saveSubProcess(bi, cj);
                    }
                    log.info("add biInCJStep cjStep.getId() = " + cjStep.getId());
                    BIInCJStep biInCJStep = biInCJStepRepository.findByCjStepIdAndBiId(cjStep.getId(), biOptional.getId());
                    biInCJStep = biInCJStep != null ? biInCJStep : saveBIInCJStep(cjStep, biOptional, biIter);
                }
                stepProcessPost(bi, biOptional, biStepTypeEnums);
            }
        }
    }

    private void stepProcessPost(BIElement bi, BI biOptional, List<BiStepTypeEnum> biStepTypeEnums) {
        for (int stepsIter = 0; stepsIter < bi.getBiSteps().size(); stepsIter++) {
            BiStep step = bi.getBiSteps().get(stepsIter);
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
                    biStepRepository.save(ru.beeline.cxbackend.domain.bi.BiStep.builder()
                            .name(step.getName())
                            .bi(biOptional)
                            .stepType(biStepTypeEnum.get())
                            .bpmnId(step.getId())
                            .build());
                }
            }
        }
    }

    private void saveOrUpdateElements(ProcessCJ processCJ, long id, CJ cj) {
        log.info("start method saveOrUpdateElements");
        List<BiStepTypeEnum> biStepTypeEnums = biStepTypeEnumRepository.findAll();
        cleanCjSteps(processCJ, id);
        for (int stageIter = 0; stageIter < processCJ.getCollapsedSubProcesses().size(); stageIter++) {
            CollapsedSubProcess stage = processCJ.getCollapsedSubProcesses().get(stageIter);
            log.info("Обработка collapsedSubProcesses: {}", stage.name);
            CJStep cjStep = cjStepRepository.findFirstByCjIdAndIdBpmn(id, stage.id);
            cjStep = cjStep != null ? updateCjStep(cjStep, stage.name, stageIter) : saveCjStep(stageIter, stage, id);
            log.info("cjStep id = {}", cjStep.getId());
            List<BIInCJStep> biInCJStepList = biInCJStepRepository.findAllByCjStepId(cjStep.getId());
            log.info("BIInCJStepList size = {}", biInCJStepList.size());
            Map<Long, BIInCJStep> biInCJStepMap = biInCJStepList.stream().collect(Collectors.toMap(
                    BIInCJStep::getBiId,
                    biInCJStep -> biInCJStep,
                    (existing, replacement) -> existing
            ));
            log.info("Количество BiElements в CollapsedSubProcess = {}", stage.getBiElements().size());
            for (Integer biIter = 0; biIter < stage.getBiElements().size(); biIter++) {
                log.info("Создание, обновление bi , cj: {}", stage.name);
                BIElement bi = stage.getBiElements().get(biIter);
                BI biOptional = null;
                if ("callActivity".equals(bi.type)) {
                    log.info("bi.type: callActivity");
                    biOptional = biRepository.findByUniqueIdentAndDeletedDateIsNull(bi.getProcessId());
                    if (biOptional != null) {
                        callActivityProcess(biOptional, cjStep, biIter, biInCJStepMap);
                    } else {
                        continue;
                    }
                } else if ("subProcess".equals(bi.type)) {
                    log.info("bi.type: subProcess. Поиск bi с id: {}", bi.getId());
                    biOptional = biRepository.findByIdBpmnAndDeletedDateIsNull(bi.getId());
                    log.info("Найден bi с bpmnId: {}", bi.getId());
                    if (biOptional == null) {
                        log.info("bi не найден сохранение нового bi");
                        biOptional = saveSubProcess(bi, cj);
                    } else {
                        updateBi(biOptional, bi);
                    }
                    log.info("add biInCJStep cjStep.getId() = " + cjStep.getId());
                    BIInCJStep biInCJStep = biInCJStepMap.get(biOptional.getId());
                    biInCJStepMap.remove(biOptional.getId());
                    biInCJStep = biInCJStep != null ? biInCJStepRepository.save(biInCJStep)
                            : saveBIInCJStep(cjStep, biOptional, biIter);
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

    private void callActivityProcess(BI biOptional, CJStep cjStep, Integer biIter, Map<Long, BIInCJStep> biInCJStepMap) {
        log.info("add biInCJStep cjStep.getId() = " + cjStep.getId());
        BIInCJStep biInCJStep = biInCJStepMap.get(biOptional.getId());
        biInCJStepMap.remove(biOptional.getId());
        biInCJStep = biInCJStep != null ? biInCJStepRepository.save(biInCJStep)
                : saveBIInCJStep(cjStep, biOptional, biIter);
    }

    private BI saveSubProcess(BIElement bi, CJ cj) {
        BI biOptional = biRepository.save(BI.builder()
                .name(bi.name)
                .lastModifiedDate(new java.sql.Date((new Date()).getTime()))
                .createdDate(new java.sql.Date((new Date()).getTime()))
                .uniqueIdent("1")
                .authorId(RequestContext.getUserId())
                .status(bIStatusRepository.findById(2L).get())
                .productId(cj.getIdProductExt())
                .idBpmn(bi.getId())
                .build());
        log.info("add BI name = " + biOptional.getName());
        biOptional.setUniqueIdent(Utils.createUniqueIdent(biOptional.getId()));
        biOptional = biRepository.save(biOptional);
        return biOptional;
    }

    private void stepProcess(BIElement bi, BI biOptional, List<BiStepTypeEnum> biStepTypeEnums,
                             List<ru.beeline.cxbackend.domain.bi.BiStep> biStepIsPresent) {
        log.info("start step process method");
        for (int stepsIter = 0; stepsIter < bi.getBiSteps().size(); stepsIter++) {
            BiStep step = bi.getBiSteps().get(stepsIter);
            Optional<BiStepTypeEnum> biStepTypeEnum = biStepTypeEnums.stream()
                    .filter(stepTypeEnum -> stepTypeEnum.getName().equalsIgnoreCase(step.getType()))
                    .findFirst();
            if (biStepTypeEnum.isPresent()) {
                Optional<ru.beeline.cxbackend.domain.bi.BiStep> stepOptional = biStepRepository.findByBiAndBpmnIdAndStepType(
                        biOptional, step.getId(), biStepTypeEnum.get());
                if (stepOptional.isEmpty()) {
                    log.info("add STEP name = " + step.getName());
                    biStepRepository.save(ru.beeline.cxbackend.domain.bi.BiStep.builder()
                            .name(step.getName())
                            .bi(biOptional)
                            .stepType(biStepTypeEnum.get())
                            .bpmnId(step.getId())
                            .build());
                } else {
                    log.info("Обновляем bi step");
                    ru.beeline.cxbackend.domain.bi.BiStep biStep = stepOptional.get();
                    if (!biStep.getName().equals(step.getName())) {
                        biStep.setName(step.getName());
                        biStepRepository.save(biStep);
                    }
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
                    biElements.add(bi);

                    findBiSteps(el, bi.biSteps);
                }
            }
        }
    }

    private void findBiSteps(Element parent, List<BiStep> steps) {
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
                    findBiSteps(el, steps);
                }
            }
        }
    }

    private void extractSequenceFlows(Element processElement, ProcessCJ processCJ) {
        NodeList sequenceFlowNodes = processElement.getElementsByTagNameNS("*", "sequenceFlow");
        for (int i = 0; i < sequenceFlowNodes.getLength(); i++) {
            Element seqFlow = (Element) sequenceFlowNodes.item(i);
            String id = seqFlow.getAttribute("id");
            String sourceRef = seqFlow.getAttribute("sourceRef");
            String targetRef = seqFlow.getAttribute("targetRef");
            if (id != null && sourceRef != null && targetRef != null) {
                processCJ.sequenceFlows.add(new SequenceFlow(id, sourceRef, targetRef));
            }
        }
    }

    private void sortModel(ProcessCJ processCJ) {
        List<CollapsedSubProcess> stages = processCJ.getCollapsedSubProcesses();
        stages = sortBySequenceFlow(stages, processCJ.sequenceFlows, stage -> stage.id);
        processCJ.setCollapsedSubProcesses(stages);
    }

    private <T> List<T> sortBySequenceFlow(List<T> elements, List<SequenceFlow> sequenceFlows,
                                           Function<T, String> getIdFunc) {
        if (elements == null || elements.size() <= 1) {
            return elements;
        }
        Set<String> elementIds = new HashSet<>();
        for (T el : elements) {
            elementIds.add(getIdFunc.apply(el));
        }
        Map<String, String> sourceToTarget = new HashMap<>();
        Map<String, String> targetToSource = new HashMap<>();
        for (SequenceFlow sf : sequenceFlows) {
            if (elementIds.contains(sf.sourceRef) && elementIds.contains(sf.targetRef)) {
                sourceToTarget.put(sf.sourceRef, sf.targetRef);
                targetToSource.put(sf.targetRef, sf.sourceRef);
            }
        }
        String startId = null;
        for (String id : elementIds) {
            if (!targetToSource.containsKey(id)) {
                startId = id;
                break;
            }
        }
        if (startId == null) {
            return elements;
        }
        Map<String, T> idToElement = new HashMap<>();
        for (T el : elements) {
            idToElement.put(getIdFunc.apply(el), el);
        }
        List<T> sortedList = new ArrayList<>();
        String currentId = startId;
        while (currentId != null) {
            T elem = idToElement.get(currentId);
            if (elem == null)
                break;
            sortedList.add(elem);
            currentId = sourceToTarget.get(currentId);
        }
        return sortedList;
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
