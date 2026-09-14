package com.theos.detectivehelper.dto;

import lombok.Data;

import java.util.List;

/**
 * 从画布提取关系图DTO
 * <p>
 * bookId 由路径参数 /api/books/{bookId}/relation-graphs/extract 提供。
 */
@Data
public class RelationGraphExtractDTO {

    /** 需要纳入提取的对象类型，如 person / thing */
    private List<String> objectTypes;

    /** 需要纳入提取的关系类型，如 unidirectional / bidirectional / dashed */
    private List<String> relationTypes;

}