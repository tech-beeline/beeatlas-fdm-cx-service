/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.cxbackend.client.UserClient;
import ru.beeline.cxbackend.domain.bi.*;
import ru.beeline.cxbackend.domain.bi.ref.BIStatus;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.domain.cj.CJStep;
import ru.beeline.cxbackend.dto.*;
import ru.beeline.cxbackend.exception.ForbiddenException;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.exception.UnprocessedEntityException;
import ru.beeline.cxbackend.mapper.BIMapper;
import ru.beeline.cxbackend.repository.*;
import ru.beeline.cxbackend.utils.Utils;

import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

import static ru.beeline.cxbackend.controller.RequestContext.*;
import static ru.beeline.cxbackend.domain.Permission.PermissionType.DESIGN_ARTIFACT;
import static ru.beeline.cxbackend.utils.AccessToProduct.validateAccessProduct;
import static ru.beeline.cxbackend.utils.AccessToProduct.validateProductId;

@Slf4j
@Service
public class BusinessInteractionService {

    @Autowired
    private BusinessInteractionRepository businessInteractionRepository;

    @Autowired
    private BIInCJStepRepository biInCJStepRepository;

    @Autowired
    private CJStepRepository cjStepRepository;

    @Autowired
    private BIRelationsRepository biRelationsRepository;

    @Autowired
    private BIFeelingRepository biFeelingRepository;

    @Autowired
    private BIStatusRepository biStatusRepository;

    @Autowired
    private CJRepository cjRepository;

    @Autowired
    private BILinkRepository biLinkRepository;

    @Autowired
    private BIParticipantsRepository biParticipantsRepository;

    @Autowired
    private BIParticipantRepository biParticipantRepository;

    @Autowired
    private BIMapper biMapper;

    @Autowired
    private UserClient userClient;

    @Autowired
    private BiStepRepository biStepRepository;

    @Autowired
    BiStepRelationRepository biStepRelationRepository;

    @Autowired
    ModelMapper modelMapper;

    public List<BIDto> getBI(Long idProduct) {
        List<BI> biList = businessInteractionRepository
                .findAll(BiSpecification.hasProductId(idProduct));
        return biList.stream().map(biMapper::biToBIDto).collect(Collectors.toList());
    }

    public List<BIDto> getBIByFilter(String text, Long idProduct, BIStatus idStatus, Boolean isDraft) {
        Specification<BI> spec = Specification
                .where(BiSpecification.hasProductId(idProduct))
                .and(BiSpecification.hasNameContaining(text).or(BiSpecification.hasBINumberContaining(text)))
                .and(BiSpecification.hasStatusId(idStatus))
                .and(BiSpecification.isDeletedDateNull())
                .and(BiSpecification.isDraft(isDraft));
        List<BI> biList = businessInteractionRepository
                .findAll(spec);
        List<BIDto> result = biList.stream().map(biMapper::biToBIDto).collect(Collectors.toList());
        if (!getUserPermissions().contains(DESIGN_ARTIFACT.toString())) {
            result = result.stream().filter(biDto -> getUserProducts().contains(biDto.getProductId()) || !biDto.isDraft()).collect(Collectors.toList());
        }
        return result;
    }

