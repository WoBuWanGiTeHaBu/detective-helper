package com.theos.detectivehelper.dto;

import lombok.Data;

/**
 * 排序条目
 * <p>
 * 对应 OpenAPI 的 SortItem，批量排序接口的请求体元素。
 */
@Data
public class SortItem {

    private Long id;
    private Integer sortOrder;

    public SortItem() {
    }

    public SortItem(Long id, Integer sortOrder) {
        this.id = id;
        this.sortOrder = sortOrder;
    }

}
