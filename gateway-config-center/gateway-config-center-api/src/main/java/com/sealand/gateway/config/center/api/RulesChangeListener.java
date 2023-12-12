package com.sealand.gateway.config.center.api;

import com.sealand.common.config.Rule;

import java.util.List;

public interface RulesChangeListener {
    void onRulesChange(List<Rule> ruleList);

}
