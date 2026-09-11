package com.theos.detectivehelper.service;

import com.theos.detectivehelper.common.exception.BusinessException;
import com.theos.detectivehelper.domain.Book;
import com.theos.detectivehelper.dto.BookCreateDTO;
import com.theos.detectivehelper.dto.BookSortDTO;
import com.theos.detectivehelper.dto.BookUpdateDTO;
import com.theos.detectivehelper.repository.BookRepository;
import com.theos.detectivehelper.vo.BookVO;
import com.theos.detectivehelper.vo.BookWorkspaceVO;
import com.theos.detectivehelper.vo.EventVO;
import com.theos.detectivehelper.vo.RelationGraphVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 案件书服务
 */
@Service
@Transactional
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
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
                .orElseThrow(() -> new BusinessException(com.theos.detectivehelper.common.ErrorCode.BOOK_NOT_FOUND));

        book.setName(dto.getName());
        if (dto.getCoverType() != null) {
            book.setCoverType(dto.getCoverType());
        }
        book.setCoverValue(dto.getCoverValue());
        book.setCoverText(dto.getCoverText());
        if (dto.getSortOrder() != null) {
            book.setSortOrder(dto.getSortOrder());
        }

        Book savedBook = bookRepository.save(book);
        return toVO(savedBook);
    }

    /**
     * 删除案件书
     */
    public void deleteBook(Long id) {
        if (!bookRepository.findById(id).isPresent()) {
            throw new BusinessException(com.theos.detectivehelper.common.ErrorCode.BOOK_NOT_FOUND);
        }
        bookRepository.deleteById(id);
    }

    /**
     * 获取案件书详情
     */
    public BookVO getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(com.theos.detectivehelper.common.ErrorCode.BOOK_NOT_FOUND));
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
     * 获取案件书工作空间
     */
    public BookWorkspaceVO getBookWorkspace(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(com.theos.detectivehelper.common.ErrorCode.BOOK_NOT_FOUND));

        BookWorkspaceVO workspaceVO = new BookWorkspaceVO();
        workspaceVO.setId(book.getId());
        workspaceVO.setName(book.getName());
        workspaceVO.setCoverType(book.getCoverType());
        workspaceVO.setCoverValue(book.getCoverValue());
        workspaceVO.setCoverText(book.getCoverText());
        workspaceVO.setSortOrder(book.getSortOrder());
        workspaceVO.setCreatedAt(book.getCreatedAt());
        workspaceVO.setUpdatedAt(book.getUpdatedAt());

        // 暂时设置为空列表，需要通过其他服务加载
        workspaceVO.setEvents(List.of());
        workspaceVO.setRelationGraphs(List.of());

        return workspaceVO;
    }

    /**
     * 批量排序案件书
     */
    public void sortBooks(BookSortDTO dto) {
        List<Long> bookIds = dto.getBookIds();
        if (bookIds == null || bookIds.isEmpty()) {
            throw new BusinessException(com.theos.detectivehelper.common.ErrorCode.INVALID_SORT_ORDER);
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