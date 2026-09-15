package com.theos.detectivehelper.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 系统自检 Mapper。
 * <p>
 * 只服务于 {@code GET /api/health}：桌面壳在把页面指过去之前，会轮询这个接口确认
 * 「后端真的能干活了」，而不是只看 Spring 上下文是否 active。
 */
@Mapper
public interface SystemMapper {

    /** 真正打一次数据库；连不上会抛异常，健康检查自然就是失败的 */
    @Select("SELECT 1")
    Integer ping();
}
