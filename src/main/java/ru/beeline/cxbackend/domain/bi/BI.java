package ru.beeline.cxbackend.domain.bi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.Where;
import ru.beeline.cxbackend.domain.bi.ref.BIFeeling;
import ru.beeline.cxbackend.domain.bi.ref.BIStatus;

import javax.persistence.*;
import java.sql.Date;
import java.util.List;

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

    @Column(name = "dt_updated")
    private Date dtUpdated;

    @Column(name = "dt_created")
    private Date dtCreated;

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

}
