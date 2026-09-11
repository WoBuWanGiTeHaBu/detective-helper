package com.theos.detectivehelper.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 提取关系图DTO
 */
public class RelationGraphExtractDTO {

    @NotNull(message = "案件书ID不能为空")
    private Long bookId;

    private String entityTypes;
    private String relationTypes;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getEntityTypes() {
        return entityTypes;
    }

    public void setEntityTypes(String entityTypes) {
        this.entityTypes = entityTypes;
    }

    public String getRelationTypes() {
        return relationTypes;
    }

    public void setRelationTypes(String relationTypes) {
        this.relationTypes = relationTypes;
    }

}