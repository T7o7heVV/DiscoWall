package de.uni_kl.informatik.disco.discowall.netfilter.bridge;

import android.util.Log;

import de.uni_kl.informatik.disco.discowall.netfilter.IptableConstants;
import de.uni_kl.informatik.disco.discowall.netfilter.IptablesControl;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class NetfilterBridgeIptablesHandler {
    private static final String LOG_TAG = NetfilterBridgeIptablesHandler.class.getSimpleName();
    private final int bridgeCommunicationPort;

    private static final String CHAIN_FIREWALL_MAIN = "discowall";
    private static final String RULE_JUMP_TO_FIREWALL_CHAIN = "-p tcp -j " + CHAIN_FIREWALL_MAIN;
    private static final String RULE_JUMP_TO_NFQUEUE = "-j NFQUEUE --queue-num 0 --queue-bypass"; // '--queue-bypass' will allow all packages, when no application is bound to the --queue-num 0
    private final String RULE_BRIDGE_COM_EXCEPTION_SERVER;
    private final String RULE_BRIDGE_COM_EXCEPTION_CLIENT;

    public NetfilterBridgeIptablesHandler(int bridgeCommunicationPort) {
        this.bridgeCommunicationPort = bridgeCommunicationPort;

        RULE_BRIDGE_COM_EXCEPTION_CLIENT = "-p tcp -s localhost -d localhost --destination-port " + bridgeCommunicationPort + " -j ACCEPT";
        RULE_BRIDGE_COM_EXCEPTION_SERVER = "-p tcp -s localhost -d localhost --source-port " + bridgeCommunicationPort + " -j ACCEPT";
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
    public void rulesEnableAll() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        Log.d(LOG_TAG, "Relevant IPTABLE chains BEFORE adding rules:\n" + getRelevantIptableChainRules(false));

        // To make sure the rule-order is correct: Remove all rules first
        rulesDisableAll(false);


        // Create iptables chain "discowall" which is the main-chain for the firewall
        IptablesControl.chainAdd(CHAIN_FIREWALL_MAIN);

        // rule: forward all TCP to firewall chain
        IptablesControl.ruleAdd(IptableConstants.Chains.INPUT, RULE_JUMP_TO_FIREWALL_CHAIN);
        IptablesControl.ruleAdd(IptableConstants.Chains.OUTPUT, RULE_JUMP_TO_FIREWALL_CHAIN);

        // rule: exceptions for netfilter-bridge
        IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, RULE_BRIDGE_COM_EXCEPTION_CLIENT); // client
        IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, RULE_BRIDGE_COM_EXCEPTION_SERVER); // server

        // rule: forward all packages
        IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, RULE_JUMP_TO_NFQUEUE);


//        IptablesControl.ruleInsert(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE);
//
//        // INPUT chain:
//        IptablesControl.ruleInsert(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE);
//        IptablesControl.ruleInsert(IptableConstants.Chains.INPUT, IPTABLES_FIREWALL_BRIDGE_COM_EXCEPTION_RULE_CLIENT);
//        IptablesControl.ruleInsert(IptableConstants.Chains.INPUT, IPTABLES_FIREWALL_BRIDGE_COM_EXCEPTION_RULE_SERVER);
//
//        // OUTPUT chain:
//        IptablesControl.ruleInsert(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
//        IptablesControl.ruleInsert(IptableConstants.Chains.OUTPUT, IPTABLES_FIREWALL_BRIDGE_COM_EXCEPTION_RULE_CLIENT);
//        IptablesControl.ruleInsert(IptableConstants.Chains.OUTPUT, IPTABLES_FIREWALL_BRIDGE_COM_EXCEPTION_RULE_SERVER);

        Log.d(LOG_TAG, "Relevant IPTABLE chains AFTER adding rules:\n" + getRelevantIptableChainRules(true));
    }

    public void rulesDisableAll(boolean logChainStatesBeforeAndAfter) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        // If the firewall-chain does not exist, it implies that no references (i.e. --jump rules) exist either.
        // ==> Nothing there, nothing to do,
        if (!IptablesControl.chainExists(CHAIN_FIREWALL_MAIN)) {
            Log.d(LOG_TAG, "No firewall-rules present. Nothing to do.");
            return;
        }


        if (logChainStatesBeforeAndAfter)
            Log.d(LOG_TAG, "Relevant IPTABLE chains BEFORE removing rules:\n" + getRelevantIptableChainRules(true));

        // 1. First all references to the chain "discowall" have to be removed
        // remove rules: forward all TCP to firewall chain
        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.INPUT, RULE_JUMP_TO_FIREWALL_CHAIN);
        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.OUTPUT, RULE_JUMP_TO_FIREWALL_CHAIN);

        // 2. Then the chain itself can be cleared from all contained rules
        // remove all rules from "discowall" chain
        IptablesControl.rulesDeleteAll(CHAIN_FIREWALL_MAIN);

        // 3. Last the chain itself can be removed
        // remove iptables chain "discowall" which is the main-chain for the firewall, if it exists
        IptablesControl.chainRemove(CHAIN_FIREWALL_MAIN);

//        // INPUT chain:
//        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE);
//        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.INPUT, IPTABLES_FIREWALL_BRIDGE_COM_EXCEPTION_RULE_CLIENT);
//        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.INPUT, IPTABLES_FIREWALL_BRIDGE_COM_EXCEPTION_RULE_SERVER);
//
//        // OUTPUT chain:
//        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
//        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.OUTPUT, IPTABLES_FIREWALL_BRIDGE_COM_EXCEPTION_RULE_CLIENT);
//        IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.OUTPUT, IPTABLES_FIREWALL_BRIDGE_COM_EXCEPTION_RULE_SERVER);

        if (logChainStatesBeforeAndAfter)
            Log.d(LOG_TAG, "Relevant IPTABLE chains AFTER removing rules:\n" + getRelevantIptableChainRules(false));
    }

    private boolean rulesAreEnabled() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return IptablesControl.ruleExists(IptableConstants.Chains.INPUT, RULE_JUMP_TO_FIREWALL_CHAIN)
                || IptablesControl.ruleExists(IptableConstants.Chains.OUTPUT, RULE_JUMP_TO_FIREWALL_CHAIN);
    }

    private String getRelevantIptableChainRules(boolean includeDiscowallChain) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        String infos = IptablesControl.getRuleInfoText(IptableConstants.Chains.INPUT, true, true)
                + "\n"
                + IptablesControl.getRuleInfoText(IptableConstants.Chains.OUTPUT, true, true);

        if (includeDiscowallChain)
            infos += "\n" + IptablesControl.getRuleInfoText(CHAIN_FIREWALL_MAIN, true, true);

        return infos;
    }

}
