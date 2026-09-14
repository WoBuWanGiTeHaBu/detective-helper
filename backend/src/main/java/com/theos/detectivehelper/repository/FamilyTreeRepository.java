package com.theos.detectivehelper.repository;

import com.theos.detectivehelper.domain.FamilyTree;
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
 * 族谱图仓储（MyBatis）。对外签名与原 JdbcTemplate 版本保持一致。
 */
@Mapper
public interface FamilyTreeRepository {

    @Insert("""
            INSERT INTO family_tree (book_id, name, data, created_at, updated_at)
            VALUES (#{bookId}, #{name}, #{data}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRow(FamilyTree tree);

    @Update("UPDATE family_tree SET name = #{name}, data = #{data}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateRow(FamilyTree tree);

    @Select("SELECT * FROM family_tree WHERE id = #{id}")
    FamilyTree selectRowById(@Param("id") Long id);

    default FamilyTree save(FamilyTree tree) {
        if (tree.getId() == null) {
            insertRow(tree);
        } else {
            tree.setUpdatedAt(Instant.now().toString());
            updateRow(tree);
        }
        return tree;
    }

    default Optional<FamilyTree> findById(Long id) {
        return Optional.ofNullable(selectRowById(id));
    }

    @Select("SELECT * FROM family_tree WHERE book_id = #{bookId} ORDER BY created_at DESC")
    List<FamilyTree> findByBookId(@Param("bookId") Long bookId);

    @Delete("DELETE FROM family_tree WHERE id = #{id}")
    void deleteById(@Param("id") Long id);

    @Delete("DELETE FROM family_tree WHERE book_id = #{bookId}")
    void deleteByBookId(@Param("bookId") Long bookId);
}
