/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.mapper;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.cxbackend.client.CapabilityClient;
import ru.beeline.cxbackend.client.ProductClient;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.bi.BIParticipants;
import ru.beeline.cxbackend.domain.bi.BiStep;
import ru.beeline.cxbackend.domain.bi.BiStepRelation;
import ru.beeline.cxbackend.dto.*;
import ru.beeline.cxbackend.repository.BiStepRelationRepository;
import ru.beeline.cxbackend.repository.BiStepRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BIMapper {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    BiStepRepository biStepRepository;

    @Autowired
    CapabilityClient capabilityClient;

    @Autowired
    ProductClient productClient;

    @Autowired
    BiStepRelationRepository biStepRelationRepository;


    public List<BIDto> biToBIDto(List<BI> biList) {
        if (biList != null) {
            return biList.stream().map(this::biToBIDto).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public BIDto biToBIDto(BI bi) {
        BIDto biDto = modelMapper.map(bi, BIDto.class);
        List<BiStep> biSteps = biStepRepository.findByBi(bi);
        if (biSteps.isEmpty()) {
            biDto.setBiSteps(new ArrayList<>());
        } else {
            biDto.setBiSteps(createBiStep(biSteps));
        }
        biDto.setParticipants(mapBIParticipants(bi.getParticipants()));
        if (bi.getFeeling() != null) {
            biDto.setFeelings(modelMapper.map(bi.getFeeling(), BIFeelingDto.class));
        }
        if (bi.getStatus() != null) {
            biDto.setStatus(modelMapper.map(bi.getStatus(), BIStatusDto.class));
        }
        return biDto;
    }

    public BIV2Dto biToBIV2Dto(BI bi, AuthorDto authorDto) {
        BIV2Dto biV2Dto = modelMapper.map(bi, BIV2Dto.class);
        biV2Dto.setParticipants(mapBIParticipants(bi.getParticipants()));
        if (bi.getFeeling() != null) {
            biV2Dto.setFeelings(modelMapper.map(bi.getFeeling(), BIFeelingDto.class));
        }
        if (bi.getStatus() != null) {
            biV2Dto.setStatus(modelMapper.map(bi.getStatus(), BIStatusDto.class));
        }
        biV2Dto.setAuthor(modelMapper.map(authorDto, AuthorDto.class));
        return biV2Dto;
    }

    private List<BIParticipantsDto> mapBIParticipants(List<BIParticipants> participants) {
        return participants.stream().map(participant -> {
            BIParticipantsDto participantDto = modelMapper.map(participant, BIParticipantsDto.class);
            participantDto.setDescr(participant.getDescr());
            participantDto.setValue(participant.getValue());
            participantDto.setParticipant(new BIParticipantDto(participant.getParticipantEnum().getId(),
                    participant.getParticipantEnum().getName()));
            return participantDto;
        }).collect(Collectors.toList());
    }

    private List<BiStepDto> createBiStep(List<BiStep> biSteps) {
        List<BiStepDto> result = new ArrayList<>();
        biSteps.forEach(biStep -> {
            BiStepDto build = BiStepDto.builder()
                    .id(biStep.getId())
                    .name(biStep.getName())
                    .latency(biStep.getLatency())
                    .errorRate(biStep.getErrorRate())
                    .rps(biStep.getRps())
                    .relations(createRelations(biStep.getId()))
                    .build();
            result.add(build);
        });
        return result;
    }

    private List<RelationDto> createRelations(Integer biStepId) {
        List<RelationDto> result = new ArrayList<>();
        List<BiStepRelation> biStepRelations = biStepRelationRepository.findByBiStepId(biStepId);
        if (biStepRelations.isEmpty()) {
            return new ArrayList<>();
        } else {
            List<GetProductsByIdsDTO> allProducts = allProductsById(biStepRelations);
            Map<Integer, GetProductsByIdsDTO> productsIdsMap = allProducts.stream().collect(Collectors.toMap(
                    GetProductsByIdsDTO::getId,
                    product -> product));
            Map<Integer, TcDTO> tcDTOMap = createTcDTOMap(biStepRelations);
            Map<String, List<ProductInterfaceDTO>> interfacesByAlias = loadInterfacesByAlias(allProducts);
            biStepRelations.forEach(biStepRelation -> {
                List<OperationDTO> operations = new ArrayList<>();
                List<ProductInterfaceDTO> productInterfaceDTOS = new ArrayList<>();
                OperationDTO operationDTO = null;
                ProductInterfaceDTO productInterface = null;
                TcDTO tcDTO = null;
                GetProductsByIdsDTO product = null;
                tcDTO = tcDTOMap.get(biStepRelation.getTcId());
                product = productsIdsMap.get(biStepRelation.getProductId());
                if (product != null && product.getAlias() != null) {
                    productInterfaceDTOS = interfacesByAlias.get(product.getAlias());
                    log.info("productClient.getProductsFromStructurizr: product Alias = {}", product.getAlias());
                }
                if (productInterfaceDTOS != null) {
                    log.info("productInterfaceDTOS != null поиск интервейса с id: {}", biStepRelation.getInterfaceId());
                    productInterface = productInterfaceDTOS.stream()
                            .filter(obj -> obj.getId().equals(biStepRelation.getInterfaceId()))
                            .findFirst()
                            .orElse(null);
                }
                if (productInterface != null && productInterface.getOperations() != null) {
                    operations = productInterface.getOperations();
                    operationDTO = operations.stream()
                            .filter(obj -> obj.getId().equals(biStepRelation.getOperationId()))
                            .findFirst()
                            .orElse(null);
                } else {
                    log.info("productInterface == null");
                }
                RelationDto build = buildRelationDto(biStepRelation, operationDTO, productInterface, tcDTO, product);
                result.add(build);
            });
        }
        return result;
    }

    private List<GetProductsByIdsDTO> allProductsById(List<BiStepRelation> biStepRelations) {
        List<Integer> productIds = biStepRelations.stream()
                .map(BiStepRelation::getProductId).filter(Objects::nonNull).toList();
        log.info("Все id продуктов: {}", productIds);
        return productClient.getProductsByIds(productIds);
    }

    private Map<Integer, TcDTO> createTcDTOMap(List<BiStepRelation> biStepRelations) {
        List<Integer> tcIds = biStepRelations.stream().map(BiStepRelation::getTcId).filter(Objects::nonNull).toList();
        log.info("Все id TcId: {}", tcIds);
        List<TcDTO> allTcDTOS = capabilityClient.getTcs(tcIds);
        return allTcDTOS.stream().collect(Collectors.toMap(
                TcDTO::getId,
                tc -> tc));
    }

    private Map<String, List<ProductInterfaceDTO>> loadInterfacesByAlias(List<GetProductsByIdsDTO> allProducts) {
        Set<String> aliases = allProducts.stream()
                .map(GetProductsByIdsDTO::getAlias)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, List<ProductInterfaceDTO>> result = new HashMap<>();
        for (String alias : aliases) {
            List<ProductInterfaceDTO> list = productClient.getProductsFromStructurizr(alias);
            result.put(alias, list);
        }
        return result;
    }

    private RelationDto buildRelationDto(BiStepRelation biStepRelation, OperationDTO operationDTO,
                                         ProductInterfaceDTO productInterface, TcDTO tcDTO, GetProductsByIdsDTO product) {
        return RelationDto.builder()
                .id(biStepRelation.getId())
                .description(biStepRelation.getDescription())
                .productId(biStepRelation.getProductId())
                .tcId(biStepRelation.getTcId())
                .operationId(biStepRelation.getOperationId())
                .operation(operationDTO != null ? operationDTO.getType() + " " + operationDTO.getName() : null)
                .interfaceId(productInterface != null ? productInterface.getId() : null)
                .interfaceName(productInterface != null ? productInterface.getName() : null)
                .interfaceCode(productInterface != null ? productInterface.getCode() : null)
                .userId(biStepRelation.getUserId())
                .order(biStepRelation.getOrder())
                .tcName(tcDTO != null ? tcDTO.getName() : null)
                .tcCode(tcDTO != null ? tcDTO.getCode() : null)
                .productName(product != null ? product.getName() : null)
                .productAlias(product != null ? product.getAlias() : null)
                .build();
    }
}