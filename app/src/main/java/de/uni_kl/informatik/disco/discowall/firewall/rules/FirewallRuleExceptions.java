package de.uni_kl.informatik.disco.discowall.firewall.rules;

public class FirewallRuleExceptions {
    public static class RuleException extends Exception {
        private final FirewallRules.IFirewallRule rule;

        public RuleException(FirewallRules.IFirewallRule rule, String message) {
            super(message);
            this.rule = rule;
        }

        public RuleException(FirewallRules.IFirewallRule rule, String message, Exception cause) {
            super(message, cause);
            this.rule = rule;
        }

        public FirewallRules.IFirewallRule getRule() {
            return rule;
        }
    }

    public static class DuplicateRuleException extends RuleException {
        public DuplicateRuleException(FirewallRules.IFirewallRule rule) {
            super(rule, "Rule is already added: " + rule);
        }
    }

    public static class RuleNotFoundException extends RuleException {
        public RuleNotFoundException(FirewallRules.IFirewallRule rule) {
            super(rule, "Rule is not registered for user " + rule.getUserId() + ". Rule: " + rule);
        }

        public RuleNotFoundException(FirewallRules.IFirewallRule rule, String message) {
            super(rule, message);
        }

        public RuleNotFoundException(FirewallRules.IFirewallRule rule, String message, Exception cause) {
            super(rule, message, cause);
        }
    }

    public static class InvalidRuleDefinitionException extends RuleException {
        public InvalidRuleDefinitionException(FirewallRules.IFirewallRule rule, String message) {
            super(rule, message);
        }

        public InvalidRuleDefinitionException(FirewallRules.IFirewallRule rule, String message, Exception cause) {
            super(rule, message, cause);
        }
    }
}
