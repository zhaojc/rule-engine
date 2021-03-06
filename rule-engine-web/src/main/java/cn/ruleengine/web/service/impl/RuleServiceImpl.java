package cn.ruleengine.web.service.impl;


import cn.ruleengine.core.value.*;
import cn.ruleengine.web.config.Context;
import cn.ruleengine.web.enums.EnableEnum;
import cn.ruleengine.web.enums.RuleStatus;
import cn.ruleengine.web.listener.body.RuleMessageBody;
import cn.ruleengine.web.listener.event.RuleEvent;
import cn.ruleengine.web.service.ConditionService;
import cn.ruleengine.web.service.RuleResolveService;
import cn.ruleengine.web.service.RuleService;
import cn.ruleengine.web.store.entity.*;
import cn.ruleengine.web.store.manager.*;
import cn.ruleengine.web.store.mapper.RuleEngineRuleMapper;
import cn.ruleengine.web.util.PageUtils;
import cn.ruleengine.web.vo.convert.BasicConversion;
import cn.ruleengine.web.vo.base.request.PageRequest;
import cn.ruleengine.web.vo.base.response.PageBase;
import cn.ruleengine.web.vo.base.response.PageResult;
import cn.ruleengine.web.vo.condition.ConditionGroupCondition;
import cn.ruleengine.web.vo.condition.ConditionGroupConfig;
import cn.ruleengine.web.vo.condition.ConditionResponse;
import cn.ruleengine.web.vo.condition.ConfigBean;
import cn.ruleengine.web.vo.rule.*;
import cn.ruleengine.core.condition.ConditionGroup;

import cn.ruleengine.core.condition.Condition;
import cn.ruleengine.web.vo.rule.DefaultAction;
import cn.ruleengine.web.vo.rule.Action;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cn.ruleengine.core.Engine;
import cn.ruleengine.core.exception.ValidException;
import cn.ruleengine.core.rule.Rule;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.ruleengine.web.vo.user.UserData;
import cn.ruleengine.web.vo.workspace.Workspace;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ???????????????????????????<br>
 * ??????
 *
 * @author dingqianwen
 * @date 2020/8/24
 * @since 1.0.0
 */
@Transactional(rollbackFor = Exception.class)
@Service
public class RuleServiceImpl implements RuleService {

    @Resource
    private RuleEngineRuleManager ruleEngineRuleManager;
    @Resource
    private RuleEngineRuleMapper ruleEngineRuleMapper;
    @Resource
    private RuleEngineConditionGroupConditionManager ruleEngineConditionGroupConditionManager;
    @Resource
    private RuleEngineConditionGroupManager ruleEngineConditionGroupManager;
    @Resource
    private Engine engine;
    @Resource
    private ConditionService conditionService;
    @Resource
    private RuleParameterService ruleCountInfoService;
    @Resource
    private RuleEngineRulePublishManager ruleEngineRulePublishManager;
    @Resource
    private RuleResolveService ruleResolveService;
    @Resource
    private RuleEngineConditionManager ruleEngineConditionManager;
    @Resource
    private RuleEngineVariableManager ruleEngineVariableManager;
    @Resource
    private RuleEngineElementManager ruleEngineElementManager;
    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * ????????????
     *
     * @param pageRequest ??????????????????
     * @return page
     */
    @Override
    public PageResult<ListRuleResponse> list(PageRequest<ListRuleRequest> pageRequest) {
        List<PageRequest.OrderBy> orders = pageRequest.getOrders();
        PageBase page = pageRequest.getPage();
        Workspace workspace = Context.getCurrentWorkspace();
        return PageUtils.page(this.ruleEngineRuleManager, page, () -> {
            QueryWrapper<RuleEngineRule> wrapper = new QueryWrapper<>();
            wrapper.lambda().eq(RuleEngineRule::getWorkspaceId, workspace.getId());
            PageUtils.defaultOrder(orders, wrapper);

            ListRuleRequest query = pageRequest.getQuery();
            if (Validator.isNotEmpty(query.getName())) {
                wrapper.lambda().like(RuleEngineRule::getName, query.getName());
            }
            if (Validator.isNotEmpty(query.getCode())) {
                wrapper.lambda().like(RuleEngineRule::getCode, query.getCode());
            }
            if (Validator.isNotEmpty(query.getStatus())) {
                wrapper.lambda().eq(RuleEngineRule::getStatus, query.getStatus());
            }
            return wrapper;
        }, m -> {
            ListRuleResponse listRuleResponse = new ListRuleResponse();
            listRuleResponse.setId(m.getId());
            listRuleResponse.setName(m.getName());
            listRuleResponse.setCode(m.getCode());
            listRuleResponse.setIsPublish(this.engine.isExistsRule(m.getWorkspaceCode(), m.getCode()));
            listRuleResponse.setCreateUserName(m.getCreateUserName());
            listRuleResponse.setStatus(m.getStatus());
            listRuleResponse.setCreateTime(m.getCreateTime());
            return listRuleResponse;
        });
    }

