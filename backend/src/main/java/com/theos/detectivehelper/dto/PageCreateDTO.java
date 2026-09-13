package com.theos.detectivehelper.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建页面DTO
 * <p>
 * eventId 由路径参数 /api/events/{eventId}/pages 提供，canvasData 默认为空画布。
 */
public class PageCreateDTO {

    @NotBlank(message = "页面名称不能为空")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}