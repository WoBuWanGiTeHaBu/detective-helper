package com.theos.detectivehelper.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建族谱图DTO
 */
@Data
public class FamilyTreeCreateDTO {

    @NotBlank(message = "族谱图名称不能为空")
    private String name;

}