    /**
     * ??????code????????????
     *
     * @param code ??????code
     * @return true??????
     */
    @Override
    public Boolean ruleCodeIsExists(String code) {
        Workspace workspace = Context.getCurrentWorkspace();
        Integer count = this.ruleEngineRuleManager.lambdaQuery()
                .eq(RuleEngineRule::getWorkspaceId, workspace.getId())
                .eq(RuleEngineRule::getCode, code)
                .count();
        return count != null && count >= 1;
    }

    /**
     * ??????????????????
     *
     * @param updateRuleRequest ??????????????????
     * @return true????????????
     */
    @Override
    public Boolean updateRule(UpdateRuleRequest updateRuleRequest) {
        RuleEngineRule ruleEngineRule = this.ruleEngineRuleManager.lambdaQuery()
                .eq(RuleEngineRule::getId, updateRuleRequest.getId())
                .one();
        if (ruleEngineRule == null) {
            throw new ValidException("???????????????:{}", updateRuleRequest.getId());
        }
        // ?????????????????????????????????????????????????????????
        if (Objects.equals(ruleEngineRule.getStatus(), RuleStatus.WAIT_PUBLISH.getStatus())) {
            this.ruleEngineRulePublishManager.lambdaUpdate()
                    .eq(RuleEngineRulePublish::getStatus, RuleStatus.WAIT_PUBLISH.getStatus())
                    .eq(RuleEngineRulePublish::getRuleId, updateRuleRequest.getId())
                    .remove();
        }
        // ???????????????????????????????????????????????????
        this.removeConditionGroupByRuleId(updateRuleRequest.getId());
        // ??????????????????
        this.saveConditionGroup(updateRuleRequest.getId(), updateRuleRequest.getConditionGroup());
        //  ??????????????????
        ruleEngineRule.setId(updateRuleRequest.getId());
        ruleEngineRule.setStatus(RuleStatus.EDIT.getStatus());
        // ????????????
        Action action = updateRuleRequest.getAction();
        ruleEngineRule.setActionType(action.getType());
        ruleEngineRule.setActionValueType(action.getValueType());
        ruleEngineRule.setActionValue(action.getValue());
        // ??????????????????
        DefaultAction defaultAction = updateRuleRequest.getDefaultAction();
        ruleEngineRule.setEnableDefaultAction(defaultAction.getEnableDefaultAction());
        ruleEngineRule.setDefaultActionValue(defaultAction.getValue());
        ruleEngineRule.setDefaultActionValueType(defaultAction.getValueType());
        ruleEngineRule.setDefaultActionType(defaultAction.getType());
        ruleEngineRule.setAbnormalAlarm(JSONObject.toJSONString(updateRuleRequest.getAbnormalAlarm()));
        this.ruleEngineRuleMapper.updateRuleById(ruleEngineRule);
        return true;
    }

