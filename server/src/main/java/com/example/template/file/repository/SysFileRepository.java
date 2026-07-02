package com.example.template.file.repository;

import com.example.template.file.model.SysFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * sys_file 数据访问。
 */
@Repository
public class SysFileRepository {

    private static final RowMapper<SysFile> ROW_MAPPER = (rs, rowNum) -> {
        SysFile file = new SysFile();
        file.setId(rs.getLong("id"));
        file.setFileKey(rs.getString("file_key"));
        file.setOriginalName(rs.getString("original_name"));
        file.setStorageType(rs.getString("storage_type"));
        file.setStoragePath(rs.getString("storage_path"));
        file.setContentType(rs.getString("content_type"));
        file.setFileSize(rs.getLong("file_size"));
        file.setFileHash(rs.getString("file_hash"));
        file.setBizType(rs.getString("biz_type"));
        file.setAccessLevel(rs.getString("access_level"));
        file.setUploaderId(rs.getLong("uploader_id"));
        file.setUploaderType(rs.getString("uploader_type"));
        file.setStatus(rs.getString("status"));
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        file.setCreatedAt(created == null ? null : created.toInstant());
        file.setUpdatedAt(updated == null ? null : updated.toInstant());
        return file;
    };

    private final JdbcTemplate jdbcTemplate;

    public SysFileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(SysFile file) {
        jdbcTemplate.update("""
                INSERT INTO sys_file
                    (file_key, original_name, storage_type, storage_path, content_type, file_size, file_hash,
                     biz_type, access_level, uploader_id, uploader_type, status, created_at, updated_at, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), 0)
                """,
                file.getFileKey(),
                file.getOriginalName(),
                file.getStorageType(),
                file.getStoragePath(),
                file.getContentType(),
                file.getFileSize(),
                file.getFileHash(),
                file.getBizType(),
                file.getAccessLevel(),
                file.getUploaderId(),
                file.getUploaderType(),
                file.getStatus());
    }

    public Optional<SysFile> findByFileKey(String fileKey) {
        List<SysFile> list = jdbcTemplate.query("""
                SELECT id, file_key, original_name, storage_type, storage_path, content_type, file_size, file_hash,
                       biz_type, access_level, uploader_id, uploader_type, status, created_at, updated_at
                FROM sys_file
                WHERE file_key = ? AND deleted = 0 AND status = 'NORMAL'
                LIMIT 1
                """, ROW_MAPPER, fileKey);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public void markDeleted(String fileKey) {
        jdbcTemplate.update("""
                UPDATE sys_file SET status = 'DELETED', deleted = 1, updated_at = now()
                WHERE file_key = ? AND deleted = 0
                """, fileKey);
    }

    public long countByUploader(Long uploaderId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sys_file
                WHERE uploader_id = ? AND deleted = 0 AND status = 'NORMAL'
                """, Long.class, uploaderId);
        return count == null ? 0L : count;
    }

    public List<SysFile> listByUploader(Long uploaderId, int offset, int limit) {
        return jdbcTemplate.query("""
                SELECT id, file_key, original_name, storage_type, storage_path, content_type, file_size, file_hash,
                       biz_type, access_level, uploader_id, uploader_type, status, created_at, updated_at
                FROM sys_file
                WHERE uploader_id = ? AND deleted = 0 AND status = 'NORMAL'
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """, ROW_MAPPER, uploaderId, limit, offset);
    }

    public List<SysFile> listAll(int offset, int limit) {
        return jdbcTemplate.query("""
                SELECT id, file_key, original_name, storage_type, storage_path, content_type, file_size, file_hash,
                       biz_type, access_level, uploader_id, uploader_type, status, created_at, updated_at
                FROM sys_file
                WHERE deleted = 0 AND status = 'NORMAL'
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """, ROW_MAPPER, limit, offset);
    }

    public long countAll() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sys_file WHERE deleted = 0 AND status = 'NORMAL'
                """, Long.class);
        return count == null ? 0L : count;
    }
}
