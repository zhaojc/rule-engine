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
package cn.ruleengine.core.cache;

import java.util.Map;

/**
 * 〈一句话功能简述〉<br>
 * 〈默认缓存key生成策略〉
 *
 * @author dingqianwen
 * @date 2020/8/14
 * @since 1.0.0
 */
public class DefaultKeyGenerator implements KeyGenerator {

    /**
     * 缓存key生成策略
     *
     * @param target 执行的函数对象
     * @param params 执行函数入参
     * @return 生成的key
     */
    @Override
    public String generate(Object target, Map<String, Object> params) {
        Class<?> aClass = target.getClass();
        return aClass.getName() + "[" + params.toString() + "]";
    }

}
