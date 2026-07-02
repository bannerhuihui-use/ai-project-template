package com.example.template.file.model;

/**
 * 文件业务分类，决定大小/MIME 策略与默认访问级别。
 */
public enum FileBizType {
    avatar,
    image,
    document,
    attachment;

    public static FileBizType parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return FileBizType.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
