/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.beeline.cxbackend.client.ProductClient;
import ru.beeline.cxbackend.dto.e2e.CjE2eDto;
import ru.beeline.cxbackend.dto.product.E2eCardDto;
import ru.beeline.cxbackend.repository.CJRepository;
import ru.beeline.cxbackend.repository.projection.CjAlertsFlatRow;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CjE2eServiceTest {

    @Mock
    private ProductClient productClient;
    @Mock
    private CJRepository cjRepository;

    @InjectMocks
    private CjE2eService service;

    @Test
    void getCjLinkedToE2e_whenProductE2eEmpty_returnsEmpty() {
        when(productClient.getE2eWithBiStep()).thenReturn(List.of());

        assertThat(service.getCjLinkedToE2e()).isEmpty();
    }

    @Test
    void getCjLinkedToE2e_whenProductE2eNull_returnsEmpty() {
        when(productClient.getE2eWithBiStep()).thenReturn(null);

        assertThat(service.getCjLinkedToE2e()).isEmpty();
    }

    @Test
    void getCjLinkedToE2e_whenE2eWithoutBiStepCode_returnsEmpty() {
        when(productClient.getE2eWithBiStep()).thenReturn(List.of(
                E2eCardDto.builder().code("E2E_1").biStepCode(null).build(),
                E2eCardDto.builder().code("E2E_2").biStepCode("  ").build()
        ));

        assertThat(service.getCjLinkedToE2e()).isEmpty();
    }

    @Test
    void getCjLinkedToE2e_whenLocalRowsEmpty_returnsEmpty() {
        when(productClient.getE2eWithBiStep()).thenReturn(List.of(
                E2eCardDto.builder().code("E2E_1").biStepCode("STEP_A").build()
        ));
        when(cjRepository.findCjAlertsFlat()).thenReturn(List.of());

        assertThat(service.getCjLinkedToE2e()).isEmpty();
    }

    @Test
    void getCjLinkedToE2e_buildsTreeOnlyForMatchedSteps() {
        when(productClient.getE2eWithBiStep()).thenReturn(List.of(
                E2eCardDto.builder().code("E2E_1").biStepCode("STEP_A").build(),
                E2eCardDto.builder().code("E2E_2").biStepCode("STEP_C").build()
        ));
        when(cjRepository.findCjAlertsFlat()).thenReturn(List.of(
                row(1L, "CJ_UID", "Оплата", 10L, "BI_UID", "Оформить", "STEP_A", "Шаг A"),
                row(1L, "CJ_UID", "Оплата", 10L, "BI_UID", "Оформить", "STEP_B", "Шаг B"),
                row(1L, "CJ_UID", "Оплата", 20L, "BI_UID_2", "Оплатить", "STEP_C", "Шаг C"),
                row(2L, "CJ_UID_2", "Поддержка", 30L, "BI_UID_3", "Звонок", "STEP_D", "Шаг D")
        ));

        List<CjE2eDto> result = service.getCjLinkedToE2e();

        assertThat(result).hasSize(1);
        CjE2eDto cj = result.get(0);
        assertThat(cj.getId()).isEqualTo(1L);
        assertThat(cj.getName()).isEqualTo("Оплата");
        assertThat(cj.getBi()).hasSize(2);

        assertThat(cj.getBi().get(0).getId()).isEqualTo(10L);
        assertThat(cj.getBi().get(0).getBiSteps()).hasSize(1);
        assertThat(cj.getBi().get(0).getBiSteps().get(0).getUid()).isEqualTo("STEP_A");
        assertThat(cj.getBi().get(0).getBiSteps().get(0).getE2eCode()).isEqualTo("E2E_1");

        assertThat(cj.getBi().get(1).getId()).isEqualTo(20L);
        assertThat(cj.getBi().get(1).getBiSteps()).hasSize(1);
        assertThat(cj.getBi().get(1).getBiSteps().get(0).getUid()).isEqualTo("STEP_C");
        assertThat(cj.getBi().get(1).getBiSteps().get(0).getE2eCode()).isEqualTo("E2E_2");
    }

    @Test
    void getCjLinkedToE2e_deduplicatesSameBiStepUid() {
        when(productClient.getE2eWithBiStep()).thenReturn(List.of(
                E2eCardDto.builder().code("E2E_1").biStepCode("STEP_A").build()
        ));
        when(cjRepository.findCjAlertsFlat()).thenReturn(List.of(
                row(1L, "CJ_UID", "Оплата", 10L, "BI_UID", "Оформить", "STEP_A", "Шаг A"),
                row(1L, "CJ_UID", "Оплата", 10L, "BI_UID", "Оформить", "STEP_A", "Шаг A")
        ));

        List<CjE2eDto> result = service.getCjLinkedToE2e();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBi()).hasSize(1);
        assertThat(result.get(0).getBi().get(0).getBiSteps()).hasSize(1);
    }

    @Test
    void getCjLinkedToE2e_keepsFirstE2eCodeForSameBiStep() {
        when(productClient.getE2eWithBiStep()).thenReturn(List.of(
                E2eCardDto.builder().code("E2E_FIRST").biStepCode("STEP_A").build(),
                E2eCardDto.builder().code("E2E_SECOND").biStepCode("STEP_A").build()
        ));
        when(cjRepository.findCjAlertsFlat()).thenReturn(List.of(
                row(1L, "CJ_UID", "Оплата", 10L, "BI_UID", "Оформить", "STEP_A", "Шаг A")
        ));

        List<CjE2eDto> result = service.getCjLinkedToE2e();

        assertThat(result.get(0).getBi().get(0).getBiSteps().get(0).getE2eCode())
                .isEqualTo("E2E_FIRST");
    }

    private static CjAlertsFlatRow row(
            Long cjId,
            String cjUid,
            String cjName,
            Long biId,
            String biUid,
            String biName,
            String bsUid,
            String bsName
    ) {
        return new StubRow(cjId, cjUid, cjName, biId, biUid, biName, bsUid, bsName);
    }

    private static final class StubRow implements CjAlertsFlatRow {
        private final Long cjId;
        private final String cjUid;
        private final String cjName;
        private final Long biId;
        private final String biUid;
        private final String biName;
        private final String bsUid;
        private final String bsName;

        private StubRow(
                Long cjId,
                String cjUid,
                String cjName,
                Long biId,
                String biUid,
                String biName,
                String bsUid,
                String bsName
        ) {
            this.cjId = cjId;
            this.cjUid = cjUid;
            this.cjName = cjName;
            this.biId = biId;
            this.biUid = biUid;
            this.biName = biName;
            this.bsUid = bsUid;
            this.bsName = bsName;
        }

        @Override
        public Long getCjId() {
            return cjId;
        }

        @Override
        public String getCjName() {
            return cjName;
        }

        @Override
        public String getCjUniqueIdent() {
            return cjUid;
        }

        @Override
        public String getCjDashboardLink() {
            return null;
        }

        @Override
        public Long getBiId() {
            return biId;
        }

        @Override
        public String getBiUniqueIdent() {
            return biUid;
        }

        @Override
        public String getBiName() {
            return biName;
        }

        @Override
        public String getBiDescr() {
            return null;
        }

        @Override
        public Integer getBsIdStepType() {
            return null;
        }

        @Override
        public String getBsUniqueIdent() {
            return bsUid;
        }

        @Override
        public String getBsName() {
            return bsName;
        }

        @Override
        public Float getBsLatency() {
            return null;
        }

        @Override
        public Float getBsRps() {
            return null;
        }

        @Override
        public Float getBsErrorRate() {
            return null;
        }

        @Override
        public Integer getBsId() {
            return null;
        }
    }
}
