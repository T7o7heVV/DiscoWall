package de.uni_kl.informatik.disco.discowall.gui.handlers;

import de.uni_kl.informatik.disco.discowall.MainActivity;
import de.uni_kl.informatik.disco.discowall.utils.GuiUtils;

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
        GuiUtils.restartActivity(mainActivity);
   }
}
