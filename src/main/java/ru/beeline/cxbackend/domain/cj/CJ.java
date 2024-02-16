package ru.beeline.cxbackend.domain.cj;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import java.sql.Date;

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

    @Column(name = "last_updated")
    @JsonProperty("last_updated")
    private Date lastUpdated;

    @Column(name = "b_draft")
    @JsonProperty("draft")
    private boolean bDraft = true;

    @Column(name = "id_author")
    @JsonProperty("id_user_profile")
    private Long authorId;

    @Column(name = "id_product_ext")
    @JsonProperty("id_product")
    private Long idProductExt;
}
