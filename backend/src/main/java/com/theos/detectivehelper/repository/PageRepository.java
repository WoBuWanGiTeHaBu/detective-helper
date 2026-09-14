package com.theos.detectivehelper.repository;

import com.theos.detectivehelper.domain.Page;
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
 * 页面仓储（MyBatis）。对外签名与原 JdbcTemplate 版本保持一致。
 * <p>
 * {@code canvas_data} 是一整块画布 JSON，读写都是整体覆盖，
 * 所以这里永远是「读一列 / 写一列」，没有局部更新。
 */
@Mapper
public interface PageRepository {

    @Insert("""
            INSERT INTO page (event_id, name, sort_order, canvas_data, created_at, updated_at)
            VALUES (#{eventId}, #{name}, #{sortOrder}, #{canvasData}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRow(Page page);

    @Update("""
            UPDATE page
               SET name = #{name}, sort_order = #{sortOrder}, canvas_data = #{canvasData}, updated_at = #{updatedAt}
             WHERE id = #{id}
            """)
    int updateRow(Page page);

    @Select("SELECT * FROM page WHERE id = #{id}")
    Page selectRowById(@Param("id") Long id);

    @Update("UPDATE page SET sort_order = #{sortOrder}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateSortOrderRow(@Param("id") Long id, @Param("sortOrder") int sortOrder, @Param("updatedAt") String updatedAt);

    default Page save(Page page) {
        if (page.getId() == null) {
            insertRow(page);
        } else {
            page.setUpdatedAt(Instant.now().toString());
            updateRow(page);
        }
        return page;
    }

    default Optional<Page> findById(Long id) {
        return Optional.ofNullable(selectRowById(id));
    }

    @Select("SELECT * FROM page WHERE event_id = #{eventId} ORDER BY sort_order ASC, created_at DESC")
    List<Page> findByEventId(@Param("eventId") Long eventId);

    @Select("""
            SELECT p.* FROM page p JOIN event e ON p.event_id = e.id
             WHERE e.book_id = #{bookId}
             ORDER BY p.sort_order ASC, p.created_at DESC
            """)
    List<Page> findByBookId(@Param("bookId") Long bookId);

    @Delete("DELETE FROM page WHERE id = #{id}")
    void deleteById(@Param("id") Long id);

    @Delete("DELETE FROM page WHERE event_id = #{eventId}")
    void deleteByEventId(@Param("eventId") Long eventId);

    default void updateSortOrder(Long id, int sortOrder) {
        updateSortOrderRow(id, sortOrder, Instant.now().toString());
    }

    @Select("SELECT COUNT(*) FROM page WHERE event_id = #{eventId}")
    int countByEventId(@Param("eventId") Long eventId);
}
