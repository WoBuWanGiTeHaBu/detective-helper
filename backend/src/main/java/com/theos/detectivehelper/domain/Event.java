package com.theos.detectivehelper.domain;

import lombok.Data;

import java.util.List;

/**
 * 事件实体
 */
@Data
public class Event {

    private Long id;
    private Long bookId;
    private String name;
    private Integer sortOrder;
    private String createdAt;
    private String updatedAt;
    private List<Page> pages;

    public Event() {
        this.sortOrder = 0;
        this.createdAt = java.time.Instant.now().toString();
        this.updatedAt = java.time.Instant.now().toString();
    }

}