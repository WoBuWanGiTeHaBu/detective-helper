package com.theos.detectivehelper.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建关系图DTO
 * <p>
 * bookId 由路径参数 /api/books/{bookId}/relation-graphs 提供。
 */
@Data
public class RelationGraphCreateDTO {

    @NotBlank(message = "关系图名称不能为空")
    private String name;

}