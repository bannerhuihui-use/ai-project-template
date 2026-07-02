package com.example.template.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页返回结构。
 *
 * @param <T> 列表元素类型
 */
@Schema(name = "PageResult", description = "统一分页返回结构")
public class PageResult<T> implements Serializable {

    @Schema(description = "总记录数", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private long total;

    @Schema(description = "当前页码，从 1 开始", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private long pageNum;

    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private long pageSize;

    @Schema(description = "当前页数据列表")
    private List<T> list;

    public PageResult() {
    }

    public PageResult(long total, long pageNum, long pageSize, List<T> list) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.list = list == null ? Collections.emptyList() : list;
    }

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> list) {
        return new PageResult<>(total, pageNum, pageSize, list);
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPageNum() {
        return pageNum;
    }

    public void setPageNum(long pageNum) {
        this.pageNum = pageNum;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
