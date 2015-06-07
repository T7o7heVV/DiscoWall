package de.uni_kl.informatik.disco.discowall.netfilter.bridge;

import android.util.Log;

import de.uni_kl.informatik.disco.discowall.netfilter.IptableConstants;
import de.uni_kl.informatik.disco.discowall.netfilter.IptablesControl;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class NetfilterBridgeIptablesHandler {
    private static final String LOG_TAG = NetfilterBridgeIptablesHandler.class.getSimpleName();
    private final int bridgeCommunicationPort;

    // chains
    private static final String CHAIN_FIREWALL_MAIN = "discowall";
    private static final String CHAIN_FIREWALL_INTERFACE_3G = "discowall-3g";
    private static final String CHAIN_FIREWALL_INTERFACE_WIFI = "discowall-wifi";
    private static final String CHAIN_FIREWALL_ACTION_ACCEPTED = "discowall-accepted";
    private static final String CHAIN_FIREWALL_ACTION_REJECTED = "discowall-rejected";

    // rules
    private static final String RULE_JUMP_TO_FIREWALL_CHAIN = "-p tcp -j " + CHAIN_FIREWALL_MAIN;
    private static final String RULE_JUMP_TO_NFQUEUE = "-j NFQUEUE --queue-num 0 --queue-bypass"; // '--queue-bypass' will allow all packages, when no application is bound to the --queue-num 0
    private static final String RULE_JUMP_TO_FIREWALL_ACCEPTED = "-j " + CHAIN_FIREWALL_ACTION_ACCEPTED;
    private final String RULE_BRIDGE_COM_EXCEPTION_SERVER;
    private final String RULE_BRIDGE_COM_EXCEPTION_CLIENT;

    // interfaces
    private static final String[] DEVICES_3G = { "rmnet+", "pdp+", "ppp+", "uwbr+", "wimax+", "vsnet+", "ccmni+", "usb+" };
    private static final String[] DEVICES_WIFI = { "tiwlan+", "wlan+", "eth+", "ra+" };

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
        Log.d(LOG_TAG, "Relevant IPTABLE chains BEFORE adding rules:\n" + IptablesControl.getRuleInfoText(true, true));

        // To make sure the rule-order is correct: Remove all rules first
        rulesDisableAll(false);


        // Create iptables chain "discowall" which is the main-chain for the firewall
        IptablesControl.chainAdd(CHAIN_FIREWALL_MAIN);
        IptablesControl.chainAdd(CHAIN_FIREWALL_INTERFACE_3G);
        IptablesControl.chainAdd(CHAIN_FIREWALL_INTERFACE_WIFI);
        IptablesControl.chainAdd(CHAIN_FIREWALL_ACTION_ACCEPTED);
        IptablesControl.chainAdd(CHAIN_FIREWALL_ACTION_REJECTED);

        // chain: INPUT, OUTPUT
        // rule: forward all TCP to firewall chain
        IptablesControl.ruleAdd(IptableConstants.Chains.INPUT, RULE_JUMP_TO_FIREWALL_CHAIN);
        IptablesControl.ruleAdd(IptableConstants.Chains.OUTPUT, RULE_JUMP_TO_FIREWALL_CHAIN);

        // chain MAIN:
        {
            // rule: exceptions for netfilter-bridge
            IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, RULE_BRIDGE_COM_EXCEPTION_CLIENT); // client
            IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, RULE_BRIDGE_COM_EXCEPTION_SERVER); // server

            // rule: filter by interface (3g/WLAN) and jump to according chain
            for(String interfaceDevice : DEVICES_3G) // forward from all 3G-interfaces to 3G-chain
                IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, "-i "+interfaceDevice+" -j " + CHAIN_FIREWALL_INTERFACE_3G);
            for(String interfaceDevice : DEVICES_WIFI) // forward from all wifi-interfaces to wifi-chain
                IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, "-i "+interfaceDevice+" -j " + CHAIN_FIREWALL_INTERFACE_WIFI);

            // rule: jump to NFQUEUE is last action ==> Everything what is not ACCEPTED or REJECTED at this point will become INTERACTIVELY handled.
            IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, RULE_JUMP_TO_NFQUEUE);

            // rule: jump to ACCEPTED
            IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, RULE_JUMP_TO_FIREWALL_ACCEPTED);
        }

        // chain ACCEPTED:
        // rule: jumpt to accept
        IptablesControl.ruleAdd(CHAIN_FIREWALL_ACTION_ACCEPTED, "-j ACCEPT");

        // chain REJECTED:
        // rule: reject with specific package
        IptablesControl.ruleAdd(CHAIN_FIREWALL_ACTION_REJECTED, "-j REJECT --reject-with icmp-host-unreachable"); // alternatively: "--reject-with icmp-port-unreachable"

        Log.d(LOG_TAG, "Relevant IPTABLE chains AFTER adding rules:\n" + IptablesControl.getRuleInfoText(true, true));
    }

    public void rulesDisableAll(boolean logChainStatesBeforeAndAfter) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        // If a iptable-chain does not exist, it implies that no references (i.e. --jump rules) exist either.

        if (logChainStatesBeforeAndAfter)
            Log.d(LOG_TAG, "Relevant IPTABLE chains BEFORE removing rules:\n" + IptablesControl.getRuleInfoText(true, true));

        /*
        *  Note:
        *  + MAIN = CHAIN_FIREWALL_MAIN
        *  + 3G = CHAIN_FIREWALL_INTERFACE_3G
        *  + WIFI = CHAIN_FIREWALL_INTERFACE_WIFI
        *  + ACCEPTED = CHAIN_FIREWALL_ACTION_ACCEPTED
        *  + REJECTED = CHAIN_FIREWALL_ACTION_REJECTED
        *
        *  Dependencies are as follows:
        *  + INPUT -> MAIN
        *  + OUTPUT -> MAIN
        *  + MAIN -> 3G
        *  + MAIN -> WIFI
        *  ( + MAIN -> NFQUEUE )
        *
        *  The rules have to be deleted from the leafs up to the root of the dependency-tree.
        *  ==> Start with INPUT/OUTPUT chain, then MAIN, then 3G & WIFI, then ACCEPTED & REJECTED
        * */

        // Removing MAIN chain:
        if (IptablesControl.chainExists(CHAIN_FIREWALL_MAIN)) {
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
        }

        // Removing 3G chain:
        if (IptablesControl.chainExists(CHAIN_FIREWALL_INTERFACE_3G)) {
            // 1. First all references to the chain
            IptablesControl.rulesDeleteAll(CHAIN_FIREWALL_INTERFACE_3G);
            // 2. the chain itself can be removed
            IptablesControl.chainRemove(CHAIN_FIREWALL_INTERFACE_3G);
        }

        // Removing WIFI chain:
        if (IptablesControl.chainExists(CHAIN_FIREWALL_INTERFACE_WIFI)) {
            // 1. First all references to the chain
            IptablesControl.rulesDeleteAll(CHAIN_FIREWALL_INTERFACE_WIFI);
            // 2. the chain itself can be removed
            IptablesControl.chainRemove(CHAIN_FIREWALL_INTERFACE_WIFI);
        }

        // Removing ACCEPT chain:
        if (IptablesControl.chainExists(CHAIN_FIREWALL_ACTION_ACCEPTED)) {
            // 1. First all references to the chain
            IptablesControl.rulesDeleteAll(CHAIN_FIREWALL_ACTION_ACCEPTED);
            // 2. the chain itself can be removed
            IptablesControl.chainRemove(CHAIN_FIREWALL_ACTION_ACCEPTED);
        }

        // Removing REJECTED chain:
        if (IptablesControl.chainExists(CHAIN_FIREWALL_ACTION_REJECTED)) {
            // 1. First all references to the chain
            IptablesControl.rulesDeleteAll(CHAIN_FIREWALL_ACTION_REJECTED);
            // 2. the chain itself can be removed
            IptablesControl.chainRemove(CHAIN_FIREWALL_ACTION_REJECTED);
        }

        if (logChainStatesBeforeAndAfter)
            Log.d(LOG_TAG, "iptable chains AFTER removing rules:\n" + IptablesControl.getRuleInfoText(true, true));
    }

    private boolean rulesAreEnabled() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return IptablesControl.ruleExists(IptableConstants.Chains.INPUT, RULE_JUMP_TO_FIREWALL_CHAIN)
                || IptablesControl.ruleExists(IptableConstants.Chains.OUTPUT, RULE_JUMP_TO_FIREWALL_CHAIN);
    }

}
