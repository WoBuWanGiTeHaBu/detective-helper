package com.theos.detectivehelper.dto;

import java.util.List;

/**
 * 页面排序DTO
 */
public class PageSortDTO {

    private List<Long> pageIds;

    public List<Long> getPageIds() {
        return pageIds;
    }

    public void setPageIds(List<Long> pageIds) {
        this.pageIds = pageIds;
    }

}