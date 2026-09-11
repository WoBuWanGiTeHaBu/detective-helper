package com.theos.detectivehelper.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新案件书DTO
 */
public class BookUpdateDTO {

    @NotBlank(message = "案件书名称不能为空")
    private String name;

    private String coverType;

    private String coverValue;

    private String coverText;

    private Integer sortOrder;

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

}