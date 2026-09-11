package com.theos.detectivehelper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建关系图DTO
 */
public class RelationGraphCreateDTO {

    @NotNull(message = "案件书ID不能为空")
    private Long bookId;

    @NotBlank(message = "关系图名称不能为空")
    private String name;

    private String data;

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

}