package com.theos.detectivehelper.domain;

import lombok.Data;

/**
 * 用户资料实体（单机单用户，{@code user_profile} 表固定 id=1 一行）。
 * <p>
 * theme 后端不做枚举校验，原样存取，由前端把关。
 */
@Data
public class UserProfile {

    private Long id;

    private String displayName;

    private String theme;
}
