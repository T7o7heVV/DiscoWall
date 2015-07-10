package de.uni_kl.informatik.disco.discowall.gui.handlers;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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
import de.uni_kl.informatik.disco.discowall.gui.DiscoWallAppAdapter;
import de.uni_kl.informatik.disco.discowall.gui.dialogs.ErrorDialog;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.gui.AppAdapter;

public class MainActivityGuiHandlerFirewallControl {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private final MainActivity mainActivity;

    public MainActivityGuiHandlerFirewallControl(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
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
        progressDialog.setCancelable(false);

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

}
