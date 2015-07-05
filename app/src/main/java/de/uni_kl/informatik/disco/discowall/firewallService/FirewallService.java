package de.uni_kl.informatik.disco.discowall.firewallService;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import de.uni_kl.informatik.disco.discowall.DiscoWallConstants;
import de.uni_kl.informatik.disco.discowall.R;

/**
 * Persistent service hosting the entire DiscoWall firewall functionality.
 */
public class FirewallService extends IntentService {
    private static final String LOG_TAG = FirewallService.class.getSimpleName();

    /** This variable is currently only used to create log-messages which specify whether the service is already running.
     */
    private boolean serviceRunning = false;
    private Firewall firewall;

    public FirewallService() {
        super("FirewallService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // There are some Android API-calls which can only be run AFTER the calling "Context" instance
        // has passed the "onCreate()" method. Therefore the firewall is only initialized here.
        if (firewall != null)
            return;

        firewall = new Firewall(this);
    }

    public Firewall getFirewall() {
        return firewall;
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "destroying firewall service.");

        // making sure, that no nfqueue rules remain - otherwise the host system's tcp/ip network would become unusable
        try {
            firewall.disableFirewall();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.e(LOG_TAG, "Could not stop all firewall-modules. Please check your rules for any nfqueue-call by using typing 'iptables -L -n -v' via a root-shell.");
        }

        Log.i(LOG_TAG, "firewall service destroyed.");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // DO NOT invoke the super-method!
        // It will result in the service behaving in "default" behavior - i.e. NON-persistent
//        super.onStartCommand(intent, flags, startId);

        Log.i(LOG_TAG, "starting firewall service.");

        if (serviceRunning) {
            Log.i(LOG_TAG, "service already running - nothing to do.");
            return START_STICKY;
        }

        serviceRunning = true;

        // WARNING: Has to be called during the lifetime of this Service. This implies NOT being called from within the constructor.
        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.firewall_service_notification_title))
                .setContentText(getString(R.string.firewall_service_notification_message))
                .setSmallIcon(R.mipmap.firewall_launcher)
                .build();

        startForeground(DiscoWallConstants.NotificationIDs.firewallService, notification);

        Log.i(LOG_TAG, "service started.");

        // I want this service to continue running until it is explicitly stopped using Activity.stopService()
        return START_STICKY;
    }

    /**
     * Android-Service method, which is being invoked AFTER the android-system has begun stopping the service.
     * @param name
     * @return
     */
    @Override
    public boolean stopService(Intent name) {
        Log.i(LOG_TAG, "stopping firewall service.");

        serviceRunning = false;

        /**
         * Note: The service is only destroyed, after
         * (1) stopService(Intent) has been called AND
         * (2) all clients have called unbind
         */

        // making sure, that no nfqueue rules remain - otherwise the host system's tcp/ip network would become unusable
        try {
            firewall.disableFirewall();
            Log.i(LOG_TAG, "service stopped.");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while stopping firewall service: " + e.getMessage());
        }

        return super.stopService(name);
    }

    /**
     * Method which makes it possible to stop the android service.
     * All connected clients (i.e. activities) must disconnect, so that the service may be stopped.
     */
    public void stopFirewallService() {
        Log.i(LOG_TAG, "service is about to be stopped. It will continue until the last client (activity) disconnects.");

        stopForeground(true);
        stopSelf();
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

}
