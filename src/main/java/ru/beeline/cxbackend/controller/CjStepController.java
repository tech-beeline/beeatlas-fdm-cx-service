package ru.beeline.cxbackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.cxbackend.domain.Permission;
import ru.beeline.cxbackend.domain.UserProfile;
import ru.beeline.cxbackend.domain.UserRoles;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.domain.cj.CJStep;
import ru.beeline.cxbackend.dto.CjStepDto;
import ru.beeline.cxbackend.dto.PermissionDto;
import ru.beeline.cxbackend.service.*;
import ru.beeline.cxbackend.utils.jwt.JwtUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping
@Api(value = "CX API", tags = "CJ Step")
public class CjStepController {

    @Autowired
    private CJStepService cjStepService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private CJService cjService;

    @Autowired
    private PermissionService permissionService;

    private Logger logger = LoggerFactory.getLogger(CjStepController.class);

    @GetMapping("/api/cx/v1/product/cj/{id}/step")
    @ResponseBody
    @ApiOperation(value = "Получение коллекции шагов CJ")
    public ResponseEntity getCJSteps(@RequestHeader("Authorization") String bearerToken, @PathVariable Long id) {
        List<CJStep> steps = cjStepService.getStepByCJId(id);
        return ResponseEntity.ok(steps);
    }

