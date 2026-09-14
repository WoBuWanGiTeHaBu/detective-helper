package com.theos.detectivehelper.vo;

/**
 * 画布VO
 *
 * @deprecated 画布端点已改用结构化的 {@link com.theos.detectivehelper.dto.CanvasResponse}
 *             （GET / PUT 同构）。本类仅为兼容旧代码保留，新的读写不要再使用。
 */
@Deprecated
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