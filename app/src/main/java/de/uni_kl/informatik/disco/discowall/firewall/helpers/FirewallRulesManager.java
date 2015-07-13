package de.uni_kl.informatik.disco.discowall.firewall.helpers;

import java.util.HashMap;
import java.util.LinkedList;

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

    public void writeRedirectionRuleToIptables(FirewallRules.FirewallTransportRule rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        // TODO
    }

    public void writePolicyRuleToIptables(FirewallRules.FirewallTransportRule rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        try {
            // If TCP should be filtered:
            if (rule.getProtocolFilter().isTcp())
                firewallIptableRulesHandler.addTransportLayerRule(Packages.TransportLayerProtocol.TCP, rule.getUserId(), new Packages.SourceDestinationPair(rule.getLocalFilter(), rule.getRemoteFilter()), rule.getRulePolicy(), rule.getDeviceFilter());

            // If UDP should be filtered:
            if (rule.getProtocolFilter().isUdp())
                firewallIptableRulesHandler.addTransportLayerRule(Packages.TransportLayerProtocol.UDP, rule.getUserId(), new Packages.SourceDestinationPair(rule.getLocalFilter(), rule.getRemoteFilter()), rule.getRulePolicy(), rule.getDeviceFilter());

        } catch (ShellExecuteExceptions.ShellExecuteException e) {

            // Remove created rule (if any), when an exception occurrs:
            if (rule.getProtocolFilter().isTcp())
                firewallIptableRulesHandler.deleteTransportLayerRule(Packages.TransportLayerProtocol.TCP, rule.getUserId(), new Packages.SourceDestinationPair(rule.getLocalFilter(), rule.getRemoteFilter()), rule.getRulePolicy(), rule.getDeviceFilter());
            if (rule.getProtocolFilter().isUdp())
                firewallIptableRulesHandler.deleteTransportLayerRule(Packages.TransportLayerProtocol.UDP, rule.getUserId(), new Packages.SourceDestinationPair(rule.getLocalFilter(), rule.getRemoteFilter()), rule.getRulePolicy(), rule.getDeviceFilter());

            throw e;
        }
    }

    public FirewallRules.FirewallTransportRule createTransportLayerRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, FirewallRules.DeviceFilter deviceFilter, FirewallRules.ProtocolFilter protocolFilter, FirewallRules.RulePolicy rulePolicy) {
        FirewallRules.FirewallTransportRule rule = new FirewallRules.FirewallTransportRule(userId, sourceFilter, destinationFilter, deviceFilter, protocolFilter, rulePolicy);
        rulesHash.addRule(rule);
        return rule;
    }

    public FirewallRules.FirewallTransportRedirectRule createTransportLayerRedirectionRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, FirewallRules.DeviceFilter deviceFilter, FirewallRules.ProtocolFilter protocolFilter, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
        FirewallRules.FirewallTransportRedirectRule rule = new FirewallRules.FirewallTransportRedirectRule(userId, sourceFilter, destinationFilter, deviceFilter, protocolFilter, redirectTo);
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
