package com.theos.detectivehelper.controller;

import com.theos.detectivehelper.common.Result;
import com.theos.detectivehelper.dto.EventCreateDTO;
import com.theos.detectivehelper.dto.EventSortDTO;
import com.theos.detectivehelper.dto.EventUpdateDTO;
import com.theos.detectivehelper.service.EventService;
import com.theos.detectivehelper.vo.EventVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 事件控制器
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * 创建事件
     */
    @PostMapping
    public Result<EventVO> createEvent(@Valid @RequestBody EventCreateDTO dto) {
        EventVO eventVO = eventService.createEvent(dto);
        return Result.success(eventVO);
    }

    /**
     * 更新事件
     */
    @PutMapping("/{id}")
    public Result<EventVO> updateEvent(@PathVariable Long id, @Valid @RequestBody EventUpdateDTO dto) {
        EventVO eventVO = eventService.updateEvent(id, dto);
        return Result.success(eventVO);
    }

    /**
     * 删除事件
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return Result.success();
    }

    /**
     * 获取事件详情
     */
    @GetMapping("/{id}")
    public Result<EventVO> getEventById(@PathVariable Long id) {
        EventVO eventVO = eventService.getEventById(id);
        return Result.success(eventVO);
    }

    /**
     * 获取页面的所有事件
     */
    @GetMapping("/page/{pageId}")
    public Result<List<EventVO>> getEventsByPageId(@PathVariable Long pageId) {
        List<EventVO> events = eventService.getEventsByPageId(pageId);
        return Result.success(events);
    }

    /**
     * 批量排序事件
     */
    @PostMapping("/page/{pageId}/sort")
    public Result<Void> sortEvents(@PathVariable Long pageId, @Valid @RequestBody EventSortDTO dto) {
        eventService.sortEvents(pageId, dto);
        return Result.success();
    }

}