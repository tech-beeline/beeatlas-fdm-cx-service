/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain.cj;

import lombok.*;
import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Builder
@Getter
@Setter
@ToString(exclude = {"cjs", "tags"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cj_tags", schema = "cx")
public class CJTag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cj_tag_id_seq")
    @SequenceGenerator(name = "cj_tag_id_seq", sequenceName = "cx.cj_tag_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<CJ> cjs = new HashSet<>();

    public void addCJ(CJ cj) {
        cjs.add(cj);
        cj.getTags().add(this);
    }
}