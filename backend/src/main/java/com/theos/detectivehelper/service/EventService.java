package com.theos.detectivehelper.service;

import com.theos.detectivehelper.common.exception.BusinessException;
import com.theos.detectivehelper.domain.Event;
import com.theos.detectivehelper.dto.EventCreateDTO;
import com.theos.detectivehelper.dto.EventSortDTO;
import com.theos.detectivehelper.dto.EventUpdateDTO;
import com.theos.detectivehelper.repository.EventRepository;
import com.theos.detectivehelper.repository.PageRepository;
import com.theos.detectivehelper.vo.EventVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事件服务
 */
@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final PageRepository pageRepository;

    public EventService(EventRepository eventRepository, PageRepository pageRepository) {
        this.eventRepository = eventRepository;
        this.pageRepository = pageRepository;
    }

    /**
     * 创建事件
     */
    public EventVO createEvent(EventCreateDTO dto) {
        if (!pageRepository.findById(dto.getPageId()).isPresent()) {
            throw new BusinessException(com.theos.detectivehelper.common.ErrorCode.PAGE_NOT_FOUND);
        }

        Event event = new Event();
        event.setPageId(dto.getPageId());
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventTime(dto.getEventTime() != null ? dto.getEventTime() : LocalDateTime.now());
        event.setSortOrder(getNextSortOrder(dto.getPageId()));

        Event savedEvent = eventRepository.save(event);
        return toVO(savedEvent);
    }

    /**
     * 更新事件
     */
    public EventVO updateEvent(Long id, EventUpdateDTO dto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new BusinessException(com.theos.detectivehelper.common.ErrorCode.EVENT_NOT_FOUND));

        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventTime(dto.getEventTime());
        if (dto.getSortOrder() != null) {
            event.setSortOrder(dto.getSortOrder());
        }

        Event savedEvent = eventRepository.save(event);
        return toVO(savedEvent);
    }

    /**
     * 删除事件
     */
    public void deleteEvent(Long id) {
        if (!eventRepository.findById(id).isPresent()) {
            throw new BusinessException(com.theos.detectivehelper.common.ErrorCode.EVENT_NOT_FOUND);
        }
        eventRepository.deleteById(id);
    }

    /**
     * 获取事件详情
     */
    public EventVO getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new BusinessException(com.theos.detectivehelper.common.ErrorCode.EVENT_NOT_FOUND));
        return toVO(event);
    }

    /**
     * 获取页面的所有事件
     */
    public List<EventVO> getEventsByPageId(Long pageId) {
        return eventRepository.findByPageId(pageId).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 批量排序事件
     */
    public void sortEvents(Long pageId, EventSortDTO dto) {
        List<Long> eventIds = dto.getEventIds();
        if (eventIds == null || eventIds.isEmpty()) {
            throw new BusinessException(com.theos.detectivehelper.common.ErrorCode.INVALID_SORT_ORDER);
        }

        for (int i = 0; i < eventIds.size(); i++) {
            Event event = eventRepository.findById(eventIds.get(i))
                    .orElseThrow(() -> new BusinessException(com.theos.detectivehelper.common.ErrorCode.EVENT_NOT_FOUND));

            if (!event.getPageId().equals(pageId)) {
                throw new BusinessException(com.theos.detectivehelper.common.ErrorCode.INVALID_OPERATION);
            }

            eventRepository.updateSortOrder(eventIds.get(i), i + 1);
        }
    }

    private int getNextSortOrder(Long pageId) {
        List<Event> events = eventRepository.findByPageId(pageId);
        return events.stream()
                .mapToInt(event -> event.getSortOrder() != null ? event.getSortOrder() : 0)
                .max()
                .orElse(0) + 1;
    }

    private EventVO toVO(Event event) {
        return new EventVO(
                event.getId(),
                event.getPageId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventTime(),
                event.getSortOrder(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

}