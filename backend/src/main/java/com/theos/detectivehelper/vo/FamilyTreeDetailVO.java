package com.theos.detectivehelper.vo;

import java.util.List;
import java.util.Map;

/**
 * 族谱图详情VO
 * <p>
 * data 为库中原始 JSON 字符串；members / relations 是解析后的结构，平铺在顶层，
 * 与关系图 RelationGraphDetailVO 形态一致（历史上「data 与顶层字段不一致」曾导致「新建正常、重开就空图」）。
 */
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

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public List<Map<String, Object>> getMembers() {
        return members;
    }

    public void setMembers(List<Map<String, Object>> members) {
        this.members = members;
    }

    public List<Map<String, Object>> getRelations() {
        return relations;
    }

    public void setRelations(List<Map<String, Object>> relations) {
        this.relations = relations;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

}
