package de.uni_kl.informatik.disco.discowall.gui.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import de.uni_kl.informatik.disco.discowall.R;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.packages.Packages;

public class AppRulesAdapter extends ArrayAdapter<FirewallRules.IFirewallRule> {
    public static interface CheckedChangedListener {
        void onCheckedChanged(AppRulesAdapter adapter, FirewallRules.IFirewallRule rule, int position, CheckBox checkBox, boolean isChecked);
    }

    private static final String LOG_TAG = AppRulesAdapter.class.getSimpleName();
    public static final int listRowLayoutResourceId = R.layout.list_item_firewall_rule;

    public static final int widgetId_imageView_rule_action_icon = R.id.list_firewall_rule__imageView_rule_action_icon;
    public static final int widgetId_textView_rule_client = R.id.list_firewall_rule__textView_rule_client;
    public static final int widgetId_textView_rule_server = R.id.list_firewall_rule__textView_rule_server;
    public static final int widgetId_checkbox_rule_interface_wifi = R.id.list_firewall_rule__checkBox_rule_interface_wifi;
    public static final int widgetId_checkbox_rule_interface_umts = R.id.list_firewall_rule__checkBox_rule_interface_umts;
    public static final int widgetId_checkbox_rule_protocol_tcp = R.id.list_firewall_rule__checkBox_rule_protocol_tcp;
    public static final int widgetId_checkbox_rule_protocol_udp = R.id.list_firewall_rule__checkBox_rule_protocol_udp;
//    public static final int widgetId_textView_rule_additional_infos = R.id.list_firewall_rule__textView_rule_additional_infos;

    private final List<FirewallRules.IFirewallRule> rules;
    private AdapterView.OnItemClickListener onItemClickListener;
    private AdapterView.OnItemLongClickListener onItemLongClickListener;
    private CheckedChangedListener onCheckedChangeListener;

    public AppRulesAdapter(Context context, List<FirewallRules.IFirewallRule> rules) {
        super(context, listRowLayoutResourceId, rules);
        this.rules = rules;
    }

    public List<FirewallRules.IFirewallRule> getRules() {
        return rules;
    }

    public CheckedChangedListener getOnCheckedChangeListener() {
        return onCheckedChangeListener;
    }

    public void setOnCheckedChangeListener(CheckedChangedListener onCheckedChangeListener) {
        this.onCheckedChangeListener = onCheckedChangeListener;
    }

    public AdapterView.OnItemLongClickListener getOnItemLongClickListener() {
        return onItemLongClickListener;
    }

    public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }

    public AdapterView.OnItemClickListener getOnItemClickListener() {
        return onItemClickListener;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final View view;

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(listRowLayoutResourceId, null);
        } else {
            view = convertView;
        }

        // --------------------------------------------------------------------------------------------------------------
        //  View references:
        // --------------------------------------------------------------------------------------------------------------

        // Image-View: Rule-Action-Icon
        ImageView imageViewActionIcon = (ImageView) view.findViewById(widgetId_imageView_rule_action_icon);

        // TextViews: Client, Server
        TextView textViewClient = (TextView) view.findViewById(widgetId_textView_rule_client);
        TextView textViewServer = (TextView) view.findViewById(widgetId_textView_rule_server);
