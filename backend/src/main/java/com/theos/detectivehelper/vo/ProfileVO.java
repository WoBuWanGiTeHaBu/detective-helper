package com.theos.detectivehelper.vo;

import lombok.Data;

/**
 * 用户资料VO
 */
@Data
public class ProfileVO {

    private String displayName;
    private String theme;

    public ProfileVO() {
    }

    public ProfileVO(String displayName, String theme) {
        this.displayName = displayName;
        this.theme = theme;
    }

}
