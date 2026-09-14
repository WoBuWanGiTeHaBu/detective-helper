package com.theos.detectivehelper.vo;

/**
 * 用户资料VO
 */
public class ProfileVO {

    private String displayName;
    private String theme;

    public ProfileVO() {
    }

    public ProfileVO(String displayName, String theme) {
        this.displayName = displayName;
        this.theme = theme;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

}
