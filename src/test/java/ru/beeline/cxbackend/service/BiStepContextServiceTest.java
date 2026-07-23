/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.bi.BIInCJStep;
import ru.beeline.cxbackend.domain.bi.BiStep;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.domain.cj.CJStep;
import ru.beeline.cxbackend.dto.bistep.BiStepContextDto;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.repository.BIInCJStepRepository;
import ru.beeline.cxbackend.repository.BiStepRepository;
import ru.beeline.cxbackend.repository.BusinessInteractionRepository;
import ru.beeline.cxbackend.repository.CJRepository;
import ru.beeline.cxbackend.repository.CJStepRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BiStepContextServiceTest {

    private static final String CODE = "Step.00.00.02.77";

    @Mock
    private BiStepRepository biStepRepository;
    @Mock
    private BusinessInteractionRepository businessInteractionRepository;
    @Mock
    private BIInCJStepRepository biInCJStepRepository;
    @Mock
    private CJStepRepository cjStepRepository;
    @Mock
    private CJRepository cjRepository;

    @InjectMocks
    private BiStepContextService service;

    @Test
    void getByCode_whenBiStepNotFound_throws404() {
        when(biStepRepository.findByUniqueIdent(CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByCode(CODE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("bi-step с code = " + CODE + " не найден");
    }

    @Test
    void getByCode_whenBiDeletedOrMissing_throws404() {
        when(biStepRepository.findByUniqueIdent(CODE)).thenReturn(Optional.of(
                BiStep.builder().id(277).uniqueIdent(CODE).name("Шаг").biId(423L).build()
        ));
        when(businessInteractionRepository.findByIdAndDeletedDateIsNull(423L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByCode(CODE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("BI с id = 423 не найден");
    }

    @Test
    void getByCode_whenNoCjLinks_returnsEmptyCj() {
        when(biStepRepository.findByUniqueIdent(CODE)).thenReturn(Optional.of(
                BiStep.builder().id(277).uniqueIdent(CODE).name("Шаг").biId(423L).build()
        ));
        when(businessInteractionRepository.findByIdAndDeletedDateIsNull(423L)).thenReturn(Optional.of(
                BI.builder().id(423L).uniqueIdent("BI.00.00.04.23").name("BI name").build()
        ));
        when(biInCJStepRepository.findBIInCJStepsByBiId(423L)).thenReturn(List.of());

        BiStepContextDto result = service.getByCode(CODE);

        assertThat(result.getBiStep().getId()).isEqualTo(277);
        assertThat(result.getBiStep().getUid()).isEqualTo(CODE);
        assertThat(result.getBi().getId()).isEqualTo(423L);
        assertThat(result.getCj()).isEmpty();
        verify(cjStepRepository, never()).findAllByIdIn(anyCollection());
    }

    @Test
    void getByCode_returnsBiAndUniqueSortedCj() {
        when(biStepRepository.findByUniqueIdent(CODE)).thenReturn(Optional.of(
                BiStep.builder().id(277).uniqueIdent(CODE).name("Шаг").biId(423L).build()
        ));
        when(businessInteractionRepository.findByIdAndDeletedDateIsNull(423L)).thenReturn(Optional.of(
                BI.builder().id(423L).uniqueIdent("BI.00.00.04.23").name("BI name").build()
        ));
        when(biInCJStepRepository.findBIInCJStepsByBiId(423L)).thenReturn(List.of(
                BIInCJStep.builder().cjStepId(100L).biId(423L).build(),
                BIInCJStep.builder().cjStepId(101L).biId(423L).build(),
                BIInCJStep.builder().cjStepId(102L).biId(423L).build()
        ));
        when(cjStepRepository.findAllByIdIn(any())).thenReturn(List.of(
                CJStep.builder().id(100L).cjId(350L).build(),
                CJStep.builder().id(101L).cjId(346L).build(),
                CJStep.builder().id(102L).cjId(346L).build()
        ));
        when(cjRepository.findAllByIdInAndDeletedDateIsNull(any())).thenReturn(List.of(
                CJ.builder().id(350L).uniqueIdent("CJ.350").name("CJ 350").bDraft(true).build(),
                CJ.builder().id(346L).uniqueIdent("CJ.346").name("CJ 346").bDraft(false).build()
        ));

        BiStepContextDto result = service.getByCode(CODE);

        assertThat(result.getCj()).hasSize(2);
        assertThat(result.getCj().get(0).getId()).isEqualTo(346L);
        assertThat(result.getCj().get(0).getUid()).isEqualTo("CJ.346");
        assertThat(result.getCj().get(0).isBDraft()).isFalse();
        assertThat(result.getCj().get(1).getId()).isEqualTo(350L);
        assertThat(result.getCj().get(1).isBDraft()).isTrue();
    }
}
