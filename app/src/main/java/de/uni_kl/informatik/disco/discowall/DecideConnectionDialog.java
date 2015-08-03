package de.uni_kl.informatik.disco.discowall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.gui.dialogs.ErrorDialog;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;


public class DecideConnectionDialog extends DialogFragment {
    public static class AppConnectionDecision {
        public final boolean allowConnection, createRule, applyForAllNewConnectionsOfThisApp;

        public AppConnectionDecision(boolean allowConnection, boolean createRule, boolean applyForAllNewConnectionsOfThisApp) {
            this.allowConnection = allowConnection;
            this.createRule = createRule;
            this.applyForAllNewConnectionsOfThisApp = applyForAllNewConnectionsOfThisApp;
        }
    }

    public static interface DecideConnectionDialogListener {
        void onConnectionDecided(ApplicationInfo appInfo, Packages.IpPortPair source, Packages.IpPortPair destination, AppConnectionDecision decision);
    }

    private static final String LOG_TAG = DecideConnectionDialog.class.getSimpleName();

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
        View layoutView = inflater.inflate(R.layout.dialog_decide_connection, null);
        builder.setView(layoutView)
                .setTitle(R.string.decide_connection_dialog__title)
                .setCancelable(false);

        // DEBUG!!!
        if (true) return builder.create(); // TODO REMOVE!

        // Fetching ApplicationInfo:
        final Activity context = getActivity();
        PackageManager packageManager = context.getPackageManager();
        String packageName = bundle.getString("app.packageName");
        final ApplicationInfo appInfo;

        try {
            appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Error fetching ApplicationInfo for app with packageName: " + packageName, e);
            ErrorDialog.showError(context, "Error fetching ApplicationInfo for app with packageName: " + packageName, e);

            return builder.create();
        }

        final String clientIp = bundle.getString("client.ip");
        final String serverIp = bundle.getString("server.ip");
        final int clientPort = bundle.getInt("client.port");
        final int serverPort = bundle.getInt("server.port");

        // App Information
        ((TextView) layoutView.findViewById(R.id.textView_app_name)).setText(appInfo.loadLabel(packageManager));
        ((TextView) layoutView.findViewById(R.id.textView_app_package)).setText(packageName);
        ((ImageView) layoutView.findViewById(R.id.imageView_app_icon)).setImageDrawable(appInfo.loadIcon(packageManager));

        // Connection - fill in data:
        ((EditText) layoutView.findViewById(R.id.editText_client_ip)).setText(clientIp);
        ((EditText) layoutView.findViewById(R.id.editText_client_port)).setText(clientPort + ""); // IMPORTANT: without the cast to string, the port-integer is being used as view-ID
        ((EditText) layoutView.findViewById(R.id.editText_server_ip)).setText(serverIp);
        ((EditText) layoutView.findViewById(R.id.editText_server_port)).setText(serverPort + ""); // ""

        // Connection Protocol:
        Packages.TransportLayerProtocol protocol = Packages.TransportLayerProtocol.valueOf(bundle.getString("connection.protocol"));
        switch(protocol) {
            case TCP:
                ((RadioButton) layoutView.findViewById(R.id.radioButton_protocol_tcp)).setChecked(true);
                break;
            case UDP:
                ((RadioButton) layoutView.findViewById(R.id.radioButton_protocol_udp)).setChecked(true);
                break;
        }

        // Make Widgets readonly:
        ((EditText) layoutView.findViewById(R.id.editText_client_ip)).setKeyListener(null);
        ((EditText) layoutView.findViewById(R.id.editText_client_port)).setKeyListener(null);
        ((EditText) layoutView.findViewById(R.id.editText_server_ip)).setKeyListener(null);
        ((EditText) layoutView.findViewById(R.id.editText_server_port)).setKeyListener(null);
        ((RadioButton) layoutView.findViewById(R.id.radioButton_protocol_tcp)).setKeyListener(null);
        ((RadioButton) layoutView.findViewById(R.id.radioButton_protocol_udp)).setKeyListener(null);

        // Checkbox: Create Rule for this connection yes/no:
        final CheckBox checkBoxCreateRules = (CheckBox) layoutView.findViewById(R.id.checkBox_create_rule);
        checkBoxCreateRules.setChecked(bundle.getBoolean("action.createRule"));

        // Checkbox: Apply decision for all new connections of this app:
        final CheckBox checkBoxApplyForAllNewConnectionsOfThisApp = (CheckBox) layoutView.findViewById(R.id.checkBox_apply_for_all_new_connections_from_this_app);

        // IMPORTANT: Dialog has always to be called from an Activity which implements this Interface
        if (! (context instanceof DecideConnectionDialogListener))
            throw new ClassCastException("Starting-Activity must implement interface " + DecideConnectionDialogListener.class.getCanonicalName());
        final DecideConnectionDialogListener dialogListener = (DecideConnectionDialogListener) context;

        // Buttons, add Click-Events:
        layoutView.findViewById(R.id.button_accept).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DiscoWallSettings.getInstance().setHandleConnectionDialogDefaultCreateRule(context, checkBoxCreateRules.isChecked()); // store last checked-state of "create-rule checkbox"

                        dialogListener.onConnectionDecided(
                                appInfo,
                                new Packages.IpPortPair(clientIp, clientPort),
                                new Packages.IpPortPair(serverIp, serverPort),
                                new AppConnectionDecision(true, checkBoxCreateRules.isChecked(), checkBoxApplyForAllNewConnectionsOfThisApp.isChecked())
                        );

                        DecideConnectionDialog.this.dismiss(); // close dialog
                    }
                }
        );
        layoutView.findViewById(R.id.button_block).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DiscoWallSettings.getInstance().setHandleConnectionDialogDefaultCreateRule(context, checkBoxCreateRules.isChecked()); // store last checked-state of "create-rule checkbox"

                        dialogListener.onConnectionDecided(
                                appInfo,
                                new Packages.IpPortPair(clientIp, clientPort),
                                new Packages.IpPortPair(serverIp, serverPort),
                                new AppConnectionDecision(false, checkBoxCreateRules.isChecked(), checkBoxApplyForAllNewConnectionsOfThisApp.isChecked())
                        );

                        DecideConnectionDialog.this.dismiss(); // close dialog
                    }
                }
        );

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putAll(getArguments());
    }

    public static DecideConnectionDialog show(Activity context, Connections.Connection connection, AppUidGroup appUidGroup) {
        if (! (context instanceof DecideConnectionDialogListener)) {
            Log.e(LOG_TAG, "Starting-Activity must implement interface " + DecideConnectionDialogListener.class.getCanonicalName());
            throw new ClassCastException("Starting-Activity must implement interface " + DecideConnectionDialogListener.class.getCanonicalName());
        }

        final PackageManager packageManager = context.getPackageManager();
        Bundle args = new Bundle();

//        args.putString("client.ip", connection.getSource().getIp());
//        args.putInt("client.port", connection.getSource().getPort());
//        args.putString("server.ip", connection.getDestination().getIp());
//        args.putInt("server.port", connection.getDestination().getPort());
//
//        args.putString("connection.protocol", connection.getTransportLayerProtocol().toString());
//
//        // Dialog-Infos:
//        args.putBoolean("action.createRule", DiscoWallSettings.getInstance().isHandleConnectionDialogDefaultCreateRule(context));
//        args.putString("app.packageName", appUidGroup.getPackageName());

        DecideConnectionDialog dialog = new DecideConnectionDialog();
        dialog.setArguments(args);
        dialog.show(context.getFragmentManager(), "");

        return dialog;
    }

}
