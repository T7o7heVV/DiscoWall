package de.uni_kl.informatik.disco.discowall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;


public class DecideConnectionDialog extends DialogFragment {
    private static final String LOG_TAG = DecideConnectionDialog.class.getSimpleName();

    private DecideConnectionDialogListener dialogListener;
    private static boolean dialogOpen = false;

    // Arguments:
    private AppUidGroup appUidGroup;
    private Connections.IConnection connection;
    private Packages.TransportLayerProtocol protocol;

    public static boolean isDialogOpen() {
        return dialogOpen;
    }

    public static class AppConnectionDecision {
        public final boolean allowConnection, createRule, applyForAllNewConnectionsOfThisApp;

        public AppConnectionDecision(boolean allowConnection, boolean createRule, boolean applyForAllNewConnectionsOfThisApp) {
            this.allowConnection = allowConnection;
            this.createRule = createRule;
            this.applyForAllNewConnectionsOfThisApp = applyForAllNewConnectionsOfThisApp;
        }
    }

    public static interface DecideConnectionDialogListener {
        void onConnectionDecided(AppUidGroup appUidGroup, Connections.IConnection connection, AppConnectionDecision decision);
        void onDialogDismissed(AppUidGroup appUidGroup, Connections.IConnection connection);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // so that it can be tracked whether this dialog is currently visible:
        dialogOpen = true;

        // Fetch arguments:
        boolean createRuleChecked = DiscoWallSettings.getInstance().isHandleConnectionDialogDefaultCreateRule(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

//        Bundle bundle = savedInstanceState;
//        if (bundle == null)
//            bundle = getArguments();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View layoutView = inflater.inflate(R.layout.dialog_decide_connection, null);
        builder.setView(layoutView)
                .setTitle(R.string.decide_connection_dialog__title)
                .setCancelable(false);

        // App Information
        ((TextView) layoutView.findViewById(R.id.textView_app_name)).setText(appUidGroup.getName());
        ((TextView) layoutView.findViewById(R.id.textView_app_package)).setText(appUidGroup.getPackageName());
        ((ImageView) layoutView.findViewById(R.id.imageView_app_icon)).setImageDrawable(appUidGroup.getIcon());

        // Connection - fill in data:
        ((EditText) layoutView.findViewById(R.id.editText_client_ip)).setText(connection.getSourceIP());
        ((EditText) layoutView.findViewById(R.id.editText_client_port)).setText(connection.getSourcePort() + ""); // IMPORTANT: without the cast to string, the port-integer is being used as view-ID
        ((EditText) layoutView.findViewById(R.id.editText_server_ip)).setText(connection.getDestinationIP());
        ((EditText) layoutView.findViewById(R.id.editText_server_port)).setText(connection.getDestinationPort() + ""); // ""

        // Connection Protocol:
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
        checkBoxCreateRules.setChecked(createRuleChecked);

        // Checkbox: Apply decision for all new connections of this app:
        final CheckBox checkBoxApplyForAllNewConnectionsOfThisApp = (CheckBox) layoutView.findViewById(R.id.checkBox_apply_for_all_new_connections_from_this_app);

        // Buttons, add Click-Events:
        layoutView.findViewById(R.id.button_accept).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DiscoWallSettings.getInstance().setHandleConnectionDialogDefaultCreateRule(getActivity(), checkBoxCreateRules.isChecked()); // store last checked-state of "create-rule checkbox"

                        dialogListener.onConnectionDecided(
                                appUidGroup,
                                connection,
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
                        DiscoWallSettings.getInstance().setHandleConnectionDialogDefaultCreateRule(getActivity(), checkBoxCreateRules.isChecked()); // store last checked-state of "create-rule checkbox"

                        dialogListener.onConnectionDecided(
                                appUidGroup,
                                connection,
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

    @Override
    public void onStart() {
        super.onStart();

        dialogOpen = true;
    }

    @Override
    public void onStop() {
        super.onStop();

        dialogOpen = false;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        dialogOpen = false;

        Log.v(LOG_TAG, this.getClass().getSimpleName() + " dismissed. Connection was: " + connection);

        dialogListener.onDialogDismissed(appUidGroup, connection);
    }

    //    public static DecideConnectionDialog show(Activity context, int uid, Connections.IConnection connection) {
//        if (! (context instanceof DecideConnectionDialogListener)) {
//            Log.e(LOG_TAG, "Starting-Activity must implement interface " + DecideConnectionDialogListener.class.getCanonicalName());
//            throw new ClassCastException("Starting-Activity must implement interface " + DecideConnectionDialogListener.class.getCanonicalName());
//        }
//
//        Bundle args = new Bundle();
//
//        args.putInt("uid", uid);
//
////        args.putString("connection.src.ip", source.getIp());
////        args.putInt("connection.src.port", source.getPort());
////        args.putString("connection.dst.ip", destination.getIp());
////        args.putInt("connection.dst.port", destination.getPort());
//
//        DecideConnectionDialog dialog = new DecideConnectionDialog();
//        dialog.setArguments(args);
//        dialog.show(context.getFragmentManager(), "");
//
//        return dialog;
//    }

    public static DecideConnectionDialog show(Activity context, DecideConnectionDialogListener dialogListener, AppUidGroup appUidGroup, Connections.IConnection connection, Packages.TransportLayerProtocol protocol) {
        DecideConnectionDialog dialog = new DecideConnectionDialog();

        // set Dialog Arguments:
        dialog.dialogListener = dialogListener;
        dialog.appUidGroup = appUidGroup;
        dialog.connection = connection;
        dialog.protocol = protocol;

        dialog.show(context.getFragmentManager(), "");

        /* Arguments-Passing Notes:
         * - Since I have confirmed that the Dialog-Instance will NOT change over its runtime,
         *   I can simply use the instances attributes to pass complex data.
         *   Would be nice if google cared to mention this - instead of using their overhead-approach of making the activity implement the listener events.
         * - Those values which I pass using the Intent are being passed in such manner, as I did not know it work otherwise at first.
         *   Also some values (especially regarding the connection) are intended to be snapshots of the connection-state.
         */

        return dialog;
    }

}
