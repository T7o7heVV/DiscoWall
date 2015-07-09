package de.uni_kl.informatik.disco.discowall.firewall;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import de.uni_kl.informatik.disco.discowall.MainActivity;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallConstants;
import de.uni_kl.informatik.disco.discowall.R;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;

/**
 * Persistent service hosting the entire DiscoWall firewall functionality.
 */
public class FirewallService extends IntentService {
    private static final String LOG_TAG = FirewallService.class.getSimpleName();

    private static final String BUNDLE_KEY__AUTOSTART_FIREWALL = "autostart";

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

        firewall.setFirewallStateListener(new Firewall.FirewallStateListener() {
            @Override
            public void onFirewallStateChanged(Firewall.FirewallState state) {
                updateServiceNotification();
            }
        });
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
        updateServiceNotification();
        Log.i(LOG_TAG, "service started.");


        // If the firewall should be automatically started:
        if ((intent != null) && (intent.getExtras() != null) && intent.getExtras().containsKey(BUNDLE_KEY__AUTOSTART_FIREWALL)) { // if autostart-flag is present
            if (intent.getExtras().getBoolean(BUNDLE_KEY__AUTOSTART_FIREWALL)) { // if flag is set to TRUE
                try {
                    firewall.enableFirewall(DiscoWallSettings.getInstance().getFirewallPort(this));
                } catch (FirewallExceptions.FirewallException e) {
                    Log.e(LOG_TAG, "ERROR autostarting firewall on service-start: " + e.getMessage(), e);
                }
            }
        }

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
        // all interaction handled synchronously via Binder interface
    }

    public class FirewallBinder extends Binder {
        public FirewallService getService() {
            return FirewallService.this;
        }
    }

    public static void startFirewallService(Context context, boolean enableFirewall) {
        Intent serviceStartIntent = new Intent(context, FirewallService.class);

        if (enableFirewall)
            serviceStartIntent.putExtra(BUNDLE_KEY__AUTOSTART_FIREWALL, true);

        context.startService(serviceStartIntent);
    }

    public void updateServiceNotification() {
        Intent clickIntent = new Intent(this, MainActivity.class);
//        clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingClickIntent = PendingIntent.getActivity(this, 1, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        PendingIntent pendingClickIntent = PendingIntent.getActivity(this, 1, clickIntent, 0);

        String text;
        Firewall.FirewallState state = firewall.getFirewallState();

        switch (state) {
            case RUNNING:
                text = getString(R.string.firewall_service_notification_message__firewall_enabled);
                break;
            case STOPPED:
                text = getString(R.string.firewall_service_notification_message__firewall_disabled);
                break;
            case PAUSED:
                text = getString(R.string.firewall_service_notification_message__firewall_paused);
                break;
            default:
                throw new RuntimeException("Implementation of case for state missing: " + state);
        }

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.firewall_service_notification_title))
                .setContentText(text)
                .setSmallIcon(R.mipmap.firewall_launcher)
                .setContentIntent(pendingClickIntent)
                .build();

        stopForeground(true);
        startForeground(DiscoWallConstants.NotificationIDs.firewallService, notification);
    }

}
