package de.uni_kl.informatik.disco.discowall.firewall.rules.serialization;

class XMLConstants {
    static class IpPortPair {
        public static final String ATTR_Ip = "ip";
        public static final String ATTR_Port = "port";
    }

    static class Root {
        public static final String TAG = "Discowall";

        public static class RuledApps {
            public static final String TAG = "RuledApps";

            public static class Group {
                public static final String TAG = "Group";

                public static final String packageNamesDelim = ";";

                public static final String ATTR_PackageNamesList = "PackageNamesList";
                public static final String ATTR_IsMonitored = "IsMonitored";
                public static final String ATTR_UserID = "UserID";

                public static class FirewallRules {
                    public static final String TAG = "Rules";

                    public static class AnyRule {
//                        public static final String ATTR_UserID = "UserID"; // UserID changes when apps are deleted and re-installed, and is device-specific
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

                    public static class PolicyRule {
                        public static final String TAG = "PolicyRule";
                        public static final String ATTR_RulePolicy = "RulePolicy";
                    }

                    public static class RedirectionRule {
                        public static final String TAG = "RedirectionRule";

                        public static class RedirectionRemoteHost extends IpPortPair {
                            public static final String TAG = "RedirectionRemoteHost";
                        }
                    }
                }
            }
        }
    }
}
