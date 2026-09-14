package com.theos.detectivehelper.common.exception;

import com.theos.detectivehelper.common.ErrorCode;
import com.theos.detectivehelper.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>
 * <b>注意最后一个兜底处理器和 {@link NoResourceFoundException} 的关系：</b>
 * 自从前端接入 SPA 回退（见 WebConfig），访问一个不存在的 {@code /api/xxx} 时，
 * Spring 抛的不再是「404 页面」而是 {@code NoResourceFoundException}。
 * 如果不给它单独的处理器，就会被 {@code @ExceptionHandler(Exception.class)} 兜住，
 * 变成一个语义错误的 <b>500</b>。所以这两个处理器必须成对存在。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), errorMessage);
    }

    /** 请求体不是合法 JSON / 类型对不上时返回 400，而不是 500 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleNotReadableException(HttpMessageNotReadableException e) {
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), "请求体不是合法的 JSON 对象");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /**
     * 静态资源 / 未知接口 找不到 → 404（必须排在 Exception 兜底之前被匹配到）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoResourceFound(NoResourceFoundException e) {
        return Result.error(ErrorCode.NOT_FOUND.getCode(), "接口或资源不存在: /" + e.getResourcePath());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("未预期的服务端异常", e);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "系统异常：" + e.getMessage());
    }

}
