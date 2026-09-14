package com.theos.detectivehelper.service;

import com.theos.detectivehelper.common.ErrorCode;
import com.theos.detectivehelper.common.exception.BusinessException;
import com.theos.detectivehelper.dto.ProfileUpdateDTO;
import com.theos.detectivehelper.repository.UserProfileRepository;
import com.theos.detectivehelper.vo.ProfileVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户资料服务（单机单用户）
 * <p>
 * GET 无记录时返回默认值（displayName=用户，theme=light），不报 404；
 * PUT 做整体 upsert，只传的字段才改。theme 后端不枚举，前端把关、原样存取。
 */
@Service
@Transactional
public class ProfileService {

    public static final String DEFAULT_DISPLAY_NAME = "用户";
    public static final String DEFAULT_THEME = "light";

    /** 展示名长度上限 */
    private static final int MAX_DISPLAY_NAME_LENGTH = 24;

    private final UserProfileRepository userProfileRepository;

    public ProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public ProfileVO getProfile() {
        return userProfileRepository.find()
                .map(row -> new ProfileVO(row.getDisplayName(), row.getTheme()))
                .orElseGet(() -> new ProfileVO(DEFAULT_DISPLAY_NAME, DEFAULT_THEME));
    }

    public ProfileVO updateProfile(ProfileUpdateDTO dto) {
        // 先取当前值，再按「只传的才改」合并，最后整行覆盖写
        ProfileVO current = getProfile();

        String displayName = current.getDisplayName();
        if (dto.getDisplayName() != null) {
            String trimmed = dto.getDisplayName().trim();
            if (trimmed.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "展示名不能为空");
            }
            if (trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "展示名最多 " + MAX_DISPLAY_NAME_LENGTH + " 个字符");
            }
            displayName = trimmed;
        }

        String theme = current.getTheme();
        if (dto.getTheme() != null && !dto.getTheme().isBlank()) {
            theme = dto.getTheme().trim();
        }

        userProfileRepository.upsert(displayName, theme);
        return new ProfileVO(displayName, theme);
    }

}
