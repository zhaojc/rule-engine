package cn.ruleengine.web.function.json;


import cn.hutool.core.lang.Validator;
import cn.ruleengine.core.annotation.Executor;
import cn.ruleengine.core.annotation.Function;

import lombok.extern.slf4j.Slf4j;


/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 * 获取JSON中指定的值返回STRING类型
 *
 * @author 丁乾文
 * @create 2020/12/13
 * @since 1.0.0
 */
@Slf4j
@Function
public class ParseJsonStringFunction implements JsonEval {

    @Executor
    public String executor(String jsonString, String jsonValuePath) {
        Object value = this.eval(jsonString, jsonValuePath);
        // 返回null 而不是String.valueOf后的null字符
        if (Validator.isEmpty(value)) {
            return null;
        }
        return String.valueOf(value);
    }

}
