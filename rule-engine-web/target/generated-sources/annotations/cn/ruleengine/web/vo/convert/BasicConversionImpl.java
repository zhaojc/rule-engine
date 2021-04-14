package cn.ruleengine.web.vo.convert;

import cn.ruleengine.web.store.entity.RuleEngineConditionGroupCondition;
import cn.ruleengine.web.store.entity.RuleEngineElement;
import cn.ruleengine.web.store.entity.RuleEngineMenu;
import cn.ruleengine.web.store.entity.RuleEngineRule;
import cn.ruleengine.web.store.entity.RuleEngineUser;
import cn.ruleengine.web.store.entity.RuleEngineVariable;
import cn.ruleengine.web.store.entity.RuleEngineWorkspace;
import cn.ruleengine.web.vo.condition.ConfigBean.Value;
import cn.ruleengine.web.vo.condition.group.condition.SaveOrUpdateConditionGroupCondition;
import cn.ruleengine.web.vo.element.GetElementResponse;
import cn.ruleengine.web.vo.menu.ListMenuResponse;
import cn.ruleengine.web.vo.rule.Action;
import cn.ruleengine.web.vo.rule.DefaultAction;
import cn.ruleengine.web.vo.rule.RuleDefinition;
import cn.ruleengine.web.vo.user.UserData;
import cn.ruleengine.web.vo.user.UserResponse;
import cn.ruleengine.web.vo.variable.GetVariableResponse;
import cn.ruleengine.web.vo.workspace.ListWorkspaceResponse;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2021-04-14T11:12:11+0800",
    comments = "version: 1.3.0.Final, compiler: Eclipse JDT (IDE) 1.3.1200.v20200916-0645, environment: Java 11.0.8 (AdoptOpenJDK)"
)
public class BasicConversionImpl implements BasicConversion {

    @Override
    public ListMenuResponse convert(RuleEngineMenu ruleEngineMenu) {
        if ( ruleEngineMenu == null ) {
            return null;
        }

        ListMenuResponse listMenuResponse = new ListMenuResponse();

        return listMenuResponse;
    }

    @Override
    public GetElementResponse convert(RuleEngineElement ruleEngineElement) {
        if ( ruleEngineElement == null ) {
            return null;
        }

        GetElementResponse getElementResponse = new GetElementResponse();

        return getElementResponse;
    }

    @Override
    public GetVariableResponse convert(RuleEngineVariable ruleEngineVariable) {
        if ( ruleEngineVariable == null ) {
            return null;
        }

        GetVariableResponse getVariableResponse = new GetVariableResponse();

        return getVariableResponse;
    }

    @Override
    public RuleDefinition convert(RuleEngineRule ruleEngineRule) {
        if ( ruleEngineRule == null ) {
            return null;
        }

        RuleDefinition ruleDefinition = new RuleDefinition();

        return ruleDefinition;
    }

    @Override
    public DefaultAction convert(Value value) {
        if ( value == null ) {
            return null;
        }

        DefaultAction defaultAction = new DefaultAction();

        return defaultAction;
    }

    @Override
    public UserData convert(RuleEngineUser ruleEngineUser) {
        if ( ruleEngineUser == null ) {
            return null;
        }

        UserData userData = new UserData();

        return userData;
    }

    @Override
    public UserResponse convert(UserData userData) {
        if ( userData == null ) {
            return null;
        }

        UserResponse userResponse = new UserResponse();

        return userResponse;
    }

    @Override
    public RuleEngineConditionGroupCondition convert(SaveOrUpdateConditionGroupCondition saveOrUpdateConditionGroup) {
        if ( saveOrUpdateConditionGroup == null ) {
            return null;
        }

        RuleEngineConditionGroupCondition ruleEngineConditionGroupCondition = new RuleEngineConditionGroupCondition();

        return ruleEngineConditionGroupCondition;
    }

    @Override
    public DefaultAction convert(Action action) {
        if ( action == null ) {
            return null;
        }

        DefaultAction defaultAction = new DefaultAction();

        return defaultAction;
    }

    @Override
    public Action convertAction(Value configValue) {
        if ( configValue == null ) {
            return null;
        }

        Action action = new Action();

        return action;
    }

    @Override
    public List<ListWorkspaceResponse> convert(List<RuleEngineWorkspace> ruleEngineWorkspaces) {
        if ( ruleEngineWorkspaces == null ) {
            return null;
        }

        List<ListWorkspaceResponse> list = new ArrayList<ListWorkspaceResponse>( ruleEngineWorkspaces.size() );
        for ( RuleEngineWorkspace ruleEngineWorkspace : ruleEngineWorkspaces ) {
            list.add( convert( ruleEngineWorkspace ) );
        }

        return list;
    }

    @Override
    public ListWorkspaceResponse convert(RuleEngineWorkspace ruleEngineWorkspace) {
        if ( ruleEngineWorkspace == null ) {
            return null;
        }

        ListWorkspaceResponse listWorkspaceResponse = new ListWorkspaceResponse();

        return listWorkspaceResponse;
    }
}
