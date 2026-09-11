package com.theos.detectivehelper.common;

/**
 * 错误码枚举
 */
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务错误码 (1000-1999)
    BOOK_NOT_FOUND(1001, "案件书不存在"),
    PAGE_NOT_FOUND(1002, "页面不存在"),
    EVENT_NOT_FOUND(1003, "事件不存在"),
    RELATION_GRAPH_NOT_FOUND(1004, "关系图不存在"),
    INVALID_SORT_ORDER(1005, "无效的排序顺序"),
    DUPLICATE_TITLE(1006, "标题已存在"),
    INVALID_OPERATION(1007, "无效的操作");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}