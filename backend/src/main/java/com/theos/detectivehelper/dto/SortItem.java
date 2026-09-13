package com.theos.detectivehelper.dto;

/**
 * 排序条目
 * <p>
 * 对应 OpenAPI 的 SortItem，批量排序接口的请求体元素。
 */
public class SortItem {

    private Long id;
    private Integer sortOrder;

    public SortItem() {
    }

    public SortItem(Long id, Integer sortOrder) {
        this.id = id;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

}
