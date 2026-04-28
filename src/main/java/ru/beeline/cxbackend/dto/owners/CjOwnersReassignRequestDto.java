package ru.beeline.cxbackend.dto.owners;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;

/**
 * newUserId может быть null, но атрибут должен присутствовать в JSON.
 * Поэтому DTO хранит флаги "передан ли атрибут".
 */
@Getter
public class CjOwnersReassignRequestDto {
    private Long currentUserId;
    private Long newUserId;

    @JsonIgnore
    private boolean currentUserIdProvided;

    @JsonIgnore
    private boolean newUserIdProvided;

    @JsonSetter("currentUserId")
    public void setCurrentUserId(Long currentUserId) {
        this.currentUserIdProvided = true;
        this.currentUserId = currentUserId;
    }

    @JsonSetter("newUserId")
    public void setNewUserId(Long newUserId) {
        this.newUserIdProvided = true;
        this.newUserId = newUserId;
    }
}

