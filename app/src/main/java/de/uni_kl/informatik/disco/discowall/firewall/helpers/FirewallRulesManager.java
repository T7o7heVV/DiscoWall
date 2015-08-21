package de.uni_kl.informatik.disco.discowall.firewall.helpers;

import java.util.HashMap;
import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallIptableRulesHandler;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRuleExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterFirewallRulesHandler;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class FirewallRulesManager {
    private final HashMap<Integer, LinkedList<FirewallRules.IFirewallRule>> userIdToRulesListHash = new HashMap<>();

    public FirewallRulesManager() {
    }

    //region direct data access

    private LinkedList<FirewallRules.IFirewallRule> getRulesOrCreate(int userId) {
        if (userIdToRulesListHash.get(userId) == null)
            userIdToRulesListHash.put(userId, new LinkedList<FirewallRules.IFirewallRule>());

        return userIdToRulesListHash.get(userId);
    }

    private void addRuleEx(FirewallRules.IFirewallRule rule, int index) {
        getRulesOrCreate(rule.getUserId()).add(index, rule);
    }

    private void addRuleEx(FirewallRules.IFirewallRule rule) {
        getRulesOrCreate(rule.getUserId()).add(rule);
    }

    //endregion

    //region public: get rules

    public LinkedList<FirewallRules.IFirewallRule> getRules() {
        LinkedList<FirewallRules.IFirewallRule> rules = new LinkedList<>();

        for(LinkedList<FirewallRules.IFirewallRule> userRules : userIdToRulesListHash.values())
            rules.addAll(userRules);

        return rules;
    }

    public LinkedList<FirewallRules.IFirewallRule> getRules(int userId) {
        return new LinkedList<>(getRulesOrCreate(userId));
    }

    public LinkedList<FirewallRules.IFirewallPolicyRule> getPolicyRules(int userId) {
        LinkedList<FirewallRules.IFirewallPolicyRule> policyRules = new LinkedList<>();

        for(FirewallRules.IFirewallRule rule : getRulesOrCreate(userId))
            if (rule instanceof FirewallRules.IFirewallPolicyRule)
                policyRules.add((FirewallRules.IFirewallPolicyRule) rule);

        return policyRules;
    }

    public LinkedList<FirewallRules.IFirewallRedirectRule> getRedirectionRules(int userId) {
        LinkedList<FirewallRules.IFirewallRedirectRule> redirectRules = new LinkedList<>();

        for(FirewallRules.IFirewallRule rule : getRulesOrCreate(userId))
            if (rule instanceof FirewallRules.IFirewallRedirectRule)
                redirectRules.add((FirewallRules.IFirewallRedirectRule) rule);

        return redirectRules;
    }

    public boolean containsRule(FirewallRules.IFirewallRule rule) {
        return getRuleByUUID(rule.getUUID(), rule.getUserId()) != null;
    }

    private FirewallRules.IFirewallRule getRuleByUUID(String ruleUUID, int userID) {
        for(FirewallRules.IFirewallRule rule : getRules(userID))
            if (rule.getUUID().equals(ruleUUID))
                return rule;

        return null;
    }

    public int getRuleIndex(FirewallRules.IFirewallRule rule) {
        return getRulesOrCreate(rule.getUserId()).indexOf(rule);
    }

    //endregion

    //region public: create rules

    public FirewallRules.FirewallTransportRule createTransportLayerRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, FirewallRules.DeviceFilter deviceFilter, FirewallRules.ProtocolFilter protocolFilter, FirewallRules.RulePolicy rulePolicy) {
        FirewallRules.FirewallTransportRule rule = new FirewallRules.FirewallTransportRule(userId, sourceFilter, destinationFilter, deviceFilter, protocolFilter, rulePolicy);
        addRuleEx(rule);
        return rule;
    }

    public FirewallRules.FirewallTransportRule createTransportLayerRule(int userId, FirewallRules.RulePolicy rulePolicy) {
        FirewallRules.FirewallTransportRule rule = new FirewallRules.FirewallTransportRule(userId, rulePolicy);
        addRuleEx(rule);
        return rule;
    }

    public FirewallRules.FirewallTransportRedirectRule createTransportLayerRedirectionRule(int userId, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
        FirewallRules.FirewallTransportRedirectRule rule = new FirewallRules.FirewallTransportRedirectRule(userId, redirectTo);
        addRuleEx(rule);
        return rule;
    }

    public FirewallRules.FirewallTransportRedirectRule createTransportLayerRedirectionRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, FirewallRules.DeviceFilter deviceFilter, FirewallRules.ProtocolFilter protocolFilter, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
        FirewallRules.FirewallTransportRedirectRule rule = new FirewallRules.FirewallTransportRedirectRule(userId, sourceFilter, destinationFilter, deviceFilter, protocolFilter, redirectTo);
        addRuleEx(rule);
        return rule;
    }

    //endregion

    //region public: add rules

    public void addRule(FirewallRules.IFirewallRule ruleToAdd) throws FirewallRuleExceptions.DuplicateRuleException {
        // throw exception if rule is already listed:
        if (containsRule(ruleToAdd))
            throw new FirewallRuleExceptions.DuplicateRuleException(ruleToAdd);

        addRuleEx(ruleToAdd);
    }

    public void addRule(FirewallRules.IFirewallRule ruleToAdd, FirewallRules.IFirewallRule existingRuleBelowNewOne) throws FirewallRuleExceptions.DuplicateRuleException, FirewallRuleExceptions.RuleNotFoundException {
        // throw exception if rule is already listed:
        if (containsRule(ruleToAdd))
            throw new FirewallRuleExceptions.DuplicateRuleException(ruleToAdd);

        int newRuleIndex = getRuleIndex(existingRuleBelowNewOne);
        if (newRuleIndex < 0)
            throw new FirewallRuleExceptions.RuleNotFoundException(existingRuleBelowNewOne);

        addRuleEx(ruleToAdd, newRuleIndex);
    }

    //endregion

    //region public: move/delete rules

    public void deleteUserRules(int uid) {
        getRulesOrCreate(uid).clear();
    }

    public void deleteRule(FirewallRules.IFirewallRule rule) {
        getRulesOrCreate(rule.getUserId()).remove(rule);
    }

    public void deleteAllRules() {
        userIdToRulesListHash.clear();
    }

    public boolean moveRuleUp(FirewallRules.IFirewallRule rule) {
        int ruleIndex = getRuleIndex(rule);
        if (ruleIndex == 0)
            return false;

        getRulesOrCreate(rule.getUserId()).remove(rule);
        addRuleEx(rule, ruleIndex - 1);

        return true;
    }

    public boolean moveRuleDown(FirewallRules.IFirewallRule rule) {
        int ruleIndex = getRuleIndex(rule);
        if (ruleIndex == getRulesOrCreate(rule.getUserId()).size()-1)
            return false;

        getRulesOrCreate(rule.getUserId()).remove(rule);
        addRuleEx(rule, ruleIndex + 1);

        return true;
    }

    //endregion

}
