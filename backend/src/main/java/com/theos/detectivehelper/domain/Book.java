package com.theos.detectivehelper.domain;

import lombok.Data;

import java.util.List;

/**
 * 案件书实体
 */
@Data
public class Book {

    private Long id;
    private String name;
    private String coverType;
    private String coverValue;
    private String coverText;
    private Integer sortOrder;
    private String createdAt;
    private String updatedAt;
    /** 内容最后改动时间；改书名 / 换封面不刷新它。老数据可空，前端自行回退 updatedAt */
    private String contentUpdatedAt;
    private List<Event> events;
    private List<RelationGraph> relationGraphs;

    public Book() {
        this.coverType = "color";
        this.sortOrder = 0;
        this.createdAt = java.time.Instant.now().toString();
        this.updatedAt = java.time.Instant.now().toString();
    }

}