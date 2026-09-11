package com.theos.detectivehelper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建事件DTO
 */
public class EventCreateDTO {

    @NotNull(message = "案件书ID不能为空")
    private Long bookId;

    @NotBlank(message = "事件名称不能为空")
    private String name;

    private Integer sortOrder;

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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

}