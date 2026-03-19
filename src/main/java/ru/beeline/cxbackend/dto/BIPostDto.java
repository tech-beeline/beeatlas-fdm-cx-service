/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.cxbackend.domain.bi.BIChannelEnum;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BIPostDto {

    private Long id;
    private String name;
    private String descr;
    private Boolean communal;
    private Boolean target;
    private Boolean draft;
    private String touchPoints;
    private BIFeelingDto feeling;
    private String eaGuid;
    private Long productId;
    private String ownerRole;
    private String metrics;
    private Long authorId;
    private StatusDto status;
    private String clientScenario;
    private String ucsReaction;
    private List<ParticipantDto> participants;
    private List<BIChannelEnum> channel;
    private List<BILinkDto> flowLink;
    private List<BILinkDto> document;
    private List<BILinkDto> mockupLink;
}