    @PostMapping("/api/cx/v1/product/cj/{id}/step")
    @ResponseBody
    @ApiOperation(value = "Добавление шага в коллекцию шагов CJ")
    public ResponseEntity addCJStep(@RequestHeader("Authorization") String bearerToken,
                                    @PathVariable Long id,
                                    @RequestBody CjStepDto cjStepDto) {
        String errors = "";

        String email = JwtUtils.getEmail(bearerToken);
        if (email != null) {
            UserProfile user = userProfileService.findProfileByEmail(email);
            if (user == null) {
                errors += "Добавлять шаги CJ могут только авторизованные пользователи";
                logger.error("401 " + errors);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
            } else {
                CJ currentCJ = cjService.getById(id);
                if (currentCJ == null) {
                    String message = "CJ с id = " + id + " не найден";
                    logger.error("404 " + message);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
                }
                userProfileService.validateAccessProduct(bearerToken, currentCJ.getIdProductExt());
                Set<Permission> rPermissions = new HashSet<>();
                for (UserRoles role : user.getUserRoles()) {
                    List<Permission> rolePermissions = roleService.getPermissions(role.getId());
                    if (!rolePermissions.isEmpty()) rPermissions.addAll(rolePermissions);
                }

                List<Permission.PermissionType> permissions = permissionService
                        .getUserPermissions(rPermissions)
                        .stream().map(PermissionDto::getAlias).collect(Collectors.toList());
                if (permissions.contains(Permission.PermissionType.EDIT_ARTIFACT)) {
                    if (currentCJ.isBDraft()) {
                        return ResponseEntity.ok(cjStepService.addStep(id, cjStepDto));
                    } else {
                        String message = "CJ с id = " + id + " находится в статусе Опубликован. Добавление шага невозможно.";
                        logger.error("409 " + message);
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
                    }
                } else {
                    errors += "Недостаточно прав для добавления шага CJ";
                    logger.error("403 " + errors);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors);
                }
            }

        } else {
            errors += "Добавлять шаги CJ могут только авторизованные пользователи";
            logger.error("401 " + errors);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
        }
    }

    @GetMapping("/api/cx/v1/product/cj/step/{id}")
    @ResponseBody
    @ApiOperation(value = "Получение шага CJ по id")
    public ResponseEntity getCJStepById(@RequestHeader("Authorization") String bearerToken, @PathVariable Long id) {
        CJStep cjStep = cjStepService.getStepById(id);
        if (cjStep == null) {
            String message = "CJ шаг с id = " + id + " не найден";
            logger.error("404 " + message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        } else {
            return ResponseEntity.ok(cjStep);
        }
    }

    @PatchMapping("/api/cx/v1/product/cj/step/{id}")
    @ResponseBody
    @ApiOperation(value = "Изменение шага CJ")
    public ResponseEntity updateCJStep(@RequestHeader("Authorization") String bearerToken,
                                       @PathVariable Long id, @RequestBody CjStepDto cjStepDto) {
        String errors = "";

        String email = JwtUtils.getEmail(bearerToken);
        if (email != null) {
            UserProfile user = userProfileService.findProfileByEmail(email);
            if (user == null) {
                errors += "Изменять шаги CJ могут только авторизованные пользователи";
                logger.error("401 " + errors);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
            } else {
                CJStep cjStep = cjStepService.getStepById(id);
                if (cjStep == null) {
                    String message = "CJ шаг с id = " + id + " не найден";
                    logger.error("404 " + message);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
                }
                String productId = cjService.getById(cjStep.getCjId()).getIdProductExt();
                userProfileService.validateAccessProduct(bearerToken, productId);
                Set<Permission> rPermissions = new HashSet<>();
                for (UserRoles role : user.getUserRoles()) {
                    List<Permission> rolePermissions = roleService.getPermissions(role.getId());
                    if (!rolePermissions.isEmpty()) rPermissions.addAll(rolePermissions);
                }

                List<Permission.PermissionType> permissions = permissionService
                        .getUserPermissions(rPermissions)
                        .stream().map(PermissionDto::getAlias).collect(Collectors.toList());
                if (permissions.contains(Permission.PermissionType.EDIT_ARTIFACT)) {
                    return ResponseEntity.ok(cjStepService.updateStep(cjStep, cjStepDto));
                } else {
                    errors += "Недостаточно прав для изменения шага CJ";
                    logger.error("403 " + errors);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors);
                }
            }
        } else {
            errors += "Изменять шаги CJ могут только авторизованные пользователи";
            logger.error("401 " + errors);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
        }
    }

    @DeleteMapping("/api/cx/v1/product/cj/step/{id}")
    @ResponseBody
    @ApiOperation(value = "Удаление шага CJ")
    public ResponseEntity deleteCJStep(@RequestHeader("Authorization") String bearerToken,
                                       @PathVariable Long id) {

        String errors = "";

        String email = JwtUtils.getEmail(bearerToken);
        if (email != null) {
            UserProfile user = userProfileService.findProfileByEmail(email);
            if (user == null) {
                errors += "Удалять шаги CJ могут только авторизованные пользователи";
                logger.error("401 " + errors);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
            } else {
                CJStep cjStep = cjStepService.getStepById(id);
                if (cjStep == null) {
                    String message = "CJ шаг с id = " + id + " не найден";
                    logger.error("404 " + message);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
                }
                String productId = cjService.getById(cjStep.getCjId()).getIdProductExt();
                userProfileService.validateAccessProduct(bearerToken, productId);
                Set<Permission> rPermissions = new HashSet<>();
                for (UserRoles role : user.getUserRoles()) {
                    List<Permission> rolePermissions = roleService.getPermissions(role.getId());
                    if (!rolePermissions.isEmpty()) rPermissions.addAll(rolePermissions);
                }

                List<Permission.PermissionType> permissions = permissionService
                        .getUserPermissions(rPermissions)
                        .stream().map(PermissionDto::getAlias).collect(Collectors.toList());
                if (permissions.contains(Permission.PermissionType.EDIT_ARTIFACT)) {
                    cjStepService.deleteStep(cjStep);
                    return ResponseEntity.ok().build();
                } else {
                    errors += "Недостаточно прав для удаления шага CJ";
                    logger.error("403 " + errors);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors);
                }
            }
        } else {
            errors += "Удалять шаги CJ могут только авторизованные пользователи";
            logger.error("401 " + errors);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
        }
    }
}
