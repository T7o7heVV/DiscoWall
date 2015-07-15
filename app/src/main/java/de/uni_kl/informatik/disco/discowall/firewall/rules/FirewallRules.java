package de.uni_kl.informatik.disco.discowall.firewall.rules;

import de.uni_kl.informatik.disco.discowall.packages.Packages;

public class FirewallRules {
    /************************* Rule Data ***********************************************************/

    public enum RulePolicy {
        ALLOW, BLOCK, INTERACTIVE }

    public enum ProtocolFilter { TCP, UDP, TCP_UDP;
        public boolean isTcp() {
            return this == TCP || this == TCP_UDP;
        }
        public boolean isUdp() {
            return this == UDP || this == TCP_UDP;
        }
        public boolean isTcpAndUdp() {
            return this == TCP_UDP;
        }

        public static ProtocolFilter construct(boolean tcp, boolean udp) throws ProtocolFilterException {
            if (tcp && udp)
                return TCP_UDP;
            else if (tcp)
                return TCP;
            else if (udp)
                return UDP;
            else
                throw new ProtocolFilterException("Invalid ProtocolFilter! At least one protocol has to be specified.");
        }

        public static class ProtocolFilterException extends Exception {
            public ProtocolFilterException(String detailMessage) {
                super(detailMessage);
            }
        }
    }

    public enum DeviceFilter {
        WIFI, UMTS, WiFi_UMTS;

        public boolean allowsWifi() {
            return this == WIFI || this == WiFi_UMTS;
        }
        public boolean allowsUmts() {
            return this == UMTS || this == WiFi_UMTS;
        }
        public boolean allowsAny() {
            return this == WiFi_UMTS;
        }

        public static DeviceFilter construct(boolean wifi, boolean umts) throws DeviceFilterException {
            if (wifi && umts)
                return WiFi_UMTS;
            else if (wifi)
                return WIFI;
            else if (umts)
                return UMTS;
            else
                throw new DeviceFilterException("Invalid DeviceFilter! At least one device has to be specified.");
        }

        public static class DeviceFilterException extends Exception {
            public DeviceFilterException(String detailMessage) {
                super(detailMessage);
            }
        }
    }

    /*************************** ARCHITECTURE ******************************************************/
    public interface IFirewallRule {
        int getUserId();

        DeviceFilter getDeviceFilter();
        void setDeviceFilter(DeviceFilter deviceFilter);
        ProtocolFilter getProtocolFilter();
        void setProtocolFilter(ProtocolFilter protocolFilter);
        Packages.IpPortPair getLocalFilter();
        Packages.IpPortPair getRemoteFilter();
        boolean appliesTo(Packages.TransportLayerPackage tlPackage);
    }

