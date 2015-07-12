package de.uni_kl.informatik.disco.discowall.firewall.helpers;

import java.util.HashMap;
import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.firewall.Firewall;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallIptableRulesHandler;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRuleExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterFirewallRulesHandler;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class FirewallRulesManager {
    private final RulesHash rulesHash = new RulesHash();
    private final FirewallIptableRulesHandler firewallIptableRulesHandler = NetfilterFirewallRulesHandler.instance;

    public FirewallRulesManager() {
    }

    public LinkedList<FirewallRules.IFirewallRule> getRules() {
        return new LinkedList<>(rulesHash.getRules());
    }

    public LinkedList<FirewallRules.IFirewallRule> getRules(int userId) {
        return new LinkedList<>(rulesHash.getRules(userId));
    }

    public LinkedList<FirewallRules.IFirewallPolicyRule> getPolicyRules(int userId) {
        LinkedList<FirewallRules.IFirewallPolicyRule> policyRules = new LinkedList<>();

        for(FirewallRules.IFirewallRule rule : rulesHash.getRules(userId))
            if (rule instanceof FirewallRules.IFirewallPolicyRule)
                policyRules.add((FirewallRules.IFirewallPolicyRule) rule);

        return policyRules;
    }

    public LinkedList<FirewallRules.IFirewallRedirectRule> getRedirectionRules(int userId) {
        LinkedList<FirewallRules.IFirewallRedirectRule> redirectRules = new LinkedList<>();

        for(FirewallRules.IFirewallRule rule : rulesHash.getRules(userId))
            if (rule instanceof FirewallRules.IFirewallRedirectRule)
                redirectRules.add((FirewallRules.IFirewallRedirectRule) rule);

        return redirectRules;
    }

    public FirewallRules.FirewallTransportRule createTransportLayerRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, FirewallRules.DeviceFilter deviceFilter, FirewallRules.ProtocolFilter protocolFilter, FirewallRules.RulePolicy rulePolicy) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        FirewallRules.FirewallTransportRule rule = new FirewallRules.FirewallTransportRule(userId, sourceFilter, destinationFilter, deviceFilter, protocolFilter, rulePolicy);

        try {
            firewallIptableRulesHandler.addTcpConnectionRule(userId, new Packages.SourceDestinationPair(sourceFilter, destinationFilter), rulePolicy, deviceFilter);
        } catch (ShellExecuteExceptions.ShellExecuteException e) {
            // Remove created rule (if any), when an exception occurrs:
            firewallIptableRulesHandler.deleteTcpConnectionRule(userId, new Packages.SourceDestinationPair(sourceFilter, destinationFilter), rulePolicy, deviceFilter);
            throw e;
        }

        rulesHash.addRule(rule);
        return rule;
    }

    public FirewallRules.FirewallTransportRedirectRule createTransportLayerRedirectionRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, FirewallRules.DeviceFilter deviceFilter, FirewallRules.ProtocolFilter protocolFilter, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
        FirewallRules.FirewallTransportRedirectRule rule = new FirewallRules.FirewallTransportRedirectRule(userId, sourceFilter, destinationFilter, deviceFilter, protocolFilter, redirectTo);

        // TODO

        rulesHash.addRule(rule);
        return rule;
    }

    private class RulesHash {
        private final HashMap<Integer, LinkedList<FirewallRules.IFirewallRule>> userIdToRulesListHash = new HashMap<>();

        public void addRule(FirewallRules.IFirewallRule rule) {
            getUserRules(rule.getUserId()).add(rule);
        }

        public LinkedList<FirewallRules.IFirewallRule> getRules(int userId) {
            return new LinkedList<>(getUserRules(userId));
        }

        private LinkedList<FirewallRules.IFirewallRule> getUserRules(int userId) {
            LinkedList<FirewallRules.IFirewallRule> rules = userIdToRulesListHash.get(userId);

            if (rules == null) {
                rules = new LinkedList<>();
                userIdToRulesListHash.put(userId, rules);
            }

            return rules;
        }

        public LinkedList<FirewallRules.IFirewallRule> getRules() {
            LinkedList<FirewallRules.IFirewallRule> rules = new LinkedList<>();

            for(LinkedList<FirewallRules.IFirewallRule> userRules : userIdToRulesListHash.values())
                rules.addAll(userRules);

            return rules;
        }

    }
}
