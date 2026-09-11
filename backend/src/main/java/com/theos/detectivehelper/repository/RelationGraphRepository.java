package com.theos.detectivehelper.repository;

import com.theos.detectivehelper.domain.RelationGraph;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 关系图仓库
 */
@Repository
public class RelationGraphRepository {

    private final JdbcTemplate jdbcTemplate;

    public RelationGraphRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<RelationGraph> relationGraphRowMapper = new RowMapper<RelationGraph>() {
        @Override
        public RelationGraph mapRow(ResultSet rs, int rowNum) throws SQLException {
            RelationGraph graph = new RelationGraph();
            graph.setId(rs.getLong("id"));
            graph.setBookId(rs.getLong("book_id"));
            graph.setName(rs.getString("name"));
            graph.setData(rs.getString("data"));
            graph.setCreatedAt(rs.getString("created_at"));
            graph.setUpdatedAt(rs.getString("updated_at"));
            return graph;
        }
    };

    public RelationGraph save(RelationGraph graph) {
        if (graph.getId() == null) {
            String sql = "INSERT INTO relation_graph (book_id, name, data, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, graph.getBookId(), graph.getName(), graph.getData(), graph.getCreatedAt(), graph.getUpdatedAt());
            graph.setId(jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class));
        } else {
            String sql = "UPDATE relation_graph SET name = ?, data = ?, updated_at = ? WHERE id = ?";
            graph.setUpdatedAt(java.time.Instant.now().toString());
            jdbcTemplate.update(sql, graph.getName(), graph.getData(), graph.getUpdatedAt(), graph.getId());
        }
        return graph;
    }

    public Optional<RelationGraph> findById(Long id) {
        String sql = "SELECT * FROM relation_graph WHERE id = ?";
        return jdbcTemplate.query(sql, relationGraphRowMapper, id).stream().findFirst();
    }

    public List<RelationGraph> findByBookId(Long bookId) {
        String sql = "SELECT * FROM relation_graph WHERE book_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, relationGraphRowMapper, bookId);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM relation_graph WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public void deleteByBookId(Long bookId) {
        String sql = "DELETE FROM relation_graph WHERE book_id = ?";
        jdbcTemplate.update(sql, bookId);
    }

}