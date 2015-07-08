package de.uni_kl.informatik.disco.discowall.firewallService;

import android.content.Context;
import android.util.Log;

import de.uni_kl.informatik.disco.discowall.firewallService.rules.FirewallIptableRulesHandler;
import de.uni_kl.informatik.disco.discowall.firewallService.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.firewallService.rules.FirewallRulesManager;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeCommunicator;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeControl;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeIptablesHandler;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterFirewallRulesHandler;
import de.uni_kl.informatik.disco.discowall.netfilter.iptables.IptablesControl;
import de.uni_kl.informatik.disco.discowall.packages.ConnectionManager;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.NetworkInterfaceHelper;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class Firewall implements NetfilterBridgeCommunicator.EventsHandler {
    public static enum FirewallState { RUNNING, PAUSED, STOPPED;}
    private FirewallState firewallState;

    public static interface FirewallStateListener {
        void onFirewallStateChanged(FirewallState state);
    }

    private static final String LOG_TAG = Firewall.class.getSimpleName();

    private final ConnectionManager connectionManager = new ConnectionManager();
    private final NetworkInterfaceHelper networkInterfaceHelper = new NetworkInterfaceHelper();
    private final FirewallIptableRulesHandler iptableRulesManager = NetfilterFirewallRulesHandler.instance;
    private final FirewallRulesManager rulesManager = new FirewallRulesManager(NetfilterFirewallRulesHandler.instance);

    private final Context firewallServiceContext;
    private FirewallStateListener firewallStateListener;

    private NetfilterBridgeControl control;
//    private DnsCacheControl dnsCacheControl;

    public Firewall(Context firewallServiceContext) {
        Log.i(LOG_TAG, "initializing firewall service...");

        this.firewallServiceContext = firewallServiceContext;
        this.firewallState = FirewallState.STOPPED;

        Log.i(LOG_TAG, "firewall service running.");
    }

    /**
     * Used by FirewallService to show state in notification-bar etc.
     * @param stateListener
     */
    void setFirewallStateListener(FirewallStateListener stateListener) {
        this.firewallStateListener = stateListener;
    }

    FirewallStateListener getStateListener() {
        return firewallStateListener;
    }

    private void onFirewallStateChanged(FirewallState state) {
        this.firewallState = state;
        Log.d(LOG_TAG, "Firewall state changed: " + state);

        if (firewallStateListener != null)
            firewallStateListener.onFirewallStateChanged(state);
    }

    public void enableFirewall(int port) throws FirewallExceptions.FirewallException {
        Log.i(LOG_TAG, "starting firewall...");

        boolean alreadyRunnig = isFirewallRunning();
        Log.d(LOG_TAG, "check if firewall already running: " + alreadyRunnig);

        if (alreadyRunnig)
        {
            Log.i(LOG_TAG, "firewall already running. nothing to do.");
        } else {

            // starting netfilter bridge - i.e. the "firewall core"
            try {
                control = new NetfilterBridgeControl(this, firewallServiceContext, port);
            } catch(Exception e) {
                throw new FirewallExceptions.FirewallException("Error initializing firewall: " + e.getMessage(), e);
            }

            // starting the dns cache for sniffing the dns-resolutions
//            dnsCacheControl = new DnsCacheControl(DiscoWallConstants.DnsCache.dnsCachePort);

            Log.i(LOG_TAG, "firewall started.");
        }

        onFirewallStateChanged(FirewallState.RUNNING);
    }

    public void disableFirewall() throws FirewallExceptions.FirewallException {
        Log.i(LOG_TAG, "disabling firewall...");

        if (control == null) {
            Log.i(LOG_TAG, "firewall already disabled. nothing to do.");
            onFirewallStateChanged(FirewallState.STOPPED); // state will be broadcasted even if the firewall is already stopped.
            return;
        }

        // I will try disconnecting the bridge - even if the communication itself is already down.
        // This is being done to make sure the user can deactivate the firewall even in an unexpected/erroneous state.

        // Disable iptables hooking-rules, so that no package will be sent to netfilter-bridge binary
        Log.v(LOG_TAG, "disconnecting bridge");

        try {
            control.disconnectBridge();
        } catch (Exception e) {
            throw new FirewallExceptions.FirewallException("Error disconnecting netfilter-bridge: " + e.getMessage(), e);
        }
        control = null;

        Log.i(LOG_TAG, "firewall disabled.");
        onFirewallStateChanged(FirewallState.STOPPED);
    }

    public FirewallState getFirewallState() {
        return firewallState;

        /* It is important to buffer the state in a variable,
         * because fast state-queries immediately after changing the state (happened when switching from DISABLED to RUNNING)
         * may return the old state.
         *
         * This is due to the time it takes for:
         *  - iptable-changes to be reflected by iptables
         *  - ports to connect/disconnect
         * etc.
         *
         * ==> Buffering the state removes the problem and removes the possibility of causing exceptions on query.
         */

//        if (!isFirewallRunning())
//            return FirewallState.STOPPED;
//
//        if (isFirewallPaused())
//            return FirewallState.PAUSED;
//        else
//            return FirewallState.RUNNING;
    }

    public boolean isFirewallRunning() {
        return firewallState == FirewallState.RUNNING;

//        if (control == null)
//            return false;
//        else
//            return control.isBridgeConnected();
    }

    public boolean isFirewallPaused() {
        return firewallState == FirewallState.PAUSED;

//        if (!isFirewallRunning())
//            return false;
//
//        try {
//            return !iptableRulesManager.isMainChainJumpsEnabled(); // the firewall is paused, when the iptable jump-rules to the firewall chain are not set
//        } catch(ShellExecuteExceptions.ShellExecuteException e) {
//            throw new FirewallExceptions.FirewallException("Error fetching firewall state: " + e.getMessage(), e);
//        }
    }

    public boolean isFirewallStopped() {
        return firewallState == FirewallState.STOPPED;
    }

    /**
     * Will add/remove the iptable-rules which forward the packages into the firewall main-chain.
     * Removing those rules will circumvent the entire firewall functionality.
     * @param paused
     */
    public void setFirewallPaused(boolean paused) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException, FirewallExceptions.FirewallInvalidStateException {
        if (!isFirewallRunning()) {
            Log.e(LOG_TAG, "Firewall is not enabled - cannot pause/unpause the firewall");
            throw new FirewallExceptions.FirewallInvalidStateException("Firewall needs to be running in order to pause/unpause it.", FirewallState.STOPPED);
        }

        if (paused)
            Log.v(LOG_TAG, "Changing firewall state to paused...");
        else
            Log.v(LOG_TAG, "Changing firewall state to running...");

        iptableRulesManager.setMainChainJumpsEnabled(!paused);

        if (paused) {
            Log.d(LOG_TAG, "new firewall state: paused");
            onFirewallStateChanged(FirewallState.PAUSED);
        } else {
            Log.d(LOG_TAG, "new firewall state: running");
            onFirewallStateChanged(FirewallState.RUNNING);
        }
    }

    private void assertFirewallRunning() {
        if (!isFirewallRunning())
            throw new FirewallExceptions.FirewallInvalidStateException("Firewall needs to be running to perform specified action.", FirewallState.STOPPED);
    }

    public FirewallRulesManager.FirewallPolicy getFirewallPolicy() {
        assertFirewallRunning();
        return rulesManager.getFirewallPolicy();
    }

    public void setFirewallPolicy(FirewallRulesManager.FirewallPolicy newRulesPolicy) throws FirewallExceptions.FirewallException {
        assertFirewallRunning();
        rulesManager.setFirewallPolicy(newRulesPolicy);
    }

    @Override
    public boolean onPackageReceived(Packages.TransportLayerPackage tlPackage) {
        // Find device-name for package:
        if (tlPackage.getInputDeviceIndex() >= 0) {
            tlPackage.setNetworkInterface(networkInterfaceHelper.getPackageInterfaceById(tlPackage.getInputDeviceIndex()));
        } else if (tlPackage.getOutputDeviceIndex() >= 0) {
            tlPackage.setNetworkInterface(networkInterfaceHelper.getPackageInterfaceById(tlPackage.getOutputDeviceIndex()));
        }

        // Store user-id within package
        tlPackage.setUserId(tlPackage.getMark() - NetfilterBridgeIptablesHandler.PACKAGE_UID_MARK_OFFSET);

        boolean accepted;

        if (tlPackage instanceof Packages.TcpPackage) {
            Packages.TcpPackage tcpPackage = (Packages.TcpPackage) tlPackage;
            Connections.TcpConnection tcpConnection = connectionManager.getTcpConnection(tcpPackage);

            accepted = rulesManager.isPackageAccepted(tcpPackage, tcpConnection);
            if (accepted)
                tcpConnection.update(tcpPackage);

            Log.v(LOG_TAG, "Connection: " + tcpConnection);
        } else if (tlPackage instanceof Packages.UdpPackage) {
            Packages.UdpPackage udpPackage = (Packages.UdpPackage) tlPackage;
            Connections.UdpConnection udpConnection = connectionManager.getUdpConnection(udpPackage);

            accepted = rulesManager.isPackageAccepted(udpPackage, udpConnection);
            if (accepted)
                udpConnection.update(udpPackage);
        } else {
            Log.e(LOG_TAG, "No handler package-protocol implemented! Package is: " + tlPackage);
            return true;
        }

        return accepted;
    }

    public String getIptableRules(boolean all) throws FirewallExceptions.FirewallException {
        try {
            if (all) {
                return IptablesControl.getRuleInfoText(true, true);
            } else {
                if (!isFirewallRunning())
                    return "< firewall has to be enabled in order to retrieve firewall rules >";
                return iptableRulesManager.getFirewallRulesText();
            }
        } catch(ShellExecuteExceptions.ShellExecuteException e) {
            throw new FirewallExceptions.FirewallException("Error fetching iptable rules: " + e.getMessage(), e);
        }
    }

    public void setAppTrafficWatched(int appUserId, boolean watchTraffic) throws FirewallExceptions.FirewallException {
        try {
            iptableRulesManager.setUserPackagesForwardToFirewall(appUserId, watchTraffic);
        } catch (ShellExecuteExceptions.ShellExecuteException e) {
            throw new FirewallExceptions.FirewallException("Error changing watched-state for apps by user id " + appUserId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void onInternalERROR(String message, Exception e) {
    }

    public void DEBUG_TEST() {
        try {
            iptableRulesManager.setUserPackagesForwardToFirewall(0, true);
            FirewallRules.FirewallTransportRule rule = rulesManager.createTcpRule(0, new Packages.IpPortPair("localhost", 0), new Packages.IpPortPair("chip.de", 80), FirewallRules.DeviceFilter.ANY,  FirewallRules.RulePolicy.ACCEPT);
            Log.i(LOG_TAG, "RULE CREATED: " + rule);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
