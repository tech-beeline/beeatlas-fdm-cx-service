package ru.beeline.cxbackend.dto;

import lombok.Data;


@Data
public class BIParticipantsDto {
    private String value;
    private String descr;
    private BIParticipantDto participant;
}
