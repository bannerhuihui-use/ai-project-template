package com.example.template.file.service.impl;

import com.example.template.auth.security.CurrentUser;
import com.example.template.auth.security.PermissionChecker;
import com.example.template.auth.security.SecurityUtils;
import com.example.template.common.BusinessException;
import com.example.template.common.PageResult;
import com.example.template.common.ResultCode;
import com.example.template.file.config.FileProperties;
import com.example.template.file.dto.FileItem;
import com.example.template.file.dto.FileUploadResult;
import com.example.template.file.dto.PublicFileView;
import com.example.template.file.model.FileAccessLevel;
import com.example.template.file.model.FileBizType;
import com.example.template.file.model.SysFile;
import com.example.template.file.repository.SysFileRepository;
import com.example.template.file.service.FileService;
import com.example.template.file.storage.FileStorageProvider;
import com.example.template.file.support.FileAuditLogger;
import com.example.template.file.support.FileTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传、下载、删除业务实现。
 */
@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_INSTANT;

    private final FileProperties fileProperties;
    private final FileStorageProvider storageProvider;
    private final SysFileRepository sysFileRepository;
    private final PermissionChecker permissionChecker;

    public FileServiceImpl(FileProperties fileProperties,
                           FileStorageProvider storageProvider,
                           SysFileRepository sysFileRepository,
                           PermissionChecker permissionChecker) {
        this.fileProperties = fileProperties;
        this.storageProvider = storageProvider;
        this.sysFileRepository = sysFileRepository;
        this.permissionChecker = permissionChecker;
    }

    @Override
    @Transactional
    public FileUploadResult upload(MultipartFile file, String bizTypeRaw, String accessLevelRaw) {
        CurrentUser user = requireCurrentUser();
        FileBizType bizType = parseBizType(bizTypeRaw);
        FileAccessLevel accessLevel = resolveAccessLevel(bizType, accessLevelRaw);
        FileTypeValidator.validateUpload(fileProperties, bizType, file);

        String fileKey = UUID.randomUUID().toString().replace("-", "");
        String originalName = FileTypeValidator.sanitizeFilename(file.getOriginalFilename());
        String extension = FileTypeValidator.extractExtension(originalName);
        String storagePath = buildStoragePath(bizType, fileKey, extension);
        String contentType = normalizeContentType(file.getContentType());
        String fileHash = computeSha256(file);

        try (InputStream in = file.getInputStream()) {
            storageProvider.store(storagePath, contentType, in, file.getSize());
        } catch (IOException ex) {
            log.error("读取上传流失败: fileKey={}", fileKey, ex);
            throw new BusinessException(ResultCode.SYSTEM_ERROR);
        }

        SysFile entity = new SysFile();
        entity.setFileKey(fileKey);
        entity.setOriginalName(originalName);
        entity.setStorageType(storageProvider.storageType());
        entity.setStoragePath(storagePath);
        entity.setContentType(contentType);
        entity.setFileSize(file.getSize());
        entity.setFileHash(fileHash);
        entity.setBizType(bizType.name());
        entity.setAccessLevel(accessLevel.externalName());
        entity.setUploaderId(user.getUserId());
        entity.setUploaderType(user.getUserType());
        entity.setStatus("NORMAL");
        sysFileRepository.insert(entity);

        FileAuditLogger.auditUpload(fileKey, bizType.name(), accessLevel.externalName(), file.getSize());

        FileUploadResult result = new FileUploadResult();
        result.setFileKey(fileKey);
        result.setOriginalName(originalName);
        result.setContentType(contentType);
        result.setFileSize(file.getSize());
        result.setBizType(bizType.name());
        result.setAccessLevel(accessLevel.externalName());
        result.setUrl(resolveUrl(entity));
        return result;
    }

    @Override
    public FileItem getMetadata(String fileKey) {
        SysFile file = requireFile(fileKey);
        assertCanRead(file);
        return toItem(file);
    }

    @Override
    public Resource loadAsResource(String fileKey, boolean publicAccess) {
        SysFile file = requireFile(fileKey);
        if (publicAccess) {
            if (!file.isPublicAccess()) {
                throw new BusinessException(ResultCode.FILE_ACCESS_DENIED);
            }
        } else {
            assertCanRead(file);
        }
        InputStream stream = storageProvider.open(file.getStoragePath());
        return new InputStreamResource(stream);
    }

    @Override
    public PublicFileView openPublicFile(String fileKey) {
        SysFile file = requireFile(fileKey);
        if (!file.isPublicAccess()) {
            throw new BusinessException(ResultCode.FILE_ACCESS_DENIED);
        }
        String redirectUrl = null;
        Resource resource = null;
        if ("oss".equalsIgnoreCase(file.getStorageType())) {
            redirectUrl = storageProvider.resolvePublicUrl(fileKey, file.getStoragePath());
        } else {
            resource = new InputStreamResource(storageProvider.open(file.getStoragePath()));
        }
        return new PublicFileView(file.getOriginalName(), file.getContentType(), resource, redirectUrl);
    }

    @Override
    @Transactional
    public void delete(String fileKey) {
        SysFile file = requireFile(fileKey);
        assertCanDelete(file);
        storageProvider.delete(file.getStoragePath());
        sysFileRepository.markDeleted(fileKey);
        FileAuditLogger.auditDelete(fileKey, file.getBizType());
    }

    @Override
    public PageResult<FileItem> listMine(int pageNum, int pageSize) {
        CurrentUser user = requireCurrentUser();
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePageNum - 1) * safePageSize;
        long total = sysFileRepository.countByUploader(user.getUserId());
        List<FileItem> list = sysFileRepository.listByUploader(user.getUserId(), offset, safePageSize)
                .stream()
                .map(this::toItem)
                .toList();
        return PageResult.of(total, safePageNum, safePageSize, list);
    }

    private SysFile requireFile(String fileKey) {
        return sysFileRepository.findByFileKey(fileKey)
                .orElseThrow(() -> new BusinessException(ResultCode.FILE_NOT_FOUND));
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    private FileBizType parseBizType(String bizTypeRaw) {
        if (!StringUtils.hasText(bizTypeRaw)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "bizType 不能为空");
        }
        FileBizType bizType = FileBizType.parse(bizTypeRaw);
        if (bizType == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "bizType 无效");
        }
        return bizType;
    }

    private FileAccessLevel resolveAccessLevel(FileBizType bizType, String accessLevelRaw) {
        if (StringUtils.hasText(accessLevelRaw)) {
            FileAccessLevel level = FileAccessLevel.parse(accessLevelRaw);
            if (level == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "accessLevel 无效");
            }
            return level;
        }
        if (bizType == FileBizType.avatar || bizType == FileBizType.image) {
            return FileAccessLevel.PUBLIC;
        }
        return FileAccessLevel.PRIVATE;
    }

    private void assertCanRead(SysFile file) {
        CurrentUser user = requireCurrentUser();
        if (permissionChecker.has("file:admin")) {
            return;
        }
        if (file.getUploaderId().equals(user.getUserId())) {
            return;
        }
        throw new BusinessException(ResultCode.FILE_ACCESS_DENIED);
    }

    private void assertCanDelete(SysFile file) {
        CurrentUser user = requireCurrentUser();
        if (permissionChecker.has("file:admin")) {
            return;
        }
        if (file.getUploaderId().equals(user.getUserId())) {
            return;
        }
        throw new BusinessException(ResultCode.FILE_DELETE_DENIED);
    }

    private FileItem toItem(SysFile file) {
        FileItem item = new FileItem();
        item.setFileKey(file.getFileKey());
        item.setOriginalName(file.getOriginalName());
        item.setContentType(file.getContentType());
        item.setFileSize(file.getFileSize());
        item.setBizType(file.getBizType());
        item.setAccessLevel(file.getAccessLevel());
        item.setUrl(resolveUrl(file));
        if (file.getCreatedAt() != null) {
            item.setCreatedAt(ISO_FORMAT.format(file.getCreatedAt()));
        }
        return item;
    }

    private String resolveUrl(SysFile file) {
        if (file.isPublicAccess()) {
            return storageProvider.resolvePublicUrl(file.getFileKey(), file.getStoragePath());
        }
        return storageProvider.resolveProtectedDownloadUrl(file.getFileKey());
    }

    private static String buildStoragePath(FileBizType bizType, String fileKey, String extension) {
        String suffix = StringUtils.hasText(extension) ? "." + extension : "";
        return "files/" + bizType.name() + "/" + fileKey.substring(0, 2) + "/" + fileKey + suffix;
    }

    private static String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "application/octet-stream";
        }
        return contentType.split(";")[0].trim().toLowerCase();
    }

    private static String computeSha256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (InputStream in = file.getInputStream()) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException ex) {
            log.warn("计算文件哈希失败", ex);
            return null;
        }
    }
}
