package com.theos.detectivehelper.vo;

import lombok.Data;

/**
 * 关系图VO
 */
@Data
public class RelationGraphVO {

    private Long id;
    private Long bookId;
    private String name;
    private String data;
    private String createdAt;
    private String updatedAt;

    public RelationGraphVO() {
    }

    public RelationGraphVO(Long id, Long bookId, String name, String data, String createdAt, String updatedAt) {
        this.id = id;
        this.bookId = bookId;
        this.name = name;
        this.data = data;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}