package de.uni_kl.informatik.disco.discowall.firewall;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.uni_kl.informatik.disco.discowall.firewall.helpers.WatchedAppsPreferencesManager;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallIptableRulesHandler;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRulesManager;
import de.uni_kl.informatik.disco.discowall.gui.dialogs.ErrorDialog;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeCommunicator;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeControl;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeIptablesHandler;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterFirewallRulesHandler;
import de.uni_kl.informatik.disco.discowall.netfilter.iptables.IptablesControl;
import de.uni_kl.informatik.disco.discowall.packages.ConnectionManager;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.NetworkInterfaceHelper;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class Firewall implements NetfilterBridgeCommunicator.EventsHandler {
    public static enum FirewallState { RUNNING, PAUSED, STOPPED;}
    private FirewallState firewallState;

    public static interface FirewallEnableProgressListener extends IptablesControl.IptablesCommandListener {
        void onWatchedAppsBeforeRestore(List<ApplicationInfo> watchedApps);
        void onWatchedAppsRestoreApp(ApplicationInfo watchedApp, int appIndex);

        void onFirewallPolicyBeforeApplyPolicy(FirewallRulesManager.FirewallPolicy policy);
    }

    public static interface FirewallStateListener {
        void onFirewallStateChanged(FirewallState state);
    }

    private static final String LOG_TAG = Firewall.class.getSimpleName();

    private final ConnectionManager connectionManager = new ConnectionManager();
    private final NetworkInterfaceHelper networkInterfaceHelper = new NetworkInterfaceHelper();
    private final FirewallIptableRulesHandler iptableRulesManager = NetfilterFirewallRulesHandler.instance;
    private final FirewallRulesManager rulesManager = new FirewallRulesManager(NetfilterFirewallRulesHandler.instance);
    private final WatchedAppsPreferencesManager watchedAppsManager;

    private final Context firewallServiceContext;
    private FirewallStateListener firewallStateListener;

    private NetfilterBridgeControl control;
//    private DnsCacheControl dnsCacheControl;

    public Firewall(FirewallService firewallServiceContext) {
        Log.i(LOG_TAG, "initializing firewall service...");

        this.firewallServiceContext = firewallServiceContext;
        this.firewallState = FirewallState.STOPPED;
        this.watchedAppsManager = new WatchedAppsPreferencesManager(firewallServiceContext);

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
        enableFirewall(port, null);
    }

    public void enableFirewall(int port, FirewallEnableProgressListener progressListener) throws FirewallExceptions.FirewallException {
        Log.i(LOG_TAG, "starting firewall...");

        boolean alreadyRunnig = isFirewallRunning();
        Log.v(LOG_TAG, "check if firewall already running: " + alreadyRunnig);

        if (alreadyRunnig)
        {
            Log.i(LOG_TAG, "firewall already running. nothing to do.");
        } else {

            // Commandlistener is only temporarily being set
            if (progressListener != null)
                IptablesControl.setCommandListener(progressListener);

            // starting netfilter bridge - i.e. the "firewall core"
            try {
                control = new NetfilterBridgeControl(this, firewallServiceContext, port);
            } catch(Exception e) {
                IptablesControl.setCommandListener(null); // removing command-listener
                throw new FirewallExceptions.FirewallException("Error initializing firewall: " + e.getMessage(), e);
            }

            // removing iptables-command-listener.
            IptablesControl.setCommandListener(null);

            // starting the dns cache for sniffing the dns-resolutions
//            dnsCacheControl = new DnsCacheControl(DiscoWallConstants.DnsCache.dnsCachePort);

            Log.d(LOG_TAG, "firewall engine running.");
            onFirewallStateChanged(FirewallState.RUNNING); // has to be called here, so that all following algorithms get the correct firewall-running-state

            // Start watching apps which have been watched before
            Log.d(LOG_TAG, "restoring forwarding-rules for watched apps...");
            {
                List<ApplicationInfo> watchedApps = watchedAppsManager.getWatchedApps();

                // reporting progress to listener
                if (progressListener != null)
                    progressListener.onWatchedAppsBeforeRestore(watchedApps);

                int appIndex = 0;
                for (ApplicationInfo watchedApp : watchedApps) {
                    if (progressListener != null)
                        progressListener.onWatchedAppsRestoreApp(watchedApp, appIndex++); // reporting progress to listener

                    setAppWatched(watchedApp, true);
                }
            }

            Log.d(LOG_TAG, "restoring firewall-policy...");
            {
                FirewallRulesManager.FirewallPolicy policy = DiscoWallSettings.getInstance().getFirewallPolicy(firewallServiceContext);

                // reporting progress to listener
                if (progressListener != null)
                    progressListener.onFirewallPolicyBeforeApplyPolicy(policy);

                rulesManager.setFirewallPolicy(policy);
            }

            Log.i(LOG_TAG, "firewall started.");
        }
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
        return rulesManager.getFirewallPolicy();
    }

    public void setFirewallPolicy(FirewallRulesManager.FirewallPolicy newRulesPolicy) throws FirewallExceptions.FirewallException {
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

    public List<ApplicationInfo> getWatchableApps() {
        return watchedAppsManager.getWatchableApps();
    }

//    public HashMap<ApplicationInfo, Boolean> getAppsToWatchStateMap() throws FirewallExceptions.FirewallException {
//        return watchedAppsManager.createAppsToWatchStateMap();
//    }

    /**
     * Disables watching of all apps besides those who are specified within the list.
     * @param appsToWatch
     * @throws FirewallExceptions.FirewallException
     */
    public void setWatchedApps(List<ApplicationInfo> appsToWatch) throws FirewallExceptions.FirewallException {
        HashMap<String, ApplicationInfo> packageNameToAppInfoMap = WatchedAppsPreferencesManager.packageNameToApplicationInfoMap(getWatchedApps());

        Set<String> appsToWatchSet = WatchedAppsPreferencesManager.applicationListToPackageNameSet(appsToWatch);
        Set<String> currentlyWatchedAppsSet = WatchedAppsPreferencesManager.applicationListToPackageNameSet(watchedAppsManager.getWatchedApps());

        // Disable watching of those apps, which are not in the "watchedAppsSet"
        for(String watchedApp : currentlyWatchedAppsSet) {
            if (!appsToWatchSet.contains(watchedApp))
                setAppWatched(packageNameToAppInfoMap.get(watchedApp), false);
        }

        // List has changed, updating list of currently watched apps:
        currentlyWatchedAppsSet = WatchedAppsPreferencesManager.applicationListToPackageNameSet(watchedAppsManager.getWatchedApps());

        // Enable watching of those apps, which are in the "appsToWatchSet" (and not currently watched)
        for(ApplicationInfo appToWatch : appsToWatch) {
            if (!currentlyWatchedAppsSet.contains(appToWatch.packageName)) // checking via Set.contains(), as this can be done within O(n), whereas isAppWatched() takes O(n²)
                setAppWatched(appToWatch, true);
        }
    }

    /**
     * Makes sure the traffic of a specified application will be monitored by the firewall. The configuration is automatically stored persistently.
     * <p></p>
     * <b>Note: </b> If the firewall is not running, this call will have no effect. If the firewall is being started, all watched-states will be restored.
     * @param appInfo
     * @param watchTraffic
     * @throws FirewallExceptions.FirewallException
     */
    public void setAppWatched(ApplicationInfo appInfo, boolean watchTraffic) throws FirewallExceptions.FirewallException {
        String appName = appInfo.loadLabel(firewallServiceContext.getPackageManager()) + "";
        Log.d(LOG_TAG, "enabling traffic-monitoring for app " + appName + " with uid " + appInfo.uid + ".");

        if (!isFirewallStopped()) {
            Log.v(LOG_TAG, "Firewall is running, iptable-rules will be created...");

            try {
                iptableRulesManager.setUserPackagesForwardToFirewall(appInfo.uid, watchTraffic);
            } catch (ShellExecuteExceptions.ShellExecuteException e) {
                throw new FirewallExceptions.FirewallException("Error changing watched-state for app(s) by user id " + appInfo.uid + ": " + e.getMessage(), e);
            }
        } else {
            Log.v(LOG_TAG, "Firewall not running, iptable-rules will be created on next firewall-activation.");
        }

        // updating watched apps persistent preferences
        watchedAppsManager.setAppWatched(appInfo, watchTraffic);
    }

    public boolean isAppWatched(ApplicationInfo appInfo) {
        return watchedAppsManager.isAppWatched(appInfo);
    }

    public List<ApplicationInfo> getWatchedApps() {
        return watchedAppsManager.getWatchedApps();
    }

    @Override
    public void onInternalERROR(String message, Exception e) {
        ErrorDialog.showError(firewallServiceContext, "DiscoWall Internal Error", "Error within package-filtering engine occurred: " + e.getMessage());
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
