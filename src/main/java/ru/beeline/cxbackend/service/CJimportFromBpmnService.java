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
    private BiStepTypeEnumRepository biStepTypeEnumRepository;

    @Autowired
    private BiStepRepository biStepRepository;

    @PostConstruct
    public void initModelMapperMapping() {
        modelMapper.typeMap(CJ.class, CJFullDtoV2.class).addMapping(CJ::getIdProductExt, CJFullDtoV2::setProductId);
    }

    public void importFromBpmn(Long id) {
        CJ cj = cjRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new NotFoundException("Сj id " + id + " does not exist"));
        List<DocumentationTypeDTO> documentationTypeDTO = documentClient.getDocumentationType("CJ");
        ResponseEntity<byte[]> document = documentClient.getDocument(id,documentationTypeDTO.get(0).getId());
        checkFileExtension(document);
        extractModel(document.getBody(), id, cj);
        cj.setBpmn(true);
        cjRepository.save(cj);
    }

    private void checkFileExtension(ResponseEntity<byte[]> document) {
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

    public void extractModel(byte[] content, Long id, CJ cj) {
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
        saveElements(processCJ, id, cj);
    }


    private void saveElements(ProcessCJ processCJ, long id, CJ cj) {
        List<BiStepTypeEnum> biStepTypeEnums = biStepTypeEnumRepository.findAll();
        for (int stageIter = 0; stageIter < processCJ.getCollapsedSubProcesses().size(); stageIter++) {
            CollapsedSubProcess stage = processCJ.getCollapsedSubProcesses().get(stageIter);

            CJStep cjStep = cjStepRepository.findFirstByCjIdAndIdBpmn(id, stage.id);
            cjStep = cjStep != null ? cjStep : cjStepRepository.save(CJStep.builder()
                    .order(stageIter)
                    .name(stage.name)
                    .cjId(id)
                    .idBpmn(stage.getId())
                    .build());
            log.info("name = " + cjStep.getName());
            for (Integer biIter = 0; biIter < stage.getBiElements().size(); biIter++) {
                BIElement bi = stage.getBiElements().get(biIter);
                BI biOptional = null;
                if ("callActivity".equals(bi.type)) {
                    biOptional = biRepository.findByUniqueIdentAndDeletedDateIsNull(bi.getProcessId());
                    if (biOptional != null) {
                        log.info("add biInCJStep cjStep.getId() = " + cjStep.getId());
                        BIInCJStep biInCJStep = biInCJStepRepository.findByCjStepIdAndBiId(cjStep.getId(), biOptional.getId());
                        biInCJStep = biInCJStep != null ? biInCJStep : biInCJStepRepository.save(BIInCJStep.builder()
                                .cjStepId(cjStep.getId())
                                .buisnessIteraction(biOptional)
                                .order(biIter.longValue())
                                .build());
                    }
                }
                if ("subProcess".equals(bi.type)) {
                    biOptional = biRepository.findByIdBpmnAndDeletedDateIsNull(bi.getId());
                    if (biOptional == null) {
                        biOptional = biRepository.save(BI.builder()
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

                    }
                    log.info("add biInCJStep cjStep.getId() = " + cjStep.getId());
                    BIInCJStep biInCJStep = biInCJStepRepository.findByCjStepIdAndBiId(cjStep.getId(), biOptional.getId());
                    biInCJStep = biInCJStep != null ? biInCJStep : biInCJStepRepository.save(BIInCJStep.builder()
                            .cjStepId(cjStep.getId())
                            .buisnessIteraction(biOptional)
                            .order(biIter.longValue())
                            .build());
                }
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
        }
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

        for (CollapsedSubProcess stage : stages) {
            stage.biElements = sortBySequenceFlow(stage.biElements, processCJ.sequenceFlows, bi -> bi.id);
            for (BIElement bi : stage.biElements) {
                bi.biSteps = sortBySequenceFlow(bi.biSteps, processCJ.sequenceFlows, step -> step.id);
            }
        }
        processCJ.setCollapsedSubProcesses(stages);

    }

    private <T> List<T> sortBySequenceFlow(List<T> elements,
                                           List<SequenceFlow> sequenceFlows,
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
