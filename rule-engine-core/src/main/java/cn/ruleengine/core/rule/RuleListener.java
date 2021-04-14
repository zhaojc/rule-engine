/**
 * Copyright (c) 2020 dingqianwen (761945125@qq.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ruleengine.core.rule;

import cn.ruleengine.core.Input;
import cn.ruleengine.core.OutPut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author dingqianwen
 * @date 2020/8/3
 * @since 1.0.0
 */
public interface RuleListener {

    Logger log = LoggerFactory.getLogger(RuleListener.class);

    /**
     * 规则执行之前
     *
     * @param rule  执行的规则
     * @param input 输入参数
     */
    default void before(Rule rule, Input input) {

    }

    /**
     * 规则执行发生异常时
     *
     * @param rule      执行的规则
     * @param input     输入参数
     * @param exception 异常信息
     */
    default void onException(Rule rule, Input input, Exception exception) {

    }

    /**
     * 规则执行之后
     *
     * @param rule   执行的规则
     * @param input  输入参数
     * @param outPut 规则执行结果
     */
    default void after(Rule rule, Input input, OutPut outPut) {

    }

}
