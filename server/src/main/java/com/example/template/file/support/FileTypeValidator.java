package com.example.template.file.support;

import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.file.config.FileProperties;
import com.example.template.file.model.FileBizType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件类型与大小校验（MIME 白名单 + 简单魔数检测）。
 */
public final class FileTypeValidator {

    private FileTypeValidator() {
    }

    public static void validateUpload(FileProperties fileProperties, FileBizType bizType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请选择要上传的文件");
        }
        String originalName = sanitizeFilename(file.getOriginalFilename());
        if (!StringUtils.hasText(originalName)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件名无效");
        }
        long maxSize = resolveMaxSize(fileProperties, bizType);
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED);
        }
        String contentType = normalizeContentType(file.getContentType());
        validateContentType(fileProperties, bizType, contentType);
        validateMagicNumber(contentType, file);
    }

    public static String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "file";
        }
        String cleaned = filename.replace('\0', ' ').replaceAll("[\\\\/]", "_").replace("..", "").trim();
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(cleaned.length() - 200);
        }
        return cleaned.isEmpty() ? "file" : cleaned;
    }

    public static String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static long resolveMaxSize(FileProperties fileProperties, FileBizType bizType) {
        Long configured = fileProperties.getUpload().getMaxSizeBytes().get(bizType.name());
        return configured == null ? 5_242_880L : configured;
    }

    private static void validateContentType(FileProperties fileProperties, FileBizType bizType, String contentType) {
        String allowed = fileProperties.getUpload().getAllowedContentTypes().get(bizType.name());
        if (!StringUtils.hasText(allowed)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
        Set<String> allowedSet = Arrays.stream(allowed.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (!allowedSet.contains(contentType)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    private static String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "application/octet-stream";
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private static void validateMagicNumber(String contentType, MultipartFile file) {
        byte[] header = new byte[12];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(header);
            if (read <= 0) {
                throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
            }
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR);
        }
        if (!matchesMagic(contentType, header)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    private static boolean matchesMagic(String contentType, byte[] header) {
        return switch (contentType) {
            case "image/jpeg" -> header[0] == (byte) 0xFF && header[1] == (byte) 0xD8;
            case "image/png" -> header[0] == (byte) 0x89 && header[1] == 0x50;
            case "image/gif" -> header[0] == 'G' && header[1] == 'I' && header[2] == 'F';
            case "image/webp" -> header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[8] == 'W';
            case "application/pdf" -> header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
            case "application/zip", "application/x-zip-compressed" -> header[0] == 'P' && header[1] == 'K';
            case "text/plain" -> true;
            case "application/msword" -> header[0] == (byte) 0xD0 && header[1] == (byte) 0xCF;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    header[0] == 'P' && header[1] == 'K';
            default -> false;
        };
    }
}
