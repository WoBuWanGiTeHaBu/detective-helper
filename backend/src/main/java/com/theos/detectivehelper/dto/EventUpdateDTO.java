package com.theos.detectivehelper.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新事件DTO
 */
public class EventUpdateDTO {

    @NotBlank(message = "事件名称不能为空")
    private String name;

    private Integer sortOrder;

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

}