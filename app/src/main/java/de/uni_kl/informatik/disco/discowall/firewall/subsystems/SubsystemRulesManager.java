package de.uni_kl.informatik.disco.discowall.firewall.subsystems;

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
import de.uni_kl.informatik.disco.discowall.firewall.rules.serialization.FirewallRulesExporter;
import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterFirewallRulesHandler;
import de.uni_kl.informatik.disco.discowall.netfilter.iptables.IptablesControl;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallConstants;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DroidWallFiles;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class SubsystemRulesManager extends FirewallSubsystem{
    private final FirewallRulesManager rulesManager;
    private final FirewallIptableRulesHandler iptableRulesManager = NetfilterFirewallRulesHandler.instance;
    private final WatchedAppsManager watchedAppsManager;

    public SubsystemRulesManager(Firewall firewall, FirewallService firewallServiceContext, FirewallRulesManager rulesManager, WatchedAppsManager watchedAppsManager) {
        super(firewall, firewallServiceContext);
        this.rulesManager = rulesManager;
        this.watchedAppsManager = watchedAppsManager;
    }

    public String getIptableRules(boolean all) throws FirewallExceptions.FirewallException {
        try {
            if (all) {
                return IptablesControl.getRuleInfoText(true, true);
            } else {
                if (!firewall.isFirewallRunning())
                    return "< firewall has to be enabled in order to retrieve firewall rules >";
                return iptableRulesManager.getFirewallRulesText();
            }
        } catch(ShellExecuteExceptions.ShellExecuteException e) {
            throw new FirewallExceptions.FirewallException("Error fetching iptable rules: " + e.getMessage(), e);
        }
    }

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

    public void deleteAllRules(AppUidGroup appUidGroup) {
        rulesManager.deleteAllRules(appUidGroup.getUid());
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

    public void saveAllRulesToFile(File exportFile) {
        exportFile.getParentFile().mkdirs(); // create all missing directories up to discowall-dir
        FirewallRulesExporter exporter = new FirewallRulesExporter();

        exporter.exportRulesToFile(firewall.getRuledApps(), exportFile);
    }

    /**
     * After a rule has been created/edited/deleted for a specific app, the list of rules has to be serialized,
     * so that it exists between Discowall-Instances.
     * @param appUidGroup
     * @see #loadRulesFromAppStorage(AppUidGroup)
     */
    public void saveRulesToAppStorage(AppUidGroup appUidGroup) {
        File rulesDir = DroidWallFiles.FIREWALL_RULES__DIR.getFile(firewallServiceContext);
        File groupRulesFiles = new File(rulesDir, DiscoWallConstants.Files.ruledAppRulesFilePrefix + appUidGroup.getUid() + ".xml");

        FirewallRulesExporter exporter = new FirewallRulesExporter();
        exporter.exportRulesToFile(firewall.getRuledApp(appUidGroup), groupRulesFiles);
    }

    /**
     * After a rule has been created/edited/deleted for a specific app, the list of rules has to be serialized,
     * so that it exists between Discowall-Instances.
     * @param appUidGroup
     * @see #saveRulesToAppStorage(AppUidGroup)
     */
    public void loadRulesFromAppStorage(AppUidGroup appUidGroup) {
        File rulesDir = DroidWallFiles.FIREWALL_RULES__DIR.getFile(firewallServiceContext);

        // TODO
    }

}
