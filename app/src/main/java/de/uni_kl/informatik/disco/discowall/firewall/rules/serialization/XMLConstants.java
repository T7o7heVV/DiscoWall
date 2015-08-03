package de.uni_kl.informatik.disco.discowall.firewall.rules.serialization;

class XMLConstants {
    public static class IpPortPair {
        public static final String ATTR_Ip = "ip";
        public static final String ATTR_Port = "port";
    }

    public static class Root {
        public static final String TAG = "ExportedRules";

        public static class FirewallRules {
            public static final String TAG = "Rules";

            public static class AbstractRule {
                public static final String TAG = "FirewallRule";

                public static final String ATTR_UserID = "UserID";
                public static final String ATTR_RuleKind = "RuleKind";
                public static final String ATTR_DeviceFilter = "DeviceFilter";
                public static final String ATTR_ProtocolFilter = "ProtocolFilter";
                public static final String ATTR_UUID = "UUID";

                public static class RemoteFilter extends IpPortPair {
                    public static final String TAG = "LocalFilter";
                }

                public static class LocalFilter extends IpPortPair {
                    public static final String TAG = "RemoteFilter";
                }
            }

            public static class PolicyRule extends Root.FirewallRules.AbstractRule {
                public static final String TAG = "PolicyRule";
                public static final String ATTR_RulePolicy = "RulePolicy";
            }

            public static class RedirectionRule extends Root.FirewallRules.AbstractRule {
                public static final String TAG = "RedirectionRule";

                public static class RedirectionRemoteHost extends IpPortPair {
                    public static final String TAG = "RedirectionRemoteHost";
                }
            }
        }
    }
}