//        TextView textViewAdditionalInfos = (TextView) view.findViewById(widgetId_textView_rule_additional_infos);

        // Checkboxes: Interfaces (WiFi, UMTS), Protocols (TCP, UDP)
        CheckBox checkBoxInterfaceWifi = (CheckBox) view.findViewById(widgetId_checkbox_rule_interface_wifi);
        CheckBox checkBoxInterfaceUmts = (CheckBox) view.findViewById(widgetId_checkbox_rule_interface_umts);
        CheckBox checkBoxProtocolTcp = (CheckBox) view.findViewById(widgetId_checkbox_rule_protocol_tcp);
        CheckBox checkBoxProtocolUdp = (CheckBox) view.findViewById(widgetId_checkbox_rule_protocol_udp);


        // --------------------------------------------------------------------------------------------------------------
        //  Write data to GUI:
        // --------------------------------------------------------------------------------------------------------------
        final FirewallRules.IFirewallRule rule = rules.get(position);

        // Rule-Action
        writeRuleActionToGui(rule, imageViewActionIcon);

        // Client/Server
        {
            // Set default font-layout for string "Host:IP" on server
            textViewServer.setTypeface(null, Typeface.BOLD); // server info is BOLD

            writeIpPortInfoToGui(rule.getLocalFilter(), textViewClient);
            writeIpPortInfoToGui(rule.getRemoteFilter(), textViewServer);
        }

        // WiFi, UMTS, TCP, UDP
        checkBoxInterfaceUmts.setChecked(rule.getDeviceFilter().allowsUmts());
        checkBoxInterfaceWifi.setChecked(rule.getDeviceFilter().allowsWifi());
        checkBoxProtocolTcp.setChecked(rule.getProtocolFilter().isTcp());
        checkBoxProtocolUdp.setChecked(rule.getProtocolFilter().isUdp());

        // --------------------------------------------------------------------------------------------------------------
        //  Associate Event-Listeners
        // --------------------------------------------------------------------------------------------------------------
        associateWidgetEventListeners(imageViewActionIcon, position, parent);
        associateWidgetEventListeners(textViewClient, position, parent);
        associateWidgetEventListeners(textViewServer, position, parent);
        associateWidgetEventListeners(checkBoxInterfaceUmts, position, parent);
        associateWidgetEventListeners(checkBoxInterfaceWifi, position, parent);
        associateWidgetEventListeners(checkBoxProtocolTcp, position, parent);
        associateWidgetEventListeners(checkBoxProtocolUdp, position, parent);


        // --------------------------------------------------------------------------------------------------------------
        //  Make Checkboxes readonly
        // --------------------------------------------------------------------------------------------------------------
        // Everything else makes no sense. Since the Android-Framework is EXTREMELY incomplete/buggy
        // reagrding ListViews, the checked-events for listviews are fired as they are initialy created by the adapter.
        // Since there is no way of reacting to the ListView-Created Event (does not exist), one cannot filter out these erroneous calls.
        // So just better don't use CheckBoxes within ListViews. -.-
        checkBoxInterfaceUmts.setKeyListener(null);
        checkBoxInterfaceWifi.setKeyListener(null);
        checkBoxProtocolTcp.setKeyListener(null);
        checkBoxProtocolUdp.setKeyListener(null);

        return view;
    }

    private void associateWidgetEventListeners(View widget, final int position, final ViewGroup parent) {
        // NOTE: Do NOT declare widget-References as final here, as the view-instances are permanently changing.
        //       Therefore I use the view-references directly from callback and cast them there (if required).


        // Checkboxes react to CHECKED-, everything else to CLICKED-events:
        if (widget instanceof CheckBox) {
            // Checkboxes only:

            final CheckBox checkbox = (CheckBox) widget;

            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (onCheckedChangeListener != null)
                        onCheckedChangeListener.onCheckedChanged(AppRulesAdapter.this, getItem(position), position, checkbox, isChecked);
                }
            });
        } else {
            // all widgets but checkboxes - click events:

            widget.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null)
                        onItemClickListener.onItemClick((AdapterView<?>) parent, v, position, position);
                }
            });

            widget.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (onItemLongClickListener != null)
                        return onItemLongClickListener.onItemLongClick((AdapterView<?>) parent, v, position, position);
                    else
                        return false;
                }
            });
        }
    }

    private void writeRuleActionToGui(FirewallRules.IFirewallRule rule, ImageView imageViewActionIcon) {
        if (rule instanceof FirewallRules.IFirewallPolicyRule) {
            FirewallRules.IFirewallPolicyRule policyRule = (FirewallRules.IFirewallPolicyRule) rule;

            switch(((FirewallRules.IFirewallPolicyRule) rule).getRulePolicy()) {
                case ALLOW:
                    imageViewActionIcon.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.symbol_rule_policy_allow));
                    break;
                case BLOCK:
                    imageViewActionIcon.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.symbol_rule_policy_block));
                    break;
                case INTERACTIVE:
                    imageViewActionIcon.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.symbol_rule_policy_interactive));
                    break;
                default:
                    Log.e(LOG_TAG, "Unknown rule policy " + policyRule.getRulePolicy() + " at rule " + rule + ".");
                    break;
            }
        } else if (rule instanceof FirewallRules.IFirewallRedirectRule) {
            imageViewActionIcon.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.symbol_rule_redirect));
        } else {
            Log.e(LOG_TAG, "Unknown rule type " + rule.getClass() + " at rule " + rule + ".");
        }
    }

    private void writeIpPortInfoToGui(Packages.IpPortPair ipPortInfo, TextView targetView) {
        String ip = ipPortInfo.getIp();
        if (ip.length() == 0)
            ip = "*";

        String port;
        if (ipPortInfo.getPort() > 0)
            port = ipPortInfo.getPort() + "";
        else
            port = "*";

        targetView.setText(ip + " : " + port);
    }

}
