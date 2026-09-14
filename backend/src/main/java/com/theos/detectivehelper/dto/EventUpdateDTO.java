package com.theos.detectivehelper.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新事件DTO
 */
@Data
public class EventUpdateDTO {

    @NotBlank(message = "事件名称不能为空")
    private String name;

}