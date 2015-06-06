package de.uni_kl.informatik.disco.discowall.firewallService;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import de.uni_kl.informatik.disco.discowall.AppManagement;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeCommunicator;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeControl;
import de.uni_kl.informatik.disco.discowall.netfilter.NetfilterExceptions;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgePackages;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class Firewall implements NetfilterBridgeCommunicator.EventsHandler {
    private static final String LOG_TAG = FirewallService.class.getSimpleName();

    private final AppManagement appManagement;
    private NetfilterBridgeControl control;

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

    public void enableFirewall(int port) throws ShellExecuteExceptions.CallException, NetfilterExceptions.NetfilterBridgeDeploymentException, ShellExecuteExceptions.ReturnValueException {
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

        Log.i(LOG_TAG, "firewall disabled.");
    }

    @Override
    public boolean onPackageReceived(NetfilterBridgePackages.TransportLayerPackage tlPackage) {
        return true;
    }
}
