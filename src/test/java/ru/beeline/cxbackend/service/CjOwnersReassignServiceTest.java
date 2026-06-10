package ru.beeline.cxbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.beeline.cxbackend.client.UserClient;
import ru.beeline.cxbackend.dto.owners.AffectedCjDto;
import ru.beeline.cxbackend.dto.owners.CjOwnersReassignRequestDto;
import ru.beeline.cxbackend.dto.owners.CjOwnersReassignResponseDto;
import ru.beeline.cxbackend.repository.CjOwnersReassignRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CjOwnersReassignServiceTest {

    @Mock
    private UserClient userClient;
    @Mock
    private CjOwnersReassignRepository reassignRepository;

    @InjectMocks
    private CjOwnersReassignService service;

    @Test
    void reassignOwners_invokesDeleteDuplicateTechOwnersBeforeTechUpdate() {
        CjOwnersReassignRequestDto body = new CjOwnersReassignRequestDto();
        body.setCurrentUserId(10L);
        body.setNewUserId(20L);

        when(userClient.userExists(10L)).thenReturn(true);
        when(userClient.userExists(20L)).thenReturn(true);
        List<AffectedCjDto> affected = List.of(AffectedCjDto.builder().cjId(1L).cjName("CJ1").build());
        when(reassignRepository.findAffectedCj(10L)).thenReturn(affected);
        when(reassignRepository.reassignBusinessOwner(10L, 20L)).thenReturn(1);
        when(reassignRepository.deleteDuplicateTechOwnersForReassign(10L, 20L)).thenReturn(1);
        when(reassignRepository.reassignTechOwner(10L, 20L)).thenReturn(0);

        CjOwnersReassignResponseDto response = service.reassignOwners(body, 0l);

        assertThat(response.getCurrentUserId()).isEqualTo(10L);
        assertThat(response.getNewUserId()).isEqualTo(20L);
        assertThat(response.getAffectedCj()).isEqualTo(affected);

        InOrder inOrder = inOrder(reassignRepository);
        inOrder.verify(reassignRepository).findAffectedCj(10L);
        inOrder.verify(reassignRepository).reassignBusinessOwner(10L, 20L);
        inOrder.verify(reassignRepository).deleteDuplicateTechOwnersForReassign(10L, 20L);
        inOrder.verify(reassignRepository).reassignTechOwner(10L, 20L);
    }
}
