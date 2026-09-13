package com.theos.detectivehelper.controller;

import com.theos.detectivehelper.common.Result;
import com.theos.detectivehelper.dto.PageCreateDTO;
import com.theos.detectivehelper.dto.PageUpdateDTO;
import com.theos.detectivehelper.dto.SortItem;
import com.theos.detectivehelper.service.PageService;
import com.theos.detectivehelper.vo.CanvasVO;
import com.theos.detectivehelper.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 页面控制器
 * <p>
 * 页面隶属于事件：列表与创建走 /api/events/{eventId}/pages，
 * 单条更新/删除走 /api/pages/{id}，画布读写走 /api/pages/{id}/canvas。
 */
@RestController
public class PageController {

    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    /**
     * 获取事件下的页面列表
     */
    @GetMapping("/api/events/{eventId}/pages")
    public Result<List<PageVO>> listPages(@PathVariable Long eventId) {
        List<PageVO> pages = pageService.getPagesByEventId(eventId);
        return Result.success(pages);
    }

    /**
     * 创建页面
     */
    @PostMapping("/api/events/{eventId}/pages")
    public Result<PageVO> createPage(@PathVariable Long eventId, @Valid @RequestBody PageCreateDTO dto) {
        PageVO pageVO = pageService.createPage(eventId, dto);
        return Result.success(pageVO);
    }

    /**
     * 批量排序页面
     */
    @PutMapping("/api/events/{eventId}/pages/sort")
    public Result<Void> sortPages(@PathVariable Long eventId, @Valid @RequestBody List<SortItem> items) {
        pageService.sortPages(items.stream().map(SortItem::getId).collect(Collectors.toList()), eventId);
        return Result.success();
    }

    /**
     * 获取页面详情
     */
    @GetMapping("/api/pages/{id}")
    public Result<PageVO> getPageById(@PathVariable Long id) {
        PageVO pageVO = pageService.getPageById(id);
        return Result.success(pageVO);
    }

    /**
     * 更新页面（仅名称）
     */
    @PutMapping("/api/pages/{id}")
    public Result<PageVO> updatePage(@PathVariable Long id, @Valid @RequestBody PageUpdateDTO dto) {
        PageVO pageVO = pageService.updatePage(id, dto);
        return Result.success(pageVO);
    }

    /**
     * 删除页面
     */
    @DeleteMapping("/api/pages/{id}")
    public Result<Void> deletePage(@PathVariable Long id) {
        pageService.deletePage(id);
        return Result.success();
    }

    /**
     * 获取画布数据
     */
    @GetMapping("/api/pages/{id}/canvas")
    public Result<CanvasVO> getCanvas(@PathVariable Long id) {
        PageVO pageVO = pageService.getPageCanvas(id);
        return Result.success(new CanvasVO(id, pageVO.getCanvasData()));
    }

    /**
     * 保存画布数据
     */
    @PutMapping("/api/pages/{id}/canvas")
    public Result<Void> saveCanvas(@PathVariable Long id, @RequestBody String canvasData) {
        pageService.savePageCanvas(id, canvasData);
        return Result.success();
    }

}
