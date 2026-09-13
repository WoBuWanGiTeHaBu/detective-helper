package com.theos.detectivehelper.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新关系图DTO
 * <p>
 * 仅承载元信息（名称），图数据通过 /api/relation-graphs/{id}/data 单独保存。
 */
public class RelationGraphUpdateDTO {

    @NotBlank(message = "关系图名称不能为空")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}