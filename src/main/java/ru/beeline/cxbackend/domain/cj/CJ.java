/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain.cj;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static ru.beeline.cxbackend.utils.Constant.DATE_FORMAT;
import static ru.beeline.cxbackend.utils.Constant.DATE_TIMEZONE;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "cj")
public class CJ {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cj_id_generator")
    @SequenceGenerator(name = "cj_id_generator", sequenceName = "cj_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Column(name = "user_portrait")
    @JsonProperty("user_portrait")
    private String userPortrait;

    @Column(name = "last_modified_date")
    @JsonFormat(pattern = DATE_FORMAT, timezone = DATE_TIMEZONE)
    private Date lastModifiedDate;

    @Column(name = "deleted_date")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Date deletedDate;

    @Column(name = "created_date")
    @JsonFormat(pattern = DATE_FORMAT, timezone = DATE_TIMEZONE)
    private Date createdDate;

    @Column(name = "b_draft")
    @JsonProperty("draft")
    private boolean bDraft = true;

    @Column(name = "id_author")
    @JsonProperty("id_user_profile")
    private Long authorId;

    @Column(name = "id_product_ext")
    @JsonProperty("id_product")
    private Long idProductExt;

    @Column(name = "unique_ident")
    private String uniqueIdent;

    @Column(name = "dashboard_link")
    @JsonProperty("dashboard_link")
    private String dashboardLink;

    @Column(name = "bpmn")
    private Boolean bpmn;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "cj_tags_relations",
            schema = "cx",
            joinColumns = @JoinColumn(name = "id_cj"),
            inverseJoinColumns = @JoinColumn(name = "id_tag")
    )
    private Set<CJTag> tags = new HashSet<>();
}
