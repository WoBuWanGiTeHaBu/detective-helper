package com.theos.detectivehelper.domain;

import java.util.List;

/**
 * 事件实体
 */
public class Event {

    private Long id;
    private Long bookId;
    private String name;
    private Integer sortOrder;
    private String createdAt;
    private String updatedAt;
    private List<Page> pages;

    public Event() {
        this.sortOrder = 0;
        this.createdAt = java.time.Instant.now().toString();
        this.updatedAt = java.time.Instant.now().toString();
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

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

}