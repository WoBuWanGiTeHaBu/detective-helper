package com.theos.detectivehelper.vo;

import lombok.Data;

/**
 * 事件VO
 */
@Data
public class EventVO {

    private Long id;
    private Long bookId;
    private String name;
    private Integer sortOrder;
    private Integer pageCount;
    private String createdAt;
    private String updatedAt;

    public EventVO() {
    }

    public EventVO(Long id, Long bookId, String name, Integer sortOrder, Integer pageCount, String createdAt, String updatedAt) {
        this.id = id;
        this.bookId = bookId;
        this.name = name;
        this.sortOrder = sortOrder;
        this.pageCount = pageCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}