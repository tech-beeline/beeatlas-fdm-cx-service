package ru.beeline.cxbackend.service;

import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.cxbackend.domain.bi.*;
import ru.beeline.cxbackend.domain.bi.ref.BIStatus;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.domain.cj.CJStep;
import ru.beeline.cxbackend.dto.BIDto;
import ru.beeline.cxbackend.dto.BIEditabilityDto;
import ru.beeline.cxbackend.dto.BiByCjStepDto;
import ru.beeline.cxbackend.exception.ForbiddenException;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.exception.UnprocessedEntityException;
import ru.beeline.cxbackend.mapper.BIMapper;
import ru.beeline.cxbackend.repository.*;

import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

import static ru.beeline.cxbackend.controller.RequestContext.*;
import static ru.beeline.cxbackend.domain.Permission.PermissionType.DESIGN_ARTIFACT;
import static ru.beeline.cxbackend.utils.AccessToProduct.validateAccessProduct;

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
        BI bi = businessInteractionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BI with id " + id + " not found"));
        validateAccessProduct(getUserPermissions(), getUserProducts(), bi);
        return biMapper.biToBIDto(bi);
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

    //TODO: Абсолютная Дичь, нужно рефакторить
    @Transactional
    public BIDto createBI(BI bi) {
        validateAccessProduct(getUserPermissions(), getUserProducts(), bi.getProductId());

        bi.setCreatedDate(new Date((new java.util.Date()).getTime()));
        bi.setLastModifiedDate(new Date((new java.util.Date()).getTime()));

        List<BILink> docs = bi.getDocument();
        List<BILink> mockupLink = bi.getMockupLink();
        List<BILink> scenarios = bi.getFlowLink();
        List<BIParticipants> participants = bi.getParticipants();

        bi.setDocument(new ArrayList<>());
        bi.setFlowLink(new ArrayList<>());
        bi.setMockupLink(new ArrayList<>());
        bi.setParticipants(new ArrayList<>());

        bi.setFeeling(biFeelingRepository.findById(bi.getFeeling().getId()).orElse(null));
        bi.setStatus(biStatusRepository.findById(bi.getStatus().getId()).orElse(null));
        bi.setUniqueIdent(UUID.randomUUID().toString());
        BI finalBi = businessInteractionRepository.save(bi);
        businessInteractionRepository.flush();

        if (docs != null) {
            docs = biLinkRepository.saveAll(docs.stream().peek(doc -> {
                doc.setIdBi(finalBi);
                doc.setType(LinkEnum.builder().id(2L).build());
            }).collect(Collectors.toList()));
        }
        if (mockupLink != null) {
            mockupLink = biLinkRepository.saveAll(mockupLink.stream().peek(doc -> {
                doc.setIdBi(finalBi);
                doc.setType(LinkEnum.builder().id(3L).build());
            }).collect(Collectors.toList()));
        }
        if (scenarios != null) {
            scenarios = biLinkRepository.saveAll(scenarios.stream().peek(doc -> {
                doc.setIdBi(finalBi);
                doc.setType(LinkEnum.builder().id(1L).build());
            }).collect(Collectors.toList()));
        }
        if (participants != null) {
            participants = biParticipantsRepository.saveAll(participants.stream().peek(participant -> {
                participant.setBuisnessIteraction(finalBi);
                participant.setParticipantEnum(biParticipantRepository.findById(participant.getIdType()).orElseGet(null));
            }).collect(Collectors.toList()));
        }
        biLinkRepository.flush();

        finalBi.setUniqueIdent(createUniqueIdent(finalBi.getId()));
        finalBi.setDocument(docs);
        finalBi.setFlowLink(scenarios);
        finalBi.setMockupLink(mockupLink);
        finalBi.setParticipants(participants);
        businessInteractionRepository.save(finalBi);
        businessInteractionRepository.flush();
        return biMapper.biToBIDto(businessInteractionRepository.findById(finalBi.getId()).orElse(null));
    }

    private String createUniqueIdent(Long id) {
        String idString = String.format("%08d", id);
        return "BI." + idString.substring(0, 2) + "." + idString.substring(2, 4) + "." + idString.substring(4, 6) + "." + idString.substring(6);
    }

    //TODO: Абсолютная Дичь, нужно рефакторить
    @Transactional
    public BIDto patchBI(Long id, BI bi) {
        validateAccessProduct(getUserPermissions(), getUserProducts(), bi.getProductId());
        if (bi.checkFieldsForNull()) {
            throw new UnprocessedEntityException("Пустой обьект BI");
        }
        BI oldEntity = businessInteractionRepository.findById(id)
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

    public List<CJ> getCJByBIID(Long id) {
        List<Long> cjStepIds = biInCJStepRepository.findBIInCJStepsByBiId(id).stream().map(BIInCJStep::getCjStepId).collect(Collectors.toList());
        List<Long> cjIds = cjStepRepository.findAllById(cjStepIds).stream().map(CJStep::getCjId).collect(Collectors.toList());
        return cjRepository.findAllByIdIn(cjIds);
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
}
