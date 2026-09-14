package com.theos.detectivehelper.repository;

import com.theos.detectivehelper.domain.Event;
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
 * 事件仓储（MyBatis）。对外签名与原 JdbcTemplate 版本保持一致。
 */
@Mapper
public interface EventRepository {

    @Insert("""
            INSERT INTO event (book_id, name, sort_order, created_at, updated_at)
            VALUES (#{bookId}, #{name}, #{sortOrder}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRow(Event event);

    @Update("UPDATE event SET name = #{name}, sort_order = #{sortOrder}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateRow(Event event);

    @Select("SELECT * FROM event WHERE id = #{id}")
    Event selectRowById(@Param("id") Long id);

    @Update("UPDATE event SET sort_order = #{sortOrder}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateSortOrderRow(@Param("id") Long id, @Param("sortOrder") int sortOrder, @Param("updatedAt") String updatedAt);

    default Event save(Event event) {
        if (event.getId() == null) {
            insertRow(event);
        } else {
            event.setUpdatedAt(Instant.now().toString());
            updateRow(event);
        }
        return event;
    }

    default Optional<Event> findById(Long id) {
        return Optional.ofNullable(selectRowById(id));
    }

    @Select("SELECT * FROM event WHERE book_id = #{bookId} ORDER BY sort_order ASC, created_at DESC")
    List<Event> findByBookId(@Param("bookId") Long bookId);

    @Delete("DELETE FROM event WHERE id = #{id}")
    void deleteById(@Param("id") Long id);

    @Delete("DELETE FROM event WHERE book_id = #{bookId}")
    void deleteByBookId(@Param("bookId") Long bookId);

    default void updateSortOrder(Long id, int sortOrder) {
        updateSortOrderRow(id, sortOrder, Instant.now().toString());
    }

    @Select("SELECT COUNT(*) FROM page WHERE event_id = #{eventId}")
    int countPagesByEventId(@Param("eventId") Long eventId);
}
