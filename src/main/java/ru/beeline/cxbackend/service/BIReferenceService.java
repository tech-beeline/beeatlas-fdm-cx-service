/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.cxbackend.domain.bi.ref.BIChannel;
import ru.beeline.cxbackend.domain.bi.ref.BIFeeling;
import ru.beeline.cxbackend.domain.bi.ref.BIParticipant;
import ru.beeline.cxbackend.domain.bi.ref.BIStatus;
import ru.beeline.cxbackend.repository.BIChannelRepository;
import ru.beeline.cxbackend.repository.BIFeelingRepository;
import ru.beeline.cxbackend.repository.BIParticipantRepository;
import ru.beeline.cxbackend.repository.BIStatusRepository;

import java.util.List;

@Service
public class BIReferenceService {

    @Autowired
    private BIFeelingRepository biFeelingRepository;

    @Autowired
    private BIStatusRepository biStatusRepository;

    @Autowired
    private BIChannelRepository biChannelRepository;

    @Autowired
    private BIParticipantRepository biParticipantRepository;

    public List<BIFeeling> getFeelings() {
        return biFeelingRepository.findAll();
    }

    public List<BIStatus> getStatus() {
        return biStatusRepository.findAll();
    }

    public List<BIChannel> getChannels() {
        return biChannelRepository.findAll();
    }

    public List<BIParticipant> getParticipants() {
        return biParticipantRepository.findAll();
    }
}
