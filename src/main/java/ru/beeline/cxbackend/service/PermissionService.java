package ru.beeline.cxbackend.service;

import org.springframework.stereotype.Service;
import ru.beeline.cxbackend.domain.Permission;
import ru.beeline.cxbackend.dto.PermissionDto;

import java.util.List;
import java.util.Set;

@Service
public class PermissionService {

    public List<Permission> getAllPermissions() {
        return null;
    }

    public List<Permission> findAllByIds(List<Long> ids) {
        return null;
    }

    public Set<PermissionDto> getUserPermissions(Set<Permission> rolePermissions) {
        return null;
    }
}
