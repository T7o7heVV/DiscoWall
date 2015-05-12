package de.uni_kl.informatik.disco.discowall.netfilter;

import java.io.IOException;

import de.uni_kl.informatik.disco.discowall.utils.DroidWallAssets;

public class NfqueueControl {
    private static final String LOG_TAG = "NfqueueControl";
    private static final String IPTABLES_NFQUEUE_RULE = "-p tcp -j NFQUEUE --queue-num 0";

    public NfqueueControl() {

    }

    private void executeNetfilterBridge() {
        // TODO
    }

    private void terminateNetfilterBridge() {
        // TODO
    }

    private class NetfilterBridgeBinaryHandler {
        public boolean isBinaryDeployed() {
            return false; // TODO
        }

        public void deployBinary() {
            // TODO
        }

        public String getBinaryPath() {
            return ""; // TODO
        }
    }

    public void rulesEnableAll() throws InterruptedException, IptablesControl.IptablesException, IOException {
        ruleAddIfMissing(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE);
        ruleAddIfMissing(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
    }

    public void rulesDisableAll() throws InterruptedException, IptablesControl.IptablesException, IOException {
        ruleDeleteIfExisting(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE);
        ruleDeleteIfExisting(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
    }

    private void ruleAddIfMissing(String chain, String rule) throws InterruptedException, IptablesControl.IptablesException, IOException {
        if (!IptablesControl.ruleExists(chain, rule))
            IptablesControl.ruleAdd(chain, rule);
    }

    private void ruleDeleteIfExisting(String chain, String rule) throws InterruptedException, IptablesControl.IptablesException, IOException {
        if (IptablesControl.ruleExists(chain, rule))
            IptablesControl.ruleDelete(chain, rule);
    }

    public boolean rulesAreEnabled() throws InterruptedException, IptablesControl.IptablesException, IOException {
        return IptablesControl.ruleExists(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE)
                || IptablesControl.ruleExists(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
    }

    public boolean CheckKernelNetfilterSupport() {
        return false; //TODO
    }
}
