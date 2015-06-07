package de.uni_kl.informatik.disco.discowall.firewallService;

public final class FirewallExceptions {
    private FirewallExceptions() {};

    public static class FirewallException extends Exception {
        public FirewallException(String message) {
            super(message);
        }
    }

    /**
     * Is being created when the firewall is in a state where the desired operation cannot be performed.
     * <p>
     * <b>States: </b> RUNNING / PAUSED / STOPPED
     */
    public static class FirewallInvalidStateException extends Exception {
        private final Firewall.FirewallState state, requiredState;

        public Firewall.FirewallState getState() { return state; }
        public Firewall.FirewallState getRequiredState() { return requiredState; }

        public FirewallInvalidStateException(String message, Firewall.FirewallState state) {
            super(message);
            this.state = state;
            this.requiredState = null;
        }

        public FirewallInvalidStateException(String message, Firewall.FirewallState currentState, Firewall.FirewallState requiredState) {
            super(message);
            this.state = currentState;
            this.requiredState = requiredState;
        }
    }

}
