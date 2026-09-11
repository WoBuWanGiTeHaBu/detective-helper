package com.theos.detectivehelper.domain;

import java.util.List;

/**
 * 案件书实体
 */
public class Book {

    private Long id;
    private String name;
    private String coverType;
    private String coverValue;
    private String coverText;
    private Integer sortOrder;
    private String createdAt;
    private String updatedAt;
    private List<Event> events;
    private List<RelationGraph> relationGraphs;

    public Book() {
        this.coverType = "color";
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

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public List<RelationGraph> getRelationGraphs() {
        return relationGraphs;
    }

    public void setRelationGraphs(List<RelationGraph> relationGraphs) {
        this.relationGraphs = relationGraphs;
    }

}