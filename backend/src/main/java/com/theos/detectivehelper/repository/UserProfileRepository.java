package com.theos.detectivehelper.repository;

import com.theos.detectivehelper.domain.UserProfile;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 用户资料仓储（MyBatis）。单机单用户，{@code user_profile} 固定 id=1 一行。
 * <p>
 * 原来 upsert 是「先 UPDATE，affected=0 再 INSERT」两步。
 * SQLite 从 3.24 起支持 {@code ON CONFLICT ... DO UPDATE}，
 * 现在一条语句搞定，也就没有「两步之间被打断」的窗口了。
 */
@Mapper
public interface UserProfileRepository {

    /** 唯一行的固定主键 */
    long SINGLETON_ID = 1L;

    @Select("SELECT id, display_name, theme FROM user_profile WHERE id = #{id}")
    UserProfile selectRowById(@Param("id") long id);

    @Insert("""
            INSERT INTO user_profile (id, display_name, theme)
            VALUES (#{id}, #{displayName}, #{theme})
            ON CONFLICT(id) DO UPDATE SET
                display_name = excluded.display_name,
                theme = excluded.theme
            """)
    int upsertRow(@Param("id") long id,
                  @Param("displayName") String displayName,
                  @Param("theme") String theme);

    /**
     * 读取资料；无记录返回 {@code Optional.empty()}，由服务层给默认值（不在此处建行）
     */
    default Optional<UserProfile> find() {
        return Optional.ofNullable(selectRowById(SINGLETON_ID));
    }

    /** 整行 upsert（覆盖写） */
    default void upsert(String displayName, String theme) {
        upsertRow(SINGLETON_ID, displayName, theme);
    }
}
