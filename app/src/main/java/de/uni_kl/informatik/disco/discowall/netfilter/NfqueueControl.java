package de.uni_kl.informatik.disco.discowall.netfilter;

import android.util.Log;

import java.io.IOException;

import de.uni_kl.informatik.disco.discowall.AppManagement;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class NfqueueControl {
    private static final String LOG_TAG = "NfqueueControl";
//    private static final String IPTABLES_NFQUEUE_RULE = "-p tcp -j NFQUEUE --queue-num 0";
    private static final String IPTABLES_NFQUEUE_RULE = "-p tcp -j NFQUEUE --queue-num 0 --queue-bypass"; // '--queue-bypass' will allow all packages, when no application is bound to the --queue-num 0
    private final AppManagement appManagement;

    private final NetfilterBridgeBinaryHandler bridgeBinaryHandler;
    private NetfilterBridgeCommunicator bridgeCommunicator;
    private final int bridgeCommunicationPort;
    private final String IPTABLES_FIREWALL_BRIDGE_COM_SOURCE_EXCEPTION_RULE, IPTABLES_FIREWALL_BRIDGE_COM_DESTINATION_EXCEPTION_RULE;

    public NfqueueControl(AppManagement appManagement, int bridgeCommunicationPort) throws NetfilterExceptions.NetfilterBridgeDeploymentException, ShellExecuteExceptions.ReturnValueException, ShellExecuteExceptions.CallException {
        Log.d(LOG_TAG, "initializing NfqueueControl...");

        this.bridgeCommunicationPort = bridgeCommunicationPort;
        this.IPTABLES_FIREWALL_BRIDGE_COM_SOURCE_EXCEPTION_RULE = "-p tcp --destination-port " + bridgeCommunicationPort + " -j ACCEPT";
        this.IPTABLES_FIREWALL_BRIDGE_COM_DESTINATION_EXCEPTION_RULE = "-p tcp --source-port " + bridgeCommunicationPort + " -j ACCEPT";

        this.appManagement = appManagement;
        this.bridgeBinaryHandler = new NetfilterBridgeBinaryHandler(appManagement);

        connectToBridge();
    }

    public boolean isBridgeConnected() {
        if (bridgeCommunicator == null)
            return false;
        else
            return bridgeCommunicator.isConnected() && bridgeBinaryHandler.isProcessRunning();
    }

    private void connectToBridge() throws NetfilterExceptions.NetfilterBridgeDeploymentException, ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        Log.d(LOG_TAG, "connecting to netfilter-bridge...");
        Log.v(LOG_TAG, "netfilter bridge is deployed: " + bridgeBinaryHandler.isDeployed());

        if (!bridgeBinaryHandler.isDeployed()) {
            bridgeBinaryHandler.deploy();

            // assert deployment
            if (!bridgeBinaryHandler.isDeployed())
                Log.e(LOG_TAG, "error deploying netfilter bridge. File has NOT been deployed!");
        }

        // Creating required iptable-rules: ESPECIALLY the rule-exceptions for the bridge-android-communication via tcp
        Log.d(LOG_TAG, "adding static iptable-rules required for bridge-android communication");
        rulesEnableAll();

        Log.d(LOG_TAG, "starting netfilter bridge communicator as listening server...");
        bridgeCommunicator = new NetfilterBridgeCommunicator(bridgeCommunicationPort);
        Log.d(LOG_TAG, "listening on port: " + bridgeCommunicationPort);

        Log.d(LOG_TAG, "killing all possibly running netfilter bridge instances...");
        bridgeBinaryHandler.killAllInstances();

        // Disabled for DEBUGGING - using external instance within shell
//        Log.d(LOG_TAG, "executing netfilter bridge binary...");
//        bridgeBinaryHandler.execute(bridgeCommunicationPort);

        Log.d(LOG_TAG, "netfilter-bridge connected.");
    }

    public void disconnectBridge() throws IOException, ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        Log.d(LOG_TAG, "disconnecting netfilter-bridge-communicator");

        // Removing any IPTABLE-rules. Has to be done first, so that no packages are "stuck" within the NFQUEUE-chain.
        Log.d(LOG_TAG, "removing all static iptable-rules");
        rulesDisableAll(true);

        if (bridgeCommunicator == null) {
            Log.v(LOG_TAG, "bridge-communicator has never been connected - nothing to disconnect.");
            return;
        }

        bridgeCommunicator.disconnect();

        Log.d(LOG_TAG, "killing all possibly running netfilter bridge instances...");
        bridgeBinaryHandler.killAllInstances();
    }

    private String getRelevantIptableChains() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return IptablesControl.execute("-L "+IptableConstants.Chains.INPUT+" -n -v")
                + "\n"
                + IptablesControl.execute("-L "+IptableConstants.Chains.OUTPUT+" -n -v");
    }

    /**
     * Adds the rules required for bridge-android communication.
     * <p>
     * This includes:
     * <li>The <b>iptables-exception</b> rule for NOT BLOCKING the bridge-communication with the android app via tcp </li>
     * <li>The <b>NFQUEUE</b> rule for fetching any package and sending it to the bridge</li>
     * @throws ShellExecuteExceptions.CallException
     * @throws ShellExecuteExceptions.NonZeroReturnValueException
     */
    private void rulesEnableAll() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        Log.d(LOG_TAG, "Relevant IPTABLE chains BEFORE adding rules:\n" + getRelevantIptableChains());

        // To make sure the rule-order is correct: Remove all rules first
        rulesDisableAll(false);

        // INPUT chain:
        IptablesControl.ruleInsert(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE);
        IptablesControl.ruleInsert(IptableConstants.Chains.INPUT, IPTABLES_FIREWALL_BRIDGE_COM_SOURCE_EXCEPTION_RULE);
        IptablesControl.ruleInsert(IptableConstants.Chains.INPUT, IPTABLES_FIREWALL_BRIDGE_COM_DESTINATION_EXCEPTION_RULE);

        // OUTPUT chain:
        IptablesControl.ruleInsert(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
        IptablesControl.ruleInsert(IptableConstants.Chains.OUTPUT, IPTABLES_FIREWALL_BRIDGE_COM_SOURCE_EXCEPTION_RULE);
        IptablesControl.ruleInsert(IptableConstants.Chains.OUTPUT, IPTABLES_FIREWALL_BRIDGE_COM_DESTINATION_EXCEPTION_RULE);

        Log.d(LOG_TAG, "Relevant IPTABLE chains AFTER adding rules:\n" + getRelevantIptableChains());
    }

    private void rulesDisableAll(boolean logChainStatesBeforeAndAfter) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        if (logChainStatesBeforeAndAfter)
            Log.d(LOG_TAG, "Relevant IPTABLE chains BEFORE removing rules:\n" + getRelevantIptableChains());

        // INPUT chain:
        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE);
        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.INPUT, IPTABLES_FIREWALL_BRIDGE_COM_SOURCE_EXCEPTION_RULE);
        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.INPUT, IPTABLES_FIREWALL_BRIDGE_COM_DESTINATION_EXCEPTION_RULE);

        // OUTPUT chain:
        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.OUTPUT, IPTABLES_FIREWALL_BRIDGE_COM_SOURCE_EXCEPTION_RULE);
        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.OUTPUT, IPTABLES_FIREWALL_BRIDGE_COM_DESTINATION_EXCEPTION_RULE);

        if (logChainStatesBeforeAndAfter)
            Log.d(LOG_TAG, "Relevant IPTABLE chains AFTER removing rules:\n" + getRelevantIptableChains());
    }

    private boolean rulesAreEnabled() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return IptablesControl.ruleExists(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE)
                || IptablesControl.ruleExists(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
    }

}
