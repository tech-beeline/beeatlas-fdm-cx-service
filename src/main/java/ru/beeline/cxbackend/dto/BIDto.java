package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.beeline.cxbackend.domain.bi.BIChannelEnum;
import ru.beeline.cxbackend.domain.bi.BILink;

import java.sql.Date;
import java.util.List;

import static ru.beeline.cxbackend.utils.Constant.DATE_FORMAT;
import static ru.beeline.cxbackend.utils.Constant.DATE_TIMEZONE;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BIDto {
    private Long id;
    private String uniqueIdent;
    private String name;
    private String descr;
    private boolean isCommunal = false;
    private boolean isTarget = false;
    private boolean isDraft = false;
    private String touchPoints;
    private BIFeelingDto feelings;
    private String eaGuid;
    private Long productId;
    private String ownerRole;
    private String metrics;
    @JsonProperty("author_id")
    private Long authorId;
    @JsonProperty("created_date")
    @JsonFormat(pattern = DATE_FORMAT, timezone = DATE_TIMEZONE)
    private Date createdDate;
    @JsonProperty("last_modified_date")
    @JsonFormat(pattern = DATE_FORMAT, timezone = DATE_TIMEZONE)
    private Date lastModifiedDate;
    private BIStatusDto status;
    private String clientScenario;
    private String ucsReaction;
    private List<BIParticipantsDto> participants;
    private List<BIChannelEnum> channel;
    private List<BILink> flowLink;
    private List<BILink> document;
    private List<BILink> mockupLink;
}
