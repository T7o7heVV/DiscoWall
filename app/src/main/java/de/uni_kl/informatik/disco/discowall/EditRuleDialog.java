package de.uni_kl.informatik.disco.discowall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;


public class EditRuleDialog extends DialogFragment {
    private static final String LOG_TAG = EditRuleDialog.class.getSimpleName();

    public static interface DialogListener {
        void onAcceptChanges(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup);
        void onDiscardChanges(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup);

        void onBeforeRuleChangesSaved(FirewallRules.IFirewallRule rule, AppUidGroup appUidGroup);
    }

    // In order to preserve the currently displayed data across dialog-instances (changing the orientation kills the instance),
    // I simply use static attributes, as storing those data-types would create a huge overhead.
    private static FirewallRules.IFirewallRule rule;
    private static AppUidGroup appUidGroup;
    private static DialogListener dialogListener;

    // Dialog:
    private Button buttonOK;
    private Button buttonCancel;

    // All Rules:
    private ImageView ruleImageView;
    private CheckBox checkboxDeviceFilterWlan;
    private CheckBox checkboxDeviceFilterUmts;
    private CheckBox checkboxProtocolFilterTcp;
    private CheckBox checkboxProtocolFilterUdp;
    private EditText textViewClientIp;
    private EditText textViewClientPort;
    private EditText textViewServerIp;
    private EditText textViewServerPort;

    // Policy Rules only:
    private RadioButton radioButtonPolicyInteractive;
    private RadioButton radioButtonPolicyAccept;
    private RadioButton radioButtonPolicyBlock;

    // Recirection Rules only:
    private EditText textViewRedirectHostIp;
    private EditText textViewRedirectHostPort;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        Bundle bundle = savedInstanceState;
        if (bundle == null)
            bundle = getArguments();

        //------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        //  Dialog View configuration:
        //------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        // Set orientation to portrait mode, so that the dialog can actually be used. ^^
        Log.v(LOG_TAG, "fixing orientation to SCREEN_ORIENTATION_PORTRAIT (will be restored to default on dismiss)...");
        EditRuleDialog.this.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View layoutView = inflater.inflate(R.layout.dialog_edit_rule, null);
        builder.setView(layoutView);

        buttonOK = (Button) layoutView.findViewById(R.id.button_ok);
        buttonCancel = (Button) layoutView.findViewById(R.id.button_cancel);

