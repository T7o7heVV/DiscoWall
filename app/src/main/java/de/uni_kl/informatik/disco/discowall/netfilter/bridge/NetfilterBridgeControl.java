package de.uni_kl.informatik.disco.discowall.netfilter.bridge;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import de.uni_kl.informatik.disco.discowall.firewallService.rules.FirewallIptableRulesHandler;
import de.uni_kl.informatik.disco.discowall.netfilter.NetfilterExceptions;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class NetfilterBridgeControl {
    private static final String LOG_TAG = NetfilterBridgeControl.class.getSimpleName();
    public static final boolean DEBUG_USE_EXTERNAL_BINARY = false;

    private final Context firewallServiceContext;
    private final NetfilterBridgeCommunicator.EventsHandler bridgeEventsHandler;

    private final NetfilterBridgeIptablesHandler iptablesHandler;
    private final NetfilterBridgeBinaryHandler bridgeBinaryHandler;
    private NetfilterBridgeCommunicator bridgeCommunicator;
    private final int bridgeCommunicationPort;

    public NetfilterBridgeControl(NetfilterBridgeCommunicator.EventsHandler bridgeEventsHandler, Context firewallServiceContext, int bridgeCommunicationPort) throws NetfilterExceptions.NetfilterBridgeDeploymentException, ShellExecuteExceptions.ReturnValueException, ShellExecuteExceptions.CallException, IOException {
        Log.d(LOG_TAG, "initializing NetfilterBridgeControl...");
        Log.d(LOG_TAG, "NetfilterBridge communication port: " + bridgeCommunicationPort);

        this.bridgeEventsHandler = bridgeEventsHandler;
        this.bridgeCommunicationPort = bridgeCommunicationPort;

        this.firewallServiceContext = firewallServiceContext;
        this.bridgeBinaryHandler = new NetfilterBridgeBinaryHandler(firewallServiceContext);
        this.iptablesHandler = new NetfilterBridgeIptablesHandler(bridgeCommunicationPort);

        // -----------------------------------------------------------------------------------------------------------
        // Connect to bridge
        // -----------------------------------------------------------------------------------------------------------

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
        iptablesHandler.rulesEnableAll();

        Log.d(LOG_TAG, "starting netfilter bridge communicator as listening server...");
        bridgeCommunicator = new NetfilterBridgeCommunicator(bridgeEventsHandler, bridgeCommunicationPort);
        Log.d(LOG_TAG, "listening on port: " + bridgeCommunicationPort);

        Log.d(LOG_TAG, "killing all possibly running netfilter bridge instances...");
        bridgeBinaryHandler.killAllInstances();

        if (!DEBUG_USE_EXTERNAL_BINARY) {
            Log.d(LOG_TAG, "executing netfilter bridge binary...");
            bridgeBinaryHandler.start(bridgeCommunicationPort);
        } else {
            Log.i(LOG_TAG, "DEBUG-Flag set. The netfilter-bridge has to be started externally. It will NOT be started from here. Typically an adb-shell will be used to start it directly.");
        }

        Log.d(LOG_TAG, "netfilter-bridge connected.");
    }

    public boolean isBridgeConnected() {
        if (bridgeCommunicator == null)
            return false;
        else
            return bridgeCommunicator.isConnected() && bridgeBinaryHandler.isProcessRunning();
    }

    public void disconnectBridge() throws IOException, ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        Log.d(LOG_TAG, "disconnecting netfilter-bridge-communicator");

        // Removing any IPTABLE-rules. Has to be done first, so that no packages are "stuck" within the NFQUEUE-chain.
        Log.d(LOG_TAG, "removing all static iptable-rules");
        iptablesHandler.rulesDisableAll(true);

        if (bridgeCommunicator == null) {
            Log.v(LOG_TAG, "bridge-communicator has never been connected - nothing to disconnect.");
            return;
        }

        bridgeCommunicator.disconnect();

        Log.d(LOG_TAG, "killing all possibly running netfilter bridge instances...");
        bridgeBinaryHandler.killAllInstances();
    }

    public FirewallIptableRulesHandler getFirewallIptableRulesHandler() {
        return iptablesHandler.getFirewallRulesHandler();
    }

}
