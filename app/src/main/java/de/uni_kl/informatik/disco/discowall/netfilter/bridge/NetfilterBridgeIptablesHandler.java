package de.uni_kl.informatik.disco.discowall.netfilter.bridge;

import android.util.Log;

import de.uni_kl.informatik.disco.discowall.netfilter.iptables.IptableConstants;
import de.uni_kl.informatik.disco.discowall.netfilter.iptables.IptablesControl;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

/**
 * IPTABLES Documentation for flags:
    --tcp-flags
        Gefolgt von einem optionalen '!', dann zwei Zeichenketten von Flags, erlaubt Dir, nach speziellen TCP-Flags zu filtern. Die erste Zeichenkette von Flags ist die Maske: eine Liste von Flags, die Du untersuchen willst. Die zweite Zeichenkette besagt, welche Flags gesetzt sein sollen. Zum Beispiel:

        # iptables -A INPUT --protocol tcp --tcp-flags ALL SYN,ACK -j DENY
        Dies besagt, dass alle Flags untersucht werden sollen ('ALL' ist synonym mit 'SYN, ACK, FIN, RST, URG, PSH'), dass aber nur SYN und ACK gesetzt sein sollen. Es gibt auch ein Argument 'NONE', was 'Keine Flags' bedeutet.
 */

public class NetfilterBridgeIptablesHandler {
    private static final String LOG_TAG = NetfilterBridgeIptablesHandler.class.getSimpleName();
    private final int bridgeCommunicationPort;

    // chains
    static final String CHAIN_FIREWALL_MAIN = "discowall";
    static final String CHAIN_FIREWALL_MAIN_PREFILTER = "discowall-prefilter";
    static final String CHAIN_FIREWALL_INTERFACE_3G = "discowall-if-3g";
    static final String CHAIN_FIREWALL_INTERFACE_WIFI = "discowall-if-wifi";
    static final String CHAIN_FIREWALL_ACTION_ACCEPT = "discowall-action-accept";
    static final String CHAIN_FIREWALL_ACTION_REJECT = "discowall-action-reject";
    static final String CHAIN_FIREWALL_ACTION_INTERACTIVE = "discowall-interactive";
    static final String CHAIN_FIREWALL_ACTION_REDIRECT = "discowall-redirect"; // IMPORTANT: this chain is table "nat" (i.e. "-t nat")

    static final String TABLE_NAT = "nat";

    // rules
    static final String RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN = "-p tcp -j " + CHAIN_FIREWALL_MAIN_PREFILTER;
//    static final String[] RULE_TCP_JUMP_TO_FIREWALL_PREFILTER_CHAIN = new String[] {
//            // "--tcp-flags Flag1,Flag2,...,FlagN <FlagX_only>" --- "--tcp-flags ALL SYN" will filter all flags and only accept if ONLY SYN is set. ==> Only the connection-esablishment is being filtered.
////            "-p tcp --tcp-flags SYN,RST SYN -j " + CHAIN_FIREWALL_MAIN_PREFILTER, // within SYN,RST,FIN has only(!) SYN
//            "-p tcp --tcp-flags SYN,RST,FIN SYN -j " + CHAIN_FIREWALL_MAIN_PREFILTER, // within SYN,RST,FIN has only(!) SYN
//            "-p tcp --tcp-flags SYN,RST,FIN,ACK FIN,ACK -j " + CHAIN_FIREWALL_MAIN_PREFILTER  // within SYN,RST,FIN,ACK has only(!) FIN+ACK
//    };
    static final String RULE_UDP_JUMP_TO_FIREWALL_PREFILTER_CHAIN = "-p udp -j " + CHAIN_FIREWALL_MAIN_PREFILTER;
    static final String RULE_JUMP_TO_NFQUEUE = "-j NFQUEUE --queue-num 0 --queue-bypass"; // '--queue-bypass' will allow all packages, when no application is bound to the --queue-num 0
    static final String RULE_JUMP_TO_FIREWALL_ACCEPTED = "-j " + CHAIN_FIREWALL_ACTION_ACCEPT;
    static final String RULE_JUMP_TO_FIREWALL_INTERACTIVE = "-j " + CHAIN_FIREWALL_ACTION_INTERACTIVE;
    static final String RULE_JUMP_TO_FIREWALL_REJECTED = "-j " + CHAIN_FIREWALL_ACTION_REJECT;
    static final String RULE_JUMP_TO_FIREWALL_REDIRECTION = "-j " + NetfilterBridgeIptablesHandler.CHAIN_FIREWALL_ACTION_REDIRECT;

