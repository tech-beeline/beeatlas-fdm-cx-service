package ru.beeline.cxbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.beeline.cxbackend.domain.bi.BIChannelEnum;
import ru.beeline.cxbackend.domain.bi.BILink;

import java.sql.Date;
import java.util.List;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BIDto {
    private Long id;
    private String uniqueIdent;
    private String name;
    private String descr;
    private Date dtUpdated;
    private Date dtCreated;
    private boolean isCommunal = false;
    private boolean isTarget = false;
    private boolean isDraft = false;
    private String touchPoints;
    private BIFeelingDto feelings;
    private String eaGuid;
    private Long productId;
    private String ownerRole;
    private BIStatusDto status;
    private String clientScenario;
    private String ucsReaction;
    private List<BIParticipantsDto> participants;
    private List<BIChannelEnum> channel;
    private List<BILink> flowLink;
    private List<BILink> document;
    private List<BILink> mockupLink;
}
