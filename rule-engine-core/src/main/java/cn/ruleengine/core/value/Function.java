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
package cn.ruleengine.core.value;

import cn.ruleengine.core.Configuration;
import cn.ruleengine.core.FunctionExecutor;
import cn.ruleengine.core.Input;
import cn.ruleengine.core.annotation.Executor;
import cn.ruleengine.core.annotation.FailureStrategy;
import cn.ruleengine.core.annotation.FunctionCacheable;
import cn.ruleengine.core.cache.FunctionCache;
import cn.ruleengine.core.cache.KeyGenerator;
import cn.ruleengine.core.exception.FunctionException;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author dingqianwen
 * @date 2020/7/16
 * @since 1.0.0
 */
@Slf4j
@ToString
public class Function implements Value {

    @Getter
    private Integer id;

    private ValueType valueType;

    /**
     * 需要执行的函数
     */
    @Getter
    private Object abstractFunction;

    /**
     * 函数执行主方法
     */
    @Getter
    private Method executor;
    /**
     * 函数失败策略方法
     */
    @Getter
    private Method failureStrategy;

    /**
     * 函数缓存key生成
     */
    @Getter
    private KeyGenerator keyGenerator;
    /**
     * 是否启用缓存
     */
    @Getter
    private Boolean enableCache = false;

    /**
     * 缓存的生存时间，单位：ms
     */
    @Getter
    private long liveOutTime;

    /**
     * 函数执行器
     */
    @Getter
    private FunctionExecutor functionExecutor = new FunctionExecutor();

    @Getter
    private Map<String, Value> param;

    Function() {
    }

    public Function(Integer id, Object abstractFunction, ValueType valueType, Map<String, Value> param) {
        this.id = id;
        this.valueType = valueType;
        this.param = param;
        this.abstractFunction = abstractFunction;
        // 预解析函数中的方法
        this.initExecutorMethod();
        this.initFailureStrategyMethod();
        this.initKeyGenerator();
    }

    /**
     * 初始化函数缓存key生成
     */
    private void initKeyGenerator() {
        Class<?> abstractFunctionClass = this.abstractFunction.getClass();
        if (!abstractFunctionClass.isAnnotationPresent(FunctionCacheable.class)) {
            return;
        }
        FunctionCacheable functionCacheable = abstractFunctionClass.getAnnotation(FunctionCacheable.class);
        if (functionCacheable.enable()) {
            this.enableCache = true;
            this.liveOutTime = functionCacheable.liveOutTime();
            Class<? extends KeyGenerator> keyGeneratorClass = functionCacheable.keyGenerator();
            try {
                Constructor<? extends KeyGenerator> constructor = keyGeneratorClass.getConstructor();
                if (!Modifier.isPublic(constructor.getModifiers())) {
                    constructor.setAccessible(true);
                }
                this.keyGenerator = constructor.newInstance();
            } catch (Exception e) {
                throw new FunctionException("Function failed to generate cache key");
            }
        }
    }

    public Function(Object abstractFunction) {
        this(null, abstractFunction, null, null);
    }

    @Override
    public Object getValue(Input input, Configuration configuration) {
        //处理函数入参
        Map<String, Object> paramMap = new HashMap<>(this.param.size());
        for (Map.Entry<String, Value> entry : this.param.entrySet()) {
            paramMap.put(entry.getKey(), entry.getValue().getValue(input, configuration));
        }
        Object value;
        if (this.enableCache) {
            // 获取缓存实现类
            FunctionCache functionCache = configuration.getFunctionCache();
            Class<?> abstractFunctionClass = this.abstractFunction.getClass();
            String key = this.keyGenerator.generate(this.abstractFunction, paramMap);
            value = functionCache.get(key);
            if (value != null) {
                log.debug("{}函数存在缓存", abstractFunctionClass.getSimpleName());
            } else {
                log.debug("{}函数不存在缓存,开始执行函数", abstractFunctionClass.getSimpleName());
                value = this.executor(paramMap);
                functionCache.put(key, value, this.liveOutTime);
            }
        } else {
            value = this.executor(paramMap);
        }
        return Optional.ofNullable(value).map(m -> {
            if (!valueType.getClassType().isAssignableFrom(m.getClass())) {
                throw new FunctionException("The return type of the function does not match the set type");
            }
            return m;
        }).orElse(null);
    }

    /**
     * 执行函数
     *
     * @param paramValue 函数值
     * @return 函数返回结果
     */
    public Object executor(Map<String, Object> paramValue) {
        return this.functionExecutor.executor(this.abstractFunction, this.executor, this.failureStrategy, paramValue);
    }

    /**
     * 获取函数中带有@Executor注解的方法
     **/
    private void initExecutorMethod() {
        Method[] methods = this.abstractFunction.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Executor.class)) {
                //如果已经存在，则抛出异常
                if (this.executor != null) {
                    throw new FunctionException("函数中存在多个@Executor");
                }
                this.executor = method;
            }
        }
        String functionName = this.abstractFunction.getClass().getSimpleName();
        if (this.executor == null) {
            throw new FunctionException("{}中没有找到可执行函数方法", functionName);
        }
        if (!Modifier.isPublic(this.executor.getModifiers())) {
            this.executor.setAccessible(true);
        }
    }

    /**
     * 获取函数中带有@FailureStrategy注解的方法
     */
    private void initFailureStrategyMethod() {
        Method[] methods = this.abstractFunction.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(FailureStrategy.class)) {
                //如果已经存在，则抛出异常
                if (this.failureStrategy != null) {
                    throw new FunctionException("函数中存在多个@FailureStrategy");
                }
                this.failureStrategy = method;
            }
        }
        if (this.failureStrategy == null) {
            return;
        }
        if (!this.executor.getReturnType().equals(this.failureStrategy.getReturnType())) {
            throw new FunctionException("失败策略方法与函数主方法返回值不一致，函数主方法返回值类型{},失败策略方法返回值类型{}", executor.getReturnType(), failureStrategy.getReturnType());
        }
        if (!Modifier.isPublic(this.failureStrategy.getModifiers())) {
            this.failureStrategy.setAccessible(true);
        }
    }

    @Override
    public ValueType getValueType() {
        return this.valueType;
    }

}
