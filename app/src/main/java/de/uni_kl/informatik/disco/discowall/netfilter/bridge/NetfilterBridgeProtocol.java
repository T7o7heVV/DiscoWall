package de.uni_kl.informatik.disco.discowall.netfilter.bridge;

public class NetfilterBridgeProtocol {
//    public static class ProtocolException extends Exception {
//        public ProtocolException(String message) {
//            super(message);
//        }
//    }
//
//    public static class ProtocolFormatException extends ProtocolException {
//        private final String receivedProtocolString;
//
//        public String getReceivedProtocolString() {
//            return receivedProtocolString;
//        }
//
//        public ProtocolFormatException(String message, String receivedProtocolString) {
//            super(message);
//            this.receivedProtocolString = receivedProtocolString;
//        }
//    }

    public static class Comment {
        public static final String MSG_PREFIX = "#COMMENT#";
    }

    public static class QueryPackageActionResponse {
        public static final String MSG_PREFIX = "#Packet.QueryAction.Resonse#";
        public static final String ACCEPT_PACKAGE = "#ACCEPT#";
        public static final String DROP_PACKAGE = "#DROP#";
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