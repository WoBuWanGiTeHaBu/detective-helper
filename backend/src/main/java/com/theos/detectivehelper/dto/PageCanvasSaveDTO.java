package com.theos.detectivehelper.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 保存页面画布DTO
 */
@Data
public class PageCanvasSaveDTO {

    @NotNull(message = "画布数据不能为空")
    private String canvasData;

}