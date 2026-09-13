package com.theos.detectivehelper.controller;

import com.theos.detectivehelper.common.Result;
import com.theos.detectivehelper.dto.EventCreateDTO;
import com.theos.detectivehelper.dto.EventSortDTO;
import com.theos.detectivehelper.dto.EventUpdateDTO;
import com.theos.detectivehelper.dto.SortItem;
import com.theos.detectivehelper.service.EventService;
import com.theos.detectivehelper.vo.EventVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 事件控制器
 * <p>
 * 事件隶属于案件书：列表与创建走 /api/books/{bookId}/events，
 * 单条更新/删除走 /api/events/{id}，排序走 /api/books/{bookId}/events/sort。
 */
@RestController
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * 获取案件书下的事件列表
     */
    @GetMapping("/api/books/{bookId}/events")
    public Result<List<EventVO>> listEvents(@PathVariable Long bookId) {
        List<EventVO> events = eventService.getEventsByBookId(bookId);
        return Result.success(events);
    }

    /**
     * 创建事件
     */
    @PostMapping("/api/books/{bookId}/events")
    public Result<EventVO> createEvent(@PathVariable Long bookId, @Valid @RequestBody EventCreateDTO dto) {
        EventVO eventVO = eventService.createEvent(bookId, dto);
        return Result.success(eventVO);
    }

    /**
     * 批量排序事件
     */
    @PutMapping("/api/books/{bookId}/events/sort")
    public Result<Void> sortEvents(@PathVariable Long bookId, @Valid @RequestBody List<SortItem> items) {
        eventService.sortEvents(bookId, toEventSortDTO(items));
        return Result.success();
    }

    /**
     * 获取事件详情
     */
    @GetMapping("/api/events/{id}")
    public Result<EventVO> getEventById(@PathVariable Long id) {
        EventVO eventVO = eventService.getEventById(id);
        return Result.success(eventVO);
    }

    /**
     * 更新事件
     */
    @PutMapping("/api/events/{id}")
    public Result<EventVO> updateEvent(@PathVariable Long id, @Valid @RequestBody EventUpdateDTO dto) {
        EventVO eventVO = eventService.updateEvent(id, dto);
        return Result.success(eventVO);
    }

    /**
     * 删除事件
     */
    @DeleteMapping("/api/events/{id}")
    public Result<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return Result.success();
    }

    private EventSortDTO toEventSortDTO(List<SortItem> items) {
        EventSortDTO dto = new EventSortDTO();
        dto.setEventIds(items.stream().map(SortItem::getId).collect(Collectors.toList()));
        return dto;
    }

}