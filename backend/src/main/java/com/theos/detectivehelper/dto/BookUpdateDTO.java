package com.theos.detectivehelper.dto;

/**
 * 更新案件书DTO
 * <p>
 * 与 OpenAPI 文档 UpdateBookRequest 一致，四个字段全部可空：null 表示「不修改该字段」
 * （前端上传封面后只补发 coverType/coverValue，不带 name）。空字符串按显式修改处理。
 */
public class BookUpdateDTO {

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