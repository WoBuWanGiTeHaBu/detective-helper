package com.theos.detectivehelper.controller;

import com.theos.detectivehelper.common.Result;
import com.theos.detectivehelper.dto.BookCreateDTO;
import com.theos.detectivehelper.dto.BookSortDTO;
import com.theos.detectivehelper.dto.BookUpdateDTO;
import com.theos.detectivehelper.service.BookService;
import com.theos.detectivehelper.vo.BookVO;
import com.theos.detectivehelper.vo.BookWorkspaceVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 案件书控制器
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * 创建案件书
     */
    @PostMapping
    public Result<BookVO> createBook(@Valid @RequestBody BookCreateDTO dto) {
        BookVO bookVO = bookService.createBook(dto);
        return Result.success(bookVO);
    }

    /**
     * 更新案件书
     */
    @PutMapping("/{id}")
    public Result<BookVO> updateBook(@PathVariable Long id, @Valid @RequestBody BookUpdateDTO dto) {
        BookVO bookVO = bookService.updateBook(id, dto);
        return Result.success(bookVO);
    }

    /**
     * 删除案件书
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return Result.success();
    }

    /**
     * 获取案件书详情
     */
    @GetMapping("/{id}")
    public Result<BookVO> getBookById(@PathVariable Long id) {
        BookVO bookVO = bookService.getBookById(id);
        return Result.success(bookVO);
    }

    /**
     * 获取所有案件书
     */
    @GetMapping
    public Result<List<BookVO>> getAllBooks() {
        List<BookVO> books = bookService.getAllBooks();
        return Result.success(books);
    }

    /**
     * 获取案件书工作空间
     */
    @GetMapping("/{id}/workspace")
    public Result<BookWorkspaceVO> getBookWorkspace(@PathVariable Long id) {
        BookWorkspaceVO workspaceVO = bookService.getBookWorkspace(id);
        return Result.success(workspaceVO);
    }

    /**
     * 批量排序案件书
     */
    @PutMapping("/sort")
    public Result<Void> sortBooks(@Valid @RequestBody BookSortDTO dto) {
        bookService.sortBooks(dto);
        return Result.success();
    }

}