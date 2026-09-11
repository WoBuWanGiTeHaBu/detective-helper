package com.theos.detectivehelper.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新关系图DTO
 */
public class RelationGraphUpdateDTO {

    @NotBlank(message = "关系图名称不能为空")
    private String name;

    private String data;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}