package com.example.template.file.service.impl;

import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.security.PermissionChecker;
import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.file.config.FileProperties;
import com.example.template.file.dto.FileUploadResult;
import com.example.template.file.model.SysFile;
import com.example.template.file.repository.SysFileRepository;
import com.example.template.file.storage.FileStorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FileServiceImpl 单元测试（Mock 存储与仓储）。
 */
@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileProperties fileProperties;

    @Mock
    private FileStorageProvider storageProvider;

    @Mock
    private SysFileRepository sysFileRepository;

    @Mock
    private PermissionChecker permissionChecker;

    @InjectMocks
    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        lenient().when(fileProperties.getUpload()).thenReturn(new FileProperties.Upload());
        lenient().when(storageProvider.storageType()).thenReturn("local");
        lenient().when(storageProvider.resolvePublicUrl(anyString(), anyString()))
                .thenAnswer(inv -> "http://localhost/api/v1/public/files/" + inv.getArgument(0) + "/download");
        CurrentUser user = new CurrentUser(1L, "ADMIN", "USERNAME", java.util.List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, java.util.List.of()));
    }

    @Test
    void upload_imageDefaultsPublic() {
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", pngHeader);

        FileUploadResult result = fileService.upload(file, "image", null);

        assertEquals("image", result.getBizType());
        assertEquals("public", result.getAccessLevel());
        verify(storageProvider).store(anyString(), eq("image/png"), any(), anyLong());
        verify(sysFileRepository).insert(any(SysFile.class));
    }

    @Test
    void delete_onlyOwnerOrAdmin() {
        SysFile file = new SysFile();
        file.setFileKey("abc");
        file.setUploaderId(99L);
        file.setBizType("image");
        file.setStoragePath("files/image/ab/abc.png");
        when(sysFileRepository.findByFileKey("abc")).thenReturn(Optional.of(file));
        when(permissionChecker.has("file:admin")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> fileService.delete("abc"));
        assertEquals(ResultCode.FILE_DELETE_DENIED.getCode(), ex.getCode());
    }

    @Test
    void openPublicFile_rejectsPrivate() {
        SysFile file = new SysFile();
        file.setFileKey("private1");
        file.setAccessLevel("private");
        when(sysFileRepository.findByFileKey("private1")).thenReturn(Optional.of(file));

        BusinessException ex = assertThrows(BusinessException.class, () -> fileService.openPublicFile("private1"));
        assertEquals(ResultCode.FILE_ACCESS_DENIED.getCode(), ex.getCode());
    }
}
