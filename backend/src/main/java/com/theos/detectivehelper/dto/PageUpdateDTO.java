package com.theos.detectivehelper.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新页面DTO
 */
public class PageUpdateDTO {

    @NotBlank(message = "页面名称不能为空")
    private String name;

    private Integer sortOrder;

    private String canvasData;

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