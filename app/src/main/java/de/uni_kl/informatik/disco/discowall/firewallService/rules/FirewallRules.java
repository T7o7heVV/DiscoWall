package de.uni_kl.informatik.disco.discowall.firewallService.rules;

import de.uni_kl.informatik.disco.discowall.packages.Packages;

public class FirewallRules {
    /************************* Rule Data ***********************************************************/

    public static enum RuleAction {
        ACCEPT, DROP;
    }

    public static enum DeviceFilter {
        WIFI, UMTS_3G, ANY;
    }

    public static enum ProtocolFilter {
        TCP, UDP, TCP_UDP
    }

    /************************ Rule Exceptions ******************************************************/
//    public static class FirewallRuleException extends Exception {
//        public FirewallRuleException(String message) {
//            super(message);
//        }
//    }

    /*************************** ARCHITECTURE ******************************************************/
    public interface IFirewallRule {
        RuleAction getRuleAction();

        DeviceFilter getDeviceFilter();
        ProtocolFilter getProtocolFilter();
        Packages.IpPortPair getSourceFilter();
        Packages.IpPortPair getDestinationFilter();

        boolean appliesTo(Packages.TransportLayerPackage tlPackage);
        boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage);
    }

    /***********************************************************************************************/

    static abstract class AbstractFirewallRule implements IFirewallRule {
        private final RuleAction action;
        private final DeviceFilter deviceFilter;
        private final ProtocolFilter protocolFilter;
        private final Packages.IpPortPair sourceFilter, destinationFilter;

        protected AbstractFirewallRule(RuleAction action, ProtocolFilter protocolFilter, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter) {
            this.action = action;
            this.deviceFilter = deviceFilter;
            this.protocolFilter = protocolFilter;
            this.sourceFilter = sourceFilter;
            this.destinationFilter = destinationFilter;
        }

        @Override
        public RuleAction getRuleAction() {
            return action;
        }

        @Override
        public DeviceFilter getDeviceFilter() {
            return deviceFilter;
        }

        @Override
        public ProtocolFilter getProtocolFilter() {
            return protocolFilter;
        }

        @Override
        public Packages.IpPortPair getSourceFilter() {
            return sourceFilter;
        }

        @Override
        public Packages.IpPortPair getDestinationFilter() {
            return destinationFilter;
        }

        @Override
        public boolean appliesTo(Packages.TransportLayerPackage tlPackage) {
            return false;
        }

        @Override
        public boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage) {
            if (!appliesTo(tlPackage))
                throw new IllegalArgumentException("Rule does not apply to package.");
            return false;
        }
    }

//    static abstract class FirewallTransportRule extends FirewallRule implements IFirewallTransportRule {
//        private final Packages.IpPortPair sourceFilter, destinationFilter;
//        private final Packages.TransportProtocol protocol;
//
//        protected FirewallTransportRule(FirewallRuleAction ruleAction, FirewallRuleKind ruleKind, Packages.TransportProtocol protocol, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter) {
//            super(ruleAction, ruleKind);
//            this.sourceFilter = sourceFilter;
//            this.destinationFilter = destinationFilter;
//            this.protocol = protocol;
//        }
//
//        @Override
//        public Packages.TransportProtocol getProtocol() {
//            return protocol;
//        }
//
//        @Override
//        public Packages.IpPortPair getSourceFilter() {
//            return sourceFilter;
//        }
//
//        @Override
//        public Packages.IpPortPair getDestinationFilter() {
//            return destinationFilter;
//        }
//
//        @Override
//        public boolean hasDestinationFilter() {
//            return destinationFilter != null;
//        }
//
//        @Override
//        public boolean hasSourceFilter() {
//            return sourceFilter != null;
//        }
//
//        @Override
//        public String getDescription() {
//            String sourceFilterStr;
//            String destinationFilterStr;
//
//            if (sourceFilter != null)
//                sourceFilterStr = sourceFilter.toString();
//            else
//                sourceFilterStr = "*:*";
//
//            if (destinationFilter != null)
//                destinationFilterStr = destinationFilter.toString();
//            else
//                destinationFilterStr = "*:*";
//
//            return "Packages matching connection: " + sourceFilterStr + " -> " + destinationFilterStr + " { action=" + getRuleAction() +", kind=" + getRuleKind() + " }";
//        }
//    }

 }
