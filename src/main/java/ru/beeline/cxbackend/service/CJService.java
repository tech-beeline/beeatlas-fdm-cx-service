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
import ru.beeline.cxbackend.dto.*;
import ru.beeline.cxbackend.exception.BadRequestException;
import ru.beeline.cxbackend.exception.ConflictException;
import ru.beeline.cxbackend.exception.ForbiddenException;
import ru.beeline.cxbackend.exception.NotFoundException;
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
        log.info("New cj created: " + newCJ);
        return modelMapper.map(newCJ, CjResponseDto.class);
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

        boolean changed = false;

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

        if (!changed) {
            return modelMapper.map(cj, CjResponseDto.class);
        }

        cj.setLastModifiedDate(new Date(System.currentTimeMillis()));
        cj = cjRepository.save(cj);

        return modelMapper.map(cj, CjResponseDto.class);
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

    public CJFullDtoV2 getFullDtoByIdV2(Long id) {
        CJ cj = getById(id);
        if (cj.getDeletedDate() != null) {
            throw new NotFoundException("CJ with id " + id + " does not exist");
        }
        UserProfileDto userProfileDto = userClient.getUserProfile(cj.getAuthorId());
        AuthorDto authorDto = AuthorDto.builder()
                .id(userProfileDto.getId())
                .Email(userProfileDto.getEmail())
                .fullName(userProfileDto.getFullName())
                .build();
        CJFullDtoV2 cjFullDtoV2 = modelMapper.map(cj, CJFullDtoV2.class);
        cjFullDtoV2.setAuthor(authorDto);
        cjFullDtoV2.setSteps(getAndConvertSteps(cjFullDtoV2.getId()));
        return cjFullDtoV2;
    }

    private CJ getAndValidateCJ(Long id) {
        CJ cj = getById(id);
        if (cj.getDeletedDate() != null) {
            throw new NotFoundException("CJ with id " + id + " does not exist");
        }
        validateAccessProduct(getUserPermissions(), getUserProducts(), cj);
        return cj;
    }

    private List<StepDtoV2> getAndConvertSteps(Long cjId) {
        List<CJStep> cjStepList = cjStepRepository.findAllByCjId(cjId);
        return cjStepList.stream()
                .map(this::convertToStepDto)
                .sorted(Comparator.comparing(StepDtoV2::getOrder))
                .collect(Collectors.toList());
    }

    private StepDtoV2 convertToStepDto(CJStep cjStep) {
        StepDtoV2 stepDtoV2 = modelMapper.map(cjStep, StepDtoV2.class);
        List<BIInCJStep> biInCJStepList = biInCJStepRepository.findAllByCjStepId(stepDtoV2.getId());
        if (!biInCJStepList.isEmpty()) {
            List<Long> biIds = biInCJStepList.stream()
                    .map(BIInCJStep::getBiId)
                    .collect(Collectors.toList());
            List<BI> biList = biRepository.findAllByIdIn(cjStep.getId(), biIds).stream()
                    .distinct()
                    .collect(Collectors.toList());
            stepDtoV2.setBi(biMapper.biToBIDto(biList));
        }
        return stepDtoV2;
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
