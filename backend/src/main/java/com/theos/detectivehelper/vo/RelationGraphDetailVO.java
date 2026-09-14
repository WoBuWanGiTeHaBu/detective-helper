package com.theos.detectivehelper.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 关系图详情VO
 * <p>
 * data 为库中原始 JSON 字符串；nodes / edges 是解析后的结构，便于前端直接使用。
 */
@Data
public class RelationGraphDetailVO {

    private Long id;
    private Long bookId;
    private String name;
    private String data;
    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> edges;
    private String createdAt;
    private String updatedAt;

    public RelationGraphDetailVO() {
    }

}
