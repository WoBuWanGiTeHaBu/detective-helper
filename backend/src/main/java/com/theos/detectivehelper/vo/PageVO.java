package com.theos.detectivehelper.vo;

import lombok.Data;

/**
 * 页面VO
 */
@Data
public class PageVO {

    private Long id;
    private Long eventId;
    private String name;
    private Integer sortOrder;
    private String canvasData;
    private String createdAt;
    private String updatedAt;

    public PageVO() {
    }

    public PageVO(Long id, Long eventId, String name, Integer sortOrder, String canvasData, String createdAt, String updatedAt) {
        this.id = id;
        this.eventId = eventId;
        this.name = name;
        this.sortOrder = sortOrder;
        this.canvasData = canvasData;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}