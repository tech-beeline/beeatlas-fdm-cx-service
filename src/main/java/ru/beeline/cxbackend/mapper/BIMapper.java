package ru.beeline.cxbackend.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.bi.BIParticipants;
import ru.beeline.cxbackend.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BIMapper {

    @Autowired
    ModelMapper modelMapper;


    public List<BIDto> biToBIDto(List<BI> biList) {
        if (biList != null) {
            return biList.stream().map(this::biToBIDto).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public BIDto biToBIDto(BI bi) {
        BIDto biDto = modelMapper.map(bi, BIDto.class);
        biDto.setParticipants(mapBIParticipants(bi.getParticipants()));
        biDto.setFeelings(modelMapper.map(bi.getFeeling(), BIFeelingDto.class));
        biDto.setStatus(modelMapper.map(bi.getStatus(), BIStatusDto.class));

        return biDto;
    }

    private List<BIParticipantsDto> mapBIParticipants(List<BIParticipants> participants) {
        return participants.stream()
                .map(participant -> {
                    BIParticipantsDto participantDto = modelMapper.map(participant, BIParticipantsDto.class);
                    participantDto.setDescr(participant.getDescr());
                    participantDto.setValue(participant.getValue());
                    participantDto.setParticipant(new BIParticipantDto(participant.getParticipantEnum().getId(), participant.getParticipantEnum().getName()));
                    return participantDto;
                })
                .collect(Collectors.toList());
    }
}