package com.theos.detectivehelper.dto;

import java.util.List;

/**
 * 案件书排序DTO
 */
public class BookSortDTO {

    private List<Long> bookIds;

    public List<Long> getBookIds() {
        return bookIds;
    }

    public void setBookIds(List<Long> bookIds) {
        this.bookIds = bookIds;
    }

}