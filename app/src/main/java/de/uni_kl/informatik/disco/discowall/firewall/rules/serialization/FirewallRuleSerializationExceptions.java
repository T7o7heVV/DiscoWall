package de.uni_kl.informatik.disco.discowall.firewall.rules.serialization;

import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;

public class FirewallRuleSerializationExceptions {
    public static class RulesSerializerException extends Exception {
        public RulesSerializerException(String detailMessage) {
            super(detailMessage);
        }

        public RulesSerializerException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }

    public static class UnknownRuleKindException extends RulesSerializerException {
        private final FirewallRules.RuleKind ruleKind;

        public FirewallRules.RuleKind getRuleKind() {
            return ruleKind;
        }

        public UnknownRuleKindException(String message, FirewallRules.RuleKind ruleKind) {
            super(message);
            this.ruleKind = ruleKind;
        }
    }
}