    // Ignoring traffic from and to loopback device (everything from/to loopback, has been sent from/to loopback)
    static final String RULE_IGNORE_TRAFFIC_FROM_LOOPBACK = "-o lo -j ACCEPT";
    static final String RULE_IGNORE_TRAFFIC_TO_LOOPBACK = "-i lo -j ACCEPT";
//    // port-dependent rules: Contain the user-selected firewall-netfilter-bridge-port
//    private final String RULE_BRIDGE_COM_EXCEPTION_SERVER;
//    private final String RULE_BRIDGE_COM_EXCEPTION_CLIENT;

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

//        RULE_BRIDGE_COM_EXCEPTION_CLIENT = "-p tcp -s localhost -d localhost --destination-port " + bridgeCommunicationPort + " -j ACCEPT";
//        RULE_BRIDGE_COM_EXCEPTION_SERVER = "-p tcp -s localhost -d localhost --source-port " + bridgeCommunicationPort + " -j ACCEPT";
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
        Log.v(LOG_TAG, "iptable chains BEFORE adding rules:\n" + IptablesControl.getRuleInfoText(true, true));

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

        IptablesControl.chainAdd(CHAIN_FIREWALL_ACTION_REDIRECT, TABLE_NAT);
        IptablesControl.chainAdd(CHAIN_FIREWALL_INTERFACE_3G, TABLE_NAT);
        IptablesControl.chainAdd(CHAIN_FIREWALL_INTERFACE_WIFI, TABLE_NAT);


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
            // rule: exceptions for all local traffic - including the netfilter-bridge
            IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN_PREFILTER, RULE_IGNORE_TRAFFIC_FROM_LOOPBACK);
            IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN_PREFILTER, RULE_IGNORE_TRAFFIC_TO_LOOPBACK);

