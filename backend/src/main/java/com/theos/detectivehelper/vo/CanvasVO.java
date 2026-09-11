package com.theos.detectivehelper.vo;

/**
 * 画布VO
 */
public class CanvasVO {

    private Long pageId;
    private String canvasData;

    public CanvasVO() {
    }

    public CanvasVO(Long pageId, String canvasData) {
        this.pageId = pageId;
        this.canvasData = canvasData;
    }

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public String getCanvasData() {
        return canvasData;
    }

    public void setCanvasData(String canvasData) {
        this.canvasData = canvasData;
    }

}