package ru.beeline.cxbackend.service;


import lombok.extern.slf4j.Slf4j;
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
import ru.beeline.cxbackend.dto.AuthorDto;
import ru.beeline.cxbackend.dto.CJDto;
import ru.beeline.cxbackend.dto.CJFullDto;
import ru.beeline.cxbackend.dto.CJFullDtoV2;
import ru.beeline.cxbackend.dto.CJV2Dto;
import ru.beeline.cxbackend.dto.StepDto;
import ru.beeline.cxbackend.dto.StepDtoV2;
import ru.beeline.cxbackend.dto.UserProfileDto;
import ru.beeline.cxbackend.exception.ConflictException;
import ru.beeline.cxbackend.exception.ForbiddenException;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.exception.UnprocessedEntityException;
import ru.beeline.cxbackend.mapper.BIMapper;
import ru.beeline.cxbackend.repository.*;

import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.beeline.cxbackend.controller.RequestContext.getHeaders;
import static ru.beeline.cxbackend.controller.RequestContext.getUserPermissions;
import static ru.beeline.cxbackend.controller.RequestContext.getUserProducts;
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
    private CJParametersViewRepository cjParametersViewRepository;

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
    }

    public CJ findByName(String name) {
        return cjRepository.findByName(name);
    }

    public CJ createCJ(CJDto cj, Long productId, Long userId) {
        CJ newCJ = CJ.builder()
                .name(cj.getName())
                .userPortrait(cj.getUserPortrait())
                .lastModifiedDate(new Date())
                .createdDate(new Date())
                .authorId(userId)
                .idProductExt(productId)
                .bDraft(true)
                .build();
        cjRepository.save(newCJ);
        return newCJ;
    }

    public CJ createCJV2(CJV2Dto cj) {
        validateAccessProduct(getUserPermissions());
        if (!(getUserPermissions()).contains(Permission.PermissionType.CREATE_ARTIFACT.toString())) {
            throw new ForbiddenException("Недостаточно прав для создания CJ");
        }
        validateCJPostDto(cj);
        CJ newCJ = createCJ(cj, Long.parseLong(getHeaders().get(USER_ID_HEADER).toString()));
        log.info("New cj created: " + newCJ);
        return newCJ;
    }

    private void validateCJPostDto(CJV2Dto cj) {
        StringBuilder errMsg = new StringBuilder();
        if (cj.getName() == null || cj.getName().isEmpty()) {
            errMsg.append("Поле name не может быть пустым.");
            throw new ConflictException(errMsg.toString());
        }
        if (findByName(cj.getName()) != null) {
            throw new UnprocessedEntityException("Указанное имя CJ уже существует");
        }
    }

    public CJ createCJ(CJV2Dto cj, Long userId) {
        Integer productId = cj.getProductId();
        CJ newCJ = CJ.builder()
                .name(cj.getName())
                .userPortrait(cj.getUserPortrait())
                .lastModifiedDate(new Date())
                .createdDate(new Date())
                .authorId(userId)
                .idProductExt(productId == null ? null : Long.valueOf(cj.getProductId()))
                .bDraft(true)
                .build();
        cjRepository.save(newCJ);
        return newCJ;
    }

    public CJ updateCJ(CJ cj, CJDto cjDto) {
        if (!(cj.isBDraft() || cjDto.getBDraft())) {
            throw new RuntimeException("Не допускается обработка CJ. Обработка возможна, только в статусе черновика");
        }
        if (Objects.nonNull(cjDto.getBDraft()) && !cjDto.getBDraft() && isCjHaveDraftBI(cj)) {
            throw new RuntimeException("Не допускается публикация CJ. Публикация возможна, с опубликованными шагами BI");
        }
        Optional.ofNullable(cjDto.getName()).ifPresent(cj::setName);
        Optional.ofNullable(cjDto.getUserPortrait()).ifPresent(cj::setUserPortrait);
        Optional.ofNullable(cjDto.getBDraft()).ifPresent(cj::setBDraft);
        if (cjDto.getBDraft() != null || cjDto.getName() != null || cjDto.getUserPortrait() != null) {
            cj.setLastModifiedDate(new Date(System.currentTimeMillis()));
            cjRepository.save(cj);
        }
        return cj;
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
                        List<BI> biList = biRepository.findAllByIdIn(cjStep.getId(), biInCJStepList.stream().map(BIInCJStep::getBiId).collect(Collectors.toList()));
                        stepDto.setBi(biMapper.biToBIDto(biList.stream().distinct().collect(Collectors.toList())));
                    }
                    return stepDto;
                }).sorted(Comparator.comparing(StepDto::getOrder))
                .collect(Collectors.toList());
        cjFullDto.setSteps(stepDtos);
        return cjFullDto;
    }

    public CJFullDtoV2 getFullDtoByIdV2(Long id) {
        CJ cj = getAndValidateCJ(id);
        validateAccessProduct(getUserPermissions(), getUserProducts(), cj);
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

    public List<CJ> getAll(Long idProduct, String sample, String search) {
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
        return userCJs;
    }

    private boolean isCjHaveDraftBI(CJ cj) {
        return cjStepRepository.countByBiIdAndDraft(cj.getId()) > 0L;
    }
}
