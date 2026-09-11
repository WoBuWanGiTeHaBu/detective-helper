package com.theos.detectivehelper.repository;

import com.theos.detectivehelper.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 页面仓库
 */
@Repository
public class PageRepository {

    private final JdbcTemplate jdbcTemplate;

    public PageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Page> pageRowMapper = new RowMapper<Page>() {
        @Override
        public Page mapRow(ResultSet rs, int rowNum) throws SQLException {
            Page page = new Page();
            page.setId(rs.getLong("id"));
            page.setEventId(rs.getLong("event_id"));
            page.setName(rs.getString("name"));
            page.setSortOrder(rs.getInt("sort_order"));
            page.setCanvasData(rs.getString("canvas_data"));
            page.setCreatedAt(rs.getString("created_at"));
            page.setUpdatedAt(rs.getString("updated_at"));
            return page;
        }
    };

    public Page save(Page page) {
        if (page.getId() == null) {
            String sql = "INSERT INTO page (event_id, name, sort_order, canvas_data, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, page.getEventId(), page.getName(), page.getSortOrder(), page.getCanvasData(), page.getCreatedAt(), page.getUpdatedAt());
            page.setId(jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class));
        } else {
            String sql = "UPDATE page SET name = ?, sort_order = ?, canvas_data = ?, updated_at = ? WHERE id = ?";
            page.setUpdatedAt(java.time.Instant.now().toString());
            jdbcTemplate.update(sql, page.getName(), page.getSortOrder(), page.getCanvasData(), page.getUpdatedAt(), page.getId());
        }
        return page;
    }

    public Optional<Page> findById(Long id) {
        String sql = "SELECT * FROM page WHERE id = ?";
        return jdbcTemplate.query(sql, pageRowMapper, id).stream().findFirst();
    }

    public List<Page> findByEventId(Long eventId) {
        String sql = "SELECT * FROM page WHERE event_id = ? ORDER BY sort_order ASC, created_at DESC";
        return jdbcTemplate.query(sql, pageRowMapper, eventId);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM page WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public void deleteByEventId(Long eventId) {
        String sql = "DELETE FROM page WHERE event_id = ?";
        jdbcTemplate.update(sql, eventId);
    }

    public void updateSortOrder(Long id, int sortOrder) {
        String sql = "UPDATE page SET sort_order = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, sortOrder, java.time.Instant.now().toString(), id);
    }

    public int countByEventId(Long eventId) {
        String sql = "SELECT COUNT(*) FROM page WHERE event_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, eventId);
    }

}