    public interface IFirewallPolicyRule extends IFirewallRule {
        RulePolicy getRulePolicy();
//        DirectionFilter getDirectionFilter();
        boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage);
    }

    public interface IFirewallRedirectRule extends IFirewallRule {
        Packages.IpPortPair getRedirectionRemoteHost();
    }

    /***********************************************************************************************/

    private static abstract class AbstractFirewallRule implements IFirewallRule {
        private final int userId;
        private final Packages.IpPortPair localFilter, remoteFilter;
        private DeviceFilter deviceFilter;
        private ProtocolFilter protocolFilter;

        protected AbstractFirewallRule(int userId, ProtocolFilter protocolFilter, Packages.IpPortPair localFilter, Packages.IpPortPair remoteFilter, DeviceFilter deviceFilter) {
            if (localFilter == null)
                throw new IllegalArgumentException("Source-Filter cannot be null!");
            if (remoteFilter == null)
                throw new IllegalArgumentException("Source-Filter cannot be null!");

            this.userId = userId;
            this.deviceFilter = deviceFilter;
            this.protocolFilter = protocolFilter;
            this.localFilter = localFilter;
            this.remoteFilter = remoteFilter;
        }

        @Override
        public int getUserId() {
            return userId;
        }

        @Override
        public DeviceFilter getDeviceFilter() {
            return deviceFilter;
        }

        @Override
        public void setDeviceFilter(DeviceFilter deviceFilter) {
            this.deviceFilter = deviceFilter;
        }

        @Override
        public ProtocolFilter getProtocolFilter() {
            return protocolFilter;
        }

        @Override
        public void setProtocolFilter(ProtocolFilter protocolFilter) {
            this.protocolFilter = protocolFilter;
        }

        @Override
        public Packages.IpPortPair getLocalFilter() {
            return localFilter;
        }

        @Override
        public Packages.IpPortPair getRemoteFilter() {
            return remoteFilter;
        }

        private boolean filterMatches(Packages.IpPortPair filter, Packages.IpPortPair packageInfo, boolean ignoreIP) {
            // check ip
            if (!ignoreIP && filter.hasIp()) {
                if (!packageInfo.getIp().equals(filter.getIp()))
                    return false;
            }

            // check port
            if (filter.hasPort()) {
                if (packageInfo.getPort() != filter.getPort())
                    return false;
            }

            return true;
        }

        @Override
        public boolean appliesTo(Packages.TransportLayerPackage tlPackage) {
            // The local-address has a irrelevant host-ip, which is sometimes "localhost" or "127.0.0.1" or even the hostname.
            // But as it specifies the localhost, only the port is relevant anyway.
            return filterMatches(localFilter, tlPackage.getLocalAddress(), true) && filterMatches(remoteFilter, tlPackage.getRemoteAddress(), false);
        }

        @Override
        public String toString() {
            String sourceFilterStr;
            String destinationFilterStr;

            if (localFilter != null)
                sourceFilterStr = localFilter.toString();
            else
                sourceFilterStr = "*:*";

            if (remoteFilter != null)
                destinationFilterStr = remoteFilter.toString();
            else
                destinationFilterStr = "*:*";

            return sourceFilterStr + " -> " + destinationFilterStr + " {" + " [" + protocolFilter + "]" + " userID=" + userId + " deviceFilter=" + deviceFilter + " }";
        }
    }

    private static abstract class AbstractFirewallPolicyRule extends AbstractFirewallRule implements IFirewallPolicyRule {
        private final RulePolicy rulePolicy;
//        private final DirectionFilter directionFilter = DirectionFilter.ANY;

        protected AbstractFirewallPolicyRule(int userId, ProtocolFilter protocolFilter, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, RulePolicy rulePolicy) {
            super(userId, protocolFilter, sourceFilter, destinationFilter, deviceFilter);
            this.rulePolicy = rulePolicy;
        }

        @Override
        public RulePolicy getRulePolicy() {
            return rulePolicy;
        }

        public boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage) {
            if (!appliesTo(tlPackage))
                throw new IllegalArgumentException("Rule does not apply to package.");
            return false;
        }

//        @Override
//        public DirectionFilter getDirectionFilter() {
//            return directionFilter;
//        }

        @Override
        public String toString() {
            return super.toString() + " { rulePolicy=" + rulePolicy + " }";
        }
    }

    public static class FirewallTransportRule extends AbstractFirewallPolicyRule implements IFirewallPolicyRule {
        public FirewallTransportRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, ProtocolFilter protocolFilter, RulePolicy rulePolicy) {
            super(userId, protocolFilter, sourceFilter, destinationFilter, deviceFilter, rulePolicy);
        }
    }

    private static abstract class AbstractFirewallRedirectRule extends AbstractFirewallRule implements IFirewallRedirectRule {
        private final Packages.IpPortPair redirectTo;

        protected AbstractFirewallRedirectRule(int userId, ProtocolFilter protocolFilter, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
            super(userId, protocolFilter, sourceFilter, destinationFilter, deviceFilter);
            this.redirectTo = redirectTo;

            if (redirectTo == null)
                throw new FirewallRuleExceptions.InvalidRuleDefinitionException(this, "No redirection target specified for redirection rule.");
            if (!redirectTo.hasIp() || !redirectTo.hasPort())
                throw new FirewallRuleExceptions.InvalidRuleDefinitionException(this, "IP or Port missing for redirection target: " + redirectTo);

            // It will be allowed to redirect multiple connections to the same host
//            if (!sourceFilter.hasIp() || !sourceFilter.hasPort())
//                throw new FirewallRuleExceptions.InvalidRuleDefinitionException(this, "IP or Port missing for connection-source: " + sourceFilter);
//            if (!destinationFilter.hasIp() || !destinationFilter.hasPort())
//                throw new FirewallRuleExceptions.InvalidRuleDefinitionException(this, "IP or Port missing for connection-destination: " + destinationFilter);
        }

        @Override
        public Packages.IpPortPair getRedirectionRemoteHost() {
            return redirectTo;
        }

        @Override
        public String toString() {
            return super.toString() + " { redirectTo=" + redirectTo + " }";
        }

    }

    public static class FirewallTransportRedirectRule extends AbstractFirewallRedirectRule  {
        public FirewallTransportRedirectRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, ProtocolFilter protocolFilter, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
            super(userId, protocolFilter, sourceFilter, destinationFilter, deviceFilter, redirectTo);
        }
    }
 }
