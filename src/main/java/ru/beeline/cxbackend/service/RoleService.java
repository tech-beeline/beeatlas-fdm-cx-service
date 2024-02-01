package ru.beeline.cxbackend.service;

import org.springframework.stereotype.Service;
import ru.beeline.cxbackend.domain.Permission;
import ru.beeline.cxbackend.domain.Role;
import ru.beeline.cxbackend.domain.RolePermissions;
import ru.beeline.cxbackend.domain.UserProfile;
import ru.beeline.cxbackend.dto.PermissionDto;
import ru.beeline.cxbackend.dto.RoleDto;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

    public List<Role> getAllRoles() {
        return null;
    }

    public Role createRole(RoleDto role) {
        return null;
    }

    public boolean checkNameIsUnique(String name) {
        return false;
    }

    public Role updateRole(Long id, RoleDto role) {
        return null;
    }

    public Optional<Role> findRole(Long id) {
        return null;
    }

    public Role findRoleByName(String name) {
        return null;
    }

    public void delete(Role role) {
    }

    public List<Permission> getPermissions(Long roleId) {
        return null;
    }

    public List<PermissionDto> getPermissionsWithStatus(Long roleId) {
        return null;
    }

    public List<RolePermissions> getRolePermissions(Long roleId) {
        return null;
    }

    public void saveRolePermissions(Role role, List<Permission> permissions) {

    }

    public void saveRolesByIds(UserProfile userProfile, List<Long> ids) {
    }

    public void deleteAllByUserProfileId(Long id) {
    }
}
