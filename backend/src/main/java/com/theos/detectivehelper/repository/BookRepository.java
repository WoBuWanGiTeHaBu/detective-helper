package com.theos.detectivehelper.repository;

import com.theos.detectivehelper.domain.Book;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 案件书仓库
 */
@Repository
public class BookRepository {

    private final JdbcTemplate jdbcTemplate;

    public BookRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Book> bookRowMapper = new RowMapper<Book>() {
        @Override
        public Book mapRow(ResultSet rs, int rowNum) throws SQLException {
            Book book = new Book();
            book.setId(rs.getLong("id"));
            book.setName(rs.getString("name"));
            book.setCoverType(rs.getString("cover_type"));
            book.setCoverValue(rs.getString("cover_value"));
            book.setCoverText(rs.getString("cover_text"));
            book.setSortOrder(rs.getInt("sort_order"));
            book.setCreatedAt(rs.getString("created_at"));
            book.setUpdatedAt(rs.getString("updated_at"));
            book.setContentUpdatedAt(rs.getString("content_updated_at"));
            return book;
        }
    };

    public Book save(Book book) {
        if (book.getId() == null) {
            String sql = "INSERT INTO book (name, cover_type, cover_value, cover_text, sort_order, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, book.getName(), book.getCoverType(), book.getCoverValue(), book.getCoverText(), book.getSortOrder(), book.getCreatedAt(), book.getUpdatedAt());
            book.setId(jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class));
        } else {
            String sql = "UPDATE book SET name = ?, cover_type = ?, cover_value = ?, cover_text = ?, sort_order = ?, updated_at = ? WHERE id = ?";
            book.setUpdatedAt(java.time.Instant.now().toString());
            jdbcTemplate.update(sql, book.getName(), book.getCoverType(), book.getCoverValue(), book.getCoverText(), book.getSortOrder(), book.getUpdatedAt(), book.getId());
        }
        return book;
    }

    public Optional<Book> findById(Long id) {
        String sql = "SELECT * FROM book WHERE id = ?";
        return jdbcTemplate.query(sql, bookRowMapper, id).stream().findFirst();
    }

    public List<Book> findAll() {
        String sql = "SELECT * FROM book ORDER BY sort_order ASC, created_at DESC";
        return jdbcTemplate.query(sql, bookRowMapper);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM book WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public void updateSortOrder(Long id, int sortOrder) {
        String sql = "UPDATE book SET sort_order = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, sortOrder, java.time.Instant.now().toString(), id);
    }

    public int countEventsByBookId(Long bookId) {
        String sql = "SELECT COUNT(*) FROM event WHERE book_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, bookId);
    }

    public int countPagesByBookId(Long bookId) {
        String sql = "SELECT COUNT(DISTINCT p.id) FROM page p JOIN event e ON p.event_id = e.id WHERE e.book_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, bookId);
    }

    /**
     * 刷新内容改动时间（只动 content_updated_at，不碰 updated_at；书不存在时是空操作）
     */
    public void touchContent(Long bookId) {
        jdbcTemplate.update("UPDATE book SET content_updated_at = ? WHERE id = ?",
                java.time.Instant.now().toString(), bookId);
    }

}