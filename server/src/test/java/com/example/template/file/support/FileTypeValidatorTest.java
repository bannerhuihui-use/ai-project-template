package com.example.template.file.support;

import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.file.config.FileProperties;
import com.example.template.file.model.FileBizType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FileTypeValidator 单元测试。
 */
class FileTypeValidatorTest {

    private FileProperties fileProperties;

    @BeforeEach
    void setUp() {
        fileProperties = new FileProperties();
    }

    @Test
    void image_pngValid() {
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", pngHeader);
        FileTypeValidator.validateUpload(fileProperties, FileBizType.image, file);
    }

    @Test
    void image_rejectWrongMagic() {
        byte[] fake = "not-image".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", fake);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> FileTypeValidator.validateUpload(fileProperties, FileBizType.image, file));
        assertEquals(ResultCode.FILE_TYPE_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void image_rejectOversize() {
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", pngHeader) {
            @Override
            public long getSize() {
                return 10_000_000L;
            }
        };
        BusinessException ex = assertThrows(BusinessException.class,
                () -> FileTypeValidator.validateUpload(fileProperties, FileBizType.image, file));
        assertEquals(ResultCode.FILE_SIZE_EXCEEDED.getCode(), ex.getCode());
    }

    @Test
    void sanitizeFilename_stripsPath() {
        assertEquals("_evil_name.png", FileTypeValidator.sanitizeFilename("..\\evil/name.png"));
    }
}