    /**
     * ???????????????
     *
     * @param ruleId         ??????id
     * @param conditionGroup ???????????????
     */
    private void saveConditionGroup(Integer ruleId, List<ConditionGroupConfig> conditionGroup) {
        if (CollUtil.isEmpty(conditionGroup)) {
            return;
        }
        List<RuleEngineConditionGroupCondition> ruleEngineConditionGroupConditions = new LinkedList<>();
        for (ConditionGroupConfig groupConfig : conditionGroup) {
            RuleEngineConditionGroup engineConditionGroup = new RuleEngineConditionGroup();
            engineConditionGroup.setName(groupConfig.getName());
            engineConditionGroup.setRuleId(ruleId);
            engineConditionGroup.setOrderNo(groupConfig.getOrderNo());
            this.ruleEngineConditionGroupManager.save(engineConditionGroup);
            List<ConditionGroupCondition> conditionGroupConditions = groupConfig.getConditionGroupCondition();
            if (CollUtil.isNotEmpty(conditionGroupConditions)) {
                for (ConditionGroupCondition conditionGroupCondition : conditionGroupConditions) {
                    RuleEngineConditionGroupCondition ruleEngineConditionGroupCondition = new RuleEngineConditionGroupCondition();
                    ruleEngineConditionGroupCondition.setConditionId(conditionGroupCondition.getCondition().getId());
                    ruleEngineConditionGroupCondition.setConditionGroupId(engineConditionGroup.getId());
                    ruleEngineConditionGroupCondition.setOrderNo(conditionGroupCondition.getOrderNo());
                    ruleEngineConditionGroupConditions.add(ruleEngineConditionGroupCondition);
                }
            }
        }
        if (CollUtil.isNotEmpty(ruleEngineConditionGroupConditions)) {
            this.ruleEngineConditionGroupConditionManager.saveBatch(ruleEngineConditionGroupConditions);
        }
    }

    /**
     * ????????????
     *
     * @param id ??????id
     * @return true
     */
    @Override
    public Boolean delete(Integer id) {
        RuleEngineRule engineRule = this.ruleEngineRuleManager.getById(id);
        if (engineRule == null) {
            return false;
        }
        // ????????????????????????
        if (this.engine.isExistsRule(engineRule.getWorkspaceCode(), engineRule.getCode())) {
            RuleMessageBody ruleMessageBody = new RuleMessageBody();
            ruleMessageBody.setType(RuleMessageBody.Type.REMOVE);
            ruleMessageBody.setWorkspaceId(engineRule.getWorkspaceId());
            ruleMessageBody.setWorkspaceCode(engineRule.getWorkspaceCode());
            ruleMessageBody.setRuleCode(engineRule.getCode());
            this.eventPublisher.publishEvent(new RuleEvent(ruleMessageBody));
        }
        // ????????????????????????
        this.ruleEngineRulePublishManager.lambdaUpdate().eq(RuleEngineRulePublish::getRuleId, id).remove();
        // ???????????????????????????
        this.removeConditionGroupByRuleId(id);
        // ????????????
        return this.ruleEngineRuleManager.removeById(id);
    }

    /**
     * ???????????????????????????
     *
     * @param ruleId ??????id
     */
    public void removeConditionGroupByRuleId(Integer ruleId) {
        List<RuleEngineConditionGroup> engineConditionGroups = ruleEngineConditionGroupManager.lambdaQuery()
                .eq(RuleEngineConditionGroup::getRuleId, ruleId)
                .list();
        if (CollUtil.isNotEmpty(engineConditionGroups)) {
            List<Integer> engineConditionGroupIds = engineConditionGroups.stream().map(RuleEngineConditionGroup::getId).collect(Collectors.toList());
            if (this.ruleEngineConditionGroupManager.removeByIds(engineConditionGroupIds)) {
                // ?????????????????????
                this.ruleEngineConditionGroupConditionManager.lambdaUpdate()
                        .in(RuleEngineConditionGroupCondition::getConditionGroupId, engineConditionGroupIds)
                        .remove();
            }
        }
    }

