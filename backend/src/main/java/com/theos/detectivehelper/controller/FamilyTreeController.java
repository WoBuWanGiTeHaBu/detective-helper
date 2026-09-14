package com.theos.detectivehelper.controller;

import com.theos.detectivehelper.common.Result;
import com.theos.detectivehelper.dto.FamilyTreeCreateDTO;
import com.theos.detectivehelper.dto.FamilyTreeUpdateDTO;
import com.theos.detectivehelper.service.FamilyTreeService;
import com.theos.detectivehelper.vo.FamilyTreeDetailVO;
import com.theos.detectivehelper.vo.FamilyTreeVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 族谱图控制器
 * <p>
 * 族谱图隶属于案件书：列表与创建走 /api/books/{bookId}/family-trees，
 * 单条操作走 /api/family-trees/{id}，图数据走 /api/family-trees/{id}/data。
 */
@RestController
public class FamilyTreeController {

    private final FamilyTreeService familyTreeService;

    public FamilyTreeController(FamilyTreeService familyTreeService) {
        this.familyTreeService = familyTreeService;
    }

    /**
     * 获取案件书下的族谱图列表
     */
    @GetMapping("/api/books/{bookId}/family-trees")
    public Result<List<FamilyTreeVO>> listFamilyTrees(@PathVariable Long bookId) {
        return Result.success(familyTreeService.getFamilyTreesByBookId(bookId));
    }

    /**
     * 创建族谱图
     */
    @PostMapping("/api/books/{bookId}/family-trees")
    public Result<FamilyTreeVO> createFamilyTree(@PathVariable Long bookId,
                                                 @Valid @RequestBody FamilyTreeCreateDTO dto) {
        return Result.success(familyTreeService.createFamilyTree(bookId, dto));
    }

    /**
     * 获取族谱图详情
     */
    @GetMapping("/api/family-trees/{id}")
    public Result<FamilyTreeDetailVO> getFamilyTree(@PathVariable Long id) {
        return Result.success(familyTreeService.getFamilyTreeById(id));
    }

    /**
     * 更新族谱图元信息（仅名称）
     */
    @PutMapping("/api/family-trees/{id}")
    public Result<FamilyTreeVO> updateFamilyTree(@PathVariable Long id,
                                                 @Valid @RequestBody FamilyTreeUpdateDTO dto) {
        return Result.success(familyTreeService.updateFamilyTree(id, dto));
    }

    /**
     * 删除族谱图
     */
    @DeleteMapping("/api/family-trees/{id}")
    public Result<Void> deleteFamilyTree(@PathVariable Long id) {
        familyTreeService.deleteFamilyTree(id);
        return Result.success();
    }

    /**
     * 保存族谱图数据
     */
    @PutMapping("/api/family-trees/{id}/data")
    public Result<Void> saveFamilyTreeData(@PathVariable Long id, @RequestBody String data) {
        familyTreeService.saveFamilyTreeData(id, data);
        return Result.success();
    }

}
