package de.uni_kl.informatik.disco.discowall.netfilter;

import android.util.Log;

import java.io.IOException;

import de.uni_kl.informatik.disco.discowall.AppManagement;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class NfqueueControl {
    private static final String LOG_TAG = "NfqueueControl";
    private static final String IPTABLES_NFQUEUE_RULE = "-p tcp -j NFQUEUE --queue-num 0";

    private final AppManagement appManagement;
    private final NetfilterBridgeBinaryHandler bridgeBinaryHandler;
    private NetfilterBridgeCommunicator bridgeCommunicator;

//    public NetfilterBridgeCommunicator getBridgeCommunicator() {
//        return bridgeCommunicator;
//    }
//
//    public NetfilterBridgeBinaryHandler getBridgeBinaryHandler() {
//        return bridgeBinaryHandler;
//    }

    public NfqueueControl(AppManagement appManagement) {
        Log.d(LOG_TAG, "initializing NfqueueControl...");

        this.appManagement = appManagement;
        this.bridgeBinaryHandler = new NetfilterBridgeBinaryHandler(appManagement);
    }

    public boolean isBridgeConnected() {
        if (bridgeCommunicator == null)
            return false;
        else
            return bridgeCommunicator.isConnected() && bridgeBinaryHandler.isProcessRunning();
    }

    public void connectToBridge(int bridgeCommunicationPort) throws NetfilterExceptions.NetfilterBridgeDeploymentException, ShellExecuteExceptions.CallException {
        Log.v(LOG_TAG, "netfilter bridge is deployed: " + bridgeBinaryHandler.isDeployed());

        if (!bridgeBinaryHandler.isDeployed()) {
            bridgeBinaryHandler.deploy();

            // assert deployment
            if (!bridgeBinaryHandler.isDeployed())
                Log.e(LOG_TAG, "error deploying netfilter bridge. File has NOT been deployed!");
        }

        Log.v(LOG_TAG, "starting netfilter bridge communicator as listening server...");
        bridgeCommunicator = new NetfilterBridgeCommunicator(bridgeCommunicationPort);
        Log.v(LOG_TAG, "listening on port: " + bridgeCommunicationPort);

        Log.v(LOG_TAG, "killing all possibly running netfilter bridge instances...");
        bridgeBinaryHandler.killAllInstances();

        Log.v(LOG_TAG, "executing netfilter bridge binary...");
        bridgeBinaryHandler.execute(bridgeCommunicationPort);

        Log.d(LOG_TAG, "nfqueueControl initialized.");
    }

    public void disconnectBridge() throws IOException, ShellExecuteExceptions.CallException {
        Log.v(LOG_TAG, "disconnecting netfilter-bridge-communicator");

        if (bridgeCommunicator == null) {
            Log.v(LOG_TAG, "bridge-communicator has never been connected - nothing to disconnect.");
            return;
        }

        bridgeCommunicator.disconnect();

        Log.v(LOG_TAG, "killing all possibly running netfilter bridge instances...");
        bridgeBinaryHandler.killAllInstances();
    }

    public void rulesEnableAll() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        ruleAddIfMissing(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE);
        ruleAddIfMissing(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
    }

    public void rulesDisableAll() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        ruleDeleteIfExisting(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE);
        ruleDeleteIfExisting(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
    }

    private void ruleAddIfMissing(String chain, String rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        if (!IptablesControl.ruleExists(chain, rule))
            IptablesControl.ruleAdd(chain, rule);
    }

    private void ruleDeleteIfExisting(String chain, String rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        if (IptablesControl.ruleExists(chain, rule))
            IptablesControl.ruleDelete(chain, rule);
    }

    public boolean rulesAreEnabled() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return IptablesControl.ruleExists(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE)
                || IptablesControl.ruleExists(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
    }

}
