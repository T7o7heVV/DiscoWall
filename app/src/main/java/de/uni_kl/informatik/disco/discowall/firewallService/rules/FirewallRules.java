package de.uni_kl.informatik.disco.discowall.firewallService.rules;

import de.uni_kl.informatik.disco.discowall.packages.Packages;

public class FirewallRules {
    /************************* Rule Data ***********************************************************/

    public enum RulePolicy { ACCEPT, BLOCK, INTERACTIVE }
    public enum DeviceFilter { WIFI, UMTS_3G, ANY; }
    public enum ProtocolFilter { TCP, UDP, TCP_UDP }

    /*************************** ARCHITECTURE ******************************************************/
    public interface IFirewallRule {
        RulePolicy getRulePolicy();
        int getUserId();

        DeviceFilter getDeviceFilter();
        ProtocolFilter getProtocolFilter();
        Packages.IpPortPair getSourceFilter();
        Packages.IpPortPair getDestinationFilter();

        boolean appliesTo(Packages.TransportLayerPackage tlPackage);
        boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage);
    }

    /***********************************************************************************************/

    static abstract class AbstractFirewallRule implements IFirewallRule {
        private final int userId;
        private final RulePolicy rulePolicy;
        private final DeviceFilter deviceFilter;
        private final ProtocolFilter protocolFilter;
        private final Packages.IpPortPair sourceFilter, destinationFilter;

        protected AbstractFirewallRule(int userId, RulePolicy rulePolicy, ProtocolFilter protocolFilter, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter) {
            if (sourceFilter == null)
                throw new IllegalArgumentException("Source-Filter cannot be null!");
            if (destinationFilter == null)
                throw new IllegalArgumentException("Source-Filter cannot be null!");

            this.userId = userId;
            this.rulePolicy = rulePolicy;
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
        public RulePolicy getRulePolicy() {
            return rulePolicy;
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
            return ( filterMatches(sourceFilter, tlPackage.getSource()) && filterMatches(destinationFilter, tlPackage.getDestination()) ) // package in direction intended for rule
                    || ( filterMatches(destinationFilter, tlPackage.getSource()) && filterMatches(sourceFilter, tlPackage.getDestination()) ); // returning package in opposite direction
        }

        @Override
        public boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage) {
            if (!appliesTo(tlPackage))
                throw new IllegalArgumentException("Rule does not apply to package.");
            return false;
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

            return sourceFilterStr + " -> " + destinationFilterStr + " {" + " [" + protocolFilter + "]" + " userID=" + userId + " rulePolicy=" + rulePolicy + " deviceFilter=" + deviceFilter + " }";
        }
    }

    public static class FirewallTransportRule extends AbstractFirewallRule  {
        FirewallTransportRule(int userId, RulePolicy rulePolicy, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, DeviceFilter deviceFilter) {
            super(userId, rulePolicy, ProtocolFilter.TCP, sourceFilter, destinationFilter, deviceFilter);
        }
    }

 }
