package com.theos.detectivehelper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建页面DTO
 */
public class PageCreateDTO {

    @NotNull(message = "事件ID不能为空")
    private Long eventId;

    @NotBlank(message = "页面名称不能为空")
    private String name;

    private Integer sortOrder;

    private String canvasData;

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

}