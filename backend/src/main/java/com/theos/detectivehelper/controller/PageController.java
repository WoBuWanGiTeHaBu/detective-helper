package com.theos.detectivehelper.controller;

import com.theos.detectivehelper.common.Result;
import com.theos.detectivehelper.dto.PageCreateDTO;
import com.theos.detectivehelper.dto.PageSortDTO;
import com.theos.detectivehelper.dto.PageUpdateDTO;
import com.theos.detectivehelper.service.PageService;
import com.theos.detectivehelper.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 页面控制器
 */
@RestController
@RequestMapping("/api/pages")
public class PageController {

    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    /**
     * 创建页面
     */
    @PostMapping
    public Result<PageVO> createPage(@Valid @RequestBody PageCreateDTO dto) {
        PageVO pageVO = pageService.createPage(dto);
        return Result.success(pageVO);
    }

    /**
     * 更新页面
     */
    @PutMapping("/{id}")
    public Result<PageVO> updatePage(@PathVariable Long id, @Valid @RequestBody PageUpdateDTO dto) {
        PageVO pageVO = pageService.updatePage(id, dto);
        return Result.success(pageVO);
    }

    /**
     * 删除页面
     */
    @DeleteMapping("/{id}")
    public Result<Void> deletePage(@PathVariable Long id) {
        pageService.deletePage(id);
        return Result.success();
    }

    /**
     * 获取页面详情
     */
    @GetMapping("/{id}")
    public Result<PageVO> getPageById(@PathVariable Long id) {
        PageVO pageVO = pageService.getPageById(id);
        return Result.success(pageVO);
    }

    /**
     * 获取案件书的所有页面
     */
    @GetMapping("/book/{bookId}")
    public Result<List<PageVO>> getPagesByBookId(@PathVariable Long bookId) {
        List<PageVO> pages = pageService.getPagesByBookId(bookId);
        return Result.success(pages);
    }

    /**
     * 保存页面画布数据
     */
    @PutMapping("/{id}/canvas")
    public Result<PageVO> savePageCanvas(@PathVariable Long id, @RequestBody String canvasData) {
        PageVO pageVO = pageService.savePageCanvas(id, canvasData);
        return Result.success(pageVO);
    }

    /**
     * 批量排序页面
     */
    @PostMapping("/book/{bookId}/sort")
    public Result<Void> sortPages(@PathVariable Long bookId, @Valid @RequestBody PageSortDTO dto) {
        pageService.sortPages(bookId, dto);
        return Result.success();
    }

}