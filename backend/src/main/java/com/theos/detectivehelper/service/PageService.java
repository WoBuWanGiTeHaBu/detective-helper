package com.theos.detectivehelper.service;

import com.theos.detectivehelper.common.exception.BusinessException;
import com.theos.detectivehelper.domain.Page;
import com.theos.detectivehelper.dto.PageCreateDTO;
import com.theos.detectivehelper.dto.PageSortDTO;
import com.theos.detectivehelper.dto.PageUpdateDTO;
import com.theos.detectivehelper.repository.BookRepository;
import com.theos.detectivehelper.repository.PageRepository;
import com.theos.detectivehelper.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 页面服务
 */
@Service
@Transactional
public class PageService {

    private final PageRepository pageRepository;
    private final BookRepository bookRepository;

    public PageService(PageRepository pageRepository, BookRepository bookRepository) {
        this.pageRepository = pageRepository;
        this.bookRepository = bookRepository;
    }

    /**
     * 创建页面
     */
    public PageVO createPage(PageCreateDTO dto) {
        if (!bookRepository.findById(dto.getBookId()).isPresent()) {
            throw new BusinessException(com.theos.detectivehelper.common.ErrorCode.BOOK_NOT_FOUND);
        }

        Page page = new Page();
        page.setBookId(dto.getBookId());
        page.setTitle(dto.getTitle());
        page.setContent(dto.getContent());
        page.setCanvasData(dto.getCanvasData());
        page.setSortOrder(getNextSortOrder(dto.getBookId()));

        Page savedPage = pageRepository.save(page);
        return toVO(savedPage, 0);
    }

    /**
     * 更新页面
     */
    public PageVO updatePage(Long id, PageUpdateDTO dto) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(com.theos.detectivehelper.common.ErrorCode.PAGE_NOT_FOUND));

        page.setTitle(dto.getTitle());
        page.setContent(dto.getContent());
        page.setCanvasData(dto.getCanvasData());
        if (dto.getSortOrder() != null) {
            page.setSortOrder(dto.getSortOrder());
        }

        Page savedPage = pageRepository.save(page);
        return toVO(savedPage, 0);
    }

    /**
     * 删除页面
     */
    public void deletePage(Long id) {
        if (!pageRepository.findById(id).isPresent()) {
            throw new BusinessException(com.theos.detectivehelper.common.ErrorCode.PAGE_NOT_FOUND);
        }
        pageRepository.deleteById(id);
    }

    /**
     * 获取页面详情
     */
    public PageVO getPageById(Long id) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(com.theos.detectivehelper.common.ErrorCode.PAGE_NOT_FOUND));

        int eventCount = page.getEvents() != null ? page.getEvents().size() : 0;
        return toVO(page, eventCount);
    }

    /**
     * 获取案件书的所有页面
     */
    public List<PageVO> getPagesByBookId(Long bookId) {
        return pageRepository.findByBookId(bookId).stream()
                .map(page -> {
                    int eventCount = page.getEvents() != null ? page.getEvents().size() : 0;
                    return toVO(page, eventCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 保存页面画布数据
     */
    public PageVO savePageCanvas(Long id, String canvasData) {
        Page page = pageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(com.theos.detectivehelper.common.ErrorCode.PAGE_NOT_FOUND));

        page.setCanvasData(canvasData);
        Page savedPage = pageRepository.save(page);

        int eventCount = savedPage.getEvents() != null ? savedPage.getEvents().size() : 0;
        return toVO(savedPage, eventCount);
    }

    /**
     * 批量排序页面
     */
    public void sortPages(Long bookId, PageSortDTO dto) {
        List<Long> pageIds = dto.getPageIds();
        if (pageIds == null || pageIds.isEmpty()) {
            throw new BusinessException(com.theos.detectivehelper.common.ErrorCode.INVALID_SORT_ORDER);
        }

        for (int i = 0; i < pageIds.size(); i++) {
            Page page = pageRepository.findById(pageIds.get(i))
                    .orElseThrow(() -> new BusinessException(com.theos.detectivehelper.common.ErrorCode.PAGE_NOT_FOUND));

            if (!page.getBookId().equals(bookId)) {
                throw new BusinessException(com.theos.detectivehelper.common.ErrorCode.INVALID_OPERATION);
            }

            pageRepository.updateSortOrder(pageIds.get(i), i + 1);
        }
    }

    private int getNextSortOrder(Long bookId) {
        List<Page> pages = pageRepository.findByBookId(bookId);
        return pages.stream()
                .mapToInt(page -> page.getSortOrder() != null ? page.getSortOrder() : 0)
                .max()
                .orElse(0) + 1;
    }

    private PageVO toVO(Page page, int eventCount) {
        return new PageVO(
                page.getId(),
                page.getBookId(),
                page.getTitle(),
                page.getContent(),
                page.getCanvasData(),
                page.getSortOrder(),
                eventCount,
                page.getCreatedAt(),
                page.getUpdatedAt()
        );
    }

}