package de.uni_kl.informatik.disco.discowall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import de.uni_kl.informatik.disco.discowall.firewallService.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.packages.Packages;


public class EditConnectionRuleDialog extends DialogFragment {

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
        builder.setView(inflater.inflate(R.layout.dialog_edit_connection_rule, null))
                .setTitle(bundle.getString("dialog.title"));

//                // Add action buttons
//                .setPositiveButton(R.string.generic_button_text_OK, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int id) {
//                    }
//                })
//                .setNegativeButton(R.string.generic_button_text_Cancel, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                    }
//                });

        AlertDialog view = builder.create();
        ((TextView) view.findViewById(R.id.dialog_edit_connection_app_package)).setText(bundle.getString("app.packageName"));

        return view;
    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.dialog_edit_connection_rule);
//
//        TextView clientLabel = (TextView) findViewById(R.id.dialog_edit_connection_textView_client_ip);
//        EditText clientIpEdit = (EditText) findViewById(R.id.dialog_edit_connection_editText_client_ip);
//        EditText clientPortEdit = (EditText) findViewById(R.id.dialog_edit_connection_editText_client_port);
//    }

    public static EditConnectionRuleDialog show(Activity context, String dialogTag, ApplicationInfo appInfo, Packages.IpPortPair client, Packages.IpPortPair server, FirewallRules.RulePolicy policy) {
//        Intent i = new Intent(context, EditConnectionRuleDialog.class);

        Bundle args = new Bundle();

        args.putString("rule.client.ip", client.getIp());
        args.putInt("rule.client.port", client.getPort());

        args.putString("rule.server.ip", server.getIp());
        args.putInt("rule.server.port", server.getPort());

        args.putSerializable("rule.policy", policy);
        args.putString("app.packageName", appInfo.packageName);

        // Create Dialog Title:
        args.putString("dialog.title", "Edit Connection Rule");
//        String clientStr = "?";
//        String serverStr = "?";
//        if (client != null)
//            clientStr = client.toString();
//        if (server  != null)
//            serverStr = server.toString();
//        args.putString("dialog.title", "Rule: " + clientStr + " -> " + serverStr);

//        context.startActivity(i);
        EditConnectionRuleDialog dialog = new EditConnectionRuleDialog();
        dialog.setArguments(args);
        dialog.show(context.getFragmentManager(), dialogTag);

        return dialog;
    }
}
