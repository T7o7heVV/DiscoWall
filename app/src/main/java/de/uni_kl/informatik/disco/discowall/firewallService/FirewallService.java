package de.uni_kl.informatik.disco.discowall.firewallService;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;

import de.uni_kl.informatik.disco.discowall.AppManagement;
import de.uni_kl.informatik.disco.discowall.DiscoWallConstants;
import de.uni_kl.informatik.disco.discowall.R;
import de.uni_kl.informatik.disco.discowall.netfilter.NetfilterExceptions;
import de.uni_kl.informatik.disco.discowall.netfilter.NfqueueControl;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

/**
 * Persistent service hosting the entire DiscoWall firewall functionality.
 */
public class FirewallService extends IntentService {
    private static final String LOG_TAG = FirewallService.class.getSimpleName();

    private final AppManagement appManagement;
    private final NfqueueControl control;

    /** This variable is currently only used to create log-messages which specify whether the service is already running.
     */
    private boolean serviceRunning = false;

    public FirewallService() {
        super("FirewallService");

        Log.i(LOG_TAG, "initializing firewall service...");

        appManagement = new AppManagement(this);
        control = new NfqueueControl(appManagement);

        Log.i(LOG_TAG, "firewall service running.");
    }

//    @Override
//    public void onCreate() {
//        // called only once in the lifetime of a service
//        // since this DiscoWall service is persistent (due to startForeground), this method will only be called ONCE
//        // during the runtime of the host operating system
//        super.onCreate();
//    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "destroying firewall service.");

        // making sure, that no nfqueue rules remain - otherwise the host system's tcp/ip network would become unusable
        try {
            control.rulesDisableAll();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.e(LOG_TAG, "Could not remove iptable rules. Please check your rules for any nfqueue-call by using typing 'iptables -L -n -v' via a root-shell.");
        }

        Log.i(LOG_TAG, "firewall service destroyed.");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(LOG_TAG, "starting firewall service.");

        if (serviceRunning) {
            Log.i(LOG_TAG, "service already running - nothing to do.");
            return START_STICKY;
        }

        serviceRunning = true;
        createPermanentNotification();

        Log.i(LOG_TAG, "service started.");

        // I want this service to continue running until it is explicitly stopped using Activity.stopService()
        return START_STICKY;
    }

    @Override
    public boolean stopService(Intent name) {
        Log.i(LOG_TAG, "stopping firewall service.");

        /**
         * Note: The service is only destroyed, after
         * (1) stopService(Intent) has been called AND
         * (2) all clients have called unbind
         */

//        removePermanentNotification();

        // making sure, that no nfqueue rules remain - otherwise the host system's tcp/ip network would become unusable
        try {
            control.rulesDisableAll();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.e(LOG_TAG, "Could not remove iptable rules. Please check your rules for any nfqueue-call by using typing 'iptables -L -n -v' via a root-shell.");
        }

        Log.i(LOG_TAG, "service stopped.");

        return super.stopService(name);
    }

    public void stopFirewallService() {
        Log.i(LOG_TAG, "service is about to be stopped. It will continue until the last client (activity) disconnects.");

        stopForeground(true);
        stopSelf();
    }

//    private void removePermanentNotification() {
//        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        notificationManager.cancel(DiscoWallConstants.NotificationIDs.firewallService);
//    }

    /**
     * Creates permanent notification required for service to run indefinitely.
     * <p>
     * <b>WARNING: </b> Has to be called during the lifetime of this Service. This implies NOT being called from within the constructor.
     * </p>
     */
    private void createPermanentNotification() {
        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.firewall_service_notification_title))
                .setContentText(getString(R.string.firewall_service_notification_message))
                .setSmallIcon(R.mipmap.firewall)
                .build();

        startForeground(DiscoWallConstants.NotificationIDs.firewallService, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new FirewallBinder();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // all interaction handled synchroneously via Binder interface
    }

    public class FirewallBinder extends Binder {
        public FirewallService getService() {
            return FirewallService.this;
        }
    }

    public boolean isFirewallRunning() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        if (!control.isBridgeConnected())
            return false;
        else
            return control.rulesAreEnabled();
    }

    public void enableFirewall(int port) throws ShellExecuteExceptions.CallException, NetfilterExceptions.NetfilterBridgeDeploymentException, ShellExecuteExceptions.NonZeroReturnValueException {
        Log.v(LOG_TAG, "starting firewall...");

        if (isFirewallRunning())
        {
            Log.v(LOG_TAG, "firewall already running. nothing to do.");
            return;
        }

        // Start and connect to netfilter-bridge
        if (!control.isBridgeConnected())
            control.connectToBridge(port);

        // Enable iptables hooking-rules, so that each package will be sent to netfilter-bridge binary
        control.rulesEnableAll();

        Log.v(LOG_TAG, "firewall started.");
    }

    public void disableFirewall() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException, IOException {
        Log.v(LOG_TAG, "disabling firewall...");

        // Disable iptables hooking-rules, so that no package will be sent to netfilter-bridge binary
        Log.v(LOG_TAG, "removing iptable rules");
        control.rulesDisableAll();

        if (control.isBridgeConnected()) {
            Log.v(LOG_TAG, "disconnecting bridge");
            control.disconnectBridge();
        } else {
            Log.v(LOG_TAG, "bridge not connected.");
        }

        Log.v(LOG_TAG, "firewall disabled.");
    }

}
