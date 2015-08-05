package de.uni_kl.informatik.disco.discowall.firewall.rules.serialization;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.firewall.util.FirewallRuledApp;
import de.uni_kl.informatik.disco.discowall.packages.Packages;

import static de.uni_kl.informatik.disco.discowall.firewall.rules.serialization.FirewallRuleSerializationExceptions.UnknownRuleKindException;

public class FirewallRulesExporter {
    private static final String LOG_TAG = FirewallRulesExporter.class.getSimpleName();

    private boolean skipAppGroupsWithoutRules = true;

    private Element exportIpPortPair(Document doc, Packages.IpPortPair ipPortPair, String tag) {
        Element pairElement = doc.createElement(tag);

        pairElement.setAttribute(XMLConstants.IpPortPair.ATTR_Ip, ipPortPair.getIp());
        pairElement.setAttribute(XMLConstants.IpPortPair.ATTR_Port, ipPortPair.getPort() + "");

        return pairElement;
    }

    public void exportRulesToFile(FirewallRuledApp ruledApp, File xmlFile) {
        LinkedList<FirewallRuledApp> ruledAppsList = new LinkedList<>();
        ruledAppsList.add(ruledApp);

        exportRulesToFile(ruledAppsList, xmlFile);
    }

    public void exportRulesToFile(LinkedList<FirewallRuledApp> ruledApps, File xmlFile) {
        Log.i(LOG_TAG, "Exporting rules of " + ruledApps.size() + " apps to xml-file: " + xmlFile.getAbsolutePath());
        exportRulesToStream(ruledApps, new StreamResult(xmlFile));
    }

    private Element exportRule(Document doc, FirewallRules.IFirewallRule rule) throws UnknownRuleKindException {
        Element ruleElement;

        switch(rule.getRuleKind()) {
            case Policy:
                ruleElement = doc.createElement(XMLConstants.Root.RuledApps.Group.FirewallRules.PolicyRule.TAG);
                break;
            case Redirect:
                ruleElement = doc.createElement(XMLConstants.Root.RuledApps.Group.FirewallRules.RedirectionRule.TAG);
                break;
            default:
                throw new UnknownRuleKindException("Expected rule of kind Redirect or Policy, but got: " + rule.getRuleKind() + ".", rule.getRuleKind());
        }

//        ruleElement.setAttribute(XMLConstants.Root.RuledApps.Group.FirewallRules.AnyRule.ATTR_UserID, rule.getUserId() + "");
        ruleElement.setAttribute(XMLConstants.Root.RuledApps.Group.FirewallRules.AnyRule.ATTR_RuleKind, rule.getRuleKind() + "");
        ruleElement.setAttribute(XMLConstants.Root.RuledApps.Group.FirewallRules.AnyRule.ATTR_ProtocolFilter, rule.getProtocolFilter() + "");
        ruleElement.setAttribute(XMLConstants.Root.RuledApps.Group.FirewallRules.AnyRule.ATTR_DeviceFilter, rule.getDeviceFilter() + "");
        ruleElement.setAttribute(XMLConstants.Root.RuledApps.Group.FirewallRules.AnyRule.ATTR_UUID, rule.getUUID());

        // Export LocalFilter+RemoteFilter
        ruleElement.appendChild(exportIpPortPair(doc, rule.getLocalFilter(), XMLConstants.Root.RuledApps.Group.FirewallRules.AnyRule.LocalFilter.TAG));
        ruleElement.appendChild(exportIpPortPair(doc, rule.getRemoteFilter(), XMLConstants.Root.RuledApps.Group.FirewallRules.AnyRule.RemoteFilter.TAG));

        // RuleKind-Specific Data:
        switch(rule.getRuleKind()) {
            case Policy:
                FirewallRules.IFirewallPolicyRule policyRule = (FirewallRules.IFirewallPolicyRule) rule;
                ruleElement.setAttribute(XMLConstants.Root.RuledApps.Group.FirewallRules.PolicyRule.ATTR_RulePolicy, policyRule.getRulePolicy() + "");
                break;
            case Redirect:
                FirewallRules.IFirewallRedirectRule redirectRule = (FirewallRules.IFirewallRedirectRule) rule;
                ruleElement.appendChild(exportIpPortPair(doc, redirectRule.getRedirectionRemoteHost(), XMLConstants.Root.RuledApps.Group.FirewallRules.RedirectionRule.RedirectionRemoteHost.TAG));
                break;
        }

        return ruleElement;
    }

    private Element exportRules(Document doc, List<FirewallRules.IFirewallRule> rules) throws UnknownRuleKindException {
        Element rulesElement = doc.createElement(XMLConstants.Root.RuledApps.Group.FirewallRules.TAG);

        for(FirewallRules.IFirewallRule rule : rules)
            rulesElement.appendChild(exportRule(doc, rule));

        return rulesElement;
    }

    private Element exportAppGroup(Document doc, FirewallRuledApp ruledApp) throws UnknownRuleKindException {
        Element groupElement = doc.createElement(XMLConstants.Root.RuledApps.Group.TAG);

        groupElement.setAttribute(XMLConstants.Root.RuledApps.Group.ATTR_PackageNamesList, ruledApp.getUidGroup().getAllPackageNames(XMLConstants.Root.RuledApps.Group.packageNamesDelim));
        groupElement.setAttribute(XMLConstants.Root.RuledApps.Group.ATTR_IsMonitored, ruledApp.isMonitored() + "");
        groupElement.setAttribute(XMLConstants.Root.RuledApps.Group.ATTR_UserID, ruledApp.getUidGroup().getUid() + "");

        groupElement.appendChild(exportRules(doc, ruledApp.getRules()));

        return groupElement;
    }


    private Element exportAppGroups(Document doc, LinkedList<FirewallRuledApp> ruledApps) throws UnknownRuleKindException {
        Element groupsElement = doc.createElement(XMLConstants.Root.RuledApps.TAG);

        for(FirewallRuledApp ruledApp : ruledApps) {
            // Do not export empty group, when not otherwise specified
            if (ruledApp.getRules().isEmpty() && skipAppGroupsWithoutRules)
                continue;

            groupsElement.appendChild(exportAppGroup(doc, ruledApp));
        }

        return groupsElement;
    }

    private void exportRulesToStream(LinkedList<FirewallRuledApp> ruledApps, StreamResult result) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root element
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement(XMLConstants.Root.TAG);
            doc.appendChild(rootElement);

            // AppUidGroups:
            rootElement.appendChild(exportAppGroups(doc, ruledApps));

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);

            transformer.transform(source, result);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error serializing/saving rules to file: " + e.getMessage(), e);
        }
    }
}
