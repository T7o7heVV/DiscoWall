package de.uni_kl.informatik.disco.discowall.gui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.uni_kl.informatik.disco.discowall.EditConnectionRuleDialog;
import de.uni_kl.informatik.disco.discowall.MainActivity;
import de.uni_kl.informatik.disco.discowall.R;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.helpers.WatchedAppsPreferencesManager;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRulesManager;
import de.uni_kl.informatik.disco.discowall.gui.dialogs.ErrorDialog;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.gui.AppAdapter;

public class MainActivityGuiHandlers {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private final MainActivity mainActivity;
    private DiscoWallAppAdapter watchedAppsListAdapter;

    public MainActivityGuiHandlers(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void setupWatchedAppsList() {
        // Fetch ListView for watchedApps and create adapter
        ListView appsListView = (ListView) mainActivity.findViewById(R.id.listViewFirewallMonitoredApps);
        final DiscoWallAppAdapter appsAdapter = new DiscoWallAppAdapter(mainActivity);
        appsListView.setAdapter(appsAdapter);

        // Storing reference, so that the list can be updated when enabling/disabling the firewall
        watchedAppsListAdapter = appsAdapter;

        // Adapter-Handler for manipulating list-view while it is being created etc.
        appsAdapter.setAdapterHandler(new AppAdapter.AdapterHandler() {
            @Override
            public void onRowCreate(AppAdapter adapter, ApplicationInfo appInfo, TextView appNameWidget, TextView appPackageNameWidget, ImageView appIconImageWidget, CheckBox appWatchedCheckboxWidget) {
                /* This method is being called when...
                 * - the individual rows are being written when creating the list
                 * - the list is being scrolled and therefore updated
                 */

                // IMPORTANT: If I would buffer the "watched apps" at any point, scrolling the list will reset the value to the buffered state!
                appWatchedCheckboxWidget.setChecked(mainActivity.firewall.isAppWatched(appInfo));
            }

            @Override
            public void onAppNameClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appNameWidgetview) {
                actionWatchedAppShowFirewallRules(appInfo);
            }

            @Override
            public void onAppPackageClicked(AppAdapter appAdapter, ApplicationInfo appInfo, TextView appPackageNameWidget) {
                actionWatchedAppShowFirewallRules(appInfo);
            }

            @Override
            public void onAppIconClicked(AppAdapter appAdapter, ApplicationInfo appInfo, ImageView appIconImageWidget) {
                actionWatchedAppShowFirewallRules(appInfo);
            }

            @Override
            public void onAppWatchedStateCheckboxCheckedChanged(AppAdapter adapter, ApplicationInfo appInfo, CheckBox appWatchedCheckboxWidget, boolean isChecked) {
                actionSetAppWatched(appInfo, appWatchedCheckboxWidget.isChecked());
            }
        });
    }

    private void actionWatchedAppShowFirewallRules(ApplicationInfo appInfo) {
        EditConnectionRuleDialog.show(mainActivity, "example tag", appInfo, new Packages.IpPortPair("192.168.178.100", 1337), new Packages.IpPortPair("192.168.178.200", 4200), FirewallRules.RulePolicy.ACCEPT);
    }

    public void actionSetAllAppsWatched(boolean watched) {
        Log.i(LOG_TAG, "set " +(watched ? "all":"no") + " apps to be watched by firewall...");

        HashMap<ApplicationInfo, Boolean> appsToWatchedStateMap = new HashMap<>();

        for(ApplicationInfo appInfo : mainActivity.firewall.getWatchableApps())
            appsToWatchedStateMap.put(appInfo, watched);

        setAppsWatched(appsToWatchedStateMap, watched ? R.string.action_main_menu_monitor_all_apps : R.string.action_main_menu_monitor_no_apps);
    }

    public void actionInvertAllAppsWatched() {
        Log.i(LOG_TAG, "invert apps to be watched by firewall...");

        List<ApplicationInfo> watchedApps = mainActivity.firewall.getWatchedApps();

        HashMap<ApplicationInfo, Boolean> appsToWatchedStateMap = new HashMap<>();

        for(ApplicationInfo appInfo : mainActivity.firewall.getWatchableApps())
            appsToWatchedStateMap.put(appInfo, !mainActivity.firewall.isAppWatched(appInfo));

        setAppsWatched(appsToWatchedStateMap, R.string.action_main_menu_monitor_invert_monitored);
    }