    public BIDto getBIById(Long id) {
        BI bi = businessInteractionRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new NotFoundException("BI with id " + id + " not found"));
        validateAccessProduct(getUserPermissions(), getUserProducts(), bi);
        return biMapper.biToBIDto(bi);
    }

    public BIV2Dto getBIByIdV2(Long id) {
        BI bi = businessInteractionRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new NotFoundException("BI with id " + id + " not found"));
        validateAccessProduct(getUserPermissions(), getUserProducts(), bi);
        UserProfileDto userProfileDto = userClient.getUserProfile(bi.getAuthorId());
        AuthorDto authorDto = AuthorDto.builder()
                .id(userProfileDto.getId())
                .Email(userProfileDto.getEmail())
                .fullName(userProfileDto.getFullName())
                .build();
        return biMapper.biToBIV2Dto(bi, authorDto);
    }

    public List<BIDto> getBIByStepId(Long idStep) {
        Long cjId = cjStepRepository.findById(idStep).orElseThrow(() -> new NotFoundException("cjStep не найдено")).getCjId();
        CJ cj = cjRepository.findById(cjId).orElseThrow(() -> new NotFoundException("CJ не найдено"));
        validateAccessProduct(getUserPermissions(), getUserProducts(), cj);

        List<BIInCJStep> biInCJStepList = biInCJStepRepository.findAllByCjStepId(idStep);
        if (!biInCJStepList.isEmpty()) {
            List<BI> biList = businessInteractionRepository.findAllByIdIn(idStep, biInCJStepList.stream().map(BIInCJStep::getBiId).collect(Collectors.toList()));
            return biList.stream().map(biMapper::biToBIDto).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Transactional
    public void editBIByStepId(Long idStep, BiByCjStepDto bi) {
        Long cjId = cjStepRepository.findById(idStep).orElseThrow(() -> new NotFoundException("cjStep не найдено")).getCjId();
        Long idProductExt = cjRepository.findById(cjId).orElseThrow(() -> new NotFoundException("cj не найдено")).getIdProductExt();
        validateAccessProduct(getUserPermissions(),
                getUserProducts(), idProductExt);

        if (biInCJStepRepository.countByCjStepIdAndSJisDraftFalse(idStep) > 0) {
            throw new RuntimeException("Не допускается редактирование шага, если он используется в опубликованных CJ");
        }
        BI biEntity = businessInteractionRepository.findById(bi.getIdBi()).orElseThrow(() -> new NotFoundException("cj не найдено"));

        List<BIInCJStep> existSteps = biInCJStepRepository.findAllByCjStepId(idStep);
        checkMaxOrder(bi, existSteps);

        Optional<BIInCJStep> currentCjByBIid = existSteps.stream().filter(biInCJStep -> biInCJStep.getBiId().equals(bi.getIdBi())).findFirst();
        Optional<BIInCJStep> currentCjByOrder = existSteps.stream().filter(biInCJStep -> biInCJStep.getOrder().equals(bi.getOrder())).findFirst();

        if (!currentCjByOrder.isPresent() && !currentCjByBIid.isPresent()) {
            existSteps.add(new BIInCJStep(null, biEntity, idStep, bi.getOrder(), bi.getIdBi()));
        }

        if (currentCjByOrder.isPresent() && !currentCjByBIid.isPresent()) {
            existSteps.forEach(
                    step -> {
                        if (step.getOrder() >= bi.getOrder()) {
                            step.setOrder(step.getOrder() + 1);
                        }
                    }
            );
            existSteps.add(new BIInCJStep(null, biEntity, idStep, bi.getOrder(), bi.getIdBi()));
        }

        if (!currentCjByOrder.isPresent() && currentCjByBIid.isPresent()) {
            existSteps.forEach(
                    step -> {
                        if (step.getOrder() > currentCjByBIid.get().getOrder()) {
                            step.setOrder(step.getOrder() - 1);
                        }
                    }
            );
            currentCjByBIid.get().setOrder(bi.getOrder());
        }
        if (currentCjByOrder.isPresent() && currentCjByBIid.isPresent()) {
            existSteps.forEach(
                    step -> {
                        if (step.getOrder() > currentCjByBIid.get().getOrder()) {
                            step.setOrder(step.getOrder() - 1);
                        }
                        if (step.getOrder() >= bi.getOrder()) {
                            step.setOrder(step.getOrder() + 1);
                        }
                    }
            );
            currentCjByBIid.get().setOrder(bi.getOrder());
        }
        biInCJStepRepository.saveAllAndFlush(existSteps);
        CJ cj = cjRepository.findById(cjId).get();
        cj.setLastModifiedDate(new Date(System.currentTimeMillis()));
        cjRepository.save(cj);
    }

    private static void checkMaxOrder(BiByCjStepDto bi, List<BIInCJStep> existSteps) {
        Long maxOrder = existSteps.stream()
                .max(Comparator.comparing(BIInCJStep::getOrder))
                .map(BIInCJStep::getOrder)
                .orElse(0L);

        if (bi.getOrder() > maxOrder) {
            bi.setOrder(maxOrder + 1);
        }
    }

    @Transactional
    public BIDto createBI(BIPostDto biPostDto) {
        validateProductId(biPostDto.getProductId());
        validateAccessProduct(getUserPermissions(), getUserProducts(), biPostDto.getProductId());
        BI saveBI = buildBI(biPostDto);
        List<BILink> docs = mapLinks(biPostDto.getDocument());
        List<BILink> mockupLink = mapLinks(biPostDto.getMockupLink());
        List<BILink> scenarios = mapLinks(biPostDto.getFlowLink());
        List<BIParticipants> participants = mapParticipants(biPostDto.getParticipants());
        docs = saveLinks(docs, saveBI, 2L);
        mockupLink = saveLinks(mockupLink, saveBI, 3L);
        scenarios = saveLinks(scenarios, saveBI, 1L);
        participants = saveParticipants(participants, saveBI);
        List<BIChannelEnum> channels = biPostDto.getChannel() != null ? biPostDto.getChannel() : null;
        saveBI.setChannel(channels);
        biLinkRepository.flush();
        saveBI.setUniqueIdent(Utils.createUniqueIdent(saveBI.getId()));
        saveBI.setDocument(docs);
        saveBI.setFlowLink(scenarios);
        saveBI.setMockupLink(mockupLink);
        saveBI.setParticipants(participants);
        businessInteractionRepository.save(saveBI);
        businessInteractionRepository.flush();
        return biMapper.biToBIDto(businessInteractionRepository.findById(saveBI.getId()).orElse(null));
    }

    private BI buildBI(BIPostDto dto) {
        BI saveBI = BI.builder()
                .name(dto.getName())
                .descr(dto.getDescr())
                .isCommunal(dto.getCommunal() != null ? dto.getCommunal() : false)
                .isTarget(dto.getTarget() != null ? dto.getTarget() : false)
                .isDraft(dto.getDraft() != null ? dto.getDraft() : false)
                .touchPoints(dto.getTouchPoints())
                .eaGuid(dto.getEaGuid())
                .productId(dto.getProductId())
                .ownerRole(dto.getOwnerRole())
                .metrics(dto.getMetrics())
                .clientScenario(dto.getClientScenario())
                .ucsReaction(dto.getUcsReaction())
                .feeling(dto.getFeeling() != null ? biFeelingRepository.findById(dto.getFeeling().getId()).orElse(null) : null)
                .status(dto.getStatus() != null ? biStatusRepository.findById(dto.getStatus().getId()).orElse(null) : null)
                .authorId(getUserId())
                .createdDate(new Date((new java.util.Date()).getTime()))
                .lastModifiedDate(new Date((new java.util.Date()).getTime()))
                .uniqueIdent(UUID.randomUUID().toString())
                .document(new ArrayList<>())
                .flowLink(new ArrayList<>())
                .mockupLink(new ArrayList<>())
                .participants(new ArrayList<>())
                .channel(new ArrayList<>())
                .build();
        return businessInteractionRepository.saveAndFlush(saveBI);
    }

    private List<BILink> mapLinks(List<BILinkDto> linkDtos) {
        if (linkDtos == null) return null;
        return linkDtos.stream().map(dto -> {
            BILink link = new BILink();
            link.setDescr(dto.getDescr());
            link.setUrl(dto.getUrl());
            return link;
        }).collect(Collectors.toList());
    }

    private List<BIParticipants> mapParticipants(List<ParticipantDto> dtos) {
        if (dtos == null) return null;
        return dtos.stream().map(dto -> {
            BIParticipants p = new BIParticipants();
            p.setIdType(dto.getIdType());
            p.setDescr(dto.getDescr());
            p.setValue(dto.getValue());
            return p;
        }).collect(Collectors.toList());
    }

    private List<BILink> saveLinks(List<BILink> links, BI bi, Long typeId) {
        if (links == null) return null;
        links.forEach(link -> {
            link.setIdBi(bi);
            link.setType(LinkEnum.builder().id(typeId).build());
        });
        return biLinkRepository.saveAll(links);
    }

    private List<BIParticipants> saveParticipants(List<BIParticipants> participants, BI bi) {
        if (participants == null) return null;
        participants.forEach(p -> {
            p.setBuisnessIteraction(bi);
            p.setParticipantEnum(
                    biParticipantRepository.findById(p.getIdType()).orElse(null)
            );
        });
        return biParticipantsRepository.saveAll(participants);
    }

    //TODO: Абсолютная Дичь, нужно рефакторить
    @Transactional
    public BIDto patchBI(Long id, BI bi) {
        validateAccessProduct(getUserPermissions(), getUserProducts(), bi.getProductId());
        if (bi.checkFieldsForNull()) {
            throw new UnprocessedEntityException("Пустой обьект BI");
        }
        BI oldEntity = businessInteractionRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new NotFoundException("BI не найдено"));

        validateUpdate(oldEntity);
        validateNewProduct(oldEntity.getProductId());
        if (bi.getFlowLink() != null) {
            biLinkRepository.deleteAllByIdBiAndType(oldEntity, new LinkEnum(1L, "Ссылка на флоу"));
            biLinkRepository.saveAll(bi.getFlowLink().stream().peek(doc -> {
                doc.setIdBi(oldEntity);
                doc.setType(LinkEnum.builder().id(1L).build());
            }).collect(Collectors.toList()));
        }
        if (bi.getDocument() != null) {
            biLinkRepository.deleteAllByIdBiAndType(oldEntity, new LinkEnum(2L, "Документ"));
            biLinkRepository.saveAll(bi.getDocument().stream().peek(doc -> {
                doc.setIdBi(oldEntity);
                doc.setType(LinkEnum.builder().id(2L).build());
            }).collect(Collectors.toList()));
        }
        if (bi.getMockupLink() != null) {
            biLinkRepository.deleteAllByIdBiAndType(oldEntity, new LinkEnum(3L, "Макет"));
            biLinkRepository.saveAll(bi.getMockupLink().stream().peek(doc -> {
                doc.setIdBi(oldEntity);
                doc.setType(LinkEnum.builder().id(3L).build());
            }).collect(Collectors.toList()));
        }
        if (bi.getChannel() != null) {
            oldEntity.setChannel(new ArrayList<>());
        }
        if (bi.getParticipants() != null) {
            biParticipantsRepository.deleteAllByBuisnessIteraction(oldEntity);
            biParticipantsRepository.flush();
            biParticipantsRepository.saveAll(bi.getParticipants().stream().peek(participant -> {
                participant.setBuisnessIteraction(oldEntity);
                participant.setParticipantEnum(biParticipantRepository.findById(participant.getIdType()).orElseGet(null));
            }).collect(Collectors.toList()));
        }
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setPropertyCondition(Conditions.isNotNull());
        mapper.map(bi, oldEntity);
        if (bi.getFeeling() != null && bi.getFeeling().getId() != null) {
            oldEntity.setFeeling(biFeelingRepository.findById(bi.getFeeling().getId()).orElse(null));
        }
        oldEntity.setLastModifiedDate(new Date((new java.util.Date()).getTime()));
        oldEntity.setId(id);
        oldEntity.setAuthorId(getUserId());
        return biMapper.biToBIDto(businessInteractionRepository.save(oldEntity));
    }

    private void validateNewProduct(Long idProduct) {
        if (!getUserProducts().contains(idProduct) && !getUserPermissions().contains(DESIGN_ARTIFACT.toString())) {
            throw new ForbiddenException("FORBIDDEN");
        }
    }

    public BIEditabilityDto getEditabilityBI(Long id) {
        BIEditabilityDto result = new BIEditabilityDto(true);
        Optional<BI> entityOptional = businessInteractionRepository.findById(id);
        if (businessInteractionRepository.countByBiIdAndDraftFalse(id) > 0
                || !entityOptional.isPresent()) {
            result.setEditability(false);
        }

        if (getUserRole().contains("DEFAULT") && !getUserProducts().contains(entityOptional.get().getProductId()))
            result.setEditability(false);

        return result;
    }

    @Transactional
    public void deleteBIById(Long id) {
        BI bi = businessInteractionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BI с id = " + id + " не найден"));
        validateUpdate(bi);
        validateAccessProduct(getUserPermissions(), getUserProducts(), bi.getProductId());
        biInCJStepRepository.deleteAllByBiId(id);
        biInCJStepRepository.flush();
        biParticipantsRepository.deleteAllByBuisnessIteraction(bi);
        biParticipantsRepository.flush();
        bi.setDeletedDate(new Date(System.currentTimeMillis()));
        businessInteractionRepository.save(bi);
    }

    public List<CjResponseDto> getCJByBIID(Long id) {
        List<Long> cjStepIds = biInCJStepRepository.findBIInCJStepsByBiId(id)
                .stream()
                .map(BIInCJStep::getCjStepId)
                .collect(Collectors.toList());
        List<Long> cjIds = cjStepRepository.findAllById(cjStepIds)
                .stream()
                .map(CJStep::getCjId)
                .collect(Collectors.toList());
        return cjRepository.findAllByIdIn(cjIds)
                .stream()
                .map(cj -> modelMapper.map(cj, CjResponseDto.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteBIByStepId(Long idStep, Long idBi) {
        Long cjId = cjStepRepository.findById(idStep).orElseThrow(() -> new NotFoundException("CJ шаг с id = " + idStep + " не найден")).getCjId();
        CJ cj = cjRepository.findById(cjId).orElseThrow(() -> new NotFoundException("CJ с id = " + cjId + " не найден"));
        cj.setLastModifiedDate(new Date(System.currentTimeMillis()));
        Long idProductExt = cj.getIdProductExt();
        validateAccessProduct(getUserPermissions(), getUserProducts(), idProductExt);

        Optional<BI> biOptional = businessInteractionRepository.findById(idBi);
        BI bi = biOptional.orElseThrow(() -> new NotFoundException("BI с id = " + idBi + " не найден"));
        validateCj(bi);

        BIInCJStep biInCjStep = biInCJStepRepository.findByCjStepIdAndBiId(idStep, idBi);

        biRelationsRepository.deleteBySourceIteractionId(idBi);
        biRelationsRepository.flush();
        List<BIInCJStep> existSteps = biInCJStepRepository.findAllByCjStepId(idStep);
        if (!existSteps.isEmpty()) {
            existSteps.stream()
                    .filter(step -> step.getOrder() > biInCjStep.getOrder())
                    .forEach(step -> step.setOrder(step.getOrder() - 1));
        }
        biInCJStepRepository.saveAllAndFlush(existSteps);
        biInCJStepRepository.delete(biInCjStep);
    }

    private void validateUpdate(BI bi) {
        validateBI(bi);
        validateCj(bi);
    }

    private static void validateBI(BI bi) {
        if (bi != null && bi.isCommunal() && !bi.isDraft()) {
            throw new RuntimeException("Не допускается обновление/удаление опубликованных и коммунальных BI");
        }
    }

    private void validateCj(BI bi) {
        if (bi != null && businessInteractionRepository.countByBiIdAndDraftFalse(bi.getId()) > 0) {
            throw new RuntimeException("Не допускается обновление/удаление, если он уже используется в CJ");
        }
    }

    public Optional<BIStatus> getStatusById(Long id) {
        return biStatusRepository.findById(id);
    }

    @Transactional
    public void patchBiStep(Integer id, PatchStepDto patchStepDto) {
        BiStep biStep = biStepRepository.findById(id).orElseThrow(()
                -> new NotFoundException("BiStep с id " + id + " не найден"));
        if (patchStepDto.getErrorRate() != null) {
            biStep.setErrorRate(patchStepDto.getErrorRate());
        }
        if (patchStepDto.getRps() != null) {
            biStep.setRps(patchStepDto.getRps());
        }
        if (patchStepDto.getLatency() != null) {
            biStep.setLatency(patchStepDto.getLatency());
        }
        biStepRepository.save(biStep);
    }

    @Transactional
    public void updateRelationBiStep(Integer biStepId, List<PatchRelationStepDto> patchRelationStepDtos, String userId,
                                     Boolean patch) {
        BiStep biStep = biStepRepository.findById(biStepId)
                .orElseThrow(() -> new NotFoundException("BiStep с id " + biStepId + " не найден"));
        List<Integer> relationIdsFromRequest = patchRelationStepDtos.stream()
                .map(PatchRelationStepDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Integer, BiStepRelation> existingRelationsMap = new HashMap<>();
        if (!relationIdsFromRequest.isEmpty()) {
            List<BiStepRelation> allExistingRelations = biStepRelationRepository.findAllById(relationIdsFromRequest);
            existingRelationsMap = allExistingRelations.stream()
                    .collect(Collectors.toMap(BiStepRelation::getId, relation -> relation));
        }
        List<BiStepRelation> relationsOfCurrentBiStep = biStepRelationRepository.findByBiStepId(biStepId);
        Set<Integer> relationIdsOfCurrentBiStep = relationsOfCurrentBiStep.stream()
                .map(BiStepRelation::getId)
                .collect(Collectors.toSet());
        Set<Integer> idsToDelete = relationIdsOfCurrentBiStep.stream()
                .filter(id -> !relationIdsFromRequest.contains(id))
                .collect(Collectors.toSet());
        if (!idsToDelete.isEmpty()) {
            biStepRelationRepository.deleteByBiStepIdAndIdIn(biStepId, idsToDelete);
            log.info("Удалено {} записей из таблицы bi_steps_relations для BiStep {}",
                    idsToDelete.size(), biStepId);
        }
        for (PatchRelationStepDto request : patchRelationStepDtos) {
            Integer relationId = request.getId();
            BiStepRelation existingRelation = existingRelationsMap.get(relationId);
            if (existingRelation != null) {
                if (patch) {
                    updateRelation(existingRelation, request, biStep, Integer.parseInt(userId));
                    log.debug("Обновлена связь patch с id {}", relationId);
                } else {
                    putRelation(existingRelation, request, biStep, Integer.parseInt(userId));
                    log.debug("Обновлена связь put с id {}", relationId);
                }
                biStepRelationRepository.save(existingRelation);
            } else {
                BiStepRelation newRelation = createRelation(request, biStep, Integer.parseInt(userId));
                biStepRelationRepository.save(newRelation);
                log.debug("Создана новая связь с id {}", relationId);
            }
        }
        log.debug("метод успешно завершен, сохранение связей.");
    }

    private BiStepRelation createRelation(PatchRelationStepDto request, BiStep biStep, Integer userId) {
        return BiStepRelation.builder()
                .id(request.getId())
                .biStep(biStep)
                .userId(userId)
                .description(request.getDescription())
                .productId(request.getProductId())
                .tcId(request.getTcId())
                .operationId(request.getOperationId())
                .interfaceId(request.getInterfaceId())
                .build();
    }

    private void updateRelation(BiStepRelation relation, PatchRelationStepDto request, BiStep biStep, Integer userId) {
        relation.setBiStep(biStep);
        relation.setUserId(userId);
        if (request.getDescription() != null) {
            relation.setDescription(request.getDescription());
        }
        if (request.getProductId() != null) {
            relation.setProductId(request.getProductId());
        }
        if (request.getTcId() != null) {
            relation.setTcId(request.getTcId());
        }
        if (request.getOperationId() != null) {
            relation.setOperationId(request.getOperationId());
        }
        if (request.getInterfaceId() != null) {
            relation.setInterfaceId(request.getInterfaceId());
        }
    }

    private void putRelation(BiStepRelation relation, PatchRelationStepDto request, BiStep biStep, Integer userId) {
        relation.setBiStep(biStep);
        relation.setUserId(userId);
        relation.setDescription(request.getDescription());
        relation.setProductId(request.getProductId());
        relation.setTcId(request.getTcId());
        relation.setOperationId(request.getOperationId());
        relation.setInterfaceId(request.getInterfaceId());
    }
}
