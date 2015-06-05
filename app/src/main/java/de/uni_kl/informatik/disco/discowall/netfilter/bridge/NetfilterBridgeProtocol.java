package de.uni_kl.informatik.disco.discowall.netfilter.bridge;

public class NetfilterBridgeProtocol {
    public static class Comment {
        public static final String MSG_PREFIX = "#COMMENT#";
    }

    public static class QueryPackageAction {
        public static final String MSG_PREFIX = "#Packet.QueryAction#";

        public static class IP {
            public static final String SRC_PREFIX = "#ip.src=";
            public static final String SRC_SUFFIX = "#";
            public static final String DST_PREFIX = "#ip.dst=";
            public static final String DST_SUFFIX = "#";
            public static final String PROTOCOL_TYPE_TCP = "#protocol=tcp#";
            public static final String PROTOCOL_TYPE_UDP = "#protocol=udp#";

            public static class TCP {
                public static final String SRC_PORT_PREFIX = "#tcp.src.port=";
                public static final String SRC_PORT_SUFFIX = "#";
                public static final String DST_PORT_PREFIX = "#tcp.dst.port=";
                public static final String DST_PORT_SUFFIX = "#";
            }

            public static class UDP {
                public static final String SRC_PORT_PREFIX = "#udp.src.port=";
                public static final String SRC_PORT_SUFFIX = "#";
                public static final String DST_PORT_PREFIX = "#udp.dst.port=";
                public static final String DST_PORT_SUFFIX = "#";
            }
        }
    }
}