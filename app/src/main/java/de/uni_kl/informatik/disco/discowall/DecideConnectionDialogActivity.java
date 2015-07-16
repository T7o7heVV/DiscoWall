package de.uni_kl.informatik.disco.discowall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import de.uni_kl.informatik.disco.discowall.packages.Packages;


public class DecideConnectionDialogActivity extends Activity implements DecideConnectionDialog.DecideConnectionDialogListener  {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_decide_connection_dialog);

//        // stick to portrait
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        DecideConnectionDialog.show(this, null, null);
    }

    @Override
    public void onConnectionDecided(ApplicationInfo appInfo, Packages.IpPortPair source, Packages.IpPortPair destination, DecideConnectionDialog.AppConnectionDecision decision) {
        // TODO
        Toast.makeText(this, decision.allowConnection ? "Allowed" : "Blocked", Toast.LENGTH_LONG).show();
    }

    public static void show(Context context) {
        Bundle args = new Bundle();

        // Args:
//        args.putInt("app.uid", appUidGroup.getUid());

        Intent showAppRulesIntent = new Intent(context, DecideConnectionDialogActivity.class);
        showAppRulesIntent.putExtras(args);
        context.startActivity(showAppRulesIntent);
    }
}
