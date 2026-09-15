package com.theos.detectivehelper.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 启动期 schema 迁移用的最小 Mapper。
 * <p>
 * 只做一件事：为老库补齐 {@code CREATE TABLE IF NOT EXISTS} 补不上的列。
 * SQLite 的 {@code ALTER TABLE} 不支持 {@code IF NOT EXISTS}，
 * 所以只能先查 {@code pragma_table_info} 再决定加不加。
 * <p>
 * <b>这里为什么用 {@code ${}} 而不是 {@code #{}}：</b>
 * {@code pragma_table_info()} 和 {@code ALTER TABLE} 的表名/列名/类型都是 SQL 语法的一部分，
 * 不能作为绑定参数。这里的三个值全部是本类调用方写死的编译期常量
 * （见 SqliteConfig），不存在外部输入，所以没有注入面。
 */
@Mapper
public interface SchemaMapper {

    @Select("SELECT COUNT(*) FROM pragma_table_info('${table}') WHERE name = #{column}")
    int countColumn(@Param("table") String table, @Param("column") String column);

    @Update("ALTER TABLE ${table} ADD COLUMN ${column} ${type}")
    void addColumn(@Param("table") String table, @Param("column") String column, @Param("type") String type);
}
