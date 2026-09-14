package com.theos.detectivehelper.controller;

import com.theos.detectivehelper.common.Result;
import com.theos.detectivehelper.dto.CanvasResponse;
import com.theos.detectivehelper.dto.PageCreateDTO;
import com.theos.detectivehelper.dto.PageUpdateDTO;
import com.theos.detectivehelper.dto.SortItem;
import com.theos.detectivehelper.service.PageService;
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
     * <p>
     * 返回完整的画布对象（objects / relationships / annotations / timelines 均非 null），
     * 与 PUT 的请求体同构，可原样回传。
     */
    @GetMapping("/api/pages/{id}/canvas")
    public Result<CanvasResponse> getCanvas(@PathVariable Long id) {
        return Result.success(pageService.getPageCanvas(id));
    }

    /**
     * 保存画布数据（整体覆盖 + 事务性写入）
     * <p>
     * 请求体就是新的完整画布，后端不做合并：某个数组传 {@code []} 即清空该部分。
     * 成功时回写保存后的完整画布，code 恒为 200。
     */
    @PutMapping("/api/pages/{id}/canvas")
    public Result<CanvasResponse> saveCanvas(@PathVariable Long id,
                                             @RequestBody(required = false) CanvasResponse canvas) {
        return Result.success(pageService.savePageCanvas(id, canvas));
    }

}
