/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain.cj;

import lombok.*;

import javax.persistence.*;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "cj_parameters_view")
public class CJParametersView {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cj_parameters_view_id_generator")
    @SequenceGenerator(name = "cj_parameters_view_id_generator", sequenceName = "cj_parameters_view_id_seq", allocationSize = 1)
    private Long id;

    // Флаг отображения даты последнего изменения
    @Column(name = "b_show_dt_updated")
    private boolean bShowDtUpdated;

    @Column(name = "id_user")
    private Long userId;

    // Флаг отображения статуса
    @Column(name = "b_show_status")
    private boolean bShowStatus;

    // Флаг отображения типа
    @Column(name = "b_show_type")
    private boolean bShowType;


    // Флаг отображения признака Коммунальный/не коммунальный
    @Column(name = "b_show_communal")
    private boolean bShowCommunal;

    // Флаг отображения точки касания
    @Column(name = "b_show_touchpoint")
    private boolean bShowTouchpoint;

    // Флаг отображения имени CJ
    @Column(name = "b_show_name")
    private boolean bShowName;

    // Флаг отображения описания / summary
    @Column(name = "b_show_descr")
    private boolean bShowDescr;

    // Флаг отображения участников
    @Column(name = "b_show_participants")
    private boolean bShowParticipants;

    // Флаг отображения ценности
    @Column(name = "b_show_value")
    private boolean bShowValue;

    // Флаг отображения чувств и эмоций
    @Column(name = "b_show_feelings")
    private boolean bShowFeelings;

    // Флаг отображения каналов
    @Column(name = "b_show_channels")
    private boolean bShowChannels;

    // Флаг отображения документов
    @Column(name = "b_show_doc")
    private boolean bShowDoc;

    // Флаг отображения макетов
    @Column(name = "b_show_mocks")
    private boolean bShowMocks;

    // Флаг отображения идентификатора
    @Column(name = "b_show_id")
    private boolean bShowId;

    // Флаг отображения ea_guid
    @Column(name = "b_show_guid")
    private boolean bShowGuid;

    @Column(name = "id_cj")
    private Long cjId;
}
