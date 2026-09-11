package com.theos.detectivehelper.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 保存页面画布DTO
 */
public class PageCanvasSaveDTO {

    @NotNull(message = "画布数据不能为空")
    private String canvasData;

    public String getCanvasData() {
        return canvasData;
    }

    public void setCanvasData(String canvasData) {
        this.canvasData = canvasData;
    }

}