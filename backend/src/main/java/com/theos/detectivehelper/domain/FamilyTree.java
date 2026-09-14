package com.theos.detectivehelper.domain;

import lombok.Data;

/**
 * 族谱图实体
 */
@Data
public class FamilyTree {

    private Long id;
    private Long bookId;
    private String name;
    private String data;
    private String createdAt;
    private String updatedAt;

    public FamilyTree() {
        this.data = "{\"members\":[],\"relations\":[]}";
        this.createdAt = java.time.Instant.now().toString();
        this.updatedAt = java.time.Instant.now().toString();
    }

}
