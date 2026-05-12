/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service;


import lombok.extern.slf4j.Slf4j;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.cxbackend.client.UserClient;
import ru.beeline.cxbackend.domain.Permission;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.bi.BIInCJStep;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.domain.cj.CJStep;
import ru.beeline.cxbackend.domain.cj.CJTag;
import ru.beeline.cxbackend.domain.cj.CJTechOwner;
import ru.beeline.cxbackend.dto.*;
import ru.beeline.cxbackend.exception.*;
import ru.beeline.cxbackend.mapper.BIMapper;
import ru.beeline.cxbackend.repository.*;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static ru.beeline.cxbackend.controller.RequestContext.*;
import static ru.beeline.cxbackend.domain.Permission.PermissionType.DESIGN_ARTIFACT;
import static ru.beeline.cxbackend.utils.AccessToProduct.validateAccessProduct;
import static ru.beeline.cxbackend.utils.Constant.USER_ID_HEADER;

@Slf4j
@Service
public class CJService {

    @Autowired
    private CJRepository cjRepository;

    @Autowired
    private BusinessInteractionRepository biRepository;

    @Autowired
    private CJStepRepository cjStepRepository;

    @Autowired
    private BIInCJStepRepository biInCJStepRepository;

    @Autowired
    private CJTagRepository cjTagRepository;

    @Autowired
    private CJTechOwnerRepository cjTechOwnerRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private BIMapper biMapper;

    @Autowired
    private UserClient userClient;

    @PostConstruct
    public void initModelMapperMapping() {
        modelMapper.typeMap(CJ.class, CJFullDtoV2.class)
                .addMapping(CJ::getIdProductExt, CJFullDtoV2::setProductId);

        Converter<Set<CJTag>, List<String>> tagsListConverter = ctx -> {
            Set<CJTag> src = ctx.getSource();
            if (src == null) {
                return null;
            }
            return src.stream()
                    .map(CJTag::getName)
                    .collect(Collectors.toList());
        };

        modelMapper.typeMap(CJ.class, CJFullDtoV2.class)
                .addMappings(mapper -> mapper.using(tagsListConverter)
                        .map(CJ::getTags, CJFullDtoV2::setTags));

        modelMapper.typeMap(CJ.class, CjResponseDtoV2.class)
                .addMapping(CJ::getIdProductExt, CjResponseDtoV2::setIdProductExt)
                .addMapping(CJ::getDashboardLink, CjResponseDtoV2::setDashboardLink);

        Converter<Set<CJTag>, Set<String>> tagsSetConverter = ctx -> {
            Set<CJTag> src = ctx.getSource();
            if (src == null) {
                return null;
            }
            return src.stream()
                    .map(CJTag::getName)
                    .collect(Collectors.toSet());
        };

        modelMapper.typeMap(CJ.class, CjResponseDtoV2.class)
                .addMappings(mapper -> mapper.using(tagsSetConverter)
                        .map(CJ::getTags, CjResponseDtoV2::setTags));
    }

    public CJ findByName(String name) {
        return cjRepository.findByName(name);
    }

    @Transactional
    public CjResponseDto createNewCJ(CJTagsDto cj, Long productId) {
        validateAccessProduct(getUserPermissions(), getUserProducts(), productId);
        validateBody(cj);

        CJ newCJ = createCJ(cj, productId, Long.parseLong(getHeaders().get(USER_ID_HEADER).toString()));

        processTags(newCJ, cj.getTags());
        cjRepository.save(newCJ);

        List<Long> techOwners = persistTechOwnersIfProvided(newCJ, cj.getTechOwners());

        log.info("New cj created: " + newCJ);
        CjResponseDto response = modelMapper.map(newCJ, CjResponseDto.class);
        response.setBusinessOwner(newCJ.getIdBusinessOwner());
        response.setTechOwners(techOwners);
        return response;
    }

    private List<Long> persistTechOwnersIfProvided(CJ cj, List<Long> techOwners) {
        if (techOwners == null || techOwners.isEmpty()) {
            return techOwners;
        }

        List<Long> uniqueOwners = new ArrayList<>(new LinkedHashSet<>(techOwners));
        List<CJTechOwner> rows = uniqueOwners.stream()
                .filter(Objects::nonNull)
                .map(ownerId -> CJTechOwner.builder()
                        .cj(cj)
                        .idUserProfile(ownerId)
                        .build())
                .collect(Collectors.toList());

        if (!rows.isEmpty()) {
            cjTechOwnerRepository.saveAll(rows);
        }
        return uniqueOwners;
    }

