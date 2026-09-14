package com.theos.detectivehelper.controller;

import com.theos.detectivehelper.common.Result;
import com.theos.detectivehelper.dto.ProfileUpdateDTO;
import com.theos.detectivehelper.service.ProfileService;
import com.theos.detectivehelper.vo.ProfileVO;
import org.springframework.web.bind.annotation.*;

/**
 * 用户资料控制器（单机单用户，无需鉴权）
 */
@RestController
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * 读取用户资料（无记录时返回默认值）
     */
    @GetMapping("/api/profile")
    public Result<ProfileVO> getProfile() {
        return Result.success(profileService.getProfile());
    }

    /**
     * 保存用户资料（upsert，只传的才改）
     */
    @PutMapping("/api/profile")
    public Result<ProfileVO> updateProfile(@RequestBody ProfileUpdateDTO dto) {
        return Result.success(profileService.updateProfile(dto));
    }

}
