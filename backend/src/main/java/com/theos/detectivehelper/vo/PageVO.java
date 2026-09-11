package com.theos.detectivehelper.vo;

/**
 * 页面VO
 */
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

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCanvasData() {
        return canvasData;
    }

    public void setCanvasData(String canvasData) {
        this.canvasData = canvasData;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

}