    private void processTags(CJ cj, List<String> tagNames) {
        cj.getTags().clear();
        if (tagNames != null && !tagNames.isEmpty()) {
            for (String tagName : tagNames) {
                CJTag tag = findOrCreateTag(tagName);
                cj.getTags().add(tag);
                cj.setLastModifiedDate(new Date(System.currentTimeMillis()));
            }
        }
    }

    private CJTag findOrCreateTag(String tagName) {
        return cjTagRepository.findByName(tagName)
                .orElseGet(() -> {
                    CJTag newTag = CJTag.builder().name(tagName).build();
                    return cjTagRepository.save(newTag);
                });
    }

    private void validateBody(CJTagsDto cj) {
        if (!getUserPermissions().contains(Permission.PermissionType.CREATE_ARTIFACT.toString())) {
            throw new ForbiddenException("Недостаточно прав для создания CJ");
        }
        String errors = "";
        if (cj.getName() == null || cj.getName().trim().isEmpty()) {
            errors += "Поле name не может быть пустым.\n";
        }
        if (!errors.isEmpty()) {
            throw new ConflictException(errors);
        }
    }

    public CJ createCJ(CJTagsDto cj, Long productId, Long userId) {
        CJ newCJ = CJ.builder()
                .name(cj.getName())
                .userPortrait(cj.getUserPortrait())
                .lastModifiedDate(new Date())
                .createdDate(new Date())
                .authorId(userId)
                .idProductExt(productId)
                .bDraft(true)
                .uniqueIdent("temporary")
                .idBusinessOwner(cj.getBusinessOwner())
                .tags(new HashSet<>())
                .build();
        cjRepository.saveAndFlush(newCJ);
        newCJ.setUniqueIdent(generateUniqueIdent(newCJ.getId()));
        return cjRepository.save(newCJ);
    }

    private String generateUniqueIdent(Long id) {
        String padded = String.format("%08d", id);
        return "CJ." +
                padded.substring(0, 2) + "." +
                padded.substring(2, 4) + "." +
                padded.substring(4, 6) + "." +
                padded.substring(6, 8);
    }

    @Transactional
    public CjResponseDto replaceCJ(CJ cj, CJTagsDto cjDto) {


        if (cjDto.getBDraft() == null) {
            throw new ConflictException("Поле bDraft обязательно для редактирования CJ");
        }

        cj.setName(cjDto.getName());
        cj.setBDraft(cjDto.getBDraft());
        cj.setUserPortrait(cjDto.getUserPortrait());
        processTags(cj, cjDto.getTags());
        cj.setLastModifiedDate(new Date(System.currentTimeMillis()));
        cj = cjRepository.save(cj);

        return modelMapper.map(cj, CjResponseDto.class);
    }

    @Transactional
    public CjResponseDto patchCJ(CJ cj, CJTagsDto cjDto) {
        if (!(cj.isBDraft() || cjDto.getBDraft())) {
            throw new RuntimeException("Не допускается обработка CJ. Обработка возможна, только в статусе черновика");
        }
        if (Objects.nonNull(cjDto.getBDraft()) && !cjDto.getBDraft() && isCjHaveDraftBI(cj)) {
            throw new RuntimeException("Не допускается публикация CJ. Публикация возможна, с опубликованными шагами BI");
        }
        if (!userClient.userExists(cjDto.getBusinessOwner())) {
            log.warn("cj business owners not found. Id={}", cjDto.getBusinessOwner());
            throw new NotFoundException("Указанный бизнес ответственный, не найден");
        }
        cjDto.getTechOwners().forEach(techOwner -> {
            if (!userClient.userExists(techOwner)) {
                log.warn("cj technical owners not found. Id={}", techOwner);
                throw new NotFoundException("Указанный технический ответственный, не найден");
            }
        });

        boolean changed = false;
        boolean ownersChanged = false;

        if (cjDto.getName() != null && !cjDto.getName().equals(cj.getName())) {
            cj.setName(cjDto.getName());
            changed = true;
        }

        if (cjDto.isUserPortraitProvided()) {
            if (!Objects.equals(cjDto.getUserPortrait(), cj.getUserPortrait())) {
                cj.setUserPortrait(cjDto.getUserPortrait());
                changed = true;
            }
        }

        if (cjDto.getBDraft() != null && !cjDto.getBDraft().equals(cj.isBDraft())) {
            cj.setBDraft(cjDto.getBDraft());
            changed = true;
        }

        if (cjDto.getTags() != null) {
            Set<String> incoming = new HashSet<>(cjDto.getTags());
            Set<String> existing = cj.getTags().stream()
                    .map(tag -> tag.getName())
                    .collect(Collectors.toSet());
            if (!incoming.equals(existing)) {
                processTags(cj, cjDto.getTags());
                changed = true;
            }
        }

        if (cjDto.isBusinessOwnerProvided()) {
            if (!Objects.equals(cj.getIdBusinessOwner(), cjDto.getBusinessOwner())) {
                cj.setIdBusinessOwner(cjDto.getBusinessOwner());
                changed = true;
            }
        }

        if (cjDto.isTechOwnersProvided()) {
            syncTechOwners(cj.getId(), cjDto.getTechOwners());
            ownersChanged = true;
        }

        if (!changed && !ownersChanged) {
            return buildCjResponseWithOwners(cj);
        }

        if (changed) {
            cj.setLastModifiedDate(new Date(System.currentTimeMillis()));
            cj = cjRepository.save(cj);
        }

        return buildCjResponseWithOwners(cj);
    }

