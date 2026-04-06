/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.cj.CJStep;
import ru.beeline.cxbackend.dto.*;

@Component
public class CjStepMapper {
    private final ModelMapper modelMapper;

    public CjStepMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public CjStepFullDto cjStepToSjStepFullDto(CJStep cjStep) {
        if (cjStep == null) {
            return null;
        }
        return modelMapper.map(cjStep, CjStepFullDto.class);
    }
}
