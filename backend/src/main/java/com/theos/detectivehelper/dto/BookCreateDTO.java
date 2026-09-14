package com.theos.detectivehelper.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建案件书DTO
 */
@Data
public class BookCreateDTO {

    @NotBlank(message = "案件书名称不能为空")
    private String name;

    private String coverType;

    private String coverValue;

    private String coverText;

}