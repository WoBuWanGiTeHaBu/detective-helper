package com.theos.detectivehelper.controller;

import com.theos.detectivehelper.common.Result;
import com.theos.detectivehelper.service.PageService;
import com.theos.detectivehelper.vo.CanvasVO;
import org.springframework.web.bind.annotation.*;

/**
 * 画布控制器
 */
@RestController
@RequestMapping("/api/canvas")
public class CanvasController {

    private final PageService pageService;

    public CanvasController(PageService pageService) {
        this.pageService = pageService;
    }

    /**
     * 获取页面画布数据
     */
    @GetMapping("/page/{pageId}")
    public Result<CanvasVO> getPageCanvas(@PathVariable Long pageId) {
        com.theos.detectivehelper.vo.PageVO pageVO = pageService.getPageById(pageId);
        CanvasVO canvasVO = new CanvasVO(pageId, pageVO.getCanvasData());
        return Result.success(canvasVO);
    }

    /**
     * 保存页面画布数据
     */
    @PutMapping("/page/{pageId}")
    public Result<CanvasVO> savePageCanvas(@PathVariable Long pageId, @RequestBody String canvasData) {
        com.theos.detectivehelper.vo.PageVO pageVO = pageService.savePageCanvas(pageId, canvasData);
        CanvasVO canvasVO = new CanvasVO(pageId, pageVO.getCanvasData());
        return Result.success(canvasVO);
    }

}