package com.theos.detectivehelper.repository;

import com.theos.detectivehelper.domain.Event;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 事件仓库
 */
@Repository
public class EventRepository {

    private final JdbcTemplate jdbcTemplate;

    public EventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Event> eventRowMapper = new RowMapper<Event>() {
        @Override
        public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
            Event event = new Event();
            event.setId(rs.getLong("id"));
            event.setBookId(rs.getLong("book_id"));
            event.setName(rs.getString("name"));
            event.setSortOrder(rs.getInt("sort_order"));
            event.setCreatedAt(rs.getString("created_at"));
            event.setUpdatedAt(rs.getString("updated_at"));
            return event;
        }
    };

    public Event save(Event event) {
        if (event.getId() == null) {
            String sql = "INSERT INTO event (book_id, name, sort_order, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, event.getBookId(), event.getName(), event.getSortOrder(), event.getCreatedAt(), event.getUpdatedAt());
            event.setId(jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class));
        } else {
            String sql = "UPDATE event SET name = ?, sort_order = ?, updated_at = ? WHERE id = ?";
            event.setUpdatedAt(java.time.Instant.now().toString());
            jdbcTemplate.update(sql, event.getName(), event.getSortOrder(), event.getUpdatedAt(), event.getId());
        }
        return event;
    }

    public Optional<Event> findById(Long id) {
        String sql = "SELECT * FROM event WHERE id = ?";
        return jdbcTemplate.query(sql, eventRowMapper, id).stream().findFirst();
    }

    public List<Event> findByBookId(Long bookId) {
        String sql = "SELECT * FROM event WHERE book_id = ? ORDER BY sort_order ASC, created_at DESC";
        return jdbcTemplate.query(sql, eventRowMapper, bookId);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM event WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public void deleteByBookId(Long bookId) {
        String sql = "DELETE FROM event WHERE book_id = ?";
        jdbcTemplate.update(sql, bookId);
    }

    public void updateSortOrder(Long id, int sortOrder) {
        String sql = "UPDATE event SET sort_order = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, sortOrder, java.time.Instant.now().toString(), id);
    }

    public int countPagesByEventId(Long eventId) {
        String sql = "SELECT COUNT(*) FROM page WHERE event_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, eventId);
    }

}