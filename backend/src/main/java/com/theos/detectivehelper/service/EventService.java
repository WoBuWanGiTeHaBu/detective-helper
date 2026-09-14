package com.theos.detectivehelper.service;

import com.theos.detectivehelper.common.ErrorCode;
import com.theos.detectivehelper.common.exception.BusinessException;
import com.theos.detectivehelper.domain.Event;
import com.theos.detectivehelper.dto.EventCreateDTO;
import com.theos.detectivehelper.dto.EventSortDTO;
import com.theos.detectivehelper.dto.EventUpdateDTO;
import com.theos.detectivehelper.repository.BookRepository;
import com.theos.detectivehelper.repository.EventRepository;
import com.theos.detectivehelper.repository.PageRepository;
import com.theos.detectivehelper.vo.EventVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 事件服务
 * <p>
 * 事件挂在案件书（book）下，页面（page）挂在事件下，层级为 Book → Event → Page。
 */
@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final PageRepository pageRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;

    public EventService(EventRepository eventRepository, PageRepository pageRepository,
                        BookRepository bookRepository, BookService bookService) {
        this.eventRepository = eventRepository;
        this.pageRepository = pageRepository;
        this.bookRepository = bookRepository;
        this.bookService = bookService;
    }

    /**
     * 创建事件
     */
    public EventVO createEvent(Long bookId, EventCreateDTO dto) {
        if (!bookRepository.findById(bookId).isPresent()) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        Event event = new Event();
        event.setBookId(bookId);
        event.setName(dto.getName());
        event.setSortOrder(getNextSortOrder(bookId));

        Event savedEvent = eventRepository.save(event);
        bookService.touchContent(bookId);
        return toVO(savedEvent);
    }

    /**
     * 更新事件（仅名称）
     */
    public EventVO updateEvent(Long id, EventUpdateDTO dto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

        event.setName(dto.getName());

        Event savedEvent = eventRepository.save(event);
        bookService.touchContent(event.getBookId());
        return toVO(savedEvent);
    }

    /**
     * 删除事件
     */
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        eventRepository.deleteById(id);
        bookService.touchContent(event.getBookId());
    }

    /**
     * 获取事件详情
     */
    public EventVO getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        return toVO(event);
    }

    /**
     * 获取案件书下的所有事件
     */
    public List<EventVO> getEventsByBookId(Long bookId) {
        if (!bookRepository.findById(bookId).isPresent()) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        return eventRepository.findByBookId(bookId).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 批量排序事件
     */
    public void sortEvents(Long bookId, EventSortDTO dto) {
        List<Long> eventIds = dto.getEventIds();
        if (eventIds == null || eventIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_SORT_ORDER);
        }

        for (int i = 0; i < eventIds.size(); i++) {
            Event event = eventRepository.findById(eventIds.get(i))
                    .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));

            if (!event.getBookId().equals(bookId)) {
                throw new BusinessException(ErrorCode.INVALID_OPERATION);
            }

            eventRepository.updateSortOrder(eventIds.get(i), i + 1);
        }
        bookService.touchContent(bookId);
    }

    private int getNextSortOrder(Long bookId) {
        List<Event> events = eventRepository.findByBookId(bookId);
        return events.stream()
                .mapToInt(event -> event.getSortOrder() != null ? event.getSortOrder() : 0)
                .max()
                .orElse(0) + 1;
    }

    private EventVO toVO(Event event) {
        int pageCount = eventRepository.countPagesByEventId(event.getId());
        return new EventVO(
                event.getId(),
                event.getBookId(),
                event.getName(),
                event.getSortOrder(),
                pageCount,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

}
