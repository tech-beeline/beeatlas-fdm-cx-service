package ru.beeline.cxbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.cxbackend.client.UserClient;
import ru.beeline.cxbackend.dto.owners.AffectedCjDto;
import ru.beeline.cxbackend.dto.owners.CjOwnersReassignRequestDto;
import ru.beeline.cxbackend.dto.owners.CjOwnersReassignResponseDto;
import ru.beeline.cxbackend.exception.BadRequestException;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.repository.CjOwnersReassignRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CjOwnersReassignService {
    private final UserClient userClient;
    private final CjOwnersReassignRepository reassignRepository;

    @Transactional
    public CjOwnersReassignResponseDto reassignOwners(CjOwnersReassignRequestDto body) {
        validateAndParse(body);


        boolean currentExists = userClient.userExists(body.getCurrentUserId());
        if (!currentExists) {
            log.warn("cj owners reassign: current user not found. currentUserId={}", body.getCurrentUserId());
            throw new NotFoundException("Указанный текущий ответственный, не найден");
        }

        boolean newExists = body.getNewUserId() == null || userClient.userExists(body.getNewUserId());
        if (!newExists) {
            log.warn("cj owners reassign: new user not found. newUserId={}", body.getNewUserId());
            throw new NotFoundException("Указанный новый ответственный, не найден");
        }

        List<AffectedCjDto> affected = reassignRepository.findAffectedCj(body.getCurrentUserId());

        int businessUpdated = reassignRepository.reassignBusinessOwner(body.getCurrentUserId(), body.getNewUserId());
        int techDeleted = reassignRepository.deleteDuplicateTechOwnersForReassign(body.getCurrentUserId(), body.getNewUserId());
        int techUpdated = reassignRepository.reassignTechOwner(body.getCurrentUserId(), body.getNewUserId());

        CjOwnersReassignResponseDto response = CjOwnersReassignResponseDto.builder()
                .currentUserId(body.getCurrentUserId())
                .newUserId(body.getNewUserId())
                .affectedCj(affected)
                .build();

        log.info("cj owners reassigned. currentUserId={}, newUserId={}, businessUpdated={}, techDeleted={}, techUpdated={}, response={}",
                body.getCurrentUserId(), body.getNewUserId(), businessUpdated, techDeleted, techUpdated, response);
        return response;
    }

    private void validateAndParse(CjOwnersReassignRequestDto body) {
        if (body == null || !body.isCurrentUserIdProvided() || !body.isNewUserIdProvided()) {
            log.warn("cj owners reassign bad request: missing attributes. body={}", body);
            throw new BadRequestException("Не переданы необходимые атрибуты");
        }

        if (body.getCurrentUserId() == null) {
            log.warn("cj owners reassign bad request: currentUserId null. body={}", body);
            throw new BadRequestException("Не переданы необходимые атрибуты");
        }

        Long currentUserId = body.getCurrentUserId();
        Long newUserId = body.getNewUserId();

        if (newUserId != null && newUserId.equals(currentUserId)) {
            log.warn("cj owners reassign bad request: currentUserId == newUserId == {}", currentUserId);
            throw new BadRequestException("Текущий и новый ответственный, должны быть разными");
        }

    }
}

