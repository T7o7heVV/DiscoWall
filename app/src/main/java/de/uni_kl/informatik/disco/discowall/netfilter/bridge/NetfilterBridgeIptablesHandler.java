package de.uni_kl.informatik.disco.discowall.netfilter.bridge;

import android.util.Log;

import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.DiscoWallConstants;
import de.uni_kl.informatik.disco.discowall.netfilter.iptables.IptableConstants;
import de.uni_kl.informatik.disco.discowall.netfilter.iptables.IptablesControl;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class NetfilterBridgeIptablesHandler {
    public static enum PackageHandlingMode { ACCEPT_PACKAGE, REJECT_PACKAGE, INTERACTIVE }

    private static final String LOG_TAG = NetfilterBridgeIptablesHandler.class.getSimpleName();
    private final int bridgeCommunicationPort;

    // chains
    private static final String CHAIN_FIREWALL_MAIN = "discowall";
    private static final String CHAIN_FIREWALL_MAIN_PREFILTER = "discowall-prefilter";
    public static final String CHAIN_FIREWALL_INTERFACE_3G = "discowall-if-3g";
    public static final String CHAIN_FIREWALL_INTERFACE_WIFI = "discowall-if-wifi";
    public static final String CHAIN_FIREWALL_ACTION_ACCEPT = "discowall-action-accept";
    public static final String CHAIN_FIREWALL_ACTION_REJECT = "discowall-action-reject";
    public static final String CHAIN_FIREWALL_ACTION_INTERACTIVE = "discowall-interactive";

    // rules
    private static final String RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN = "-p tcp -j " + CHAIN_FIREWALL_MAIN_PREFILTER;
    private static final String RULE_UDP_JUMP_TO_FIREWALL_PREFILTER_CHAIN = "-p udp -j " + CHAIN_FIREWALL_MAIN_PREFILTER;
    private static final String RULE_JUMP_TO_NFQUEUE = "-j NFQUEUE --queue-num 0 --queue-bypass"; // '--queue-bypass' will allow all packages, when no application is bound to the --queue-num 0
    private static final String RULE_JUMP_TO_FIREWALL_ACCEPTED = "-j " + CHAIN_FIREWALL_ACTION_ACCEPT;
    private static final String RULE_JUMP_TO_FIREWALL_INTERACTIVE = "-j " + CHAIN_FIREWALL_ACTION_INTERACTIVE;
    private static final String RULE_JUMP_TO_FIREWALL_REJECTED = "-j " + CHAIN_FIREWALL_ACTION_REJECT;
    private final String RULE_BRIDGE_COM_EXCEPTION_SERVER;
    private final String RULE_BRIDGE_COM_EXCEPTION_CLIENT;

    // interfaces
    private static final String[] DEVICES_3G = { "rmnet+", "pdp+", "ppp+", "uwbr+", "wimax+", "vsnet+", "ccmni+", "usb+" };
    private static final String[] DEVICES_WIFI = { "tiwlan+", "wlan+", "eth+", "ra+" };

    /**
     * The number which is being added to the mark in order to encode the user-id of the process which created the package.
     * Therefore the mark will have the value pid+PACKAGE_UID_MARK_OFFSET.
     * <p></p>
     * This offset is required, so that the user-id "0" (root) can be encoded, as the mark "0" cannot be assigned.
     */
    public static final int PACKAGE_UID_MARK_OFFSET = 1000;

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
        Log.d(LOG_TAG, "iptable chains BEFORE adding rules:\n" + IptablesControl.getRuleInfoText(true, true));

        // To make sure the rule-order is correct: Remove all rules first
        Log.i(LOG_TAG, "removing old iptable entries (if any)");
        rulesDisableAll(false);

        Log.i(LOG_TAG, "creating iptable chains");

        // Create iptable chaines for discowall:
        IptablesControl.chainAdd(CHAIN_FIREWALL_MAIN);
        IptablesControl.chainAdd(CHAIN_FIREWALL_MAIN_PREFILTER);
        IptablesControl.chainAdd(CHAIN_FIREWALL_INTERFACE_3G);
        IptablesControl.chainAdd(CHAIN_FIREWALL_INTERFACE_WIFI);
        IptablesControl.chainAdd(CHAIN_FIREWALL_ACTION_ACCEPT);
        IptablesControl.chainAdd(CHAIN_FIREWALL_ACTION_REJECT);
        IptablesControl.chainAdd(CHAIN_FIREWALL_ACTION_INTERACTIVE);

        Log.i(LOG_TAG, "adding iptable rules");

        // chain: INPUT, OUTPUT
        // rule: forward all TCP packages to firewall chain
        IptablesControl.ruleAdd(IptableConstants.Chains.INPUT, RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);
        IptablesControl.ruleAdd(IptableConstants.Chains.OUTPUT, RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);
        // NOTE: setMainChainJumpsEnabled() will add or remove those chains

        // adding those rules will add the rules for forwarding UDP packages into the firewall MAIN chain
        // chain: INPUT, OUTPUT
        // rule: forward all UDP packages to firewall chain
        IptablesControl.ruleAdd(IptableConstants.Chains.INPUT, RULE_UDP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);
        IptablesControl.ruleAdd(IptableConstants.Chains.OUTPUT, RULE_UDP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);

        // chain MAIN-PREFILTER:
        {
            // rule: exceptions for netfilter-bridge
            IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN_PREFILTER, RULE_BRIDGE_COM_EXCEPTION_CLIENT); // client
            IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN_PREFILTER, RULE_BRIDGE_COM_EXCEPTION_SERVER); // server
        }

        // chain MAIN:
        {
            // rule: forward to according interface
            {
                // interface 3G
                for(String interfaceDevice : DEVICES_3G)
                    IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, "-i " + interfaceDevice + " -j " + CHAIN_FIREWALL_INTERFACE_3G); // for incomming packets
                for(String interfaceDevice : DEVICES_3G)
                    IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, "-o " + interfaceDevice + " -j " + CHAIN_FIREWALL_INTERFACE_3G); // for outgoing packets

                // interface WIFI
                for(String interfaceDevice : DEVICES_WIFI)
                    IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, "-i "+interfaceDevice+" -j " + CHAIN_FIREWALL_INTERFACE_WIFI);  // for incomming packets
                for(String interfaceDevice : DEVICES_WIFI)
                    IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, "-o "+interfaceDevice+" -j " + CHAIN_FIREWALL_INTERFACE_WIFI);  // for outgoing packets
            }

            // Default-Action on the end of the MAIN chain will be set according to the wishes of the user
            // rule: jump to INTERACTIVE is last action ==> Everything what is not ACCEPTED or REJECTED at this point will be INTERACTIVELY handled.
            IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, RULE_JUMP_TO_FIREWALL_INTERACTIVE); // setDefaultPackageHandlingMode() does the same thing - setting the rules directly is more effecient, as it does not perform cleanup first.
