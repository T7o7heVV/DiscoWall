package de.uni_kl.informatik.disco.discowall.utils.gui;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

public class ErrorDialog {
    public static void showError(Context context, String title, String message) {
        Log.e("ERROR", title + "\n" + message);

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public static void showError(Context context, String title, Exception e) {
        showError(context, title, "ERROR: " + e.getMessage());
    }
}