    private void syncTechOwners(Long cjId, List<Long> techOwners) {
        if (techOwners == null) {
            return;
        }

        List<Long> desired = new ArrayList<>(new LinkedHashSet<>(techOwners)).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (desired.isEmpty()) {
            cjTechOwnerRepository.deleteAllByCj_Id(cjId);
            return;
        }

        List<CJTechOwner> existing = cjTechOwnerRepository.findAllByCj_Id(cjId);
        Set<Long> existingIds = existing.stream()
                .map(CJTechOwner::getIdUserProfile)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<CJTechOwner> toDelete = existing.stream()
                .filter(row -> row.getIdUserProfile() == null || !desired.contains(row.getIdUserProfile()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            cjTechOwnerRepository.deleteAll(toDelete);
        }

        List<CJTechOwner> toAdd = desired.stream()
                .filter(id -> !existingIds.contains(id))
                .map(id -> CJTechOwner.builder()
                        .cj(CJ.builder().id(cjId).build())
                        .idUserProfile(id)
                        .build())
                .collect(Collectors.toList());
        if (!toAdd.isEmpty()) {
            cjTechOwnerRepository.saveAll(toAdd);
        }
    }

    private CjResponseDto buildCjResponseWithOwners(CJ cj) {
        CjResponseDto dto = modelMapper.map(cj, CjResponseDto.class);
        dto.setBusinessOwner(cj.getIdBusinessOwner());
        List<Long> techOwners = cjTechOwnerRepository.findAllByCj_Id(cj.getId()).stream()
                .map(CJTechOwner::getIdUserProfile)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        dto.setTechOwners(techOwners);
        return dto;
    }

    @Transactional
    public void deleteCJbyId(CJ cj) {
        if (!cj.isBDraft()) {
            throw new RuntimeException("Не допускается удаление опубликованных CJ.");
        }
        List<CJStep> cjStepList = cjStepRepository.findAllByCjId(cj.getId());
        if (!cjStepList.isEmpty()) {
            List<Long> stepIds = cjStepList.stream()
                    .map(CJStep::getId)
                    .collect(Collectors.toList());
            biInCJStepRepository.deleteAllByCjStepIdIn(stepIds);
        }
        cjStepRepository.deleteAllByCjId(cj.getId());
        cj.setDeletedDate(new Date(System.currentTimeMillis()));
        cjRepository.save(cj);
    }

    public CJ getById(Long id) {
        return cjRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CJ with id " + id + " does not exist"));
    }

    public CJFullDto getFullDtoById(Long id) {
        CJ cj = getAndValidateCJ(id);
        validateAccessProduct(getUserPermissions(), getUserProducts(), cj);
        CJFullDto cjFullDto = modelMapper.map(cj, CJFullDto.class);
        List<CJStep> cjStepList = cjStepRepository.findAllByCjId(cjFullDto.getId());
        List<StepDto> stepDtos = cjStepList.stream().map(cjStep -> {
                    StepDto stepDto = modelMapper.map(cjStep, StepDto.class);
                    List<BIInCJStep> biInCJStepList = biInCJStepRepository.findAllByCjStepId(stepDto.getId());
                    if (!biInCJStepList.isEmpty()) {
                        List<BI> biList = biRepository.findAllByIdIn(cjStep.getId(), biInCJStepList.stream()
                                .map(BIInCJStep::getBiId).collect(Collectors.toList()));
                        stepDto.setBi(biMapper.biToBIDto(biList.stream().distinct().collect(Collectors.toList())));
                    }
                    return stepDto;
                }).sorted(Comparator.comparing(StepDto::getOrder))
                .collect(Collectors.toList());
        cjFullDto.setSteps(stepDtos);
        return cjFullDto;
    }

    public CJFullDtoV3 getFullDtoByIdV3(Long id) {
        CJ cj = getById(id);
        if (cj.getDeletedDate() != null) {
            throw new NotFoundException("CJ with id " + id + " does not exist");
        }

        Long authorId = cj.getAuthorId();
        Long businessOwnerId = cj.getIdBusinessOwner();
        List<Long> techOwnerIds = cjTechOwnerRepository.findAllByCj_Id(id).stream()
                .map(CJTechOwner::getIdUserProfile)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<Long> idsToFetch = new ArrayList<>();
        if (authorId != null) {
            idsToFetch.add(authorId);
        }
        if (businessOwnerId != null) {
            idsToFetch.add(businessOwnerId);
        }
        idsToFetch.addAll(techOwnerIds);
        idsToFetch = idsToFetch.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, UserShortDto> usersById = new HashMap<>();
        if (!idsToFetch.isEmpty()) {
            try {
                List<AuthUserDto> users = userClient.getUsersByIds(idsToFetch);
                if (users != null) {
                    usersById = users.stream()
                            .filter(Objects::nonNull)
                            .filter(u -> u.getId() != null)
                            .collect(Collectors.toMap(
                                    AuthUserDto::getId,
                                    u -> UserShortDto.builder()
                                            .id(u.getId())
                                            .fullName(u.getFullName())
                                            .email(u.getEmail())
                                            .build(),
                                    (a, b) -> a
                            ));
                }
            } catch (Exception e) {
                String msg = "Ошибка сервиса авторизации:" + (e.getMessage() == null ? "" : e.getMessage());
                throw new AuthServiceException(msg, e);
            }
        }

        UserShortDto authorDto = authorId == null ? null : usersById.get(authorId);
        UserShortDto businessOwnerDto = businessOwnerId == null ? null : usersById.get(businessOwnerId);
        List<UserShortDto> techOwnerDtos = techOwnerIds.stream()
                .map(usersById::get) // может быть null (пользователь не найден) — по требованию оставляем null
                .collect(Collectors.toList());

        CJFullDtoV3 cjFullDtoV3 = modelMapper.map(cj, CJFullDtoV3.class);
        cjFullDtoV3.setAuthor(authorDto);
        cjFullDtoV3.setBusinessOwner(businessOwnerDto);
        cjFullDtoV3.setTechOwners(techOwnerDtos);
        cjFullDtoV3.setSteps(getAndConvertSteps(cjFullDtoV3.getId()));
        cjFullDtoV3.setProductId(cj.getIdProductExt());
        cjFullDtoV3.setLink(cj.getLinks()
                                    .stream()
                                    .map(link -> LinkDTO.builder().descr(link.getDescr()).url(link.getUrl()).build())
                                    .collect(Collectors.toList()));
        return cjFullDtoV3;
    }

    private CJ getAndValidateCJ(Long id) {
        CJ cj = getById(id);
        if (cj.getDeletedDate() != null) {
            throw new NotFoundException("CJ with id " + id + " does not exist");
        }
        validateAccessProduct(getUserPermissions(), getUserProducts(), cj);
        return cj;
    }

    private List<StepDtoV3> getAndConvertSteps(Long cjId) {
        List<CJStep> cjStepList = cjStepRepository.findAllByCjId(cjId);
        return cjStepList.stream()
                .map(this::convertToStepDto)
                .sorted(Comparator.comparing(StepDtoV3::getOrder))
                .collect(Collectors.toList());
    }

    private StepDtoV3 convertToStepDto(CJStep cjStep) {
        StepDtoV3 stepDtoV3 = modelMapper.map(cjStep, StepDtoV3.class);
        List<BIInCJStep> biInCJStepList = biInCJStepRepository.findAllByCjStepId(stepDtoV3.getId());
        if (!biInCJStepList.isEmpty()) {
            List<Long> biIds = biInCJStepList.stream()
                    .map(BIInCJStep::getBiId)
                    .collect(Collectors.toList());
            List<BI> biList = biRepository.findAllByIdIn(cjStep.getId(), biIds).stream()
                    .distinct()
                    .collect(Collectors.toList());
            stepDtoV3.setBi(biMapper.biToBIDtoV3(biList));
        }
        return stepDtoV3;
    }

    public List<CjResponseDto> getAll(Long idProduct, String sample, String search) {
        List<CJ> result;
        switch (sample) {
            case "PUBLIC":
                result = cjRepository.findAllByNameContainsIgnoreCase(search).stream().filter(cj -> !cj.isBDraft()).collect(Collectors.toList());
                break;
            case "DRAFT":
                result = getProducts(search).stream().filter(CJ::isBDraft).collect(Collectors.toList());
                break;
            default:
                result = getMyProductsDefault(search);
        }
        if (idProduct != null) {
            result = result.stream().filter(cj -> Objects.equals(cj.getIdProductExt(), idProduct)).collect(Collectors.toList());
        }
        return result.stream()
                .filter(cj -> cj.getDeletedDate() == null)
                .map(cj -> {
                    return modelMapper.map(cj, CjResponseDto.class);
                })
                .collect(Collectors.toList());
    }

    public List<CjResponseDtoV2> getAllv2(Long idProduct, String sample, String search) {
        List<CJ> result;
        switch (sample) {
            case "PUBLIC":
                result = cjRepository.findAllByNameContainsIgnoreCase(search).stream().filter(cj -> !cj.isBDraft()).collect(Collectors.toList());
                break;
            case "DRAFT":
                result = cjRepository.findAllByNameContainsIgnoreCase(search).stream().filter(CJ::isBDraft).collect(Collectors.toList());
                break;
            default:
                result = cjRepository.findAllByNameContainsIgnoreCase(search);
        }
        if (idProduct != null) {
            result = result.stream().filter(cj -> Objects.equals(cj.getIdProductExt(), idProduct)).collect(Collectors.toList());
        }
        return result.stream()
                .filter(cj -> cj.getDeletedDate() == null)
                .map(cj -> {
                    return modelMapper.map(cj, CjResponseDtoV2.class);
                })
                .collect(Collectors.toList());
    }

    private List<CJ> getProducts(String search) {
        if (getUserPermissions().contains(DESIGN_ARTIFACT.toString())) {
            return cjRepository.findAllByNameContainsIgnoreCase(search);
        }
        return cjRepository.findAllByNameContainsIgnoreCaseAndIdProductExtIn(search, getUserProducts());
    }

    private List<CJ> getMyProductsDefault(String search) {
        if (getUserPermissions().contains(DESIGN_ARTIFACT.toString())) {
            return cjRepository.findAllByNameContainsIgnoreCase(search);
        }

        List<CJ> userCJs;
        List<CJ> otherCJs;
        userCJs = cjRepository.findAllByNameContainsIgnoreCaseAndIdProductExtIn(search, getUserProducts());
        otherCJs = cjRepository.findAllByNameContainsIgnoreCaseAndIdProductExtNotIn(search, getUserProducts());
        otherCJs = otherCJs.stream().filter(cj -> !cj.isBDraft()).collect(Collectors.toList());
        if (!otherCJs.isEmpty()) {
            userCJs.addAll(otherCJs);
        }
        List<CJ> withProductExtNull = cjRepository.findAllByNameContainsIgnoreCaseAndIdProductExtIsNull(search);
        if (!withProductExtNull.isEmpty()) {
            userCJs.addAll(withProductExtNull);
        }
        return userCJs;
    }

    private boolean isCjHaveDraftBI(CJ cj) {
        return cjStepRepository.countByBiIdAndDraft(cj.getId()) > 0L;
    }

    public void savingLink(Long id, DashboardLinkDTO dashboardLinkDTO) {
        if(dashboardLinkDTO.getDashboardLink()==null || dashboardLinkDTO.getDashboardLink().isEmpty()){
            throw new BadRequestException("The dashboardLink field cannot be empty.");
        }
        CJ cj =  cjRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CJ with id " + id + " does not exist"));
        cj.setDashboardLink(dashboardLinkDTO.getDashboardLink());
        cjRepository.save(cj);
    }
}