        // Button-Events:
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogListener.onBeforeRuleChangesSaved(rule, appUidGroup); // so that one can react to the changes within the rule (used to delete rule from iptables before changing)
                saveRuleDataFromGui();
                EditRuleDialog.this.dismiss();
                dialogListener.onAcceptChanges(rule, appUidGroup);
            }
        });
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performCancelAction();
            }
        });

        //------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        //  View - fetch widget references:
        //------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        // Device- & Protocol-Filter:
        checkboxDeviceFilterWlan = (CheckBox) layoutView.findViewById(R.id.checkBox_rule_device_wlan);
        checkboxDeviceFilterUmts = (CheckBox) layoutView.findViewById(R.id.checkBox_rule_device_3g_4g);
        checkboxProtocolFilterTcp = (CheckBox) layoutView.findViewById(R.id.checkBox_rule_protocol_tcp);
        checkboxProtocolFilterUdp = (CheckBox) layoutView.findViewById(R.id.checkBox_rule_protocol_udp);

        // Connection-Filter:
        textViewClientIp = (EditText) layoutView.findViewById(R.id.editText_client_ip);
        textViewClientPort = (EditText) layoutView.findViewById(R.id.editText_client_port);
        textViewServerIp = (EditText) layoutView.findViewById(R.id.editText_server_ip);
        textViewServerPort = (EditText) layoutView.findViewById(R.id.editText_server_port);


        //------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        //  Widgets (all rules) - attach listeners:
        //------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        // Device Filter Checkboxes:
        CompoundButton.OnCheckedChangeListener deviceFilterCheckboxListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!checkboxDeviceFilterUmts.isChecked() && !checkboxDeviceFilterWlan.isChecked()) {
                    Toast.makeText(getActivity(), R.string.message_interfaces_at_least_one_required, Toast.LENGTH_SHORT).show();
                    buttonView.setChecked(true); // re-check checkbox, so that at least one is checked.
                }
            }
        };
        checkboxDeviceFilterWlan.setOnCheckedChangeListener(deviceFilterCheckboxListener);
        checkboxDeviceFilterUmts.setOnCheckedChangeListener(deviceFilterCheckboxListener);

        // Protocol Filter Checkboxes:
        CompoundButton.OnCheckedChangeListener protocolFilterCheckboxListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!checkboxProtocolFilterTcp.isChecked() && !checkboxProtocolFilterUdp.isChecked()) {
                    Toast.makeText(getActivity(), R.string.message_protocol_at_least_one_required, Toast.LENGTH_SHORT).show();
                    buttonView.setChecked(true); // re-check checkbox, so that at least one is checked.
                }
            }
        };
        checkboxProtocolFilterTcp.setOnCheckedChangeListener(protocolFilterCheckboxListener);
        checkboxProtocolFilterUdp.setOnCheckedChangeListener(protocolFilterCheckboxListener);

        //------------------------------------------------------------------------------------------------------------------------------------------------------------------------
        //  Dialog - fill in rue-dat:
        //------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        // App Information
        ((TextView) layoutView.findViewById(R.id.textView_app_name)).setText(appUidGroup.getName());
        ((TextView) layoutView.findViewById(R.id.textView_app_package)).setText(appUidGroup.getPackageName());
        ((ImageView) layoutView.findViewById(R.id.imageView_app_icon)).setImageDrawable(appUidGroup.getIcon());

        // Rule Information - Generic Rules:
        writeIpPortInfoToGui(rule.getLocalFilter(), textViewClientIp, textViewClientPort);
        writeIpPortInfoToGui(rule.getRemoteFilter(), textViewServerIp, textViewServerPort);

        checkboxDeviceFilterWlan.setChecked(rule.getDeviceFilter().allowsWifi());
        checkboxDeviceFilterUmts.setChecked(rule.getDeviceFilter().allowsUmts());
        checkboxProtocolFilterTcp.setChecked(rule.getProtocolFilter().isTcp());
        checkboxProtocolFilterUdp.setChecked(rule.getProtocolFilter().isUdp());
        ruleImageView = (ImageView) layoutView.findViewById(R.id.imageView_rule_icon);


        // Handle PolicyRules:
        if (rule instanceof FirewallRules.IFirewallPolicyRule) {
            FirewallRules.IFirewallPolicyRule policyRule = (FirewallRules.IFirewallPolicyRule) rule;

            // Update Dialog-Title:
            builder.setTitle("Edit Policy Rule");

            // show rule-icon according to policy
            ruleImageView.setImageDrawable(getPolicyImage(policyRule.getRulePolicy()));

            // Inflating the viewStub which holds the policy-widgets. These are only visible by doing so - otherwise the policy-widgets are not part of the view.
            View policyLayout = ((ViewStub) layoutView.findViewById(R.id.stub__dialog_edit_rule_policy_config)).inflate();

            // fetch references:
            radioButtonPolicyInteractive = (RadioButton) policyLayout.findViewById(R.id.radioButton_rule_policy_interactive);
            radioButtonPolicyAccept = (RadioButton) policyLayout.findViewById(R.id.radioButton_rule_policy_accept);
            radioButtonPolicyBlock = (RadioButton) policyLayout.findViewById(R.id.radioButton_rule_policy_block);

            // Set Radio-Button according to policy:
            switch (policyRule.getRulePolicy()) {
                // NOTE: Due to an Android-Bug, updating the imageview from within another widgets event will not work.

                case INTERACTIVE: {
                    radioButtonPolicyInteractive.setChecked(true);
                    break;
                }
                case ALLOW: {
                    radioButtonPolicyAccept.setChecked(true);
                    break;
                }
                case BLOCK: {
                    radioButtonPolicyBlock.setChecked(true);
                    break;
                }
            }
        } else if (rule instanceof FirewallRules.IFirewallRedirectRule) {
            FirewallRules.IFirewallRedirectRule redirectRule = (FirewallRules.IFirewallRedirectRule) rule;

            // Update Dialog-Title:
            builder.setTitle("Edit Redirection-Rule");

            // Show redirect-rule-symbol:
            ruleImageView.setImageDrawable(getResources().getDrawable(R.mipmap.symbol_rule_redirect));

            // Inflate viewStub containing redirection-rule-widgetes
            View redirectionLayout = ((ViewStub) layoutView.findViewById(R.id.stub__dialog_edit_rule_redirection_config)).inflate();

            // fetch references:
            textViewRedirectHostIp = (EditText) redirectionLayout.findViewById(R.id.editText_redirectHost_ip);
            textViewRedirectHostPort = (EditText) redirectionLayout.findViewById(R.id.editText_redirectHost_port);

            // write data to gui:
            writeIpPortInfoToGui(redirectRule.getRedirectionRemoteHost(), textViewRedirectHostIp, textViewRedirectHostPort);

            // listeners for redirection-textviews:
            TextWatcher redirectionHostEditListener = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String ip = (textViewRedirectHostIp.getText() + "").trim();
                    String port = (textViewRedirectHostPort.getText() + "").trim(); // note that the EditText only accepts postive numbers from 0...n

                    boolean noRedirIp = ip.isEmpty(); // no ip
                    boolean noRedirPort = port.isEmpty() || port.equals("0"); // no port, or port is "0"

                    Log.v(LOG_TAG, "Validate redirection host IP+Port: >>" + ip + ":" + port + "<<");

                    if (noRedirIp)
                        Toast.makeText(getActivity(), "ip required", Toast.LENGTH_SHORT).show();

                    if (noRedirPort) {
                        if (port.isEmpty())
                            Toast.makeText(getActivity(), "port required", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(getActivity(), "invalid port: " + port, Toast.LENGTH_SHORT).show();
                    } else {
                        int portInt = Integer.parseInt(port);

                        if (portInt > Packages.IpPortPair.PORT_MAX) {
                            Toast.makeText(getActivity(), "invalid port " + port + " exceeds limit: " + Packages.IpPortPair.PORT_MAX, Toast.LENGTH_SHORT).show();
                            noRedirPort = true;
                        }
                    }

                    // OK only clickable, if valid redirection-target - i.e. IP+PORT
                    buttonOK.setEnabled(!noRedirIp && !noRedirPort);
                }
            };

            // redirection host: associate listeners, so that no empty IP or port is allowed
            textViewRedirectHostIp.addTextChangedListener(redirectionHostEditListener);
            textViewRedirectHostPort.addTextChangedListener(redirectionHostEditListener);
        }

        return builder.create();
    }

    private void performCancelAction() {
        Log.i(LOG_TAG, this.getClass().getSimpleName() + " closed with CANCEL. Rule was: " + rule);
        dismiss(); // close dialog

        dialogListener.onDiscardChanges(rule, appUidGroup);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        performCancelAction();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        // Restore default (unspecified) orientation:
        if (EditRuleDialog.this.getActivity() != null) { // the activity is null, when the calling activity has already called "finish()"
            EditRuleDialog.this.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            Log.v(LOG_TAG, "default-orientation (SCREEN_ORIENTATION_UNSPECIFIED) restored.");
        }
    }

    private Drawable getPolicyImage(FirewallRules.RulePolicy policy) {
        switch (policy) {
            case INTERACTIVE:
                return getResources().getDrawable(R.mipmap.symbol_rule_policy_interactive);
            case ALLOW:
                return getResources().getDrawable(R.mipmap.symbol_rule_policy_allow);
            case BLOCK:
                return getResources().getDrawable(R.mipmap.symbol_rule_policy_block);
            default:
                throw new RuntimeException("Missing image specification for policy: " + policy);
        }
    }

