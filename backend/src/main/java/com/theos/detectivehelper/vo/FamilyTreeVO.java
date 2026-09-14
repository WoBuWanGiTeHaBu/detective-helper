package com.theos.detectivehelper.vo;

/**
 * 族谱图VO
 */
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
