package de.uni_kl.informatik.disco.discowall.firewallService.rules;

import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeIptablesHandler;
import de.uni_kl.informatik.disco.discowall.netfilter.iptables.IptablesControl;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;
import static de.uni_kl.informatik.disco.discowall.firewallService.rules.FirewallRules.*;

public class StaticFirewallRules {
//    private static abstract class StaticTransportRule extends FirewallTransportRule implements IStaticFirewallTransportRule {
//        private final int userIdFilter;
//        private final DeviceFilter deviceFilter;
//        private final String iptablesChain, iptablesRule, ipablesReverseRule;
//
//        StaticTransportRule(FirewallRuleAction ruleAction, Packages.TransportProtocol protocol, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter) {
//            this(ruleAction, protocol, sourceFilter, destinationFilter, deviceFilter, -1);
//        }
//
//        StaticTransportRule(FirewallRuleAction ruleAction, Packages.TransportProtocol protocol, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, int userIdFilter) {
//            super(ruleAction, FirewallRuleKind.STATIC, protocol, sourceFilter, destinationFilter);
//            this.userIdFilter = userIdFilter;
//            this.deviceFilter = deviceFilter;
//
//            switch(deviceFilter) {
//                case UMTS_3G:
//                    iptablesChain = NetfilterBridgeIptablesHandler.CHAIN_FIREWALL_INTERFACE_3G;
//                    break;
//                case WIFI:
//                    iptablesChain = NetfilterBridgeIptablesHandler.CHAIN_FIREWALL_INTERFACE_WIFI;
//                    break;
//                default:
//                    throw new RuntimeException("Implementation missing! No iptable chain assigned to device-filter: " + deviceFilter);
//            }
//
//            iptablesRule = createIptablesRule(true);
//            ipablesReverseRule = createIptablesRule(false);
//        }
//
//        @Override
//        public boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage) {
//            return getRuleAction() == FirewallRuleAction.ACCEPT; // static rules accept either all packages, or none according to their action-policy.
//        }
//
//        public int getUserIdFilter() {
//            return userIdFilter;
//        }
//
//        public boolean hasUserIdFilter() {
//            return userIdFilter >= 0;
//        }
//
//        @Override
//        public DeviceFilter getDeviceFilter() {
//            return deviceFilter;
//        }
//
//        @Override
//        public String getDescription() {
//            if (hasUserIdFilter())
//                return super.getDescription() + " [ userID="+ userIdFilter +" ]";
//            else
//                return super.getDescription() + " [ userID=* ]";
//        }
//
//        private static String createIptablesSourceFilter(Packages.IpPortPair source) {
//            if (source == null)
//                return "";
//
//            String rule = "";
//
//            if (!source.getIp().isEmpty())
//                rule += " -s " + source.getIp();
//            if (source.getPort() > 0)
//                rule += " --source-port " + source.getPort();
//
//            return rule;
//        }
//
//        private static String createIptablesDestinationFilter(Packages.IpPortPair destination) {
//            if (destination == null)
//                return "";
//
//            String rule = "";
//
//            if (!destination.getIp().isEmpty())
//                rule += " -d " + destination.getIp();
//            if (destination.getPort() > 0)
//                rule += " --destination-port " + destination.getPort();
//
//            return rule;
//        }
//
//        private String createIptablesRule(boolean directionSourceToDestination) {
//            String rule = "";
//
//            switch(getProtocol()) {
//                case TCP:
//                    rule = "-p tcp";
//                    break;
//                case UDP:
//                    rule = "-p udp";
//                    break;
//            }
//
//            if (directionSourceToDestination) {
//                // Filtering for direction source ==> destination
//                rule += createIptablesSourceFilter(getSourceFilter());
//                rule += createIptablesDestinationFilter(getDestinationFilter());
//            } else {
//                // Filtering for answering-direction destination => source
//                rule += createIptablesSourceFilter(getDestinationFilter());
//                rule += createIptablesDestinationFilter(getSourceFilter());
//            }
//
//            if (hasUserIdFilter())
//                rule += " -m owner --uid-owner " + userIdFilter;
//
//            switch(getRuleAction()) {
//                case ACCEPT:
//                    rule += " -j " + NetfilterBridgeIptablesHandler.CHAIN_FIREWALL_ACTION_ACCEPT;
//                    break;
//                case DROP:
//                    rule += " -j " + NetfilterBridgeIptablesHandler.CHAIN_FIREWALL_ACTION_REJECT;
//                    break;
//            }
//
//            return rule;
//        }
//
//        @Override
//        public void setRuleEnabled(boolean enabled) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
//            if (enabled) {
//                IptablesControl.ruleAddIfMissing(iptablesChain, iptablesRule);
//                IptablesControl.ruleAddIfMissing(iptablesChain, ipablesReverseRule);
//            } else {
//                IptablesControl.ruleDeleteIgnoreIfMissing(iptablesChain, iptablesRule);
//                IptablesControl.ruleDeleteIgnoreIfMissing(iptablesChain, ipablesReverseRule);
//            }
//        }
//
//        @Override
//        public boolean isRuleEnabled() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
//            return IptablesControl.ruleExists(iptablesChain, iptablesRule)
//                    && IptablesControl.ruleExists(iptablesChain, ipablesReverseRule);
//        }
//    }
//
//    static class StaticTcpRule extends StaticTransportRule {
//        StaticTcpRule(FirewallRuleAction ruleAction, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter) {
//            super(ruleAction, Packages.TransportProtocol.TCP, sourceFilter, destinationFilter, deviceFilter);
//        }
//
//        StaticTcpRule(FirewallRuleAction ruleAction, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, int userIdFilter) {
//            super(ruleAction, Packages.TransportProtocol.TCP, sourceFilter, destinationFilter, deviceFilter, userIdFilter);
//        }
//    }
//
//    static class StaticUdpRule extends StaticTransportRule {
//        StaticUdpRule(FirewallRuleAction ruleAction, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter) {
//            super(ruleAction, Packages.TransportProtocol.UDP, sourceFilter, destinationFilter, deviceFilter);
//        }
//
//        StaticUdpRule(FirewallRuleAction ruleAction, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, int userIdFilter) {
//            super(ruleAction, Packages.TransportProtocol.UDP, sourceFilter, destinationFilter, deviceFilter, userIdFilter);
//        }
//    }
}
