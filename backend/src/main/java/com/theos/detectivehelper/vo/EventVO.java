package com.theos.detectivehelper.vo;

/**
 * 事件VO
 */
public class EventVO {

    private Long id;
    private Long bookId;
    private String name;
    private Integer sortOrder;
    private Integer pageCount;
    private String createdAt;
    private String updatedAt;

    public EventVO() {
    }

    public EventVO(Long id, Long bookId, String name, Integer sortOrder, Integer pageCount, String createdAt, String updatedAt) {
        this.id = id;
        this.bookId = bookId;
        this.name = name;
        this.sortOrder = sortOrder;
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

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

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

}