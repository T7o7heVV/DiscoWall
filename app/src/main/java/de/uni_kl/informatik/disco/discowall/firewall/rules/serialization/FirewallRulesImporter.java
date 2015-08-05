package de.uni_kl.informatik.disco.discowall.firewall.rules.serialization;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRuleExceptions;
import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallRules;
import de.uni_kl.informatik.disco.discowall.packages.Packages;

import static de.uni_kl.informatik.disco.discowall.firewall.rules.serialization.FirewallRuleSerializationExceptions.*;

public class FirewallRulesImporter {
    private static final String LOG_TAG = FirewallRulesImporter.class.getSimpleName();

    public static class ImportedRuledApp {
        private final LinkedList<FirewallRules.IFirewallRule> rules = new LinkedList<>();
        private final LinkedList<String> packageNames = new LinkedList<>();
        private final int userID;
        private final boolean isMonitored;

        public ImportedRuledApp(int userID, List<FirewallRules.IFirewallRule> rules, List<String> packageNames, boolean isMonitored) {
            this.userID = userID;
            this.rules.addAll(rules);
            this.packageNames.addAll(packageNames);
            this.isMonitored = isMonitored;
        }

        public int getUserID() {
            return userID;
        }

        public boolean isMonitored() {
            return isMonitored;
        }

        public LinkedList<FirewallRules.IFirewallRule> getRules() {
            return rules;
        }

        public LinkedList<String> getPackageNames() {
            return packageNames;
        }
    }

    private int uidFilter = -1;

    public int getUidFilter() {
        return uidFilter;
    }

    public void setUidFilter(int uidFilter) {
        this.uidFilter = uidFilter;
    }