    private void setAppsWatched(final HashMap<ApplicationInfo, Boolean> appsToWatchedStateMap, final int updateDialogTitleStringRessourceId) {
        // Since this operation might take up to a minute on slow devides ==> run with progress-bar etc..
        new AsyncTask<List<ApplicationInfo>, Integer, Boolean>() {
            private String errorMessage;
            private ProgressDialog progressDialog;
            private LinkedList<ApplicationInfo> apps = new LinkedList<ApplicationInfo>(appsToWatchedStateMap.keySet()); // a list, so that I can iterate over it and use integers to show progress

            @Override
            protected Boolean doInBackground(List<ApplicationInfo>... params) {
                int i=0;

                for(ApplicationInfo appInfo : apps) {
                    publishProgress(i++);
                    boolean watchApp = appsToWatchedStateMap.get(appInfo);

                    try {
                        if (mainActivity.firewall.isAppWatched(appInfo) != watchApp)
                            mainActivity.firewall.setAppWatched(appInfo, watchApp);
                    } catch(FirewallExceptions.FirewallException e) {
                        if (!errorMessage.isEmpty())
                            errorMessage += "\n";
                        errorMessage += "Error changing watched-state for app '" + appInfo.packageName + "': " + e.getMessage();

                        Log.e(LOG_TAG, "Error changing watched-state for app '" + appInfo.packageName + "': " + e.getMessage(), e);
                    }
                }

                return true;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                progressDialog = new ProgressDialog(mainActivity);

                /* IMPORTANT:
                 * The design of the progress-dialog has to be defined BEFORE showing.
                 * ==> Any Message, Title and Icon have to be defined so that the dialog can be updated with new information after it's visible.
                 *     If the dialog is shown without an icon/title/message, it cannot show one later on.
                 */

                progressDialog.setTitle(updateDialogTitleStringRessourceId);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMessage("");
                progressDialog.setIcon(apps.get(0).loadIcon(mainActivity.getPackageManager()));
                progressDialog.setMax(appsToWatchedStateMap.size());
                progressDialog.setProgress(0);

                progressDialog.show();
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                progressDialog.setProgress(progressDialog.getMax());
                progressDialog.dismiss();

                // So that the checkboxes for watched-state are updated
//                watchedAppsListAdapter.notifyDataSetChanged(); // sometimes not working

                // Restart Main-Activity to update gui:
                refreshMainActivity();
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                final PackageManager packageManager = mainActivity.getPackageManager();
                final int appIndex = values[0];

                ApplicationInfo appInfo = apps.get(appIndex);
                String appName = appInfo.loadLabel(packageManager) + "";

                progressDialog.setMessage(appName + "\n" + appInfo.packageName);
                progressDialog.setIcon(appInfo.loadIcon(packageManager));
                progressDialog.setProgress(appIndex);
            }
        }.execute();
    }

