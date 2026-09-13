package com.theos.detectivehelper.service;

import com.theos.detectivehelper.common.ErrorCode;
import com.theos.detectivehelper.common.exception.BusinessException;
import com.theos.detectivehelper.domain.Page;
import com.theos.detectivehelper.dto.PageCreateDTO;
import com.theos.detectivehelper.dto.PageSortDTO;
import com.theos.detectivehelper.dto.PageUpdateDTO;
import com.theos.detectivehelper.repository.EventRepository;
import com.theos.detectivehelper.repository.PageRepository;
import com.theos.detectivehelper.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 页面服务
 * <p>
 * 页面挂在事件（event）下，画布数据以 canvas_data 字段整体存库。
 */
@Service
@Transactional
public class PageService {

    /** 画布数据单页上限：1MB */
    private static final int MAX_CANVAS_LENGTH = 1024 * 1024;

    private final PageRepository pageRepository;
    private final EventRepository eventRepository;

    public PageService(PageRepository pageRepository, EventRepository eventRepository) {
        this.pageRepository = pageRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * 创建页面
     */
    public PageVO createPage(Long eventId, PageCreateDTO dto) {
        if (!eventRepository.findById(eventId).isPresent()) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        Page page = new Page();
        page.setEventId(eventId);
        page.setName(dto.getName());
        page.setSortOrder(getNextSortOrder(eventId));

        Page savedPage = pageRepository.save(page);
        return toVO(savedPage);
    }

    /**
     * 更新页面（仅名称）
     */
    public PageVO updatePage(Long id, PageUpdateDTO dto) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAGE_NOT_FOUND));

        page.setName(dto.getName());

        Page savedPage = pageRepository.save(page);
        return toVO(savedPage);
    }

    /**
     * 删除页面
     */
    public void deletePage(Long id) {
        if (!pageRepository.findById(id).isPresent()) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND);
        }
        pageRepository.deleteById(id);
    }

    /**
     * 获取页面详情
     */
    public PageVO getPageById(Long id) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAGE_NOT_FOUND));
        return toVO(page);
    }

    /**
     * 获取事件下的所有页面
     */
    public List<PageVO> getPagesByEventId(Long eventId) {
        if (!eventRepository.findById(eventId).isPresent()) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        return pageRepository.findByEventId(eventId).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取画布数据
     */
    public PageVO getPageCanvas(Long id) {
        return getPageById(id);
    }

    /**
     * 保存画布数据
     */
    public PageVO savePageCanvas(Long id, String canvasData) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAGE_NOT_FOUND));

        if (canvasData != null && canvasData.length() > MAX_CANVAS_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "画布数据超过大小限制");
        }

        page.setCanvasData(canvasData);
        Page savedPage = pageRepository.save(page);
        return toVO(savedPage);
    }

    /**
     * 批量排序页面
     */
    public void sortPages(Long eventId, PageSortDTO dto) {
        List<Long> pageIds = dto.getPageIds();
        if (pageIds == null || pageIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_SORT_ORDER);
        }

        for (int i = 0; i < pageIds.size(); i++) {
            Page page = pageRepository.findById(pageIds.get(i))
                    .orElseThrow(() -> new BusinessException(ErrorCode.PAGE_NOT_FOUND));

            if (!page.getEventId().equals(eventId)) {
                throw new BusinessException(ErrorCode.INVALID_OPERATION);
            }

            pageRepository.updateSortOrder(pageIds.get(i), i + 1);
        }
    }

    /**
     * 批量排序页面（请求体为 SortItem 列表，按给定顺序从 0 开始编号）
     */
    public void sortPages(List<Long> pageIds, Long eventId) {
        if (pageIds == null || pageIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_SORT_ORDER);
        }

        for (int i = 0; i < pageIds.size(); i++) {
            Page page = pageRepository.findById(pageIds.get(i))
                    .orElseThrow(() -> new BusinessException(ErrorCode.PAGE_NOT_FOUND));

            if (!page.getEventId().equals(eventId)) {
                throw new BusinessException(ErrorCode.INVALID_OPERATION);
            }

            pageRepository.updateSortOrder(pageIds.get(i), i);
        }
    }

    private int getNextSortOrder(Long eventId) {
        List<Page> pages = pageRepository.findByEventId(eventId);
        return pages.stream()
                .mapToInt(page -> page.getSortOrder() != null ? page.getSortOrder() : 0)
                .max()
                .orElse(0) + 1;
    }

    private PageVO toVO(Page page) {
        return new PageVO(
                page.getId(),
                page.getEventId(),
                page.getName(),
                page.getSortOrder(),
                page.getCanvasData(),
                page.getCreatedAt(),
                page.getUpdatedAt()
        );
    }

}
