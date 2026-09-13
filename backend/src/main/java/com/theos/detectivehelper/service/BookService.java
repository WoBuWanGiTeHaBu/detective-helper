package com.theos.detectivehelper.service;

import com.theos.detectivehelper.common.ErrorCode;
import com.theos.detectivehelper.common.exception.BusinessException;
import com.theos.detectivehelper.domain.Book;
import com.theos.detectivehelper.domain.Event;
import com.theos.detectivehelper.domain.Page;
import com.theos.detectivehelper.domain.RelationGraph;
import com.theos.detectivehelper.dto.BookCreateDTO;
import com.theos.detectivehelper.dto.BookSortDTO;
import com.theos.detectivehelper.dto.BookUpdateDTO;
import com.theos.detectivehelper.repository.BookRepository;
import com.theos.detectivehelper.repository.EventRepository;
import com.theos.detectivehelper.repository.PageRepository;
import com.theos.detectivehelper.repository.RelationGraphRepository;
import com.theos.detectivehelper.vo.BookVO;
import com.theos.detectivehelper.vo.BookWorkspaceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 案件书服务
 */
@Service
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final EventRepository eventRepository;
    private final PageRepository pageRepository;
    private final RelationGraphRepository relationGraphRepository;

    public BookService(BookRepository bookRepository,
                       EventRepository eventRepository,
                       PageRepository pageRepository,
                       RelationGraphRepository relationGraphRepository) {
        this.bookRepository = bookRepository;
        this.eventRepository = eventRepository;
        this.pageRepository = pageRepository;
        this.relationGraphRepository = relationGraphRepository;
    }

    /**
     * 创建案件书
     */
    public BookVO createBook(BookCreateDTO dto) {
        Book book = new Book();
        book.setName(dto.getName());
        book.setCoverType(dto.getCoverType() != null ? dto.getCoverType() : "color");
        book.setCoverValue(dto.getCoverValue());
        book.setCoverText(dto.getCoverText());
        book.setSortOrder(getNextSortOrder());

        Book savedBook = bookRepository.save(book);
        return toVO(savedBook);
    }

    /**
     * 更新案件书
     */
    public BookVO updateBook(Long id, BookUpdateDTO dto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        book.setName(dto.getName());
        if (dto.getCoverType() != null) {
            book.setCoverType(dto.getCoverType());
        }
        book.setCoverValue(dto.getCoverValue());
        book.setCoverText(dto.getCoverText());

        Book savedBook = bookRepository.save(book);
        return toVO(savedBook);
    }

    /**
     * 删除案件书（级联删除事件、页面、关系图）
     */
    public void deleteBook(Long id) {
        if (!bookRepository.findById(id).isPresent()) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        for (Event event : eventRepository.findByBookId(id)) {
            pageRepository.deleteByEventId(event.getId());
        }
        eventRepository.deleteByBookId(id);
        relationGraphRepository.deleteByBookId(id);
        bookRepository.deleteById(id);
    }

    /**
     * 获取案件书详情
     */
    public BookVO getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
        return toVO(book);
    }

    /**
     * 获取所有案件书
     */
    public List<BookVO> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取案件书工作空间：案件书 + 事件（含页面）+ 关系图
     */
    public BookWorkspaceVO getBookWorkspace(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        List<BookWorkspaceVO.WorkspaceEventVO> events = new ArrayList<>();
        for (Event event : eventRepository.findByBookId(id)) {
            List<BookWorkspaceVO.WorkspacePageVO> pages = pageRepository.findByEventId(event.getId()).stream()
                    .map(this::toWorkspacePage)
                    .collect(Collectors.toList());

            events.add(new BookWorkspaceVO.WorkspaceEventVO(
                    event.getId(),
                    event.getName(),
                    event.getSortOrder(),
                    pages
            ));
        }

        List<BookWorkspaceVO.WorkspaceRelationGraphVO> relationGraphs = relationGraphRepository.findByBookId(id).stream()
                .map(this::toWorkspaceRelationGraph)
                .collect(Collectors.toList());

        return new BookWorkspaceVO(toVO(book), events, relationGraphs);
    }

    /**
     * 批量排序案件书
     */
    public void sortBooks(BookSortDTO dto) {
        List<Long> bookIds = dto.getBookIds();
        if (bookIds == null || bookIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_SORT_ORDER);
        }

        for (int i = 0; i < bookIds.size(); i++) {
            bookRepository.updateSortOrder(bookIds.get(i), i + 1);
        }
    }

    private int getNextSortOrder() {
        List<Book> books = bookRepository.findAll();
        return books.stream()
                .mapToInt(book -> book.getSortOrder() != null ? book.getSortOrder() : 0)
                .max()
                .orElse(0) + 1;
    }

    private BookWorkspaceVO.WorkspacePageVO toWorkspacePage(Page page) {
        return new BookWorkspaceVO.WorkspacePageVO(page.getId(), page.getName(), page.getSortOrder());
    }

    private BookWorkspaceVO.WorkspaceRelationGraphVO toWorkspaceRelationGraph(RelationGraph graph) {
        return new BookWorkspaceVO.WorkspaceRelationGraphVO(graph.getId(), graph.getName());
    }

    private BookVO toVO(Book book) {
        int eventCount = bookRepository.countEventsByBookId(book.getId());
        int pageCount = bookRepository.countPagesByBookId(book.getId());

        return new BookVO(
                book.getId(),
                book.getName(),
                book.getCoverType(),
                book.getCoverValue(),
                book.getCoverText(),
                book.getSortOrder(),
                eventCount,
                pageCount,
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }

}
