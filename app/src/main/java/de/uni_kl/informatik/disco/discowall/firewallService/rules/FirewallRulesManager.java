package de.uni_kl.informatik.disco.discowall.firewallService.rules;

import java.util.HashMap;

import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;

public class FirewallRulesManager {
    private final HashMap<String, Connections.Connection> connectionIdToConnectionMap = new HashMap<>();

    public FirewallRulesManager() {
    }

    public StaticFirewallRules.StaticTcpRule createRule_Accept(FirewallRules.DeviceFilter device, Packages.TcpPackage tcpPackage) {
        return new StaticFirewallRules.StaticTcpRule(FirewallRules.FirewallRuleAction.ACCEPT, tcpPackage.getSource(), tcpPackage.getDestination(), device);
    }

    public StaticFirewallRules.StaticTcpRule createRule_AcceptTCP(FirewallRules.DeviceFilter device, Packages.IpPortPair source, Packages.IpPortPair destination) {
        return new StaticFirewallRules.StaticTcpRule(FirewallRules.FirewallRuleAction.ACCEPT, source, destination, device);
    }


}
