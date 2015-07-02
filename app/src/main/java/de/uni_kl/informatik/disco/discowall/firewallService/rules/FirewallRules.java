package de.uni_kl.informatik.disco.discowall.firewallService.rules;

import de.uni_kl.informatik.disco.discowall.packages.Packages;

public class FirewallRules {
    /************************* Rule Data ***********************************************************/

    public enum RulePolicy { ACCEPT, BLOCK, INTERACTIVE }
    public enum DeviceFilter { WIFI, UMTS_3G, ANY; }
    public enum ProtocolFilter { TCP, UDP, TCP_UDP }

    /*************************** ARCHITECTURE ******************************************************/
    public interface IFirewallRule {
        int getUserId();

        DeviceFilter getDeviceFilter();
        ProtocolFilter getProtocolFilter();
        Packages.IpPortPair getSourceFilter();
        Packages.IpPortPair getDestinationFilter();
    }

    public interface IFirewallPolicyRule extends IFirewallRule {
        RulePolicy getRulePolicy();
        boolean appliesTo(Packages.TransportLayerPackage tlPackage);
        boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage);
    }

    public interface IFirewallRedirectRule extends IFirewallRule {
        Packages.IpPortPair getRedirectionRemoteHost();
    }

    /***********************************************************************************************/

    private static abstract class AbstractFirewallRule implements IFirewallRule {
        private final int userId;
        private final DeviceFilter deviceFilter;
        private final ProtocolFilter protocolFilter;
        private final Packages.IpPortPair sourceFilter, destinationFilter;

        protected AbstractFirewallRule(int userId, ProtocolFilter protocolFilter, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter) {
            if (sourceFilter == null)
                throw new IllegalArgumentException("Source-Filter cannot be null!");
            if (destinationFilter == null)
                throw new IllegalArgumentException("Source-Filter cannot be null!");

            this.userId = userId;
            this.deviceFilter = deviceFilter;
            this.protocolFilter = protocolFilter;
            this.sourceFilter = sourceFilter;
            this.destinationFilter = destinationFilter;
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
        public String toString() {
            String sourceFilterStr;
            String destinationFilterStr;

            if (sourceFilter != null)
                sourceFilterStr = sourceFilter.toString();
            else
                sourceFilterStr = "*:*";

            if (destinationFilter != null)
                destinationFilterStr = destinationFilter.toString();
            else
                destinationFilterStr = "*:*";

            return sourceFilterStr + " -> " + destinationFilterStr + " {" + " [" + protocolFilter + "]" + " userID=" + userId + " deviceFilter=" + deviceFilter + " }";
        }
    }

    private static abstract class AbstractFirewallPolicyRule extends AbstractFirewallRule implements IFirewallPolicyRule {
        private final RulePolicy rulePolicy;

        protected AbstractFirewallPolicyRule(int userId, ProtocolFilter protocolFilter, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, RulePolicy rulePolicy) {
            super(userId, protocolFilter, sourceFilter, destinationFilter, deviceFilter);
            this.rulePolicy = rulePolicy;
        }

        @Override
        public RulePolicy getRulePolicy() {
            return rulePolicy;
        }

        private boolean filterMatches(Packages.IpPortPair filter, Packages.IpPortPair packageInfo) {
            // check ip
            if (filter.hasIp()) {
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
            // Since the connection allows the passing of a connectin-package in BOTH directions, the role of the source- and destination-filter has to be tested in both directions for one package.
            return ( filterMatches(getSourceFilter(), tlPackage.getSource()) && filterMatches(getDestinationFilter(), tlPackage.getDestination()) ) // package in direction intended for rule
                    || ( filterMatches(getDestinationFilter(), tlPackage.getSource()) && filterMatches(getSourceFilter(), tlPackage.getDestination()) ); // returning package in opposite direction
        }

        @Override
        public boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage) {
            if (!appliesTo(tlPackage))
                throw new IllegalArgumentException("Rule does not apply to package.");
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + " { rulePolicy=" + rulePolicy + " }";
        }
    }

    public static class FirewallTransportRule extends AbstractFirewallPolicyRule  {
        FirewallTransportRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, RulePolicy rulePolicy) {
            super(userId, ProtocolFilter.TCP, sourceFilter, destinationFilter, deviceFilter, rulePolicy);
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
        FirewallTransportRedirectRule(int userId, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
            super(userId, ProtocolFilter.TCP, sourceFilter, destinationFilter, deviceFilter, redirectTo);
        }
    }
 }
