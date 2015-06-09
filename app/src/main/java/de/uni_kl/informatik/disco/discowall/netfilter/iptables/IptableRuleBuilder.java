package de.uni_kl.informatik.disco.discowall.netfilter.iptables;

public class IptableRuleBuilder {
    public static class IptableRuleRuntimeException extends RuntimeException {
        private final String rule;

        public String getRule() {
            return rule;
        }

        public IptableRuleRuntimeException(String message, String rule) {
            super(message);
            this.rule = rule;
        }
    }

    public static class IptableRuleIncompleteException extends IptableRuleRuntimeException {
        public IptableRuleIncompleteException(String rule) {
            super("Iptable rule incomplete: " + rule, rule);
        }
    }


    public static IptableRuleBuilder appendRule() {
        return new IptableRuleBuilder("-A");
    }

    public static IptableRuleBuilder deleteRule() {
        return new IptableRuleBuilder("-D");
    }

    public static IptableRuleBuilder insertRule() {
        return new IptableRuleBuilder("-I");
    }

    public static IptableRuleBuilder insertRule(int index) {
        IptableRuleBuilder builder = new IptableRuleBuilder("-I");
        builder.insertActionIndex = " " + index + " ";
        return builder;
    }

    private final String manipulationCommand;
    private String action, chain, sourceIpFilter, destinationIpFilter, sourcePortFilter, destinationPortFilter;
    private String additionalArgs = " ";
    private String insertActionIndex = "";

    private IptableRuleBuilder(String manipulationCommand) {
        this.manipulationCommand = manipulationCommand;
    }

    public IptableRuleBuilder setChain(String chain) {
        this.chain = chain;
        return this;
    }

    public IptableRuleBuilder setAction(String action) {
        this.action = action;
        return this;
    }

    public IptableRuleBuilder setSourceIP(String ip) {
        this.sourceIpFilter = " -s " + ip + " ";
        return this;
    }

    public IptableRuleBuilder setDestinationIP(String ip) {
        this.destinationIpFilter = " -d " + ip + " ";
        return this;
    }

    public IptableRuleBuilder setSourcePort(int port) {
        this.sourcePortFilter = " --source-port " + port + " ";
        return this;
    }

    public IptableRuleBuilder setDestinationPort(int port) {
        this.destinationPortFilter = " --destination-port " + port + " ";
        return this;
    }

    public IptableRuleBuilder appendAdditional(String filter) {
        additionalArgs += filter + " ";
        return this;
    }

    public IptableRuleBuilder appendProtocolFilter(String protocol) {
        additionalArgs += "-p " + protocol;
        return this;
    }

    public IptableRuleBuilder appendUserIdFilter(String uid) {
        additionalArgs += "-m owner --uid-owner " + uid + " ";
        return this;
    }

    public IptableRuleBuilder appendGroupIdFilter(String gid) {
        additionalArgs += "-m owner --gid-owner " + gid + " ";
        return this;
    }

    public IptableRuleBuilder appendProcessIdFilter(int pid) {
        additionalArgs += "-m owner --pid-owner " + pid + " ";
        return this;
    }

    private String createCommand() {
        return manipulationCommand + " " + chain + insertActionIndex + sourceIpFilter + sourcePortFilter + destinationIpFilter + destinationPortFilter + additionalArgs + action;
    }

    public String build() throws IptableRuleIncompleteException {
        if (chain == null || chain.isEmpty() || action == null || action.isEmpty())
            throw new IptableRuleIncompleteException(createCommand());

        return createCommand();
    }

}
