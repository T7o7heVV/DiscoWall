package de.uni_kl.informatik.disco.discowall.firewall.rules;

import java.util.UUID;

import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterFirewallRulesHandler;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class FirewallRules {
    /************************* Rule Data ***********************************************************/

    public enum RulePolicy { ALLOW, BLOCK, INTERACTIVE }
    public enum RuleKind { Policy, Redirect }
    public enum ConnectionDirectionFilter {
        LOCAL_TO_REMOTE, REMOTE_TO_LOCAL, ANY
    }
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
        String getUUID();

        int getUserId();
        RuleKind getRuleKind();

        DeviceFilter getDeviceFilter();
        ProtocolFilter getProtocolFilter();
        Packages.IpPortPair getLocalFilter();
        Packages.IpPortPair getRemoteFilter();

        void setDeviceFilter(DeviceFilter deviceFilter);
        void setProtocolFilter(ProtocolFilter protocolFilter);
        void setLocalFilter(Packages.IpPortPair localFilter);
        void setRemoteFilter(Packages.IpPortPair remoteFilter);

        boolean appliesTo(Packages.TransportLayerPackage tlPackage);

        void addToIptables() throws Exception;
        void removeFromIptables() throws Exception;
    }

    public interface IFirewallPolicyRule extends IFirewallRule {
        RulePolicy getRulePolicy();
        void setRulePolicy(RulePolicy policy);
    }

    public interface IFirewallRedirectRule extends IFirewallRule {
        Packages.IpPortPair getRedirectionRemoteHost();
        void setRedirectionRemoteHost(Packages.IpPortPair redirectionRemoteHost) throws FirewallRuleExceptions.InvalidRuleDefinitionException;
    }

    /***********************************************************************************************/

    private static abstract class AbstractFirewallRule implements IFirewallRule {
        /**
         * This UUID can be used to check whether two rule instances are the same rule - even after the data of one instance has been changed.
         * This is required for loading rules (to know which rule has to be updated/changed, and which is a new rule).
         */
        private String uuid = UUID.randomUUID().toString();

        private final int userId;
        private Packages.IpPortPair localFilter, remoteFilter;
        private DeviceFilter deviceFilter;
        private ProtocolFilter protocolFilter;

        /**
         * Can limit rule-direction. Currently not supported by GUI and therefore fixed.
         */
        private ConnectionDirectionFilter connectionDirectionFilter = ConnectionDirectionFilter.ANY;

        protected AbstractFirewallRule(int userId, ProtocolFilter protocolFilter, Packages.IpPortPair localFilter, Packages.IpPortPair remoteFilter, DeviceFilter deviceFilter) {
            if (localFilter == null)
                throw new IllegalArgumentException("Source-Filter cannot be null!");
            if (remoteFilter == null)
                throw new IllegalArgumentException("Remote-Filter cannot be null!");

            this.userId = userId;
            this.deviceFilter = deviceFilter;
            this.protocolFilter = protocolFilter;
            this.localFilter = localFilter;
            this.remoteFilter = remoteFilter;
        }

        public String getUUID() {
            return uuid;
        }

        /**
         * Only used when loading a rule from storage.
         */
        public void setUUID(String uuid) {
            this.uuid = uuid;
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

        @Override
        public void setLocalFilter(Packages.IpPortPair localFilter) {
            if (localFilter == null)
                throw new IllegalArgumentException("Source-Filter cannot be null!");

            this.localFilter = localFilter;
        }

        @Override
        public void setRemoteFilter(Packages.IpPortPair remoteFilter) {
            if (remoteFilter == null)
                throw new IllegalArgumentException("Remote-Filter cannot be null!");

            this.remoteFilter = remoteFilter;
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
            // Device-Filter - check interface:
            switch(deviceFilter) {
                case WiFi_UMTS:
                    // any interface will do
                    break;
                case WIFI:
                    if (tlPackage.getNetworkInterface() != Packages.NetworkInterface.WiFi)
                        return false;
                    break;
                case UMTS:
                    // umts matches only
                    if (tlPackage.getNetworkInterface() != Packages.NetworkInterface.Umts)
                        return false;
                    break;
                default:
                    throw new RuntimeException("Device-Filter unknown: " + deviceFilter);
            }

            // Protocol Filter
            switch(protocolFilter) {
                case TCP_UDP:
                    // tcp+udp will do
                    break;
                case UDP:
                    if (tlPackage.getProtocol() != Packages.TransportLayerProtocol.UDP)
                        return false;
                    break;
                case TCP:
                    if (tlPackage.getProtocol() != Packages.TransportLayerProtocol.TCP)
                        return false;
                    break;
                default:
                    throw new RuntimeException("Protocol-Filter unknown: " + protocolFilter);
            }

            // Source- & Destination-Filter:
            // The local-address has a irrelevant host-ip, which is sometimes "localhost" or "127.0.0.1" or even the hostname.
            // But as it specifies the localhost, only the port is relevant anyway.
            boolean packageMatches = filterMatches(localFilter, tlPackage.getLocalAddress(), true) && filterMatches(remoteFilter, tlPackage.getRemoteAddress(), false);
            if (!packageMatches)
                return false;

            // Connection-Direction Filter:
            // only client to server connections are allowed by any rule:
            // The connection-direction is being checked, if the filter is set
            if (connectionDirectionFilter != ConnectionDirectionFilter.ANY) {
                // since TCP-Connections have connection-directions - UDP don't
                if (tlPackage.getProtocol() == Packages.TransportLayerProtocol.TCP) {
                    Packages.TcpPackage tcpPackage = (Packages.TcpPackage) tlPackage;

                    if (connectionDirectionFilter == ConnectionDirectionFilter.LOCAL_TO_REMOTE) {
                        if (tcpPackage.isRemoteConnectionEstablishSyn()) // remote host tries to establishe connection
                            return false;
                    }
                    if (connectionDirectionFilter == ConnectionDirectionFilter.REMOTE_TO_LOCAL) {
                        if (tcpPackage.isLocalConnectionEstablishSyn()) // localhost tries to establishe connection
                            return false;
                    }
                }
            }

            return true;
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
        private RulePolicy rulePolicy;
//        private final DirectionFilter directionFilter = DirectionFilter.ANY;

        protected AbstractFirewallPolicyRule(int userId, ProtocolFilter protocolFilter, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, RulePolicy rulePolicy) {
            super(userId, protocolFilter, sourceFilter, destinationFilter, deviceFilter);
            this.rulePolicy = rulePolicy;
        }

        @Override
        public RulePolicy getRulePolicy() {
            return rulePolicy;
        }

        @Override
        public void setRulePolicy(RulePolicy rulePolicy) {
            this.rulePolicy = rulePolicy;
        }

        @Override
        public RuleKind getRuleKind() {
            return RuleKind.Policy;
        }

        @Override
        public String toString() {
            return super.toString() + " { rulePolicy=" + rulePolicy + " }";
        }
    }

    public static class FirewallTransportRule extends AbstractFirewallPolicyRule implements IFirewallPolicyRule {
        public FirewallTransportRule(int userId, RulePolicy rulePolicy) {
            this(userId, new Packages.IpPortPair("", 0), new Packages.IpPortPair("", 0), DeviceFilter.WiFi_UMTS, ProtocolFilter.TCP_UDP, rulePolicy);
        }

        public FirewallTransportRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, ProtocolFilter protocolFilter, RulePolicy rulePolicy) {
            super(userId, protocolFilter, sourceFilter, destinationFilter, deviceFilter, rulePolicy);
        }

        @Override
        public void addToIptables() throws ShellExecuteExceptions.ShellExecuteException {
            try {
                // If TCP should be filtered:
                if (getProtocolFilter().isTcp())
                    NetfilterFirewallRulesHandler.instance.addPolicyRule(Packages.TransportLayerProtocol.TCP, getUserId(), new Connections.SimpleConnection(getLocalFilter(), getRemoteFilter()), getRulePolicy(), getDeviceFilter());

                // If UDP should be filtered:
                if (getProtocolFilter().isUdp())
                    NetfilterFirewallRulesHandler.instance.addPolicyRule(Packages.TransportLayerProtocol.UDP, getUserId(), new Connections.SimpleConnection(getLocalFilter(), getRemoteFilter()), getRulePolicy(), getDeviceFilter());

            } catch (ShellExecuteExceptions.ShellExecuteException e) {

                // Remove created rule (if any), when an exception occurrs:
                removeFromIptables();

                // forward exception after removing rule (if any)
                throw e;
            }
        }

        @Override
        public void removeFromIptables() throws ShellExecuteExceptions.ShellExecuteException {
            if (getProtocolFilter().isTcp())
                NetfilterFirewallRulesHandler.instance.deletePolicyRule(Packages.TransportLayerProtocol.TCP, getUserId(), new Connections.SimpleConnection(getLocalFilter(), getRemoteFilter()), getRulePolicy(), getDeviceFilter());
            if (getProtocolFilter().isUdp())
                NetfilterFirewallRulesHandler.instance.deletePolicyRule(Packages.TransportLayerProtocol.UDP, getUserId(), new Connections.SimpleConnection(getLocalFilter(), getRemoteFilter()), getRulePolicy(), getDeviceFilter());
        }

    }

    private static abstract class AbstractFirewallRedirectRule extends AbstractFirewallRule implements IFirewallRedirectRule {
        private Packages.IpPortPair redirectTo;

        protected AbstractFirewallRedirectRule(int userId, ProtocolFilter protocolFilter, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
            super(userId, protocolFilter, sourceFilter, destinationFilter, deviceFilter);
            setRedirectionRemoteHost(redirectTo);
        }

        @Override
        public RuleKind getRuleKind() {
            return RuleKind.Redirect;
        }

        @Override
        public Packages.IpPortPair getRedirectionRemoteHost() {
            return redirectTo;
        }

        public void setRedirectionRemoteHost(Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
            if (redirectTo == null)
                throw new FirewallRuleExceptions.InvalidRuleDefinitionException(this, "No redirection target specified for redirection rule.");
            if (!redirectTo.hasIp() || !redirectTo.hasPort())
                throw new FirewallRuleExceptions.InvalidRuleDefinitionException(this, "IP or Port missing for redirection target: " + redirectTo);

            // It will be allowed to redirect multiple connections to the same host
//            if (!sourceFilter.hasIp() || !sourceFilter.hasPort())
//                throw new FirewallRuleExceptions.InvalidRuleDefinitionException(this, "IP or Port missing for connection-source: " + sourceFilter);
//            if (!destinationFilter.hasIp() || !destinationFilter.hasPort())
//                throw new FirewallRuleExceptions.InvalidRuleDefinitionException(this, "IP or Port missing for connection-destination: " + destinationFilter);

            this.redirectTo = redirectTo;
        }

        @Override
        public String toString() {
            return super.toString() + " { redirectTo=" + redirectTo + " }";
        }

    }

    public static class FirewallTransportRedirectRule extends AbstractFirewallRedirectRule  {
        public FirewallTransportRedirectRule(int userId, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
            this(userId, new Packages.IpPortPair("", 0), new Packages.IpPortPair("", 0), DeviceFilter.WiFi_UMTS, ProtocolFilter.TCP_UDP, redirectTo);
        }

        public FirewallTransportRedirectRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, ProtocolFilter protocolFilter, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
            super(userId, protocolFilter, sourceFilter, destinationFilter, deviceFilter, redirectTo);
        }

        @Override
        public void addToIptables() throws Exception {
            try {
                // If TCP should be redirected:
                if (getProtocolFilter().isTcp())
                    NetfilterFirewallRulesHandler.instance.addRedirectionRule(Packages.TransportLayerProtocol.TCP, getUserId(), getLocalFilter().getPort(), getRemoteFilter(), getRedirectionRemoteHost(), getDeviceFilter());

                // If UDP should be redirected:
                if (getProtocolFilter().isUdp())
                    NetfilterFirewallRulesHandler.instance.addRedirectionRule(Packages.TransportLayerProtocol.UDP, getUserId(), getLocalFilter().getPort(), getRemoteFilter(), getRedirectionRemoteHost(), getDeviceFilter());

            } catch (ShellExecuteExceptions.ShellExecuteException e) {

                // Remove created rule (if any), when an exception occurs:
                removeFromIptables();

                // forward exception after removing rule (if any)
                throw e;
            }
        }

        @Override
        public void removeFromIptables() throws Exception {
            if (getProtocolFilter().isTcp())
                NetfilterFirewallRulesHandler.instance.deleteRedirectionRule(Packages.TransportLayerProtocol.TCP, getUserId(), getLocalFilter().getPort(), getRemoteFilter(), getRedirectionRemoteHost(), getDeviceFilter());
            if (getProtocolFilter().isUdp())
                NetfilterFirewallRulesHandler.instance.deleteRedirectionRule(Packages.TransportLayerProtocol.UDP, getUserId(), getLocalFilter().getPort(), getRemoteFilter(), getRedirectionRemoteHost(), getDeviceFilter());
        }
    }
 }