    private void actionSetAppWatched(final ApplicationInfo appInfo, final boolean watched) {
        // Nothing to do if firewall not yet initialized.
//        // These calls happen while the app is starting. The apps-list is being refreshed multiple times while the first calls (as firewall==null) are redundant and can be ignored.
//        if (mainActivity.firewall == null)
//            return;

        // Nothing to do, if the desired watched-state is already present. This happens when the GUI refreshes (as it does on scrolling).
        if (watched == mainActivity.firewall.isAppWatched(appInfo)) {
            Log.v(LOG_TAG, "App already " + (watched?"":"not ") + "watched. No change in sate required. This call happens when creating the list.");
            return;
        }

        // Show toast as this operation takes a few seconds
        if (watched)
            Toast.makeText(mainActivity, "monitor app traffic: " + appInfo.loadLabel(mainActivity.getPackageManager()), Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(mainActivity, "ignore app traffic: " + appInfo.loadLabel(mainActivity.getPackageManager()), Toast.LENGTH_SHORT).show();

        // Since this operation takes around a second, it is anoying that the GUI freezes for this amount of time ==> parallel task
        new AsyncTask<Boolean, Boolean, Boolean>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground(Boolean... params) {
                try {
                    mainActivity.firewall.setAppWatched(appInfo, watched);
                } catch (FirewallExceptions.FirewallException e) {
                    errorMessage = "Error changing watched-state for app '" + appInfo.packageName + "': " + e.getMessage();
                }

                return true;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);

                if (errorMessage != null)
                    ErrorDialog.showError(mainActivity, "App-Watch", errorMessage);
            }
        }.execute();
    }

    public void onFirewallSwitchCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
        actionSetFirewallEnabled(isChecked, true);
    }

    public void actionSetFirewallEnabled(final boolean enabled, boolean showToastIfAlreadyEnabled) {
        try {
            if (enabled && mainActivity.firewall.isFirewallRunning()) {
                if (showToastIfAlreadyEnabled)
                    Toast.makeText(mainActivity, "Firewall already enabled.", Toast.LENGTH_SHORT).show();

                return;
            } else if (!enabled && !mainActivity.firewall.isFirewallRunning()) {
                if (showToastIfAlreadyEnabled)
                    Toast.makeText(mainActivity, "Firewall already disabled.", Toast.LENGTH_SHORT).show();

                return;
            }
        } catch (Exception e) {
            new AlertDialog.Builder(mainActivity)
                    .setTitle("Firewall ERROR")
                    .setMessage("Firewall could not fetch state due to error: " + e.getMessage())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            e.printStackTrace();

            return;
        }

        String actionName, message;
        if (enabled) {
            actionName = "Enabling Firewall";
            message = "adding iptable rules...";
        } else {
            actionName = "Disabling Firewall";
            message = "removing iptable rules...";
        }

        // Creating "busy dialog" (will be shown before async-task is being started)
        final ProgressDialog progressDialog = new ProgressDialog(mainActivity);
        progressDialog.setTitle(actionName);
        progressDialog.setIcon(R.drawable.firewall_launcher);
        progressDialog.setMessage(message);

        class FirewallSetupTask extends AsyncTask<Boolean, Boolean, Boolean> {
            private AlertDialog.Builder errorAlert;

            @Override
            protected Boolean doInBackground(Boolean... params) {
                if (enabled) {
                    try {
                        mainActivity.firewall.enableFirewall(mainActivity.discowallSettings.getFirewallPort(mainActivity));
                    } catch (Exception e) {
                        errorAlert = new AlertDialog.Builder(mainActivity)
                                .setTitle("Firewall ERROR")
                                .setMessage("Firewall could not start due to error: " + e.getMessage())
                                .setIcon(android.R.drawable.ic_dialog_alert);
                        e.printStackTrace();
                    }
                } else {
                    try {
                        mainActivity.firewall.disableFirewall();
                    } catch (Exception e) {
                        errorAlert = new AlertDialog.Builder(mainActivity)
                                .setTitle("Firewall ERROR")
                                .setMessage("Firewall could not stop correctly due to error: " + e.getMessage())
                                .setIcon(android.R.drawable.ic_dialog_alert);
                        e.printStackTrace();
                    }
                }

                return null;
            }

            protected void onPostExecute(Boolean result) {
                if (errorAlert != null) {
                    errorAlert.show();
                    return;
                }

                if (enabled)
                    Toast.makeText(mainActivity, "Firewall Enabled.", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(mainActivity, "Firewall Disabled.", Toast.LENGTH_LONG).show();

                // If this action for changing the firewall-state has been called by code only
                // (i.e. not as reaction to the user changing the state via gui),
                // then the firewall-switch will differ from the firewall state.
                // (This happens as the firewall-state is restored on firewall-app-launch)
                // Therefore the switch is set here:
                ((Switch) mainActivity.findViewById(R.id.switchFirewallEnabled)).setChecked(enabled);

                // Gui-Update actions etc.
                onAfterFirewallEnabledStateChanged(enabled);

                // Hide Busy-Dialog
                progressDialog.dismiss();
            }
        }

        // Store enabled/disabled state in settings, so that it can be restored on app-start
        mainActivity.discowallSettings.setFirewallEnabled(mainActivity, enabled);

        progressDialog.show();
        new FirewallSetupTask().execute();
    }

    /**
     * Is being called after the firewall has been enabled or disabled.
     *
     * @param firewallEnabled
     */
    private void onAfterFirewallEnabledStateChanged(boolean firewallEnabled) {
        Log.v(LOG_TAG, "Firewall enabled-state changed.");

        // Select RadioButton matching current Firewall Policy and Enable/Disable RadioButtons
        updateFirewallPolicyRadioButtonsWithCurrentPolicy();
    }

    public void showFirewallEnabledState() {
        Switch firewallEnabledSwitch = (Switch) mainActivity.findViewById(R.id.switchFirewallEnabled);
        try {
            firewallEnabledSwitch.setChecked(mainActivity.firewall.isFirewallRunning());
        } catch (Exception e) {
            new AlertDialog.Builder(mainActivity)
                    .setTitle("Firewall ERROR")
                    .setMessage("Firewall determine firewall state: " + e.getMessage())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            e.printStackTrace();

            firewallEnabledSwitch.setChecked(false); // assuming firewall is NOT running
        }
    }

    public void updateFirewallPolicyRadioButtonsWithCurrentPolicy() {
        RadioButton buttonAllow = (RadioButton) mainActivity.findViewById(R.id.radioButtonFirewallModeAllow);
        RadioButton buttonBlock = (RadioButton) mainActivity.findViewById(R.id.radioButtonFirewallModeBlock);
        RadioButton buttonInteractive = (RadioButton) mainActivity.findViewById(R.id.radioButtonFirewallModeInteractive);

        boolean firewallRunning = mainActivity.firewall.isFirewallRunning();

        // Buttons can only be used, if firewall is running
        buttonAllow.setEnabled(firewallRunning);
        buttonBlock.setEnabled(firewallRunning);
        buttonInteractive.setEnabled(firewallRunning);

        if (firewallRunning) {
            switch (mainActivity.firewall.getFirewallPolicy()) {
                case ALLOW:
                    buttonAllow.setChecked(true);
                    break;
                case BLOCK:
                    buttonBlock.setChecked(true);
                    break;
                case INTERACTIVE:
                    buttonInteractive.setChecked(true);
                    break;
            }
        }
    }

    public void setupFirewallPolicyRadioButtons() {
        // Bind on-check events:
        setupFirewallPolicyRadioButton((RadioButton) mainActivity.findViewById(R.id.radioButtonFirewallModeAllow), FirewallRulesManager.FirewallPolicy.ALLOW);
        setupFirewallPolicyRadioButton((RadioButton) mainActivity.findViewById(R.id.radioButtonFirewallModeBlock), FirewallRulesManager.FirewallPolicy.BLOCK);
        setupFirewallPolicyRadioButton((RadioButton) mainActivity.findViewById(R.id.radioButtonFirewallModeInteractive), FirewallRulesManager.FirewallPolicy.INTERACTIVE);
    }

    private void setupFirewallPolicyRadioButton(RadioButton button, final FirewallRulesManager.FirewallPolicy associatedFirewallPolicy) {
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) // This method is also being called on the "un-check" event
                    return;

                // No policy-update required if the current-policy is the same as the requested one:
                if (mainActivity.firewall.getFirewallPolicy() == associatedFirewallPolicy)
                    return;

                Log.v(LOG_TAG, "Change firewall-policy to " + associatedFirewallPolicy);

                try {
                    mainActivity.firewall.setFirewallPolicy(associatedFirewallPolicy);
                } catch (FirewallExceptions.FirewallException e) {
                    ErrorDialog.showError(mainActivity, "Firewall Policy", "Error changing policy: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void refreshMainActivity() {
        // == refreshing the MainActivity be closing and reopening the activity ==

        mainActivity.finish();
        mainActivity.overridePendingTransition(0, 0); // disabling slide-animation on Activity-finish

        mainActivity.startActivity(mainActivity.getIntent());
    }
}
