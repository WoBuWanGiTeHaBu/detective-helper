package com.theos.detectivehelper.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建事件DTO
 * <p>
 * bookId 由路径参数 /api/books/{bookId}/events 提供，不在请求体中重复传递。
 */
public class EventCreateDTO {

    @NotBlank(message = "事件名称不能为空")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}