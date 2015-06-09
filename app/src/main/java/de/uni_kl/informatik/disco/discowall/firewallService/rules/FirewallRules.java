package de.uni_kl.informatik.disco.discowall.firewallService.rules;

import de.uni_kl.informatik.disco.discowall.netfilter.iptables.IptablesControl;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeIptablesHandler;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class FirewallRules {
    public static enum FirewallRuleAction {
        ACCEPT, DROP;
    }

    public static enum FirewallRuleKind {
        STATIC, INTERACTIVE;
    }

    public static enum DeviceFilter {
        WIFI, UMTS_3G;
    }

    public static enum TransportRuleProtocolKind {
        TCP, UDP;
    }

    /*************************** ARCHITECTURE ******************************************************/
    interface IFirewallRule {
        String getDescription();
        FirewallRuleAction getRuleAction();
        FirewallRuleKind getRuleKind();

        void setRuleEnabled(boolean enabled) throws Exception;
        boolean isRuleEnabled() throws Exception;
    }

    interface IFirewallTransportRule extends IFirewallRule {
        TransportRuleProtocolKind getProtocol();

        Packages.IpPortPair getSourceFilter();
        boolean hasSourceFilter();
        Packages.IpPortPair getDestinationFilter();
        boolean hasDestinationFilter();
    }

    interface IStaticFirewallTransportRule extends IFirewallTransportRule {
        int getUserIdFilter();
        boolean hasUserIdFilter();
        DeviceFilter getDeviceFilter();
    }

    interface IDynamicFirewallTransportRule extends IFirewallTransportRule {
        boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage);
    }
    /***********************************************************************************************/


    static abstract class FirewallRule implements IFirewallRule {
        private final FirewallRuleAction ruleAction;
        private final FirewallRuleKind ruleKind;

        private FirewallRule(FirewallRuleAction ruleAction, FirewallRuleKind ruleKind) {
            this.ruleAction = ruleAction;
            this.ruleKind = ruleKind;
        }

        public FirewallRuleAction getRuleAction() {
            return ruleAction;
        }

        public FirewallRuleKind getRuleKind() {
            return ruleKind;
        }

        @Override
        public String toString() {
            return getDescription();
        }

    }

    static abstract class FirewallTransportRule extends FirewallRule implements IFirewallTransportRule {
        private final Packages.IpPortPair sourceFilter, destinationFilter;

        protected FirewallTransportRule(FirewallRuleAction ruleAction, FirewallRuleKind ruleKind, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter) {
            super(ruleAction, ruleKind);
            this.sourceFilter = sourceFilter;
            this.destinationFilter = destinationFilter;
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
        public boolean hasDestinationFilter() {
            return destinationFilter != null;
        }

        @Override
        public boolean hasSourceFilter() {
            return sourceFilter != null;
        }

        @Override
        public String getDescription() {
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

            return "Packages matching connection: " + sourceFilterStr + " -> " + destinationFilterStr + " { action=" + getRuleAction() +", kind=" + getRuleKind() + " }";
        }
    }

    static abstract class DynamicTransportRule extends FirewallRule implements IDynamicFirewallTransportRule {
        private DynamicTransportRule(FirewallRuleAction ruleAction, FirewallRuleKind ruleKind) {
            super(ruleAction, ruleKind);
        }
    }
 }
