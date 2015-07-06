package de.uni_kl.informatik.disco.discowall.firewallService;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import de.uni_kl.informatik.disco.discowall.firewallService.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.firewallService.rules.FirewallRulesManager;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeCommunicator;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeControl;
import de.uni_kl.informatik.disco.discowall.netfilter.NetfilterExceptions;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeIptablesHandler;
import de.uni_kl.informatik.disco.discowall.netfilter.iptables.IptablesControl;
import de.uni_kl.informatik.disco.discowall.packages.ConnectionManager;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.NetworkInterfaceHelper;
import de.uni_kl.informatik.disco.discowall.utils.shell.RootShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class Firewall implements NetfilterBridgeCommunicator.EventsHandler {
    public static enum FirewallState { RUNNING, PAUSED, STOPPED }

    private static final String LOG_TAG = Firewall.class.getSimpleName();

    private final ConnectionManager connectionManager = new ConnectionManager();
    private FirewallRulesManager rulesManager;
    private final NetworkInterfaceHelper networkInterfaceHelper = new NetworkInterfaceHelper();
    private final Context firewallServiceContext;

    private NetfilterBridgeControl control;
//    private DnsCacheControl dnsCacheControl;

    public Firewall(Context firewallServiceContext) {
        Log.i(LOG_TAG, "initializing firewall service...");

        this.firewallServiceContext = firewallServiceContext;

        Log.i(LOG_TAG, "firewall service running.");
    }

    public boolean isFirewallRunning() {
        if (control == null)
            return false;
        else
            return control.isBridgeConnected();
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
            rulesManager = new FirewallRulesManager(control.getFirewallIptableRulesHandler()); // creating rulesManager with IptableRulesHandler from NetfitlerBridge, which requires the bridge-port

            // starting the dns cache for sniffing the dns-resolutions
//            dnsCacheControl = new DnsCacheControl(DiscoWallConstants.DnsCache.dnsCachePort);

            Log.i(LOG_TAG, "firewall started.");
        }
    }

    public void disableFirewall() throws FirewallExceptions.FirewallException {
        Log.i(LOG_TAG, "disabling firewall...");

        if (control == null) {
            Log.i(LOG_TAG, "firewall already disabled. nothing to do.");
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
    }

    public boolean isFirewallPaused() throws FirewallExceptions.FirewallException {
        if (!isFirewallRunning())
            return false;

        try {
            return !control.getFirewallIptableRulesHandler().isMainChainJumpsEnabled(); // the firewall is paused, when the iptable jump-rules to the firewall chain are not set
        } catch(ShellExecuteExceptions.ShellExecuteException e) {
            throw new FirewallExceptions.FirewallException("Error fetching firewall state: " + e.getMessage(), e);
        }
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

        control.getFirewallIptableRulesHandler().setMainChainJumpsEnabled(!paused);

        if (paused)
            Log.d(LOG_TAG, "new firewall state: paused");
        else
            Log.d(LOG_TAG, "new firewall state: running");
    }

    public FirewallState getFirewallState() throws FirewallExceptions.FirewallException {
        if (!isFirewallRunning())
            return FirewallState.STOPPED;

        if (isFirewallPaused())
            return FirewallState.PAUSED;
        else
            return FirewallState.RUNNING;
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

    public String getIptableRules() throws FirewallExceptions.FirewallException {
        try {
            return IptablesControl.getRuleInfoText(true, true);
        } catch(ShellExecuteExceptions.ShellExecuteException e) {
            throw new FirewallExceptions.FirewallException("Error fetching iptable rules: " + e.getMessage(), e);
        }
    }

    @Override
    public void onInternalERROR(String message, Exception e) {
    }

    public void DEBUG_TEST() {
        try {
            control.getFirewallIptableRulesHandler().setUserPackagesForwardToFirewall(0, true);
            FirewallRules.FirewallTransportRule rule = rulesManager.createTcpRule(0, new Packages.IpPortPair("localhost", 0), new Packages.IpPortPair("chip.de", 80), FirewallRules.DeviceFilter.ANY,  FirewallRules.RulePolicy.ACCEPT);
            Log.i(LOG_TAG, "RULE CREATED: " + rule);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
