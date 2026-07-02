package com.example.template.auth.service;

import com.example.template.auth.dto.RoleCreateRequest;
import com.example.template.auth.dto.RoleItem;
import com.example.template.auth.repository.SysRoleRepository;
import com.example.template.auth.service.impl.AuthRoleAdminServiceImpl;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRoleAdminServiceTest {

    @Mock
    private SysRoleRepository sysRoleRepository;

    private AuthRoleAdminService authRoleAdminService;

    @BeforeEach
    void setUp() {
        authRoleAdminService = new AuthRoleAdminServiceImpl(sysRoleRepository);
    }

    @Test
    void createRole_success() {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleCode("operator");
        request.setRoleName("运营人员");
        when(sysRoleRepository.existsActiveByCode("OPERATOR")).thenReturn(false);
        when(sysRoleRepository.insert(eq("OPERATOR"), eq("运营人员"))).thenReturn(10L);

        RoleItem item = authRoleAdminService.createRole(request);

        assertEquals(10L, item.getRoleId());
        assertEquals("OPERATOR", item.getRoleCode());
        verify(sysRoleRepository).insert("OPERATOR", "运营人员");
    }

    @Test
    void createRole_reservedCode() {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleCode("SUPER_ADMIN");
        request.setRoleName("超管");

        BusinessException ex = assertThrows(BusinessException.class, () -> authRoleAdminService.createRole(request));
        assertEquals(ResultCode.AUTH_ROLE_CODE_RESERVED.getCode(), ex.getCode());
    }

    @Test
    void createRole_duplicateCode() {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleCode("ADMIN");
        request.setRoleName("管理员");
        when(sysRoleRepository.existsActiveByCode(anyString())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authRoleAdminService.createRole(request));
        assertEquals(ResultCode.AUTH_ROLE_CODE_EXISTS.getCode(), ex.getCode());
    }
}
