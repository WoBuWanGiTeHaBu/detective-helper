package com.theos.detectivehelper.domain;

import lombok.Data;

/**
 * 页面实体
 */
@Data
public class Page {

    private Long id;
    private Long eventId;
    private String name;
    private Integer sortOrder;
    private String canvasData;
    private String createdAt;
    private String updatedAt;

    public Page() {
        this.sortOrder = 0;
        this.canvasData = "{}";
        this.createdAt = java.time.Instant.now().toString();
        this.updatedAt = java.time.Instant.now().toString();
    }

}