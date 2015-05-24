package de.uni_kl.informatik.disco.discowall.netfilter;

public class NetfilterExceptions {

    public static abstract class NetfilterException extends Exception {
        public NetfilterException(String message) {
            super(message);
        }

        public NetfilterException(String message, Exception cause) {
            super(message, cause);
        }
    }

    public static class NetfilterBridgeDeploymentException extends NetfilterException {
        public NetfilterBridgeDeploymentException(String message, Exception cause) {
            super(message, cause);
        }
    }

    public static class NetfilterBridgeCommunicationException extends NetfilterException {
        public NetfilterBridgeCommunicationException(String message, Exception cause) {
            super(message, cause);
        }
    }
}