    public LinkedList<ImportedRuledApp> importRulesFromFile(File xmlFile) throws RulesSerializerException {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            Element rootElement = doc.getDocumentElement(); // has the TAG 'XMLConstants.Root.TAG'
            return importRuledApps(getElementByName(rootElement, XMLConstants.Root.RuledApps.TAG));
        } catch (Exception e) {
            throw new RulesSerializerException("Error importing rules XML document: " + xmlFile + "\nException was: " + e.getMessage(), e);
        }
    }

    private Packages.IpPortPair importIpPortPair(Element ipPortPairElement) throws XmlTagMissingException {
        Log.v(LOG_TAG, "importing ip-port-pair...");

        String ipStr = ipPortPairElement.getAttribute(XMLConstants.IpPortPair.ATTR_Ip);
        String portStr = ipPortPairElement.getAttribute(XMLConstants.IpPortPair.ATTR_Port);

        Packages.IpPortPair ipPortPair = new Packages.IpPortPair(ipStr, Integer.parseInt(portStr));
        Log.v(LOG_TAG, "ip-port-pair: " + ipPortPair);

        return ipPortPair;
    }

    private FirewallRules.IFirewallRule importRule(Element ruleElement, int userID) throws XmlTagMissingException, UnknownRuleKindException, FirewallRuleExceptions.InvalidRuleDefinitionException {
        String ruleKindStr = ruleElement.getAttribute(XMLConstants.Root.RuledApps.Group.FirewallRules.AnyRule.ATTR_RuleKind);

        Log.v(LOG_TAG, "importing rule: " + ruleKindStr);

        String deviceFilterStr = ruleElement.getAttribute(XMLConstants.Root.RuledApps.Group.FirewallRules.AnyRule.ATTR_DeviceFilter);
        String protocolFilterStr = ruleElement.getAttribute(XMLConstants.Root.RuledApps.Group.FirewallRules.AnyRule.ATTR_ProtocolFilter);
        String uuid = ruleElement.getAttribute(XMLConstants.Root.RuledApps.Group.FirewallRules.AnyRule.ATTR_UUID);

        // Decode DeviceFilter, ProtocolFilter, RuleKind:
        FirewallRules.DeviceFilter deviceFilter = FirewallRules.DeviceFilter.valueOf(deviceFilterStr);
        FirewallRules.ProtocolFilter protocolFilter = FirewallRules.ProtocolFilter.valueOf(protocolFilterStr);
        FirewallRules.RuleKind ruleKind = FirewallRules.RuleKind.valueOf(ruleKindStr);

        // Decode LocalFilter + RemoteFilter:
        Element localFilterElement = getElementByName(ruleElement, XMLConstants.Root.RuledApps.Group.FirewallRules.AnyRule.LocalFilter.TAG);
        Element remoteFilterElement = getElementByName(ruleElement, XMLConstants.Root.RuledApps.Group.FirewallRules.AnyRule.RemoteFilter.TAG);
        Packages.IpPortPair localFilter = importIpPortPair(localFilterElement);
        Packages.IpPortPair remoteFilter = importIpPortPair(remoteFilterElement);

        switch(ruleKind) {
            case Policy:
            {
                String rulePolicyStr = ruleElement.getAttribute(XMLConstants.Root.RuledApps.Group.FirewallRules.PolicyRule.ATTR_RulePolicy);
                FirewallRules.RulePolicy rulePolicy = FirewallRules.RulePolicy.valueOf(rulePolicyStr);

                FirewallRules.FirewallTransportRule rule = new FirewallRules.FirewallTransportRule(userID, localFilter, remoteFilter, deviceFilter, protocolFilter, rulePolicy);
                rule.setUUID(uuid);

                return rule;
            }
            case Redirect:
            {
                Element redirectionHostElement = getElementByName(ruleElement, XMLConstants.Root.RuledApps.Group.FirewallRules.RedirectionRule.RedirectionRemoteHost.TAG);
                Packages.IpPortPair redirectionHost = importIpPortPair(redirectionHostElement);

                FirewallRules.FirewallTransportRedirectRule rule = new FirewallRules.FirewallTransportRedirectRule(userID, localFilter, remoteFilter, deviceFilter, protocolFilter, redirectionHost);
                rule.setUUID(uuid);

                return rule;
            }
            default:
                throw new UnknownRuleKindException("Expected rule of kind Redirect or Policy, but got: " + ruleKind + ".", ruleKind);
        }
    }

    private LinkedList<FirewallRules.IFirewallRule> importRules(Element rulesElement, int userID) throws XmlTagMissingException, UnknownRuleKindException, FirewallRuleExceptions.InvalidRuleDefinitionException {
        Log.v(LOG_TAG, "importing rules...");

        LinkedList<FirewallRules.IFirewallRule> firewallRules = new LinkedList<>();

        // Fetch all Rules: PolicyRules + RedirectionRules:
        LinkedList<Element> ruleElements = new LinkedList<>();
        ruleElements.addAll(getElementsByName(rulesElement, XMLConstants.Root.RuledApps.Group.FirewallRules.PolicyRule.TAG));
        ruleElements.addAll(getElementsByName(rulesElement, XMLConstants.Root.RuledApps.Group.FirewallRules.RedirectionRule.TAG));

        for(Element ruleElement : ruleElements) {
            firewallRules.add(importRule(ruleElement, userID));
        }

        return firewallRules;
    }

    private ImportedRuledApp importRuledApp(Element ruledAppElement) throws XmlTagMissingException, UnknownRuleKindException, FirewallRuleExceptions.InvalidRuleDefinitionException {
        String packageNamesList = ruledAppElement.getAttribute(XMLConstants.Root.RuledApps.Group.ATTR_PackageNamesList);
        String isMonitoredStr = ruledAppElement.getAttribute(XMLConstants.Root.RuledApps.Group.ATTR_IsMonitored);
        String userIdStr = ruledAppElement.getAttribute(XMLConstants.Root.RuledApps.Group.ATTR_UserID);

        Log.v(LOG_TAG, "importing ruled app: " + packageNamesList);

        // Decode PackageNamesList from format "package1;package2;...;packageN" into list
        String[] packageNamesRaw = packageNamesList.split(Pattern.quote(XMLConstants.Root.RuledApps.Group.packageNamesDelim));
        LinkedList<String> packageNames = new LinkedList<>();
        for(String packageName : packageNames) {
            packageName = packageName.trim();
            if (packageName.isEmpty())
                continue;

            packageNames.add(packageName);
        }

        // Decode monitored state:
        boolean isMonitored = Boolean.parseBoolean(isMonitoredStr);

        // Decode userID:
        int userID = Integer.parseInt(userIdStr);

        if (uidFilter > 0 && uidFilter != userID) {
            Log.v(LOG_TAG, "RuledApp with uid " + userID + " skipped according to id filter: " + uidFilter);
            return null;
        }

        // Import FirewallRules:
        LinkedList<FirewallRules.IFirewallRule> rules = importRules(getElementByName(ruledAppElement, XMLConstants.Root.RuledApps.Group.FirewallRules.TAG), userID);

        return new ImportedRuledApp(userID, rules, packageNames, isMonitored);
    }

    private LinkedList<ImportedRuledApp> importRuledApps(Element ruledAppsElement) throws XmlTagMissingException {
        Log.v(LOG_TAG, "importing ruled apps...");

        LinkedList<ImportedRuledApp> importedRuledApps = new LinkedList<>();
        LinkedList<Element> importedRuledAppsElement = getElementsByName(ruledAppsElement, XMLConstants.Root.RuledApps.Group.TAG);

        for(Element appGroupElement : importedRuledAppsElement) {
            try {
                ImportedRuledApp ruledApp = importRuledApp(appGroupElement);

                // The ruledApp is NULL, if it has been skipped due to a filter:
                if (ruledApp != null)
                    importedRuledApps.add(ruledApp);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error while importing AppGroup: " + appGroupElement + "\n\nItem will be skipped. Exception was: " + e.getMessage(), e);
            }
        }

        return importedRuledApps;
    }

    private Element getElementByName(Element parent, String tagName) throws XmlTagMissingException {
        LinkedList<Element> elementsByName = getElementsByName(parent, tagName);

        if (elementsByName.size() == 0)
            throw new FirewallRuleSerializationExceptions.XmlTagMissingException(tagName, parent.getTagName());

        if (elementsByName.size() > 1)
            Log.w(LOG_TAG, "Only one TAG of name '"+tagName+"' expected, but got " + elementsByName.size() + ". The first element will be handled, the rest will be ignored.");

        return elementsByName.get(0);
    }

    private LinkedList<Element> getElementsByName(Element parent, String tagName) throws XmlTagMissingException {
        NodeList nodeListByName = parent.getElementsByTagName(tagName);
        return extractElementsFromNodeList(nodeListByName);
    }

    private LinkedList<Element> extractElementsFromNodeList(NodeList childNodes) {
        LinkedList<Element> childElements = new LinkedList<>();

        for(int childIndex = 0; childIndex < childNodes.getLength(); childIndex++) {
            Node childNode = childNodes.item(childIndex);
            if (childNode instanceof Element)
                childElements.add((Element) childNode);
        }

        return childElements;
    }

}
