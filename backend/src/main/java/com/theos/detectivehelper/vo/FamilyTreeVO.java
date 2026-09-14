package com.theos.detectivehelper.vo;

import lombok.Data;

/**
 * 族谱图VO
 */
@Data
public class FamilyTreeVO {

    private Long id;
    private Long bookId;
    private String name;
    private String data;
    private String createdAt;
    private String updatedAt;

    public FamilyTreeVO() {
    }

    public FamilyTreeVO(Long id, Long bookId, String name, String data, String createdAt, String updatedAt) {
        this.id = id;
        this.bookId = bookId;
        this.name = name;
        this.data = data;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}
