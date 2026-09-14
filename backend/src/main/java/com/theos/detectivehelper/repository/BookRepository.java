package com.theos.detectivehelper.repository;

import com.theos.detectivehelper.domain.Book;
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
 * 案件书仓储（MyBatis）。
 * <p>
 * 对外的方法签名与原来的 JdbcTemplate 版本<b>完全一致</b>，所以 BookService 等调用方不需要改。
 * 需要 Java 侧分支或生成时间戳的少数方法做成 {@code default} 方法，包住底下的原生 SQL 方法
 * （原生方法统一带 {@code Row} 后缀，避免与对外 API 重名）。
 * <p>
 * 时间戳一律在 Java 侧用 {@code Instant.now().toString()} 生成，不用 SQLite 的
 * {@code datetime('now')}：后者产出 {@code 2026-09-14 12:46:20} 这种空格分隔格式，
 * 而前端一直在按 ISO-8601 解析 {@code createdAt} / {@code updatedAt}，换了格式就会解析失败。
 */
@Mapper
public interface BookRepository {

    // ---------- 原生 SQL ----------

    @Insert("""
            INSERT INTO book (name, cover_type, cover_value, cover_text, sort_order, created_at, updated_at)
            VALUES (#{name}, #{coverType}, #{coverValue}, #{coverText}, #{sortOrder}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRow(Book book);

    @Update("""
            UPDATE book
               SET name = #{name}, cover_type = #{coverType}, cover_value = #{coverValue},
                   cover_text = #{coverText}, sort_order = #{sortOrder}, updated_at = #{updatedAt}
             WHERE id = #{id}
            """)
    int updateRow(Book book);

    @Select("SELECT * FROM book WHERE id = #{id}")
    Book selectRowById(@Param("id") Long id);

    @Update("UPDATE book SET sort_order = #{sortOrder}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateSortOrderRow(@Param("id") Long id, @Param("sortOrder") int sortOrder, @Param("updatedAt") String updatedAt);

    @Update("UPDATE book SET content_updated_at = #{contentUpdatedAt} WHERE id = #{bookId}")
    int touchContentRow(@Param("bookId") Long bookId, @Param("contentUpdatedAt") String contentUpdatedAt);

    // ---------- 对外 API ----------

    /**
     * 有 id 走更新、无 id 走插入，并把生成的主键回填到入参对象上。
     */
    default Book save(Book book) {
        if (book.getId() == null) {
            insertRow(book);
        } else {
            book.setUpdatedAt(Instant.now().toString());
            updateRow(book);
        }
        return book;
    }

    default Optional<Book> findById(Long id) {
        return Optional.ofNullable(selectRowById(id));
    }

    @Select("SELECT * FROM book ORDER BY sort_order ASC, created_at DESC")
    List<Book> findAll();

    @Delete("DELETE FROM book WHERE id = #{id}")
    void deleteById(@Param("id") Long id);

    default void updateSortOrder(Long id, int sortOrder) {
        updateSortOrderRow(id, sortOrder, Instant.now().toString());
    }

    @Select("SELECT COUNT(*) FROM event WHERE book_id = #{bookId}")
    int countEventsByBookId(@Param("bookId") Long bookId);

    @Select("SELECT COUNT(DISTINCT p.id) FROM page p JOIN event e ON p.event_id = e.id WHERE e.book_id = #{bookId}")
    int countPagesByBookId(@Param("bookId") Long bookId);

    /**
     * 刷新内容改动时间（只动 content_updated_at，不碰 updated_at；书不存在时是空操作）
     */
    default void touchContent(Long bookId) {
        touchContentRow(bookId, Instant.now().toString());
    }
}
