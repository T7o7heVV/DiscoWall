package de.uni_kl.informatik.disco.discowall;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import de.uni_kl.informatik.disco.discowall.firewallService.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.packages.Packages;


public class EditConnectionRuleDialog extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_edit_connection_rule);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_connection_rule_dialog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_main_menu_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static void show(Context context, String protocol, Packages.IpPortPair client, Packages.IpPortPair server, FirewallRules.RulePolicy policy) {
        Intent i = new Intent(context, EditConnectionRuleDialog.class);

        i.putExtra("rule.protocol", protocol);

        i.putExtra("rule.client.ip", client.getIp());
        i.putExtra("rule.client.port", client.getPort());

        i.putExtra("rule.server.ip", server.getIp());
        i.putExtra("rule.server.port", server.getPort());

        i.putExtra("rule.policy", policy);

        context.startActivity(i);
    }

    public static void test() {

    }
}
