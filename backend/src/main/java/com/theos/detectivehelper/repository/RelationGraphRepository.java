package com.theos.detectivehelper.repository;

import com.theos.detectivehelper.domain.RelationGraph;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 关系图仓储（MyBatis）。对外签名与原 JdbcTemplate 版本保持一致。
 */
@Mapper
public interface RelationGraphRepository {

    @Insert("""
            INSERT INTO relation_graph (book_id, name, data, created_at, updated_at)
            VALUES (#{bookId}, #{name}, #{data}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRow(RelationGraph graph);

    @Update("UPDATE relation_graph SET name = #{name}, data = #{data}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateRow(RelationGraph graph);

    @Select("SELECT * FROM relation_graph WHERE id = #{id}")
    RelationGraph selectRowById(@Param("id") Long id);

    default RelationGraph save(RelationGraph graph) {
        if (graph.getId() == null) {
            insertRow(graph);
        } else {
            graph.setUpdatedAt(Instant.now().toString());
            updateRow(graph);
        }
        return graph;
    }

    default Optional<RelationGraph> findById(Long id) {
        return Optional.ofNullable(selectRowById(id));
    }

    @Select("SELECT * FROM relation_graph WHERE book_id = #{bookId} ORDER BY created_at DESC")
    List<RelationGraph> findByBookId(@Param("bookId") Long bookId);

    @Delete("DELETE FROM relation_graph WHERE id = #{id}")
    void deleteById(@Param("id") Long id);

    @Delete("DELETE FROM relation_graph WHERE book_id = #{bookId}")
    void deleteByBookId(@Param("bookId") Long bookId);
}