//            setDefaultPackageHandlingMode(PackageHandlingMode.INTERACTIVE);
        }

        // chain ACCEPTED:
        // rule: jumpt to accept
        IptablesControl.ruleAdd(CHAIN_FIREWALL_ACTION_ACCEPT, "-j ACCEPT");

        // chain REJECTED:
        // rule: reject with specific package
        IptablesControl.ruleAdd(CHAIN_FIREWALL_ACTION_REJECT, "-j REJECT --reject-with icmp-port-unreachable"); // alternatively: "--reject-with icmp-host-unreachable"

        // chain INTERACTIVE:
        // rule: jump to NFQUEUE and handle package interactively
        IptablesControl.ruleAdd(CHAIN_FIREWALL_ACTION_INTERACTIVE, RULE_JUMP_TO_NFQUEUE);

//        // chain Interface-WIFI:
//        // rule: Mark wifi-packages with wifi-mark
//        IptablesControl.ruleAdd(CHAIN_FIREWALL_INTERFACE_WIFI, "-j MARK --set-mark " + DiscoWallConstants.Iptables.markPackageInterfaceWifi);
//
//        // chain Interface-3G:
//        // rule: Mark umts-packages with 3G-mark
//        IptablesControl.ruleAdd(CHAIN_FIREWALL_INTERFACE_3G, "-j MARK --set-mark " + DiscoWallConstants.Iptables.markPackageInterfaceUmts);

        Log.d(LOG_TAG, "iptable chains AFTER adding rules:\n" + IptablesControl.getRuleInfoText(true, true));

        Log.i(LOG_TAG, "iptable setup completed.");
    }

    public void rulesDisableAll(boolean logChainStatesBeforeAndAfter) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        // If a iptable-chain does not exist, it implies that no references (i.e. --jump rules) exist either.

        if (logChainStatesBeforeAndAfter)
            Log.d(LOG_TAG, "iptable chains BEFORE removing rules:\n" + IptablesControl.getRuleInfoText(true, true));

        /*
        *  Note:
        *  + MAIN = CHAIN_FIREWALL_MAIN
        *  + PREFILTER = CHAIN_FIREWALL_MAIN_PREFILTER
        *       - used for forwarding ONLY the desired app-packages into the discowall firewall
        *       - every app (via its UID) which is NOT listed here, will return into the INPUT/OUTPUT chain.
        *  + 3G = CHAIN_FIREWALL_INTERFACE_3G
        *  + WIFI = CHAIN_FIREWALL_INTERFACE_WIFI
        *  + ACCEPT= CHAIN_FIREWALL_ACTION_ACCEPT
        *  + REJECT = CHAIN_FIREWALL_ACTION_REJECT
        *  + INTERACTIVE = CHAIN_FIREWALL_ACTION_INTERACTIVE
        *
        *  Dependencies are as follows:
        *  + INPUT -> PREFILTER
        *  + OUTPUT -> PREFILTER
        *  + PREFILTER -> MAIN
        *  + MAIN -> 3G, WIFI, ACCEPT, REJECT, INTERACTIVE
        *  + INTERACTIVE -> NFQUEUE
        *
        *  The rules have to be deleted from the leafs up to the root of the dependency-tree.
        *  ==> Start with INPUT/OUTPUT chain, then MAIN, then 3G & WIFI, then ACCEPTED & REJECTED
        * */

        // Removing PREFILTER chain:
        if (IptablesControl.chainExists(CHAIN_FIREWALL_MAIN_PREFILTER)) {
            // 1. First all references to the chain "discowall" have to be removed
            // remove rules: forward all TCP to prefilter chain
            IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.INPUT, RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);
            IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.OUTPUT, RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);

            // remove rules: forward all UDP to prefilter chain
            IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.INPUT, RULE_UDP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);
            IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.OUTPUT, RULE_UDP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);

            // Clear & Delete chain itself
            IptablesControl.rulesDeleteAll(CHAIN_FIREWALL_MAIN_PREFILTER);
            IptablesControl.chainRemove(CHAIN_FIREWALL_MAIN_PREFILTER);
        }

        // Removing MAIN chain:
        safelyRemoveChain(CHAIN_FIREWALL_MAIN);

        // Removing 3G chain:
        safelyRemoveChain(CHAIN_FIREWALL_INTERFACE_3G);

        // Removing WIFI chain:
        safelyRemoveChain(CHAIN_FIREWALL_INTERFACE_WIFI);

        // Removing ACCEPT chain:
        safelyRemoveChain(CHAIN_FIREWALL_ACTION_ACCEPT);

        // Removing REJECT chain:
        safelyRemoveChain(CHAIN_FIREWALL_ACTION_REJECT);

        // Removing INTERACTIVE chain:
        safelyRemoveChain(CHAIN_FIREWALL_ACTION_INTERACTIVE);


        if (logChainStatesBeforeAndAfter)
            Log.d(LOG_TAG, "iptable chains AFTER removing rules:\n" + IptablesControl.getRuleInfoText(true, true));
    }

    public boolean isMainChainJumpsEnabled() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        return IptablesControl.ruleExists(IptableConstants.Chains.INPUT, RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN)
                && IptablesControl.ruleExists(IptableConstants.Chains.OUTPUT, RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);
    }

    /**
     * Adds/Removes the rules forwarding packages to the firewall main-chain. Can be used "pause" the firewall.
     * @param enableJumpsToMainChain
     * @throws ShellExecuteExceptions.CallException
     * @throws ShellExecuteExceptions.ReturnValueException
     */
    public void setMainChainJumpsEnabled(boolean enableJumpsToMainChain) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        if (enableJumpsToMainChain) {
            IptablesControl.ruleAddIfMissing(IptableConstants.Chains.INPUT, RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);
            IptablesControl.ruleAddIfMissing(IptableConstants.Chains.INPUT, RULE_UDP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);

            IptablesControl.ruleAddIfMissing(IptableConstants.Chains.OUTPUT, RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);
            IptablesControl.ruleAddIfMissing(IptableConstants.Chains.OUTPUT, RULE_UDP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);
        } else {
            IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.INPUT, RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);
            IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.INPUT, RULE_UDP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);

            IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.OUTPUT, RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);
            IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.OUTPUT, RULE_UDP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);
        }
    }

    private void safelyRemoveChain(String chain) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        if (IptablesControl.chainExists(chain)) {
            // 1. First all references to the chain
            IptablesControl.rulesDeleteAll(chain);
            // 2. the chain itself can be removed
            IptablesControl.chainRemove(chain);
        }
    }

    private boolean rulesAreEnabled() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return IptablesControl.ruleExists(IptableConstants.Chains.INPUT, RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN)
                || IptablesControl.ruleExists(IptableConstants.Chains.OUTPUT, RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN);
    }

    private String[] getFirewallForwardingRulesForUser(int uid) {
        return new String[] {
                "-m owner --uid-owner "+uid+" -j MARK --set-mark " + (uid + PACKAGE_UID_MARK_OFFSET), // rule which encodes the user-id as package-mark
                "-m owner --uid-owner "+uid+" -j " + CHAIN_FIREWALL_MAIN // rule which forwards package to firewall main-chain
        };
    }

    public boolean isUserPackagesForwardedToFirewall(int uid) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        int forwardedCount = 0;
        int notForwardedCount = 0;

        for(String rule : getFirewallForwardingRulesForUser(uid)) {
            if (IptablesControl.ruleExists(CHAIN_FIREWALL_MAIN_PREFILTER, rule))
                forwardedCount++;
            else
                notForwardedCount++;
        }

        // Inconsistency in case only part of the rules are set
        if (forwardedCount > 0 && notForwardedCount > 0) {
            Log.e(LOG_TAG, "Inconsistent forwarding rule for user-packages found! Missing rules will be added.");
            setUserPackagesForwardToFirewall(uid, true);
            return true;
        }

        return (forwardedCount > 0);
    }

    public void setUserPackagesForwardToFirewall(int uid, boolean forward) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        if (forward) {
            for(String rule : getFirewallForwardingRulesForUser(uid))
                IptablesControl.ruleAddIfMissing(CHAIN_FIREWALL_MAIN_PREFILTER, rule);

//        IptablesControl.ruleAddIfMissing(CHAIN_FIREWALL_MAIN_PREFILTER, "-m owner --uid-owner "+uid+" -j MARK --set-mark" + uid);
//        IptablesControl.ruleAddIfMissing(CHAIN_FIREWALL_MAIN_PREFILTER, "-m owner --uid-owner "+uid+" -j " + CHAIN_FIREWALL_MAIN);
        } else {
            for(String rule : getFirewallForwardingRulesForUser(uid))
                IptablesControl.ruleDeleteIgnoreIfMissing(CHAIN_FIREWALL_MAIN_PREFILTER, rule);
        }
    }

    public PackageHandlingMode getDefaultPackageHandlingMode() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        if (IptablesControl.ruleExists(CHAIN_FIREWALL_MAIN, RULE_JUMP_TO_FIREWALL_ACCEPTED))
            return PackageHandlingMode.ACCEPT_PACKAGE;

        if (IptablesControl.ruleExists(CHAIN_FIREWALL_MAIN, RULE_JUMP_TO_FIREWALL_REJECTED))
            return PackageHandlingMode.REJECT_PACKAGE;

        if (IptablesControl.ruleExists(CHAIN_FIREWALL_MAIN, RULE_JUMP_TO_FIREWALL_INTERACTIVE))
            return PackageHandlingMode.INTERACTIVE;

        // This case cannot happen, as long as the iptables have not been edited from the outside
        return null;
    }

    public void setDefaultPackageHandlingMode(PackageHandlingMode mode) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        // Delete rule for current behavior
        IptablesControl.ruleDeleteIgnoreIfMissing(CHAIN_FIREWALL_MAIN, RULE_JUMP_TO_FIREWALL_ACCEPTED);
        IptablesControl.ruleDeleteIgnoreIfMissing(CHAIN_FIREWALL_MAIN, RULE_JUMP_TO_FIREWALL_REJECTED);
        IptablesControl.ruleDeleteIgnoreIfMissing(CHAIN_FIREWALL_MAIN, RULE_JUMP_TO_FIREWALL_INTERACTIVE);

        // set new behavior
        switch (mode) {
            case ACCEPT_PACKAGE:
                IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, RULE_JUMP_TO_FIREWALL_ACCEPTED);
                return;
            case REJECT_PACKAGE:
                IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, RULE_JUMP_TO_FIREWALL_REJECTED);
                return;
            case INTERACTIVE:
                IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN, RULE_JUMP_TO_FIREWALL_INTERACTIVE);
                return;
        }
    }

}
