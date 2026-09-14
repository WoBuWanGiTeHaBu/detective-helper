package com.theos.detectivehelper.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新页面DTO
 * <p>
 * 仅承载名称，画布数据通过 /api/pages/{id}/canvas 单独保存。
 */
@Data
public class PageUpdateDTO {

    @NotBlank(message = "页面名称不能为空")
    private String name;

}