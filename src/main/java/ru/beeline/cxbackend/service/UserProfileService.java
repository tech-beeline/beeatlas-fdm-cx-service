package ru.beeline.cxbackend.service;

import org.springframework.stereotype.Service;
import ru.beeline.cxbackend.domain.UserProfile;
import ru.beeline.cxbackend.dto.RoleDto;
import ru.beeline.cxbackend.dto.UserProfileDto;
import ru.beeline.cxbackend.utils.jwt.JwtUserData;

import java.util.List;
import java.util.Optional;

@Service
public class UserProfileService {


    public List<UserProfile> getAllUsers() {
        return null;
    }

    public UserProfileDto createUserProfileDto(UserProfileDto userProfileDto) {
        return null;
    }

    public UserProfile createUser(JwtUserData userData) {
        return null;
    }

    public UserProfile createUserProfile(UserProfileDto userProfileDto) {
        return null;
    }

    public Optional<UserProfile> findProfileById(Long id) {
        return null;
    }

    public UserProfile findProfileByLogin(String login) {
        return null;
    }

    public UserProfile findProfileByEmail(String email) {
        return null;
    }

    public Long hasLinkProductIdWithProfileId(Long profileId, String productId) {
        return null;
    }

    public UserProfileDto editUserProfile(UserProfile userProfile, UserProfileDto userProfileDto) {
        return null;
    }

    public UserProfileDto setRoles(UserProfile userProfile, List<RoleDto> roles) {
        return null;
    }

    public void updateLastLogin(UserProfile userProfile) {
    }

    public void validateAccessProduct(String bearerToken, String productId) {
    }
}
