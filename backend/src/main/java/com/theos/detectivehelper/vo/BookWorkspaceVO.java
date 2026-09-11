package com.theos.detectivehelper.vo;

import java.util.List;

/**
 * 案件书工作空间VO
 */
public class BookWorkspaceVO {

    private Long id;
    private String name;
    private String coverType;
    private String coverValue;
    private String coverText;
    private Integer sortOrder;
    private List<EventVO> events;
    private List<RelationGraphVO> relationGraphs;
    private String createdAt;
    private String updatedAt;

    public BookWorkspaceVO() {
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

    public List<EventVO> getEvents() {
        return events;
    }

    public void setEvents(List<EventVO> events) {
        this.events = events;
    }

    public List<RelationGraphVO> getRelationGraphs() {
        return relationGraphs;
    }

    public void setRelationGraphs(List<RelationGraphVO> relationGraphs) {
        this.relationGraphs = relationGraphs;
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