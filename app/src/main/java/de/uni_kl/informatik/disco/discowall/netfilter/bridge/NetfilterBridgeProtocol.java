package de.uni_kl.informatik.disco.discowall.netfilter.bridge;

public class NetfilterBridgeProtocol {
    public static class ProtocolException extends Exception {
        public ProtocolException(String message) {
            super(message);
        }
    }

    public static class ProtocolFormatException extends ProtocolException {
        private final String receivedProtocolString;

        public String getReceivedProtocolString() {
            return receivedProtocolString;
        }

        public ProtocolFormatException(String message, String receivedProtocolString) {
            super(message);
            this.receivedProtocolString = receivedProtocolString;
        }
    }

    public static class ProtocolValueException extends ProtocolFormatException {
        private final String value;

        public String getValue() {
            return value;
        }

        public ProtocolValueException(String message, String value, String receivedProtocolString) {
            super(message, receivedProtocolString);
            this.value = value;
        }

    }

    public static class ProtocolValueTypeException extends ProtocolValueException {
        public ProtocolValueTypeException(Class expectedType, String value, String receivedProtocolString) {
            super("Expected protocol value of type '"+expectedType+"' but got string.", value, receivedProtocolString);
        }
    }

    public static class ProtocolValueMissingException extends ProtocolValueException {
        public ProtocolValueMissingException(String value, String receivedProtocolString) {
            super("Received protocol-string does not contain required value '"+value+"'.", value, receivedProtocolString);
        }
    }

    public static final String VALUE_PREFIX = "#";
    public static final String VALUE_SUFFIX = "#";
    public static final String VALUE_KEY_DELIM = "=";

    public static class Comment {
        public static final String MSG_PREFIX = "#COMMENT#";
    }

    public static class QueryPackageActionResponse {
        public static final String MSG_PREFIX = "#Packet.QueryAction.Response#";
        public static final String FLAG_ACCEPT_PACKAGE = "#ACCEPT#";
        public static final String FLAG_DROP_PACKAGE = "#DROP#";
    }

    public static class QueryPackageAction {
        public static final String MSG_PREFIX = "#Packet.QueryAction#";

        public static class IP {
            public static final String VALUE_SOURCE = "ip.src";
            public static final String VALUE_DESTINATION = "ip.dst";
            public static final String FLAG_PROTOCOL_TYPE_TCP = "protocol=tcp";
            public static final String FLAG_PROTOCOL_TYPE_UDP = "protocol=udp";

            public static class TCP {
                public static final String VALUE_SOURCE_PORT = "tcp.src.port";
                public static final String VALUE_DESTINATION_PORT = "tcp.dst.port";
                public static final String VALUE_LENGTH = "tcp.length";
                public static final String VALUE_CHECKSUM = "tcp.checksum";
                public static final String VALUE_SEQUENCE_NUMBER = "tcp.seqnr";
                public static final String VALUE_ACK_NUMBER = "tcp.acknr";
                public static final String VALUE_FLAG_URGENT = "tcp.flag.urgent";
                public static final String VALUE_FLAG_IS_ACK = "tcp.flag.ack";
                public static final String VALUE_FLAG_PUSH = "tcp.flag.push";
                public static final String VALUE_FLAG_RESET = "tcp.flag.reset";
                public static final String VALUE_FLAG_SYN = "tcp.flag.syn";
                public static final String VALUE_FLAG_FIN = "tcp.flag.fin";
            }

            public static class UDP {
                public static final String VALUE_SOURCE_PORT = "udp.src.port";
                public static final String VALUE_DESTINATION_PORT = "udp.dst.port";
                public static final String VALUE_LENGTH = "udp.length";
                public static final String VALUE_CHECKSUM = "udp.checksum";
            }
        }
    }

}