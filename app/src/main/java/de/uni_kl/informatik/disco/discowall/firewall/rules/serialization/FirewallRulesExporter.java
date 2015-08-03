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
import de.uni_kl.informatik.disco.discowall.packages.Packages;

import static de.uni_kl.informatik.disco.discowall.firewall.rules.serialization.FirewallRuleSerializationExceptions.*;

public class FirewallRulesExporter {
    private static final String LOG_TAG = FirewallRulesExporter.class.getSimpleName();

    private Element exportIpPortPair(Document doc, Packages.IpPortPair ipPortPair, String tag) {
        Element pairElement = doc.createElement(tag);

        pairElement.setAttribute(XMLConstants.IpPortPair.ATTR_Ip, ipPortPair.getIp());
        pairElement.setAttribute(XMLConstants.IpPortPair.ATTR_Port, ipPortPair.getPort() + "");

        return pairElement;
    }

    private Element exportRule(Document doc, FirewallRules.IFirewallRule rule) throws UnknownRuleKindException {
        Element ruleElement;

        switch(rule.getRuleKind()) {
            case Policy:
                ruleElement = doc.createElement(XMLConstants.Root.FirewallRules.PolicyRule.TAG);
                break;
            case Redirect:
                ruleElement = doc.createElement(XMLConstants.Root.FirewallRules.RedirectionRule.TAG);
                break;
            default:
                throw new UnknownRuleKindException("Expected rule of kind Redirect or Policy, but got: " + rule.getRuleKind() + ".", rule.getRuleKind());
        }

        ruleElement.setAttribute(XMLConstants.Root.FirewallRules.AbstractRule.ATTR_UserID, rule.getUserId() + "");
        ruleElement.setAttribute(XMLConstants.Root.FirewallRules.AbstractRule.ATTR_RuleKind, rule.getRuleKind() + "");
        ruleElement.setAttribute(XMLConstants.Root.FirewallRules.AbstractRule.ATTR_ProtocolFilter, rule.getProtocolFilter() + "");
        ruleElement.setAttribute(XMLConstants.Root.FirewallRules.AbstractRule.ATTR_DeviceFilter, rule.getDeviceFilter() + "");
        ruleElement.setAttribute(XMLConstants.Root.FirewallRules.AbstractRule.ATTR_UUID, rule.getUUID());

        // Export LocalFilter+RemoteFilter
        ruleElement.appendChild(exportIpPortPair(doc, rule.getLocalFilter(), XMLConstants.Root.FirewallRules.AbstractRule.LocalFilter.TAG));
        ruleElement.appendChild(exportIpPortPair(doc, rule.getRemoteFilter(), XMLConstants.Root.FirewallRules.AbstractRule.RemoteFilter.TAG));

        // RuleKind-Specific Data:
        switch(rule.getRuleKind()) {
            case Policy:
                FirewallRules.IFirewallPolicyRule policyRule = (FirewallRules.IFirewallPolicyRule) rule;
                ruleElement.setAttribute(XMLConstants.Root.FirewallRules.PolicyRule.ATTR_RulePolicy, policyRule.getRulePolicy() + "");
                break;
            case Redirect:
                FirewallRules.IFirewallRedirectRule redirectRule = (FirewallRules.IFirewallRedirectRule) rule;
                ruleElement.appendChild(exportIpPortPair(doc, redirectRule.getRedirectionRemoteHost(), XMLConstants.Root.FirewallRules.PolicyRule.RemoteFilter.TAG));
                break;
        }

        return ruleElement;
    }

    private Element exportRules(Document doc, List<FirewallRules.IFirewallRule> rules) throws UnknownRuleKindException {
        Element rulesElement = doc.createElement(XMLConstants.Root.FirewallRules.TAG);

        for(FirewallRules.IFirewallRule rule : rules)
            rulesElement.appendChild(exportRule(doc, rule));

        return rulesElement;
    }

    public void saveRulesToFile(List<FirewallRules.IFirewallRule> rules, File xmlFile) {
        saveRulesToFile(rules, new StreamResult(xmlFile));
    }

    private void saveRulesToFile(List<FirewallRules.IFirewallRule> rules, StreamResult result) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root element
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement(XMLConstants.Root.TAG);
            doc.appendChild(rootElement);

            // rules
            rootElement.appendChild(exportRules(doc, rules));

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
