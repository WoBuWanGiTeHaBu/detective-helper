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
import java.util.Map;

/**
 * 关系图控制器
 * <p>
 * 关系图隶属于案件书：列表与创建走 /api/books/{bookId}/relation-graphs，
 * 单条操作走 /api/relation-graphs/{id}，图数据走 /api/relation-graphs/{id}/data，
 * 画布提取走 /api/books/{bookId}/relation-graphs/extract。
 */
@RestController
public class RelationGraphController {

    private final RelationGraphService relationGraphService;

    public RelationGraphController(RelationGraphService relationGraphService) {
        this.relationGraphService = relationGraphService;
    }

    /**
     * 获取案件书下的关系图列表
     */
    @GetMapping("/api/books/{bookId}/relation-graphs")
    public Result<List<RelationGraphVO>> listRelationGraphs(@PathVariable Long bookId) {
        return Result.success(relationGraphService.getRelationGraphsByBookId(bookId));
    }

    /**
     * 创建关系图
     */
    @PostMapping("/api/books/{bookId}/relation-graphs")
    public Result<RelationGraphVO> createRelationGraph(@PathVariable Long bookId,
                                                       @Valid @RequestBody RelationGraphCreateDTO dto) {
        return Result.success(relationGraphService.createRelationGraph(bookId, dto));
    }

    /**
     * 从画布提取关系图数据
     */
    @PostMapping("/api/books/{bookId}/relation-graphs/extract")
    public Result<Map<String, Object>> extractRelationGraph(@PathVariable Long bookId,
                                                            @Valid @RequestBody RelationGraphExtractDTO dto) {
        return Result.success(relationGraphService.extractRelationGraph(bookId, dto));
    }

    /**
     * 获取关系图详情
     */
    @GetMapping("/api/relation-graphs/{id}")
    public Result<RelationGraphDetailVO> getRelationGraph(@PathVariable Long id) {
        return Result.success(relationGraphService.getRelationGraphById(id));
    }

    /**
     * 更新关系图元信息（仅名称）
     */
    @PutMapping("/api/relation-graphs/{id}")
    public Result<RelationGraphVO> updateRelationGraph(@PathVariable Long id,
                                                       @Valid @RequestBody RelationGraphUpdateDTO dto) {
        return Result.success(relationGraphService.updateRelationGraph(id, dto));
    }

    /**
     * 删除关系图
     */
    @DeleteMapping("/api/relation-graphs/{id}")
    public Result<Void> deleteRelationGraph(@PathVariable Long id) {
        relationGraphService.deleteRelationGraph(id);
        return Result.success();
    }

    /**
     * 保存关系图数据
     */
    @PutMapping("/api/relation-graphs/{id}/data")
    public Result<Void> saveRelationGraphData(@PathVariable Long id, @RequestBody String data) {
        relationGraphService.saveRelationGraphData(id, data);
        return Result.success();
    }

}
