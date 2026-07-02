package com.example.template.file.service;

import com.example.template.common.PageResult;
import com.example.template.file.dto.FileItem;
import com.example.template.file.dto.FileUploadResult;
import com.example.template.file.dto.PublicFileView;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传与访问业务。
 */
public interface FileService {

    FileUploadResult upload(MultipartFile file, String bizType, String accessLevel);

    FileItem getMetadata(String fileKey);

    Resource loadAsResource(String fileKey, boolean publicAccess);

    PublicFileView openPublicFile(String fileKey);

    void delete(String fileKey);

    PageResult<FileItem> listMine(int pageNum, int pageSize);
}
