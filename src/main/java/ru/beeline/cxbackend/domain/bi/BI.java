/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain.bi;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.Where;
import ru.beeline.cxbackend.domain.bi.ref.BIFeeling;
import ru.beeline.cxbackend.domain.bi.ref.BIStatus;
import javax.persistence.*;
import java.sql.Date;
import java.util.List;

import static ru.beeline.cxbackend.utils.Constant.DATE_FORMAT;
import static ru.beeline.cxbackend.utils.Constant.DATE_TIMEZONE;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "business_iteraction")
public class BI {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "buisness_iteraction_id_generator")
    @SequenceGenerator(name = "buisness_iteraction_id_generator", sequenceName = "BI_id_seq", allocationSize = 1)
    private Long id;


    @Column(name = "unique_ident")
    @JsonIgnore
    private String uniqueIdent;

    @Column(name = "name")
    private String name;

    private String descr;

    @Column(name = "last_modified_date")
    @JsonFormat(pattern = DATE_FORMAT, timezone = DATE_TIMEZONE)
    private Date lastModifiedDate;

    @Column(name = "created_date")
    @JsonFormat(pattern = DATE_FORMAT, timezone = DATE_TIMEZONE)
    private Date createdDate;

    @Column(name = "b_Communal")
    private boolean isCommunal = false;

    @Column(name = "b_target")
    private boolean isTarget = false;

    @Column(name = "b_draft")
    private boolean isDraft = false;

    @Column(name = "touchpoints")
    private String touchPoints;

    @ManyToOne
    @JoinColumn(name = "feelings")
    private BIFeeling feeling;

    @Column(name = "ea_guid")
    private String eaGuid;

    @Column(name = "id_product_ext")
    private Long productId;

    @Column(name = "owner_role")
    private String ownerRole;

    @Column(name = "metrics")
    private String metrics;

    @Column(name = "deleted_date")
    private Date deletedDate;

    @Column(name = "author_id")
    private Long authorId;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private BIStatus status;

    @Column(name = "client_scenario")
    private String clientScenario;

    @Column(name = "ucs_reaction")
    private String ucsReaction;

    @OneToMany
    @JoinColumn(name = "id_bi")
    private List<BIParticipants> participants;

    @ManyToMany
    @JoinTable(
            name = "bi_channel",
            joinColumns = @JoinColumn(name = "id_bi"),
            inverseJoinColumns = @JoinColumn(name = "id_channel")
    )
    private List<BIChannelEnum> channel;

    @OneToMany(mappedBy = "idBi", cascade = CascadeType.ALL)
    @Where(clause = "type_id = 1")
    private List<BILink> flowLink;

    @OneToMany(mappedBy = "idBi", cascade = CascadeType.ALL)
    @Where(clause = "type_id = 2")
    private List<BILink> document;

    @OneToMany(mappedBy = "idBi", cascade = CascadeType.ALL)
    @Where(clause = "type_id = 3")
    private List<BILink> mockupLink;

    @Column(name = "id_bpmn", length = 50, nullable = true)
    private String idBpmn;

    public boolean checkFieldsForNull() {

        if (getId() != null) {
            return false;
        }

        if (getUniqueIdent() != null) {
            return false;
        }

        if (getName() != null) {
            return false;
        }

        if (getDescr() != null) {
            return false;
        }

        if (getLastModifiedDate() != null) {
            return false;
        }

        if (getCreatedDate() != null) {
            return false;
        }

        if (getTouchPoints() != null) {
            return false;
        }

        if (getFeeling() != null) {
            return false;
        }

        if (getEaGuid() != null) {
            return false;
        }

        if (getProductId() != null) {
            return false;
        }

        if (getOwnerRole() != null) {
            return false;
        }

        if (getStatus() != null) {
            return false;
        }

        if (getClientScenario() != null) {
            return false;
        }

        if (getUcsReaction() != null) {
            return false;
        }

        if (getParticipants() != null) {
            return false;
        }

        if (getChannel() != null) {
            return false;
        }

        if (getFlowLink() != null) {
            return false;
        }

        if (getDocument() != null) {
            return false;
        }

        if (getMockupLink() != null) {
            return false;
        }

        if (getMetrics() != null) {
            return false;
        }

        return true;
    }
}
