package com.theos.detectivehelper.controller;

import com.theos.detectivehelper.common.Result;
import com.theos.detectivehelper.dto.RelationGraphCreateDTO;
import com.theos.detectivehelper.dto.RelationGraphExtractDTO;
import com.theos.detectivehelper.dto.RelationGraphUpdateDTO;
import com.theos.detectivehelper.service.RelationGraphService;
import com.theos.detectivehelper.vo.RelationGraphDetailVO;
import com.theos.detectivehelper.vo.RelationGraphVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关系图控制器
 */
@RestController
@RequestMapping("/api/relation-graphs")
public class RelationGraphController {

    private final RelationGraphService relationGraphService;

    public RelationGraphController(RelationGraphService relationGraphService) {
        this.relationGraphService = relationGraphService;
    }

    /**
     * 创建关系图
     */
    @PostMapping
    public Result<RelationGraphVO> createRelationGraph(@Valid @RequestBody RelationGraphCreateDTO dto) {
        RelationGraphVO graphVO = relationGraphService.createRelationGraph(dto);
        return Result.success(graphVO);
    }

    /**
     * 更新关系图
     */
    @PutMapping("/{id}")
    public Result<RelationGraphVO> updateRelationGraph(@PathVariable Long id, @Valid @RequestBody RelationGraphUpdateDTO dto) {
        RelationGraphVO graphVO = relationGraphService.updateRelationGraph(id, dto);
        return Result.success(graphVO);
    }

    /**
     * 删除关系图
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRelationGraph(@PathVariable Long id) {
        relationGraphService.deleteRelationGraph(id);
        return Result.success();
    }

    /**
     * 获取关系图详情
     */
    @GetMapping("/{id}")
    public Result<RelationGraphDetailVO> getRelationGraphById(@PathVariable Long id) {
        RelationGraphDetailVO graphVO = relationGraphService.getRelationGraphById(id);
        return Result.success(graphVO);
    }

    /**
     * 获取案件书的所有关系图
     */
    @GetMapping("/book/{bookId}")
    public Result<List<RelationGraphVO>> getRelationGraphsByBookId(@PathVariable Long bookId) {
        List<RelationGraphVO> graphs = relationGraphService.getRelationGraphsByBookId(bookId);
        return Result.success(graphs);
    }

    /**
     * 从案件中提取关系图
     */
    @PostMapping("/extract")
    public Result<RelationGraphVO> extractRelationGraph(@Valid @RequestBody RelationGraphExtractDTO dto) {
        RelationGraphVO graphVO = relationGraphService.extractRelationGraph(dto);
        return Result.success(graphVO);
    }

}