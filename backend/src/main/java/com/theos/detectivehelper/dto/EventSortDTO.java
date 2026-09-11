package com.theos.detectivehelper.dto;

import java.util.List;

/**
 * 事件排序DTO
 */
public class EventSortDTO {

    private List<Long> eventIds;

    public List<Long> getEventIds() {
        return eventIds;
    }

    public void setEventIds(List<Long> eventIds) {
        this.eventIds = eventIds;
    }

}