/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import lombok.Data;


@Data
public class BIParticipantsDto {

    private String value;
    private String descr;
    private BIParticipantDto participant;
}
