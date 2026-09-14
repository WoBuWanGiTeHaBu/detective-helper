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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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
     * 上传书籍封面图片（multipart/form-data，字段名 file，JPEG/PNG/WebP，≤2MB）
     * <p>
     * 成功返回 {"coverValue":"covers/book_x.webp"}，书籍 coverType 自动切为 image；
     * 静态访问走 /files/covers/**（前端 coverUrl() 拼 STATIC_BASE=/files）。
     */
    @PostMapping("/{id}/cover")
    public Result<Map<String, String>> uploadBookCover(@PathVariable Long id,
                                                       @RequestParam("file") MultipartFile file) {
        return Result.success(bookService.uploadCover(id, file));
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