package com.theos.detectivehelper.dto;

import lombok.Data;

/**
 * 更新用户资料DTO
 * <p>
 * 两个字段都可空：null 表示「不修改该字段」（与案件书 PATCH 语义一致）。
 */
@Data
public class ProfileUpdateDTO {

    private String displayName;

    private String theme;

}
