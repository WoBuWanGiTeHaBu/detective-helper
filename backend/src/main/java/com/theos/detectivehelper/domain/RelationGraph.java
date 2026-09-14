package com.theos.detectivehelper.domain;

import lombok.Data;

/**
 * 关系图实体
 */
@Data
public class RelationGraph {

    private Long id;
    private Long bookId;
    private String name;
    private String data;
    private String createdAt;
    private String updatedAt;

    public RelationGraph() {
        this.data = "{}";
        this.createdAt = java.time.Instant.now().toString();
        this.updatedAt = java.time.Instant.now().toString();
    }

}