package de.uni_kl.informatik.disco.discowall.firewallService.rules;

import de.uni_kl.informatik.disco.discowall.packages.Packages;

import static de.uni_kl.informatik.disco.discowall.firewallService.rules.FirewallRules.*;

public class DynamicFirewallRules {
//    private static abstract class DynamicTransportRule extends FirewallTransportRule implements IDynamicFirewallTransportRule {
//        private boolean enabled = true;
//
//        protected DynamicTransportRule(RuleAction ruleAction, Packages.TransportProtocol protocol, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter) {
//            super(ruleAction, FirewallRuleKind.DYNAMIC, protocol, sourceFilter, destinationFilter);
//        }
//
//        @Override
//        public void setRuleEnabled(boolean enabled) throws Exception {
//            this.enabled = enabled;
//        }
//
//        @Override
//        public boolean isRuleEnabled() throws Exception {
//            return enabled;
//        }
//    }
//
//    static class DynamicPermanentRule extends DynamicTransportRule {
//        protected DynamicPermanentRule(RuleAction ruleAction, Packages.TransportProtocol protocol, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter) {
//            super(ruleAction, protocol, sourceFilter, destinationFilter);
//        }
//
//        @Override
//        public boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage) {
//            return getRuleAction() == RuleAction.ACCEPT;
//        }
//    }
//
//    static abstract class DynamicTemporarayRule extends DynamicTransportRule {
//        protected DynamicTemporarayRule(RuleAction ruleAction, Packages.TransportProtocol protocol, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter) {
//            super(ruleAction, protocol, sourceFilter, destinationFilter);
//        }
//    }

}