    /**
     * ????????????????????????????????????
     *
     * @param ruleDefinition ??????????????????
     * @return ??????id
     */
    @Override
    public Integer saveOrUpdateRuleDefinition(RuleDefinition ruleDefinition) {
        // ????????????
        RuleEngineRule ruleEngineRule = new RuleEngineRule();
        if (ruleDefinition.getId() == null) {
            if (this.ruleCodeIsExists(ruleDefinition.getCode())) {
                throw new ValidException("??????Code???{}????????????", ruleDefinition.getCode());
            }
            Workspace workspace = Context.getCurrentWorkspace();
            UserData userData = Context.getCurrentUser();
            ruleEngineRule.setCreateUserId(userData.getId());
            ruleEngineRule.setCreateUserName(userData.getUsername());
            ruleEngineRule.setWorkspaceId(workspace.getId());
            ruleEngineRule.setWorkspaceCode(workspace.getCode());
        } else {
            Integer count = this.ruleEngineRuleManager.lambdaQuery()
                    .eq(RuleEngineRule::getId, ruleDefinition.getId())
                    .count();
            if (count == null || count == 0) {
                throw new ValidException("???????????????:{}", ruleDefinition.getId());
            }
        }
        ruleEngineRule.setId(ruleDefinition.getId());
        ruleEngineRule.setName(ruleDefinition.getName());
        ruleEngineRule.setCode(ruleDefinition.getCode());
        ruleEngineRule.setDescription(ruleDefinition.getDescription());
        ruleEngineRule.setStatus(RuleStatus.EDIT.getStatus());
        this.ruleEngineRuleManager.saveOrUpdate(ruleEngineRule);
        return ruleEngineRule.getId();
    }

