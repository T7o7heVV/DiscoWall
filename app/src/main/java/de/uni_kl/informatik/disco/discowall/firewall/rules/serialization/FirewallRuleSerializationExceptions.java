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

    public static class XmlDocumentFormatException extends RulesSerializerException {
        public XmlDocumentFormatException(String message) {
            super(message);
        }

        public XmlDocumentFormatException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class XmlTagMissingException extends XmlDocumentFormatException {
        public XmlTagMissingException(String missingTag, String parentTag) {
            super("Expected tag "+ missingTag +" not found within parent tag " + parentTag);
        }

        public XmlTagMissingException(String missingTag, String parentTag, Throwable cause) {
            super("Expected tag "+ missingTag +" not found within parent tag " + parentTag, cause);
        }
    }
}
