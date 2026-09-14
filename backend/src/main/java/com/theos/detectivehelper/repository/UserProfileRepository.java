package com.theos.detectivehelper.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户资料仓库（单机单用户，user_profile 固定 id=1 一行）
 */
@Repository
public class UserProfileRepository {

    /** 唯一行的固定主键 */
    private static final long SINGLETON_ID = 1L;

    private final JdbcTemplate jdbcTemplate;

    public UserProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 读取资料；无记录返回 Optional.empty()，由服务层给默认值（不在此处建行）
     */
    public Optional<Map<String, String>> find() {
        String sql = "SELECT display_name, theme FROM user_profile WHERE id = ?";
        List<Map<String, String>> rows = jdbcTemplate.query(sql,
                (rs, rowNum) -> Map.of("displayName", rs.getString("display_name"), "theme", rs.getString("theme")),
                SINGLETON_ID);
        return rows.stream().findFirst();
    }

    /**
     * 整行 upsert（覆盖写）
     */
    public void upsert(String displayName, String theme) {
        int updated = jdbcTemplate.update(
                "UPDATE user_profile SET display_name = ?, theme = ? WHERE id = ?",
                displayName, theme, SINGLETON_ID);
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO user_profile (id, display_name, theme) VALUES (?, ?, ?)",
                    SINGLETON_ID, displayName, theme);
        }
    }

}
