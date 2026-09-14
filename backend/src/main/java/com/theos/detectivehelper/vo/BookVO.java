package com.theos.detectivehelper.vo;

import lombok.Data;

/**
 * 案件书VO
 */
@Data
public class BookVO {

    private Long id;
    private String name;
    private String coverType;
    private String coverValue;
    private String coverText;
    private Integer sortOrder;
    private Integer eventCount;
    private Integer pageCount;
    private String createdAt;
    private String updatedAt;
    /** 内容最后改动时间；改书名 / 换封面不刷新它。老数据可空，前端自行回退 updatedAt */
    private String contentUpdatedAt;

    public BookVO() {
    }

    public BookVO(Long id, String name, String coverType, String coverValue, String coverText, Integer sortOrder, Integer eventCount, Integer pageCount, String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.coverType = coverType;
        this.coverValue = coverValue;
        this.coverText = coverText;
        this.sortOrder = sortOrder;
        this.eventCount = eventCount;
        this.pageCount = pageCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}