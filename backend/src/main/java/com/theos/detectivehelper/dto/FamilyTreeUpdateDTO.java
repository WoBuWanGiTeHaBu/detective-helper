package com.theos.detectivehelper.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新族谱图DTO
 */
public class FamilyTreeUpdateDTO {

    @NotBlank(message = "族谱图名称不能为空")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
