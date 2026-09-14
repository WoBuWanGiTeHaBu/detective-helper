package com.theos.detectivehelper.vo;

/**
 * 案件书VO
 */
public class BookVO {

    private Long id;
    private String name;
    private String coverType;
    private String coverValue;
    private String coverText;
    private Integer sortOrder;
    private Integer eventCount;
    private Integer pageCount;
    private String createdAt;
    private String updatedAt;
    /** 内容最后改动时间；改书名 / 换封面不刷新它。老数据可空，前端自行回退 updatedAt */
    private String contentUpdatedAt;

    public BookVO() {
    }

    public BookVO(Long id, String name, String coverType, String coverValue, String coverText, Integer sortOrder, Integer eventCount, Integer pageCount, String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.coverType = coverType;
        this.coverValue = coverValue;
        this.coverText = coverText;
        this.sortOrder = sortOrder;
        this.eventCount = eventCount;
        this.pageCount = pageCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCoverType() {
        return coverType;
    }

    public void setCoverType(String coverType) {
        this.coverType = coverType;
    }

    public String getCoverValue() {
        return coverValue;
    }

    public void setCoverValue(String coverValue) {
        this.coverValue = coverValue;
    }

    public String getCoverText() {
        return coverText;
    }

    public void setCoverText(String coverText) {
        this.coverText = coverText;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getEventCount() {
        return eventCount;
    }

    public void setEventCount(Integer eventCount) {
        this.eventCount = eventCount;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getContentUpdatedAt() {
        return contentUpdatedAt;
    }

    public void setContentUpdatedAt(String contentUpdatedAt) {
        this.contentUpdatedAt = contentUpdatedAt;
    }

}