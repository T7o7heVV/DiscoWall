package de.uni_kl.informatik.disco.discowall.firewallService;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import de.uni_kl.informatik.disco.discowall.AppManagement;
import de.uni_kl.informatik.disco.discowall.firewallService.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.firewallService.rules.FirewallRulesManager;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeCommunicator;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeControl;
import de.uni_kl.informatik.disco.discowall.netfilter.NetfilterExceptions;
import de.uni_kl.informatik.disco.discowall.packages.ConnectionManager;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class Firewall implements NetfilterBridgeCommunicator.EventsHandler {
    public static enum FirewallState { RUNNING, PAUSED, STOPPED }

    private static final String LOG_TAG = FirewallService.class.getSimpleName();

    private NetfilterBridgeControl control;

    private final ConnectionManager connectionManager = new ConnectionManager();
    private final AppManagement appManagement;
    private final FirewallRulesManager rulesManager = new FirewallRulesManager();

    public Firewall(Context context) {
        Log.i(LOG_TAG, "initializing firewall service...");
        appManagement = new AppManagement(context);
        Log.i(LOG_TAG, "firewall service running.");
    }

    public boolean isFirewallRunning() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        if (control == null)
            return false;
        else
            return control.isBridgeConnected();
    }

    public void enableFirewall(int port) throws ShellExecuteExceptions.CallException, NetfilterExceptions.NetfilterBridgeDeploymentException, ShellExecuteExceptions.ReturnValueException, IOException {
        Log.i(LOG_TAG, "starting firewall...");

        if (isFirewallRunning())
        {
            Log.i(LOG_TAG, "firewall already running. nothing to do.");
        } else {
            control = new NetfilterBridgeControl(this, appManagement, appManagement.getSettings().getFirewallPort());
            Log.i(LOG_TAG, "firewall started.");
        }
    }

    public void disableFirewall() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException, IOException {
        Log.i(LOG_TAG, "disabling firewall...");

        if (control == null) {
            Log.i(LOG_TAG, "firewall already disabled. nothing to do.");
            return;
        }

        // I will try disconnecting the bridge - even if the communication itself is already down.
        // This is being done to make sure the user can deactivate the firewall even in an unexpected/erroneous state.

        // Disable iptables hooking-rules, so that no package will be sent to netfilter-bridge binary
        Log.v(LOG_TAG, "disconnecting bridge");
        control.disconnectBridge();
        control = null;

        Log.i(LOG_TAG, "firewall disabled.");
    }

    public boolean isFirewallPaused() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException, FirewallExceptions.FirewallInvalidStateException {
        if (!isFirewallRunning()) {
            Log.e(LOG_TAG, "Firewall is not enabled - it is neither paused nor unpaused.");
            throw new FirewallExceptions.FirewallInvalidStateException("Firewall needs to be running in order to pause/unpause it.", FirewallState.STOPPED);
        }

        return isFirewallPausedEx();
    }

    private boolean isFirewallPausedEx() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        return !control.isIptableJumpsToFirewallEnabled(); // the firewall is paused, when the iptable jump-rules to the firewall chain are not set
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

        control.setIptableJumpsToFirewallEnabled(!paused);

        if (paused)
            Log.d(LOG_TAG, "new firewall state: paused");
        else
            Log.d(LOG_TAG, "new firewall state: running");
    }

    public FirewallState getFirewallState() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        if (!isFirewallRunning())
            return FirewallState.STOPPED;

        if (isFirewallPausedEx())
            return FirewallState.PAUSED;
        else
            return FirewallState.RUNNING;
    }

    @Override
    public boolean onPackageReceived(Packages.TransportLayerPackage tlPackage) {
        if (tlPackage instanceof Packages.TcpPackage) {
            Packages.TcpPackage tcpPackage = (Packages.TcpPackage) tlPackage;

//            if (!connectionManager.containsConnection(tlPackage)) {
//                try {
//                    new FirewallRulesManager().createRule_Accept(FirewallRules.DeviceFilter.WIFI, tlPackage).setRuleEnabled(true);
//                } catch (ShellExecuteExceptions.CallException e) {
//                    e.printStackTrace();
//                } catch (ShellExecuteExceptions.ReturnValueException e) {
//                    e.printStackTrace();
//                }
//            }

            Connections.Connection connection = connectionManager.getConnection(tcpPackage);
            Log.v(LOG_TAG, "Connection: " + connection);
        }

        return true;
    }

    @Override
    public void onInternalERROR(String message, Exception e) {
    }

}
