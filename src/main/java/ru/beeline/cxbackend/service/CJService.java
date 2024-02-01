package ru.beeline.cxbackend.service;


import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.cxbackend.domain.Permission;
import ru.beeline.cxbackend.domain.Product;
import ru.beeline.cxbackend.domain.UserProfile;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.bi.BIInCJStep;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.domain.cj.CJParametersView;
import ru.beeline.cxbackend.domain.cj.CJStep;
import ru.beeline.cxbackend.dto.CJDto;
import ru.beeline.cxbackend.dto.CJFullDto;
import ru.beeline.cxbackend.dto.StepDto;
import ru.beeline.cxbackend.exception.CJNotExistException;
import ru.beeline.cxbackend.mapper.BIMapper;
import ru.beeline.cxbackend.repository.*;
import ru.beeline.cxbackend.utils.jwt.JwtUtils;

import java.sql.Date;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.beeline.cxbackend.utils.jwt.JwtUtils.encodeJWT;

@Service
public class CJService {

    private static final String USER_LOGIN = "winaccountname";

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
    private UserProfileService userProfileService;

    public CJ findByName(String name) {
        return cjRepository.findByName(name);
    }

    public CJ createCJ(CJDto cj, Product product, UserProfile userProfile) {
        CJ newCJ = CJ.builder()
                .name(cj.getName())
                .userPortrait(cj.getUserPortrait())
                .lastUpdated(new Date(System.currentTimeMillis()))
                .authorId(userProfile.getId())
                .idProductExt(product.getId())
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
        cj.setLastUpdated(new Date(System.currentTimeMillis()));
        cjRepository.save(cj);
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
        cjRepository.deleteById(cj.getId());
    }

    public List<CJParametersView> getParametersViewByCJId(Long id) {
        return cjParametersViewRepository.findByCjId(id);
    }

    public CJ getById(Long id) {
        return cjRepository.findById(id).orElseGet(null);
    }

    public CJFullDto getFullDtoById(Long id, String bearerToken) throws CJNotExistException {
        CJ cj = getById(id);
        if (cj == null) throw new CJNotExistException("CJ with id " + id + " is not exist");
        else userProfileService.validateAccessProduct(bearerToken, cj.getIdProductExt());
        CJ result = cjRepository.findById(id).orElseGet(null);
        CJFullDto cjFullDto = modelMapper.map(result, CJFullDto.class);

        List<CJStep> cjStepList = cjStepRepository.findAllByCjId(cjFullDto.getId());
        List<StepDto> stepDtos = cjStepList.stream().map(cjStep -> {
                    StepDto stepDto = modelMapper.map(cjStep, StepDto.class);
                    List<BIInCJStep> biInCJStepList = biInCJStepRepository.findAllByCjStepId(stepDto.getId());
                    List<BI> biList = biRepository.findAllByIdIn(cjStep.getId(), biInCJStepList.stream().map(BIInCJStep::getBiId).collect(Collectors.toList()));
                    stepDto.setBi(biMapper.biToBIDto(biList.stream().distinct().collect(Collectors.toList())));
                    return stepDto;
                }).sorted(Comparator.comparing(StepDto::getOrder))
                .collect(Collectors.toList());
        cjFullDto.setSteps(stepDtos);

        return cjFullDto;
    }

    public List<CJ> getAll(String idProduct, String sample, String search, String token) {
        String userLogin = Objects.requireNonNull(encodeJWT(token)).get(USER_LOGIN);
        String email = JwtUtils.getEmail(token);
        UserProfile user = userProfileService.findProfileByEmail(email);
        Boolean designer = user.getUserRoles().stream().noneMatch(userRoles ->
                        userRoles.getRole().getPermissions().stream().anyMatch(rolePermissions ->
                                rolePermissions.getPermission().getAlias() == Permission.PermissionType.DESIGN_ARTIFACT));
        List<CJ> result;
        switch (sample) {
            case "PUBLIC":
                result = cjRepository.findAllByNameContainsIgnoreCase(search).stream().filter(cj -> !cj.isBDraft()).collect(Collectors.toList());
                break;
            case "DRAFT":
                result = getProducts(search, userLogin).stream().filter(CJ::isBDraft).collect(Collectors.toList());
                break;
            default:
                result = getMyProductsDefault(search, userLogin);
        }
        if (idProduct != null) {
            result = result.stream().filter(cj -> cj.getIdProductExt().equals(idProduct)).collect(Collectors.toList());
        }

        return result;
    }

    private List<CJ> getProducts(String search, String userLogin) {
        UserProfile user = userProfileService.findProfileByLogin(userLogin);
        if (user.getUserRoles().stream().anyMatch(userRoles ->
                userRoles.getRole().getPermissions().stream().anyMatch(rolePermissions ->
                        rolePermissions.getPermission().getAlias() == Permission.PermissionType.DESIGN_ARTIFACT))) {
            return cjRepository.findAllByNameContainsIgnoreCase(search);
        }
        List<String> userProducts = cjRepository.findProductIdsByUserLogin(userLogin);
        return cjRepository.findAllByNameContainsIgnoreCaseAndIdProductExtIn(search, userProducts);
    }

    private List<CJ> getMyProductsDefault(String search, String userLogin) {
        UserProfile user = userProfileService.findProfileByLogin(userLogin);
        if (user.getUserRoles().stream().anyMatch(userRoles ->
                userRoles.getRole().getPermissions().stream().anyMatch(rolePermissions ->
                        rolePermissions.getPermission().getAlias() == Permission.PermissionType.DESIGN_ARTIFACT))) {
            return cjRepository.findAllByNameContainsIgnoreCase(search);
        }

        List<CJ> userCJs;
        List<CJ> otherCJs;
        List<String> userProducts = cjRepository.findProductIdsByUserLogin(userLogin);
        userCJs = cjRepository.findAllByNameContainsIgnoreCaseAndIdProductExtIn(search, userProducts);
        otherCJs = cjRepository.findAllByNameContainsIgnoreCaseAndIdProductExtNotIn(search, userProducts).stream().filter(cj -> !cj.isBDraft()).collect(Collectors.toList());
        if (!otherCJs.isEmpty()) {
            userCJs.addAll(otherCJs);
        }
        return userCJs;
    }

    private boolean isCjHaveDraftBI(CJ cj) {
        return cjStepRepository.countByBiIdAndDraft(cj.getId()) > 0L;
    }
}