//    private boolean isRuleDataFromGuiValid() {
//        if (textViewClientPort.getText().length() == 0) {
//
//        }
//
//        return true;
//    }

    private Packages.IpPortPair extractIpPortPairFromGui(TextView textViewIp, TextView textViewPort) {
        String ip = textViewIp.getText() + "";
        int port;

        if (textViewPort.getText().length() > 0)
            port = Integer.parseInt(textViewPort.getText() + "");
        else
            port = 0; // i.e. any port

        return new Packages.IpPortPair(ip, port);
    }

    private void saveRuleDataFromGui() {
        try {
            // Source- & Destination-Fitlers:
            rule.setLocalFilter(extractIpPortPairFromGui(textViewClientIp, textViewClientPort));
            rule.setRemoteFilter(extractIpPortPairFromGui(textViewServerIp, textViewServerPort));

            // Interface- & Protocol-Filters:
            rule.setDeviceFilter(FirewallRules.DeviceFilter.construct(checkboxDeviceFilterWlan.isChecked(), checkboxDeviceFilterUmts.isChecked()));
            rule.setProtocolFilter(FirewallRules.ProtocolFilter.construct(checkboxProtocolFilterTcp.isChecked(), checkboxProtocolFilterUdp.isChecked()));

            // Specific Rule-Kinds:
            if (rule instanceof FirewallRules.IFirewallPolicyRule) { // policy rules:
                FirewallRules.IFirewallPolicyRule policyRule = (FirewallRules.IFirewallPolicyRule) rule;

                // Rule-Policy:
                if (radioButtonPolicyAccept.isChecked())
                    policyRule.setRulePolicy(FirewallRules.RulePolicy.ALLOW);
                else if (radioButtonPolicyBlock.isChecked())
                    policyRule.setRulePolicy(FirewallRules.RulePolicy.BLOCK);
                else if (radioButtonPolicyInteractive.isChecked())
                    policyRule.setRulePolicy(FirewallRules.RulePolicy.INTERACTIVE);
            } else if (rule instanceof FirewallRules.IFirewallRedirectRule) { // redirection rules:
                FirewallRules.IFirewallRedirectRule redirectionRule = (FirewallRules.IFirewallRedirectRule) rule;

                // Redirection-Host:
                redirectionRule.setRedirectionRemoteHost(extractIpPortPairFromGui(textViewRedirectHostIp, textViewRedirectHostPort));
            }
        } catch(Exception e) {
            Log.e(LOG_TAG, "Invalid rule-specification caused error: " + e.getMessage(), e);
            Toast.makeText(getActivity(), "Invalid rule-specification caused error: " + e.getMessage(), Toast.LENGTH_LONG).show();
//            ErrorDialog.showError(getActivity(), "Invalid rule-specification caused error: " + e.getMessage(), e); // will be closed as the parent activity refreshes
        }
    }

    private void writeIpPortInfoToGui(Packages.IpPortPair ipPortInfo, TextView targetViewIp, TextView targetViewPort) {
        // IP
        if (ipPortInfo.getIp().length() == 0)
            targetViewIp.setText("*");
        else
            targetViewIp.setText(ipPortInfo.getIp());

        // Port
        if (ipPortInfo.getPort() > 0)
            targetViewPort.setText(ipPortInfo.getPort() + "");
        else
            targetViewPort.setText("0"); // no negative numbers - "any port" is simply the port 0
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (outState != null && getArguments() != null)
            outState.putAll(getArguments());
    }

    public static EditRuleDialog show(Activity context, DialogListener dialogListener, AppUidGroup appUidGroup, FirewallRules.IFirewallRule rule) {
        EditRuleDialog dialog = new EditRuleDialog();

        // Supplying the arguments per object-attribute:
        dialog.rule = rule;
        dialog.appUidGroup = appUidGroup;
        dialog.dialogListener = dialogListener;

        dialog.show(context.getFragmentManager(), "");

        return dialog;
    }
}
