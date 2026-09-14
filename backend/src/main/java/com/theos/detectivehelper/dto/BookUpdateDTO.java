package com.theos.detectivehelper.dto;

import lombok.Data;

/**
 * 更新案件书DTO
 * <p>
 * 与 OpenAPI 文档 UpdateBookRequest 一致，四个字段全部可空：null 表示「不修改该字段」
 * （前端上传封面后只补发 coverType/coverValue，不带 name）。空字符串按显式修改处理。
 */
@Data
public class BookUpdateDTO {

    private String name;

    private String coverType;

    private String coverValue;

    private String coverText;

    private Integer sortOrder;

}