//            // rule: exceptions for netfilter-bridge
//            IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN_PREFILTER, RULE_BRIDGE_COM_EXCEPTION_CLIENT); // client
//            IptablesControl.ruleAdd(CHAIN_FIREWALL_MAIN_PREFILTER, RULE_BRIDGE_COM_EXCEPTION_SERVER); // server
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
        {
//            // rule: jump to NFQUEUE and handle package interactively
//            IptablesControl.ruleAdd(CHAIN_FIREWALL_ACTION_INTERACTIVE, RULE_JUMP_TO_NFQUEUE);

            // rule, TCP: only SYN/FIN packages will jump to NFQUEUE and handle package interactively
            IptablesControl.ruleAdd(CHAIN_FIREWALL_ACTION_INTERACTIVE, "-p tcp --tcp-flags SYN,RST,FIN SYN " + RULE_JUMP_TO_NFQUEUE);
            IptablesControl.ruleAdd(CHAIN_FIREWALL_ACTION_INTERACTIVE, "-p tcp --tcp-flags SYN,RST,FIN,ACK FIN,ACK " + RULE_JUMP_TO_NFQUEUE);

            // rule, UDP: since packages are indistinguishable ALL have to be forwarded into the firewall
            IptablesControl.ruleAdd(CHAIN_FIREWALL_ACTION_INTERACTIVE, "-p udp " + RULE_JUMP_TO_NFQUEUE);
        }

        // chain REDIRECT, table NAT:
        {
            // interface 3G
            for(String interfaceDevice : DEVICES_3G)
                IptablesControl.ruleAdd(CHAIN_FIREWALL_ACTION_REDIRECT, "-o " + interfaceDevice + " -j " + CHAIN_FIREWALL_INTERFACE_WIFI, TABLE_NAT); // for outgoing packets

            // interface WIFI
            for(String interfaceDevice : DEVICES_WIFI)
                IptablesControl.ruleAdd(CHAIN_FIREWALL_ACTION_REDIRECT, "-o "+interfaceDevice+" -j " + CHAIN_FIREWALL_INTERFACE_WIFI, TABLE_NAT);  // for outgoing packets


            IptablesControl.ruleAdd(IptableConstants.Chains.OUTPUT, RULE_JUMP_TO_FIREWALL_REDIRECTION, TABLE_NAT);
        }


        Log.v(LOG_TAG, "iptable chains AFTER adding rules:\n" + IptablesControl.getRuleInfoText(true, true));

        Log.i(LOG_TAG, "iptable setup completed.");
    }

    public void rulesDisableAll(boolean logChainStatesBeforeAndAfter) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        // If a iptable-chain does not exist, it implies that no references (i.e. --jump rules) exist either.

        if (logChainStatesBeforeAndAfter)
            Log.v(LOG_TAG, "iptable chains BEFORE removing rules:\n" + IptablesControl.getRuleInfoText(true, true));

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
        *  + REDIRECT = CHAIN_FIREWALL_ACTION_REDIRECT
        *
        *  Dependencies are as follows:
        *  + INPUT -> PREFILTER
        *  + OUTPUT -> PREFILTER
        *  + PREFILTER -> MAIN
        *  + MAIN -> 3G, WIFI, ACCEPT, REJECT, INTERACTIVE
        *  + INTERACTIVE -> NFQUEUE
        *  + [table 'nat']
        *    + OUTPUT -> REDIRECT
        *    + REDIRECT -> 3G [in 'nat' table]
        *    + REDIRECT -> WIFI [in 'nat' table]
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

        // Removing REDIRECT chain:
        if (IptablesControl.chainExists(CHAIN_FIREWALL_ACTION_REDIRECT, TABLE_NAT)) {
            IptablesControl.ruleDeleteIgnoreIfMissing(IptableConstants.Chains.OUTPUT, RULE_JUMP_TO_FIREWALL_REDIRECTION, TABLE_NAT); // remove jump to Discowall chain "REDIRECT" from chain "OUTPUT" in table "NAT"
            safelyRemoveChain(CHAIN_FIREWALL_ACTION_REDIRECT, TABLE_NAT); // remove Discowall chain "REDIRECT" from table "NAT"
            safelyRemoveChain(CHAIN_FIREWALL_INTERFACE_3G, TABLE_NAT); // remove Discowall chain "3G" from table "NAT"
            safelyRemoveChain(CHAIN_FIREWALL_INTERFACE_WIFI, TABLE_NAT); // remove Discowall chain "WIFI" from table "NAT"
        }


        if (logChainStatesBeforeAndAfter)
            Log.v(LOG_TAG, "iptable chains AFTER removing rules:\n" + IptablesControl.getRuleInfoText(true, true));
    }

    private void safelyRemoveChain(String chain) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        if (IptablesControl.chainExists(chain)) {
            // 1. First all references to the chain
            IptablesControl.rulesDeleteAll(chain);
            // 2. the chain itself can be removed
            IptablesControl.chainRemove(chain);
        }
    }

    private void safelyRemoveChain(String chain, String table) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        if (IptablesControl.chainExists(chain, table)) {
            // 1. First all references to the chain
            IptablesControl.rulesDeleteAll(chain, table);
            // 2. the chain itself can be removed
            IptablesControl.chainRemove(chain, table);
        }
    }

}
