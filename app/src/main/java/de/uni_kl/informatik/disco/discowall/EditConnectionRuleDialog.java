package de.uni_kl.informatik.disco.discowall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.gui.dialogs.ErrorDialog;
import de.uni_kl.informatik.disco.discowall.packages.Packages;


public class EditConnectionRuleDialog extends DialogFragment {
    private static final String LOG_TAG = EditConnectionRuleDialog.class.getSimpleName();

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        // Inflate the layout to use as dialog or embedded fragment
//        return inflater.inflate(R.layout.dialog_edit_connection_rule, container, false);
//    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        Bundle bundle = savedInstanceState;
        if (bundle == null)
            bundle = getArguments();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View layoutView = inflater.inflate(R.layout.dialog_edit_connection_rule, null);
        builder.setView(layoutView)
                .setTitle("Edit Connection Rule");

        // Fetching ApplicationInfo:
        Activity context = getActivity();
        PackageManager packageManager = context.getPackageManager();
        String packageName = bundle.getString("app.packageName");
        ApplicationInfo appInfo;

        try {
            appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Error fetching ApplicationInfo for app with packageName: " + packageName, e);
            ErrorDialog.showError(context, "Error fetching ApplicationInfo for app with packageName: " + packageName, e);

            return builder.create();
        }

        // App Information
        ((TextView) layoutView.findViewById(R.id.dialog_edit_connection_app_name)).setText(appInfo.loadLabel(packageManager));
        ((TextView) layoutView.findViewById(R.id.dialog_edit_connection_app_package)).setText(packageName);
        ((ImageView) layoutView.findViewById(R.id.dialog_edit_connection_app_icon)).setImageDrawable(appInfo.loadIcon(packageManager));

        // Rule Information:
        ((RadioButton) layoutView.findViewById(R.id.dialog_edit_connection_radio_button_rule_policy_accept)).setChecked(true);

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putAll(getArguments());
    }

    public static EditConnectionRuleDialog show(Activity context, String dialogTag, ApplicationInfo appInfo, Packages.IpPortPair client, Packages.IpPortPair server, FirewallRules.RulePolicy policy) {
        final PackageManager packageManager = context.getPackageManager();
        Bundle args = new Bundle();

        args.putString("rule.client.ip", client.getIp());
        args.putInt("rule.client.port", client.getPort());

        args.putString("rule.server.ip", server.getIp());
        args.putInt("rule.server.port", server.getPort());

        args.putSerializable("rule.policy", policy);

        // Dialog-Infos:
        args.putString("app.packageName", appInfo.packageName);

        EditConnectionRuleDialog dialog = new EditConnectionRuleDialog();
        dialog.setArguments(args);
        dialog.show(context.getFragmentManager(), dialogTag);

        return dialog;
    }
}
