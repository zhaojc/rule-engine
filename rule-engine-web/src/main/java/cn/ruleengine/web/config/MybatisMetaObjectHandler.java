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
package cn.ruleengine.web.config;

import cn.ruleengine.web.enums.DeletedEnum;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author dingqianwen
 * @date 2020/8/26
 * @since 1.0.0
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    private static final String DELETED = "deleted";
    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_TIME = "updateTime";

    @Override
    public void insertFill(MetaObject metaObject) {
        setFieldValByName(DELETED, DeletedEnum.ENABLE.getStatus(), metaObject);
        Date currentDate = new Date();
        setFieldValByName(CREATE_TIME, currentDate, metaObject);
        setFieldValByName(UPDATE_TIME, currentDate, metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName(UPDATE_TIME, new Date(), metaObject);
    }

}
