package de.uni_kl.informatik.disco.discowall.gui.handlers;

import android.util.Log;

import de.uni_kl.informatik.disco.discowall.MainActivity;
import de.uni_kl.informatik.disco.discowall.gui.DiscoWallAppAdapter;

abstract class MainActivityGuiHandler {
    protected static final String LOG_TAG = MainActivity.class.getSimpleName();

    protected final MainActivity mainActivity;

    public MainActivityGuiHandler(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    /**
     * Is being called after the firewall has been enabled or disabled.
     *
     * @param firewallEnabled
     */
    protected abstract void onAfterFirewallEnabledStateChanged(boolean firewallEnabled);

    protected void refreshMainActivity() {
        // == refreshing the MainActivity be closing and reopening the activity ==

        mainActivity.finish();
        mainActivity.overridePendingTransition(0, 0); // disabling slide-animation on Activity-finish

        mainActivity.startActivity(mainActivity.getIntent());
    }
}
