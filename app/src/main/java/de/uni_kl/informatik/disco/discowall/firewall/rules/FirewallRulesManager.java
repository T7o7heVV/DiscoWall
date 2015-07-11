package de.uni_kl.informatik.disco.discowall.firewall.rules;

import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.firewall.FirewallExceptions;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterFirewallRulesHandler;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class FirewallRulesManager {
    private static final String LOG_TAG = FirewallRulesManager.class.getSimpleName();

    public enum FirewallMode { FILTER_TCP, FILTER_UDP, FILTER_ALL, ALLOW_ALL, BLOCK_ALL }
    public enum FirewallPolicy { ALLOW, BLOCK, INTERACTIVE }

    //    private final HashMap<String, Connections.Connection> connectionIdToConnectionMap = new HashMap<>();
    private final RulesHash rulesHash = new RulesHash();
    private final FirewallIptableRulesHandler firewallIptableRulesHandler;
    private FirewallMode firewallMode;
    private FirewallPolicy firewallUnknownConnectionPolicy;

    public FirewallRulesManager(FirewallIptableRulesHandler firewallIptableRulesHandler) {
        this.firewallIptableRulesHandler = NetfilterFirewallRulesHandler.instance;
        this.firewallMode = FirewallMode.FILTER_TCP;
        this.firewallUnknownConnectionPolicy = FirewallPolicy.INTERACTIVE; // Is default-policy as defined by NetfilterBridgeIptablesHandler
    }

    public FirewallPolicy getFirewallPolicy() {
        return firewallUnknownConnectionPolicy;
    }

    public void setFirewallPolicy(FirewallPolicy policy) throws FirewallExceptions.FirewallException {
        Log.i(LOG_TAG, "changing firewall policy: " + firewallUnknownConnectionPolicy + " --> " + policy);
        // it is allowed to re-apply the same policy, so that it is possible to re-write the iptable rule

        /* IMPORTANT NOTE:
         * As usually only SYN/RST/FIN packages are of interest to the firewall,
         * changing the firewall-policy will NOT effect already established  connections.
         *    [ Theoretically only relevant, when current policy is INTERACTIVE/ALLOW (i.e. when there can be active connections)
         *      BUT: Connections could have been established even before changing to BLOCK, so also in BLOCK there could be existing connections ]
         *
         * ==> When changing to BLOCKED policy,ANY package must be blocked, so that running connections are closed too.
         *    BUT: If all packages were to be forwarded into the firewall, the interactive-
         *    ==> Only filter flags within interactive-chain
         */

        try {
            switch(policy) {
                case ALLOW:
                    firewallIptableRulesHandler.setDefaultPackageHandlingMode(FirewallIptableRulesHandler.PackageHandlingMode.ACCEPT_PACKAGE);
                    break;
                case BLOCK:
                    firewallIptableRulesHandler.setDefaultPackageHandlingMode(FirewallIptableRulesHandler.PackageHandlingMode.REJECT_PACKAGE);
                    break;
                case INTERACTIVE:
                    firewallIptableRulesHandler.setDefaultPackageHandlingMode(FirewallIptableRulesHandler.PackageHandlingMode.INTERACTIVE);
                    break;
            }
        } catch (ShellExecuteExceptions.ShellExecuteException e) {
            throw new FirewallExceptions.FirewallException("Unable to change firewall policy du to error: " + e.getMessage(), e);
        }

        firewallUnknownConnectionPolicy = policy;
    }

    public FirewallMode getFirewallMode() {
        return firewallMode;
    }

    public void setFirewallMode(FirewallMode firewallMode) {
        this.firewallMode = firewallMode;
    }

    public boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage, Connections.Connection connection) {
        switch(firewallMode) {
            case ALLOW_ALL:     // MODE: accept all packages
                return true;

            case BLOCK_ALL:     // MODE: block all packages
                return false;

            case FILTER_TCP:    // MODE: filter tcp only, accept rest
                if (tlPackage.getProtocol() != Packages.TransportProtocol.TCP)
                    return true; // package is not tcp ==> will not be filtered
                else
                    break;

            case FILTER_UDP:    // MODE: filter udp only, accept rest
                if (tlPackage.getProtocol() != Packages.TransportProtocol.UDP)
                    return true;
                else
                    break;

            case FILTER_ALL:    // MODE: filter any package
                break;
        }

//        switch(tlPackage.getProtocol())
//        {
//            case TCP:
//                return isFilteredTcpPackageAccepted((Packages.TcpPackage) tlPackage, (Connections.TcpConnection) connection);
//            case UDP:
//                return isFilteredUdpPackageAccepted((Packages.UdpPackage) tlPackage, (Connections.UdpConnection) connection);
//            default:
//                Log.e(LOG_TAG, "Unsupported Protocol: " + tlPackage.getProtocol());
//                return true;
//        }

        return isFilteredPackageAccepted(tlPackage, connection);
    }

    private boolean isFilteredPackageAccepted(Packages.TransportLayerPackage tlPackage, Connections.Connection connection) {
        Log.i(LOG_TAG, "filtering package: " + tlPackage);

        return true;
    }

    public FirewallRules.FirewallTransportRule createTcpRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, FirewallRules.DeviceFilter deviceFilter, FirewallRules.RulePolicy rulePolicy) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        FirewallRules.FirewallTransportRule rule = new FirewallRules.FirewallTransportRule(userId, sourceFilter, destinationFilter, deviceFilter, rulePolicy);

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

    public FirewallRules.FirewallTransportRedirectRule createTcpRedirectionRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, FirewallRules.DeviceFilter deviceFilter, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
        FirewallRules.FirewallTransportRedirectRule rule = new FirewallRules.FirewallTransportRedirectRule(userId, sourceFilter, destinationFilter, deviceFilter, redirectTo);

        // TODO

        rulesHash.addRule(rule);
        return rule;
    }

//    private boolean isFilteredUdpPackageAccepted(Packages.UdpPackage udpPackage, Connections.UdpConnection connection) {
//        return true;
//    }
//
//    private boolean isFilteredTcpPackageAccepted(Packages.TcpPackage tcpPackage, Connections.TcpConnection connection) {
//        Log.i(LOG_TAG, "Connection: " + connection);
//
//        return true;
//    }

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
