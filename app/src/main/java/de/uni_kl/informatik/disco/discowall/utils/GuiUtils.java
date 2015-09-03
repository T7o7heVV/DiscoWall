package de.uni_kl.informatik.disco.discowall.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.uni_kl.informatik.disco.discowall.R;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.gui.dialogs.ErrorDialog;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallSettings;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class GuiUtils {
    /**
     * refreshing the Activity be closing and reopening the activity
     * @param activity
     */
    public static void restartActivity(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(0, 0); // disabling slide-animation on Activity-finish

        activity.startActivity(activity.getIntent());
    }

    public static void expandStatusbar(Context context) throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        Object sbservice = context.getSystemService("statusbar");
        Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
        Method showsb;
        if (Build.VERSION.SDK_INT >= 17) {
            showsb = statusbarManager.getMethod("expandNotificationsPanel");
        } else {
            showsb = statusbarManager.getMethod("expand");
        }

        showsb.invoke(sbservice);
    }

    public static abstract class AsyncTaskSpinnerProgress<TParams, TProgress, TResult> extends AsyncTask<TParams, TProgress, TResult> {
        private final Context context;
        private final String dialogTitle;
        private final String dialogMessage;

        public AsyncTaskSpinnerProgress(Context context, String dialogTitle, String dialogMessage) {
            this.context = context;
            this.dialogTitle = dialogTitle;
            this.dialogMessage = dialogMessage;
        }

        private ProgressDialog progressDialog;

        public ProgressDialog getProgressDialog() {
            return progressDialog;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(context);

                /* IMPORTANT:
                 * The design of the progress-dialog has to be defined BEFORE showing.
                 * ==> Any Message, Title and Icon have to be defined so that the dialog can be updated with new information after it's visible.
                 *     If the dialog is shown without an icon/title/message, it cannot show one later on.
                 */

            progressDialog.setTitle(dialogTitle);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage(dialogMessage);
            progressDialog.setIcon(R.drawable.firewall_launcher);
            progressDialog.setProgress(0);
            progressDialog.setCancelable(false);

            progressDialog.show();
        }

        @Override
        protected void onPostExecute(TResult tResult) {
            super.onPostExecute(tResult);
            progressDialog.dismiss();
        }
    }
}
