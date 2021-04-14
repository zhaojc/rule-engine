package cn.ruleengine.web.exception;

import cn.hutool.core.text.StrFormatter;
import cn.ruleengine.web.enums.ErrorCodeEnum;
import cn.ruleengine.web.controller.exception.ApiExceptionHandler;
import lombok.Getter;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author 丁乾文
 * @create 2020/1/10
 * @since 1.0.0
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 6405345374923437770L;

    @Getter
    private int code;

    /**
     * 例如:
     * <blockquote>
     * throw new ApiException("根据Name:{},没有查询到数据!",name);
     * </blockquote>
     *
     * @param message 异常消息
     * @param args    消息中参数
     * @see ApiExceptionHandler
     */
    public ApiException(String message, Object... args) {
        super(StrFormatter.format(message, args));
        code = ErrorCodeEnum.RULE99990100.getCode();
    }

    /**
     * 说明{@link ApiException#ApiException(String, Object...)}
     *
     * @param code    异常错误码
     * @param message 异常消息
     * @param args    消息中参数
     */
    public ApiException(int code, String message, Object... args) {
        super(StrFormatter.format(message, args));
        this.code = code;
    }
}
