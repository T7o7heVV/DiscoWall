package de.uni_kl.informatik.disco.discowall.firewall.subsystems;

import android.util.Log;

import java.io.File;
import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.firewall.Firewall;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.FirewallService;
import de.uni_kl.informatik.disco.discowall.firewall.helpers.FirewallRulesManager;
import de.uni_kl.informatik.disco.discowall.firewall.helpers.WatchedAppsManager;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallIptableRulesHandler;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRuleExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.firewall.rules.serialization.FirewallRuleSerializationExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.rules.serialization.FirewallRulesExporter;
import de.uni_kl.informatik.disco.discowall.firewall.rules.serialization.FirewallRulesImporter;
import de.uni_kl.informatik.disco.discowall.firewall.util.FirewallRuledApp;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterFirewallRulesHandler;
import de.uni_kl.informatik.disco.discowall.netfilter.iptables.IptablesControl;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallConstants;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DroidWallFiles;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class SubsystemRulesManager extends FirewallSubsystem{
    private static final String LOG_TAG = SubsystemRulesManager.class.getSimpleName();

    private final FirewallRulesManager rulesManager;
    private final FirewallIptableRulesHandler firewallIptableRulesHandler = NetfilterFirewallRulesHandler.instance;

    private final WatchedAppsManager watchedAppsManager;

    public SubsystemRulesManager(Firewall firewall, FirewallService firewallServiceContext, FirewallRulesManager rulesManager, WatchedAppsManager watchedAppsManager) {
        super(firewall, firewallServiceContext);
        this.rulesManager = rulesManager;
        this.watchedAppsManager = watchedAppsManager;
    }

    //region iptables-stuff

    public String getIptableRules(boolean all) throws FirewallExceptions.FirewallException {
        try {
            if (all) {
                return IptablesControl.getRuleInfoText(true, true);
            } else {
                if (!firewall.isFirewallRunning())
                    return "< firewall has to be enabled in order to retrieve firewall rules >";
                return firewallIptableRulesHandler.getFirewallRulesText();
            }
        } catch(ShellExecuteExceptions.ShellExecuteException e) {
            throw new FirewallExceptions.FirewallException("Error fetching iptable rules: " + e.getMessage(), e);
        }
    }

    public void writeRedirectionRuleToIptables(FirewallRules.FirewallTransportRule rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        // TODO
    }

    public void writePolicyRuleToIptables(FirewallRules.FirewallTransportRule rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        try {
            // If TCP should be filtered:
            if (rule.getProtocolFilter().isTcp())
                firewallIptableRulesHandler.addTransportLayerRule(Packages.TransportLayerProtocol.TCP, rule.getUserId(), new Connections.SimpleConnection(rule.getLocalFilter(), rule.getRemoteFilter()), rule.getRulePolicy(), rule.getDeviceFilter());

            // If UDP should be filtered:
            if (rule.getProtocolFilter().isUdp())
                firewallIptableRulesHandler.addTransportLayerRule(Packages.TransportLayerProtocol.UDP, rule.getUserId(), new Connections.SimpleConnection(rule.getLocalFilter(), rule.getRemoteFilter()), rule.getRulePolicy(), rule.getDeviceFilter());

        } catch (ShellExecuteExceptions.ShellExecuteException e) {

            // Remove created rule (if any), when an exception occurrs:
            if (rule.getProtocolFilter().isTcp())
                firewallIptableRulesHandler.deleteTransportLayerRule(Packages.TransportLayerProtocol.TCP, rule.getUserId(), new Connections.SimpleConnection(rule.getLocalFilter(), rule.getRemoteFilter()), rule.getRulePolicy(), rule.getDeviceFilter());
            if (rule.getProtocolFilter().isUdp())
                firewallIptableRulesHandler.deleteTransportLayerRule(Packages.TransportLayerProtocol.UDP, rule.getUserId(), new Connections.SimpleConnection(rule.getLocalFilter(), rule.getRemoteFilter()), rule.getRulePolicy(), rule.getDeviceFilter());

            throw e;
        }
    }

    //endregion

    //region get/delete/move/create rules

    public LinkedList<FirewallRules.IFirewallRule> getAllRules() {
        return new LinkedList<>(rulesManager.getRules());
    }

    public LinkedList<FirewallRules.IFirewallRule> getRules(AppUidGroup appGroup) {
        return new LinkedList<>(rulesManager.getRules(appGroup.getUid()));
    }

    public LinkedList<FirewallRules.IFirewallPolicyRule> getPolicyRules(AppUidGroup appGroup) {
        return rulesManager.getPolicyRules(appGroup.getUid());
    }

    public LinkedList<FirewallRules.IFirewallRedirectRule> getRedirectionRules(AppUidGroup appGroup) {
        return rulesManager.getRedirectionRules(appGroup.getUid());
    }

    public FirewallRules.FirewallTransportRule createTransportLayerRule(AppUidGroup appGroup, FirewallRules.RulePolicy rulePolicy) {
        return rulesManager.createTransportLayerRule(appGroup.getUid(), rulePolicy);
    }

    public FirewallRules.FirewallTransportRule createTransportLayerRule(AppUidGroup appGroup, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, FirewallRules.DeviceFilter deviceFilter, FirewallRules.ProtocolFilter protocolFilter, FirewallRules.RulePolicy rulePolicy) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        return rulesManager.createTransportLayerRule(appGroup.getUid(), sourceFilter, destinationFilter, deviceFilter, protocolFilter, rulePolicy);
    }

    public FirewallRules.FirewallTransportRedirectRule createTransportLayerRedirectionRule(AppUidGroup appGroup, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
        return rulesManager.createTransportLayerRedirectionRule(appGroup.getUid(), redirectTo);
    }

    public FirewallRules.FirewallTransportRedirectRule createTransportLayerRedirectionRule(AppUidGroup appGroup, Packages.IpPortPair sourceFilter, Packages.IpPortPair destinationFilter, FirewallRules.DeviceFilter deviceFilter, FirewallRules.ProtocolFilter protocolFilter, Packages.IpPortPair redirectTo) throws FirewallRuleExceptions.InvalidRuleDefinitionException {
        return rulesManager.createTransportLayerRedirectionRule(appGroup.getUid(), sourceFilter, destinationFilter, deviceFilter, protocolFilter, redirectTo);
    }

    /**
     * Adds a FirewallRule instance, which has been created manually - i.e. by calling the rules constructor directly.
     * @param rule
     * @throws FirewallRuleExceptions.DuplicateRuleException if the rule has already been added before.
     */
    public void addRule(FirewallRules.IFirewallRule rule) throws FirewallRuleExceptions.DuplicateRuleException {
        rulesManager.addRule(rule);
    }

    public void deleteUserRules(AppUidGroup appUidGroup) {
        rulesManager.deleteUserRules(appUidGroup.getUid());
    }

    public void deleteRule(FirewallRules.IFirewallRule rule) {
        rulesManager.deleteRule(rule);
    }

    public void deleteAllRules() {
        rulesManager.deleteAllRules();
    }

    public void addRule(FirewallRules.IFirewallRule rule, FirewallRules.IFirewallRule existingRuleBelowNewOne) throws FirewallRuleExceptions.DuplicateRuleException, FirewallRuleExceptions.RuleNotFoundException {
        rulesManager.addRule(rule, existingRuleBelowNewOne);
    }

    public int getRuleIndex(FirewallRules.IFirewallRule rule) {
        return rulesManager.getRuleIndex(rule);
    }

    public boolean moveRuleUp(FirewallRules.IFirewallRule rule) {
        return rulesManager.moveRuleUp(rule);
    }

    public boolean moveRuleDown(FirewallRules.IFirewallRule rule) {
        return rulesManager.moveRuleDown(rule);
    }

    //endregion

    //region rule-serialization & -storage

    public void saveAllRulesToFile(File exportFile) {
        exportFile.getParentFile().mkdirs(); // create all missing directories up to discowall-dir
        FirewallRulesExporter exporter = new FirewallRulesExporter();

        exporter.exportRulesToFile(firewall.getRuledApps(), exportFile);
    }

    private File getAppGroupRulesFile(AppUidGroup appUidGroup) {
        File rulesDir = DroidWallFiles.FIREWALL_RULES__DIR.getFile(firewallServiceContext);
        return new File(rulesDir, DiscoWallConstants.Files.ruledAppRulesFilePrefix + appUidGroup.getUid() + ".xml");
    }

    /**
     * After a rule has been created/edited/deleted for a specific app, the list of rules has to be serialized,
     * so that it exists between Discowall-Instances.
     * @param appUidGroup
     * @see #loadRulesFromAppStorage(AppUidGroup)
     */
    public void saveRulesToAppStorage(AppUidGroup appUidGroup) {
        File groupRulesFiles = getAppGroupRulesFile(appUidGroup);

        FirewallRulesExporter exporter = new FirewallRulesExporter();
        exporter.exportRulesToFile(firewall.getRuledApp(appUidGroup), groupRulesFiles);
    }

    public LinkedList<FirewallRuledApp> loadAllRulesFromAppStorage() throws FirewallRuleSerializationExceptions.RulesSerializerException {
        Log.i(LOG_TAG, "importing firewall-rules from storage...");

        LinkedList<FirewallRuledApp> installedRuledAppsWithRestoredRules = new LinkedList<>();

        for(FirewallRuledApp ruledApp : firewall.getRuledApps()) {
            AppUidGroup uidGroup = ruledApp.getUidGroup();
            Log.d(LOG_TAG, "loading rules for apps by user-id: " + uidGroup.getUid());

            FirewallRulesImporter.ImportedRuledApp importedRuledApp = loadRulesFromAppStorage(uidGroup);
            if (importedRuledApp == null) {
                Log.d(LOG_TAG, "no rules stored for app.");
                continue;
            }

            FirewallRuledApp installedRuledAppWithLoadedRules = new FirewallRuledApp(uidGroup, importedRuledApp.getRules(), ruledApp.isMonitored());
            installedRuledAppsWithRestoredRules.add(installedRuledAppWithLoadedRules);

//            for(FirewallRules.IFirewallRule rule : app.getRules()) {
//                try {
//                    addRule(rule);
//                } catch (FirewallRuleExceptions.DuplicateRuleException e) {
//                    Log.e(LOG_TAG, "Trying to import rule which already exists. Rule will not be imported: " + e.getMessage(), e);
//                }
//            }
        }

        return installedRuledAppsWithRestoredRules;
    }

    /**
     * After a rule has been created/edited/deleted for a specific app, the list of rules has to be serialized,
     * so that it exists between Discowall-Instances.
     * @param appUidGroup
     * @see #saveRulesToAppStorage(AppUidGroup)
     */
    private FirewallRulesImporter.ImportedRuledApp loadRulesFromAppStorage(AppUidGroup appUidGroup) throws FirewallRuleSerializationExceptions.RulesSerializerException {
        File groupRulesFiles = getAppGroupRulesFile(appUidGroup);

        // Return empty list, of no stored rules for this appGroup exist
        if (!groupRulesFiles.exists())
            return null;

        FirewallRulesImporter importer = new FirewallRulesImporter();
        importer.setUidFilter(appUidGroup.getUid());

        LinkedList<FirewallRulesImporter.ImportedRuledApp> importedRules = importer.importRulesFromFile(groupRulesFiles);
        if (importedRules.size() > 1) {
            Log.w(LOG_TAG, "Expected 1 app-group with uid " + appUidGroup.getUid() + " but got " + importedRules.size() + ".");
        } else if (importedRules.size() == 0) {
            Log.e(LOG_TAG, "Expected 1 app-group with uid " + appUidGroup.getUid() + " but got 0.");
            return null;
        }

        FirewallRulesImporter.ImportedRuledApp importedRuledApp = importedRules.get(0);
        Log.d(LOG_TAG, "rules imported: " + importedRuledApp.getRules().size());

        return importedRuledApp;
    }

    //endregion

}
