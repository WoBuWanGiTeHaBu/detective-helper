package com.theos.detectivehelper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theos.detectivehelper.common.ErrorCode;
import com.theos.detectivehelper.common.exception.BusinessException;
import com.theos.detectivehelper.domain.Page;
import com.theos.detectivehelper.dto.CanvasResponse;
import com.theos.detectivehelper.dto.PageCreateDTO;
import com.theos.detectivehelper.dto.PageSortDTO;
import com.theos.detectivehelper.dto.PageUpdateDTO;
import com.theos.detectivehelper.repository.EventRepository;
import com.theos.detectivehelper.repository.PageRepository;
import com.theos.detectivehelper.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 页面服务
 * <p>
 * 页面挂在事件（event）下，画布数据以 canvas_data 字段整体存库。
 * <p>
 * 画布读写语义（重要）：
 * <ul>
 *   <li>整个画布作为一个 JSON 对象存进 canvas_data（SQLite TEXT，无长度限制），
 *       objects / relationships / annotations / timelines 是它的四个字段，
 *       因此一次 UPDATE 就是一次完整替换，天然具备事务性，不存在「清了对象但没清连线」的中间态。</li>
 *   <li>{@code PUT /api/pages/{id}/canvas} 是<b>整体覆盖</b>：请求体就是新的完整画布，
 *       某个数组传 {@code []} 即表示清空该部分。</li>
 *   <li>{@code GET} 返回的四个数组一定非 null（最坏是 {@code []}），可以直接当作 PUT 的请求体回传。</li>
 * </ul>
 */
@Service
@Transactional
public class PageService {

    /** 画布数据单页上限：1MB */
    private static final int MAX_CANVAS_LENGTH = 1024 * 1024;

    /** 画布背景默认值 */
    private static final String DEFAULT_BACKGROUND = "plain";

    private final PageRepository pageRepository;
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final BookService bookService;

    public PageService(PageRepository pageRepository, EventRepository eventRepository,
                       ObjectMapper objectMapper, BookService bookService) {
        this.pageRepository = pageRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.bookService = bookService;
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
        touchBookByEvent(eventId);
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
        touchBookByEvent(page.getEventId());
        return toVO(savedPage);
    }

    /**
     * 删除页面
     */
    public void deletePage(Long id) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAGE_NOT_FOUND));
        pageRepository.deleteById(id);
        touchBookByEvent(page.getEventId());
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
     * <p>
     * 返回结构化的完整画布（四个数组保证非 null），前端拿到的东西可以直接原样 PUT 回去。
     */
    public CanvasResponse getPageCanvas(Long id) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAGE_NOT_FOUND));
        return decodeCanvas(page.getCanvasData());
    }

    /**
     * 保存画布数据（整体覆盖）
     *
     * @param canvas 新的完整画布；四个数组为 null 时按空数组处理
     * @return 实际写库后的完整画布，便于前端校正本地状态
     */
    public CanvasResponse savePageCanvas(Long id, CanvasResponse canvas) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAGE_NOT_FOUND));

        if (canvas == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "画布数据不能为空");
        }

        CanvasResponse normalized = normalize(canvas);
        String canvasData;
        try {
            canvasData = objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "画布数据格式不合法");
        }

        // 按字符数估算，中文等多字节字符会被低估，这里只做粗粒度兜底
        if (canvasData.length() > MAX_CANVAS_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "画布数据超过大小限制");
        }

        page.setCanvasData(canvasData);
        pageRepository.save(page);
        touchBookByEvent(page.getEventId());
        return normalized;
    }

    /**
     * 把库里的 canvas_data 还原成结构化画布；空值或损坏时返回空画布，不让前端拿到 null
     */
    private CanvasResponse decodeCanvas(String canvasData) {
        if (canvasData == null || canvasData.isBlank()) {
            return emptyCanvas();
        }
        try {
            return normalize(objectMapper.readValue(canvasData, CanvasResponse.class));
        } catch (Exception e) {
            return emptyCanvas();
        }
    }

    /** 补齐缺省字段，保证读写两端形状一致 */
    private CanvasResponse normalize(CanvasResponse canvas) {
        if (canvas == null) {
            return emptyCanvas();
        }
        if (canvas.getObjects() == null) {
            canvas.setObjects(new ArrayList<>());
        }
        if (canvas.getRelationships() == null) {
            canvas.setRelationships(new ArrayList<>());
        }
        if (canvas.getAnnotations() == null) {
            canvas.setAnnotations(new ArrayList<>());
        }
        if (canvas.getTimelines() == null) {
            canvas.setTimelines(new ArrayList<>());
        }
        if (canvas.getBackground() == null || canvas.getBackground().isBlank()) {
            canvas.setBackground(DEFAULT_BACKGROUND);
        }
        return canvas;
    }

    private CanvasResponse emptyCanvas() {
        CanvasResponse canvas = new CanvasResponse();
        canvas.setObjects(new ArrayList<>());
        canvas.setRelationships(new ArrayList<>());
        canvas.setAnnotations(new ArrayList<>());
        canvas.setTimelines(new ArrayList<>());
        canvas.setBackground(DEFAULT_BACKGROUND);
        return canvas;
    }

    /**
     * 把页面所属事件映射回案件书，刷新其内容改动时间
     */
    private void touchBookByEvent(Long eventId) {
        eventRepository.findById(eventId)
                .ifPresent(event -> bookService.touchContent(event.getBookId()));
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
        touchBookByEvent(eventId);
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
        touchBookByEvent(eventId);
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
