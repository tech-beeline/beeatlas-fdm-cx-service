package ru.beeline.cxbackend.domain.cj;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "cj_link", schema = "cx")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CJLink {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "url", length = 4000, nullable = false)
    private String url;

    @Column(name = "descr", length = 50)
    private String descr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cj", nullable = false, foreignKey = @ForeignKey(name = "fk_cj_link_cj_id"))
    private CJ cj;
}