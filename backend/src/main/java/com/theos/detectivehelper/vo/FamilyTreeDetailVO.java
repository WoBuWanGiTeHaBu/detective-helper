package com.theos.detectivehelper.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 族谱图详情VO
 * <p>
 * data 为库中原始 JSON 字符串；members / relations 是解析后的结构，平铺在顶层，
 * 与关系图 RelationGraphDetailVO 形态一致（历史上「data 与顶层字段不一致」曾导致「新建正常、重开就空图」）。
 */
@Data
public class FamilyTreeDetailVO {

    private Long id;
    private Long bookId;
    private String name;
    private String data;
    private List<Map<String, Object>> members;
    private List<Map<String, Object>> relations;
    private String createdAt;
    private String updatedAt;

    public FamilyTreeDetailVO() {
    }

}
