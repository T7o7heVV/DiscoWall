package de.uni_kl.informatik.disco.discowall.gui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import de.uni_kl.informatik.disco.discowall.R;

public class AboutDialog {
    public static void show(Activity context) {
        // Inflate the about message contents
        View messageView = context.getLayoutInflater().inflate(R.layout.dialog_about, null, false);

        // When linking text, force to always use default color. This works
        // around a pressed color state bug.
        TextView textView = (TextView) messageView.findViewById(R.id.dialog_about_credits);
        int defaultColor = textView.getTextColors().getDefaultColor();
        textView.setTextColor(defaultColor);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setIcon(R.drawable.firewall_launcher);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }
}
