package de.uni_kl.informatik.disco.discowall.firewall.subsystems;

import de.uni_kl.informatik.disco.discowall.netfilter.bridge.NetfilterBridgeCommunicator;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.utils.apps.AppUidGroup;

public interface SubsystemPendingPackagesManager extends NetfilterBridgeCommunicator.PackageActionCallback {
    /**
     * When the user opens the connection-decision dialog.
     */
    void OnDecisionDialogOpened(AppUidGroup appUidGroup, Connections.IConnection connection);

    /**
     * When the user does not chose anything, but simply dismisses the dialog.
     */
    void OnDecisionDialogDismissed(AppUidGroup appUidGroup, Connections.IConnection connection);
}
