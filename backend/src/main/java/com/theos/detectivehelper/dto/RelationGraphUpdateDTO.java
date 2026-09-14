package com.theos.detectivehelper.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新关系图DTO
 * <p>
 * 仅承载元信息（名称），图数据通过 /api/relation-graphs/{id}/data 单独保存。
 */
@Data
public class RelationGraphUpdateDTO {

    @NotBlank(message = "关系图名称不能为空")
    private String name;

}