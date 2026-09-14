package com.theos.detectivehelper.repository;

import com.theos.detectivehelper.domain.FamilyTree;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 族谱图仓库
 */
@Repository
public class FamilyTreeRepository {

    private final JdbcTemplate jdbcTemplate;

    public FamilyTreeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<FamilyTree> familyTreeRowMapper = new RowMapper<FamilyTree>() {
        @Override
        public FamilyTree mapRow(ResultSet rs, int rowNum) throws SQLException {
            FamilyTree tree = new FamilyTree();
            tree.setId(rs.getLong("id"));
            tree.setBookId(rs.getLong("book_id"));
            tree.setName(rs.getString("name"));
            tree.setData(rs.getString("data"));
            tree.setCreatedAt(rs.getString("created_at"));
            tree.setUpdatedAt(rs.getString("updated_at"));
            return tree;
        }
    };

    public FamilyTree save(FamilyTree tree) {
        if (tree.getId() == null) {
            String sql = "INSERT INTO family_tree (book_id, name, data, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, tree.getBookId(), tree.getName(), tree.getData(), tree.getCreatedAt(), tree.getUpdatedAt());
            tree.setId(jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class));
        } else {
            String sql = "UPDATE family_tree SET name = ?, data = ?, updated_at = ? WHERE id = ?";
            tree.setUpdatedAt(java.time.Instant.now().toString());
            jdbcTemplate.update(sql, tree.getName(), tree.getData(), tree.getUpdatedAt(), tree.getId());
        }
        return tree;
    }

    public Optional<FamilyTree> findById(Long id) {
        String sql = "SELECT * FROM family_tree WHERE id = ?";
        return jdbcTemplate.query(sql, familyTreeRowMapper, id).stream().findFirst();
    }

    public List<FamilyTree> findByBookId(Long bookId) {
        String sql = "SELECT * FROM family_tree WHERE book_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, familyTreeRowMapper, bookId);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM family_tree WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public void deleteByBookId(Long bookId) {
        String sql = "DELETE FROM family_tree WHERE book_id = ?";
        jdbcTemplate.update(sql, bookId);
    }

}
