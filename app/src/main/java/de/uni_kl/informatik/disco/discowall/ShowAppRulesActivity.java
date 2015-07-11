package de.uni_kl.informatik.disco.discowall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import de.uni_kl.informatik.disco.discowall.gui.dialogs.ErrorDialog;


public class ShowAppRulesActivity extends AppCompatActivity {
    private final String LOG_TAG = ShowAppRulesActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_app_rules);

        // Either this activity has been resumed, or just recently started. Either way, fetch args:
        Bundle args;
        if (savedInstanceState != null)
            args = savedInstanceState;
        else
            args = getIntent().getExtras();

        // Fetching ApplicationInfo:
        final Activity context = this;
        PackageManager packageManager = context.getPackageManager();
        String packageName = args.getString("app.packageName");
        ApplicationInfo appInfo;

        try {
            appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Error fetching ApplicationInfo for app with packageName: " + packageName, e);
            ErrorDialog.showError(context, "Error fetching ApplicationInfo for app with packageName: " + packageName, e);

            finish();
            return;
        }

        // Activity layout:
        setTitle("Rules: " + appInfo.loadLabel(packageManager));

        // App Information
        ((TextView) findViewById(R.id.activity_show_app_rules_app_name)).setText(appInfo.loadLabel(packageManager));
        ((TextView) findViewById(R.id.activity_show_app_rules_app_package)).setText(packageName);
        ((ImageView) findViewById(R.id.activity_show_app_rules_app_icon)).setImageDrawable(appInfo.loadIcon(packageManager));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putAll(getIntent().getExtras()); // preserve arguments
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_app_rules, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    public static void showAppRules(Context context, ApplicationInfo appInfo) {
        Bundle args = new Bundle();

        // Dialog-Infos:
        args.putString("app.packageName", appInfo.packageName);

        Intent showAppRulesIntent = new Intent(context, ShowAppRulesActivity.class);
        showAppRulesIntent.putExtras(args);
        context.startActivity(showAppRulesIntent);
    }
}
