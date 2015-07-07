package de.uni_kl.informatik.disco.discowall.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import de.uni_kl.informatik.disco.discowall.firewallService.FirewallService;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;

public class AndroidBootCompletedReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = AndroidBootCompletedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
//        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) // unnecessary check, as this broadcast-receiver is only called by this intent, as specified in manifest
//            return;

        Log.d(LOG_TAG, "DiscoWall autostart called.");

        if (!DiscoWallSettings.getInstance().isAutostartFirewallService(context)) {
            Log.i(LOG_TAG, "Discowall Autostart: service autostart disabled, nothing to do.");
        }

        if (DiscoWallSettings.getInstance().isFirewallEnabled(context)) {
            Log.i(LOG_TAG, "Discowall Autostart: starting firewall service...");
            FirewallService.startFirewallService(context, true);
        } else {
            Log.i(LOG_TAG, "Discowall Autostart: Firewall disabled, nothing to do.");
            FirewallService.startFirewallService(context, false);
        }
    }
}