    /**
     * ????????????????????????
     *
     * @param id ??????id
     * @return ??????????????????
     */
    @Override
    public RuleDefinition getRuleDefinition(Integer id) {
        RuleEngineRule ruleEngineRule = this.ruleEngineRuleManager.lambdaQuery()
                .eq(RuleEngineRule::getId, id)
                .one();
        if (ruleEngineRule == null) {
            return null;
        }
        return BasicConversion.INSTANCE.convert(ruleEngineRule);
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param releaseRequest ??????????????????
     * @return true
     */
    @Override
    public Boolean generationRelease(GenerationReleaseRequest releaseRequest) {
        // ????????????
        RuleEngineRule ruleEngineRule = this.ruleEngineRuleManager.lambdaQuery()
                .eq(RuleEngineRule::getId, releaseRequest.getId())
                .one();
        if (ruleEngineRule == null) {
            throw new ValidException("???????????????:{}", releaseRequest.getId());
        }
        Integer originStatus = ruleEngineRule.getStatus();
        // ???????????????????????????
        DefaultAction defaultAction = releaseRequest.getDefaultAction();
        if (EnableEnum.ENABLE.getStatus().equals(defaultAction.getEnableDefaultAction())) {
            if (Validator.isEmpty(defaultAction.getType())) {
                throw new ValidException("??????????????????????????????");
            }
            if (Validator.isEmpty(defaultAction.getValueType())) {
                throw new ValidException("?????????????????????????????????");
            }
            if (Validator.isEmpty(defaultAction.getValue())) {
                throw new ValidException("???????????????????????????");
            }
        }
        // ???????????????????????????????????????????????????
        this.removeConditionGroupByRuleId(releaseRequest.getId());
        // ??????????????????
        this.saveConditionGroup(releaseRequest.getId(), releaseRequest.getConditionGroup());
        //  ??????????????????
        ruleEngineRule.setId(releaseRequest.getId());
        ruleEngineRule.setStatus(RuleStatus.WAIT_PUBLISH.getStatus());
        // ????????????
        Action action = releaseRequest.getAction();
        ruleEngineRule.setActionType(action.getType());
        ruleEngineRule.setActionValueType(action.getValueType());
        ruleEngineRule.setActionValue(action.getValue());
        // ??????????????????
        ruleEngineRule.setEnableDefaultAction(defaultAction.getEnableDefaultAction());
        ruleEngineRule.setDefaultActionValue(defaultAction.getValue());
        ruleEngineRule.setDefaultActionValueType(defaultAction.getValueType());
        ruleEngineRule.setDefaultActionType(defaultAction.getType());
        ruleEngineRule.setAbnormalAlarm(JSONObject.toJSONString(releaseRequest.getAbnormalAlarm()));
        this.ruleEngineRuleMapper.updateRuleById(ruleEngineRule);
        // ?????????????????????
        if (Objects.equals(originStatus, RuleStatus.WAIT_PUBLISH.getStatus())) {
            // ???????????????????????????
            this.ruleEngineRulePublishManager.lambdaUpdate()
                    .eq(RuleEngineRulePublish::getStatus, RuleStatus.WAIT_PUBLISH.getStatus())
                    .eq(RuleEngineRulePublish::getRuleId, releaseRequest.getId())
                    .remove();
        }
        // ???????????????????????????
        Rule rule = this.ruleResolveService.ruleProcess(ruleEngineRule);
        RuleEngineRulePublish rulePublish = new RuleEngineRulePublish();
        rulePublish.setRuleId(rule.getId());
        rulePublish.setRuleCode(rule.getCode());
        rulePublish.setData(rule.toJson());
        rulePublish.setStatus(RuleStatus.WAIT_PUBLISH.getStatus());
        rulePublish.setWorkspaceId(rule.getWorkspaceId());
        rulePublish.setWorkspaceCode(rule.getWorkspaceCode());
        this.ruleEngineRulePublishManager.save(rulePublish);
        return true;
    }


    /**
     * ????????????
     *
     * @param id ??????id
     * @return true
     */
    @Override
    public Boolean publish(Integer id) {
        RuleEngineRule ruleEngineRule = ruleEngineRuleManager.lambdaQuery()
                .eq(RuleEngineRule::getId, id)
                .one();
        if (ruleEngineRule == null) {
            throw new ValidException("???????????????:{}", id);
        }
        if (ruleEngineRule.getStatus().equals(RuleStatus.EDIT.getStatus())) {
            throw new ValidException("?????????????????????:{}", id);
        }
        // ??????????????????????????????
        if (ruleEngineRule.getStatus().equals(RuleStatus.PUBLISHED.getStatus())) {
            return true;
        }
        // ??????????????????
        this.ruleEngineRuleManager.lambdaUpdate()
                .set(RuleEngineRule::getStatus, RuleStatus.PUBLISHED.getStatus())
                .eq(RuleEngineRule::getId, ruleEngineRule.getId())
                .update();
        // ????????????????????????????????????
        this.ruleEngineRulePublishManager.lambdaUpdate()
                .eq(RuleEngineRulePublish::getStatus, RuleStatus.PUBLISHED.getStatus())
                .eq(RuleEngineRulePublish::getRuleId, ruleEngineRule.getId())
                .remove();
        // ???????????????????????????
        this.ruleEngineRulePublishManager.lambdaUpdate()
                .set(RuleEngineRulePublish::getStatus, RuleStatus.PUBLISHED.getStatus())
                .eq(RuleEngineRulePublish::getStatus, RuleStatus.WAIT_PUBLISH.getStatus())
                .eq(RuleEngineRulePublish::getRuleId, ruleEngineRule.getId())
                .update();
        // ????????????
        RuleMessageBody ruleMessageBody = new RuleMessageBody();
        ruleMessageBody.setType(RuleMessageBody.Type.LOAD);
        ruleMessageBody.setRuleCode(ruleEngineRule.getCode());
        ruleMessageBody.setWorkspaceId(ruleEngineRule.getWorkspaceId());
        ruleMessageBody.setWorkspaceCode(ruleEngineRule.getWorkspaceCode());
        this.eventPublisher.publishEvent(new RuleEvent(ruleMessageBody));
        return true;
    }

    /**
     * ??????????????????
     *
     * @param id ??????id
     * @return ????????????
     */
    @Override
    public GetRuleResponse getRuleConfig(Integer id) {
        RuleEngineRule ruleEngineRule = this.ruleEngineRuleManager.lambdaQuery()
                .eq(RuleEngineRule::getId, id)
                .one();
        if (ruleEngineRule == null) {
            return null;
        }
        GetRuleResponse ruleResponse = new GetRuleResponse();
        ruleResponse.setId(ruleEngineRule.getId());
        ruleResponse.setCode(ruleEngineRule.getCode());
        ruleResponse.setName(ruleEngineRule.getName());
        ruleResponse.setDescription(ruleEngineRule.getDescription());
        List<RuleEngineConditionGroup> engineConditionGroups = this.ruleEngineConditionGroupManager.lambdaQuery()
                .eq(RuleEngineConditionGroup::getRuleId, id)
                .orderByAsc(RuleEngineConditionGroup::getOrderNo)
                .list();
        if (CollUtil.isNotEmpty(engineConditionGroups)) {
            // ???????????????????????????????????????
            Set<Integer> conditionGroupIds = engineConditionGroups.stream().map(RuleEngineConditionGroup::getId).collect(Collectors.toSet());
            List<RuleEngineConditionGroupCondition> ruleEngineConditionGroupConditions = this.ruleEngineConditionGroupConditionManager.lambdaQuery()
                    .in(RuleEngineConditionGroupCondition::getConditionGroupId, conditionGroupIds)
                    .orderByAsc(RuleEngineConditionGroupCondition::getOrderNo)
                    .list();
            if (CollUtil.isNotEmpty(ruleEngineConditionGroupConditions)) {
                Map<Integer, List<RuleEngineConditionGroupCondition>> conditionGroupConditionMaps = ruleEngineConditionGroupConditions.stream()
                        .collect(Collectors.groupingBy(RuleEngineConditionGroupCondition::getConditionGroupId));
                Set<Integer> conditionIds = conditionGroupConditionMaps.values().stream().flatMap(Collection::stream).map(RuleEngineConditionGroupCondition::getConditionId)
                        .collect(Collectors.toSet());
                List<RuleEngineCondition> ruleEngineConditions = this.ruleEngineConditionManager.lambdaQuery().in(RuleEngineCondition::getId, conditionIds).list();
                if (CollUtil.isNotEmpty(ruleEngineConditions)) {
                    Map<Integer, RuleEngineCondition> conditionMap = ruleEngineConditions.stream().collect(Collectors.toMap(RuleEngineCondition::getId, Function.identity()));
                    Map<Integer, RuleEngineElement> elementMap = this.conditionService.getConditionElementMap(conditionMap.values());
                    Map<Integer, RuleEngineVariable> variableMap = this.conditionService.getConditionVariableMap(conditionMap.values());
                    // ?????????????????????
                    List<ConditionGroupConfig> conditionGroup = new ArrayList<>();
                    for (RuleEngineConditionGroup engineConditionGroup : engineConditionGroups) {
                        ConditionGroupConfig group = new ConditionGroupConfig();
                        group.setId(engineConditionGroup.getId());
                        group.setName(engineConditionGroup.getName());
                        group.setOrderNo(engineConditionGroup.getOrderNo());
                        List<RuleEngineConditionGroupCondition> conditionGroupConditions = conditionGroupConditionMaps.get(engineConditionGroup.getId());
                        if (CollUtil.isEmpty(conditionGroupConditions)) {
                            continue;
                        }
                        List<ConditionGroupCondition> groupConditions = new ArrayList<>(conditionGroupConditions.size());
                        for (RuleEngineConditionGroupCondition conditionGroupCondition : conditionGroupConditions) {
                            ConditionGroupCondition conditionSet = new ConditionGroupCondition();
                            conditionSet.setId(conditionGroupCondition.getId());
                            conditionSet.setOrderNo(conditionGroupCondition.getOrderNo());
                            RuleEngineCondition engineCondition = conditionMap.get(conditionGroupCondition.getConditionId());
                            conditionSet.setCondition(this.conditionService.getConditionResponse(engineCondition, variableMap, elementMap));
                            groupConditions.add(conditionSet);
                        }
                        group.setConditionGroupCondition(groupConditions);
                        conditionGroup.add(group);
                    }
                    ruleResponse.setConditionGroup(conditionGroup);
                }
            }
        }
        // ??????
        Action action = getAction(ruleEngineRule.getActionValue(), ruleEngineRule.getActionType(), ruleEngineRule.getActionValueType());
        ruleResponse.setAction(action);
        // ????????????
        Action defaultValue = getAction(ruleEngineRule.getDefaultActionValue(), ruleEngineRule.getDefaultActionType(), ruleEngineRule.getDefaultActionValueType());
        DefaultAction defaultAction = BasicConversion.INSTANCE.convert(defaultValue);
        defaultAction.setEnableDefaultAction(ruleEngineRule.getEnableDefaultAction());
        ruleResponse.setDefaultAction(defaultAction);
        ruleResponse.setAbnormalAlarm(JSON.parseObject(ruleEngineRule.getAbnormalAlarm(), Rule.AbnormalAlarm.class));
        return ruleResponse;
    }

    /**
     * ??????????????????????????????
     *
     * @param id ??????id
     * @return GetRuleResponse
     */
    @Override
    public ViewRuleResponse getPublishRule(Integer id) {
        RuleEngineRulePublish engineRulePublish = this.ruleEngineRulePublishManager.lambdaQuery()
                .eq(RuleEngineRulePublish::getStatus, RuleStatus.PUBLISHED.getStatus())
                .eq(RuleEngineRulePublish::getRuleId, id)
                .one();
        if (engineRulePublish == null) {
            throw new ValidException("????????????????????????:{}", id);
        }
        String data = engineRulePublish.getData();
        Rule rule = Rule.buildRule(data);
        return this.getRuleResponseProcess(rule);
    }

    /**
     * ????????????
     *
     * @param id ??????id
     * @return GetRuleResponse
     */
    @Override
    public ViewRuleResponse getViewRule(Integer id) {
        RuleEngineRule ruleEngineRule = this.ruleEngineRuleManager.getById(id);
        if (ruleEngineRule == null) {
            throw new ValidException("??????????????????????????????:{}", id);
        }
        // ?????????????????????
        if (ruleEngineRule.getStatus().equals(RuleStatus.PUBLISHED.getStatus())) {
            return this.getPublishRule(id);
        }
        RuleEngineRulePublish engineRulePublish = this.ruleEngineRulePublishManager.lambdaQuery()
                .eq(RuleEngineRulePublish::getStatus, RuleStatus.WAIT_PUBLISH.getStatus())
                .eq(RuleEngineRulePublish::getRuleId, id)
                .one();
        if (engineRulePublish == null) {
            throw new ValidException("??????????????????????????????:{}", id);
        }
        String data = engineRulePublish.getData();
        Rule rule = Rule.buildRule(data);
        return this.getRuleResponseProcess(rule);
    }

    /**
     * ??????Rule???????????????GetRuleResponse
     *
     * @param rule Rule
     * @return GetRuleResponse
     */
    private ViewRuleResponse getRuleResponseProcess(Rule rule) {
        ViewRuleResponse ruleResponse = new ViewRuleResponse();
        ruleResponse.setId(rule.getId());
        ruleResponse.setName(rule.getName());
        ruleResponse.setCode(rule.getCode());
        ruleResponse.setWorkspaceId(rule.getWorkspaceId());
        ruleResponse.setWorkspaceCode(rule.getWorkspaceCode());
        ruleResponse.setDescription(rule.getDescription());

        List<ConditionGroup> conditionGroups = rule.getConditionSet().getConditionGroups();
        List<ConditionGroupConfig> groupArrayList = new ArrayList<>(conditionGroups.size());
        for (ConditionGroup conditionGroup : conditionGroups) {
            ConditionGroupConfig group = new ConditionGroupConfig();
            List<Condition> conditions = conditionGroup.getConditions();
            List<ConditionGroupCondition> conditionGroupConditions = new ArrayList<>(conditions.size());
            for (Condition condition : conditions) {
                ConditionGroupCondition conditionSet = new ConditionGroupCondition();
                ConditionResponse conditionResponse = new ConditionResponse();
                conditionResponse.setName(condition.getName());
                ConfigBean configBean = new ConfigBean();
                configBean.setLeftValue(this.getConfigValue(condition.getLeftValue()));
                configBean.setSymbol(condition.getOperator().getExplanation());
                configBean.setRightValue(this.getConfigValue(condition.getRightValue()));
                conditionResponse.setConfig(configBean);
                conditionSet.setCondition(conditionResponse);
                conditionGroupConditions.add(conditionSet);
            }
            group.setConditionGroupCondition(conditionGroupConditions);
            groupArrayList.add(group);
        }
        ruleResponse.setConditionGroup(groupArrayList);
        Value actionValue = rule.getActionValue();
        ruleResponse.setAction(BasicConversion.INSTANCE.convertAction(this.getConfigValue(actionValue)));
        DefaultAction defaultAction;
        if (rule.getDefaultActionValue() != null) {
            ConfigBean.Value value = this.getConfigValue(rule.getDefaultActionValue());
            defaultAction = BasicConversion.INSTANCE.convert(value);
            defaultAction.setEnableDefaultAction(EnableEnum.ENABLE.getStatus());
        } else {
            defaultAction = new DefaultAction();
            defaultAction.setEnableDefaultAction(EnableEnum.DISABLE.getStatus());
        }
        ruleResponse.setDefaultAction(defaultAction);
        ruleResponse.setAbnormalAlarm(rule.getAbnormalAlarm());
        // ???????????????????????????????????????
        ruleResponse.setParameters(this.ruleCountInfoService.getParameters(rule));
        return ruleResponse;
    }


    /**
     * ?????????/??????/??????/?????????
     *
     * @param cValue Value
     * @return ConfigBean.Value
     */
    public ConfigBean.Value getConfigValue(Value cValue) {
        ConfigBean.Value value = new ConfigBean.Value();
        value.setValueType(cValue.getValueType().getValue());
        if (cValue instanceof Constant) {
            value.setType(VariableType.CONSTANT.getType());
            Constant constant = (Constant) cValue;
            value.setValue(String.valueOf(constant.getValue()));
            value.setValueName(String.valueOf(constant.getValue()));
        } else if (cValue instanceof Element) {
            value.setType(VariableType.ELEMENT.getType());
            Element element = (Element) cValue;
            RuleEngineElement ruleEngineElement = this.ruleEngineElementManager.getById(element.getElementId());
            value.setValue(String.valueOf(element.getElementId()));
            value.setValueName(ruleEngineElement.getName());
        } else if (cValue instanceof Variable) {
            value.setType(VariableType.VARIABLE.getType());
            Variable variable = (Variable) cValue;
            value.setValue(String.valueOf(variable.getVariableId()));
            RuleEngineVariable engineVariable = this.ruleEngineVariableManager.getById(variable.getVariableId());
            value.setValueName(engineVariable.getName());
            if (engineVariable.getType().equals(VariableType.CONSTANT.getType())) {
                value.setVariableValue(engineVariable.getValue());
            }
        }
        return value;
    }

    /**
     * ????????????/????????????
     *
     * @param value     ?????????/???????????????/??????
     * @param type      ??????/??????/?????????
     * @param valueType STRING/NUMBER...
     * @return Action
     */
    public Action getAction(String value, Integer type, String valueType) {
        Action action = new Action();
        if (Validator.isEmpty(type)) {
            return action;
        }
        action.setValueType(valueType);
        action.setType(type);
        if (Validator.isEmpty(value)) {
            return action;
        }
        if (type.equals(VariableType.ELEMENT.getType())) {
            action.setValueName(this.ruleEngineElementManager.getById(value).getName());
        } else if (type.equals(VariableType.VARIABLE.getType())) {
            RuleEngineVariable engineVariable = this.ruleEngineVariableManager.getById(value);
            action.setValueName(engineVariable.getName());
            if (engineVariable.getType().equals(VariableType.CONSTANT.getType())) {
                action.setVariableValue(engineVariable.getValue());
            }
        }
        action.setValue(value);
        return action;
    }

}
