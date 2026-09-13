package com.theos.detectivehelper.vo;

import java.util.List;
import java.util.Map;

/**
 * 关系图详情VO
 * <p>
 * data 为库中原始 JSON 字符串；nodes / edges 是解析后的结构，便于前端直接使用。
 */
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

    public List<Map<String, Object>> getNodes() {
        return nodes;
    }

    public void setNodes(List<Map<String, Object>> nodes) {
        this.nodes = nodes;
    }

    public List<Map<String, Object>> getEdges() {
        return edges;
    }

    public void setEdges(List<Map<String, Object>> edges) {
        this.edges = edges;
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
