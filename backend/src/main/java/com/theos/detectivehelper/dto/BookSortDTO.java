package com.theos.detectivehelper.dto;

import lombok.Data;

import java.util.List;

/**
 * 案件书排序DTO
 */
@Data
public class BookSortDTO {

    private List<Long> bookIds;

}