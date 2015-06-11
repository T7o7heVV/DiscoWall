package de.uni_kl.informatik.disco.discowall.utils;

import android.util.Log;

import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class NetworkInterfaceHelper {
    private static final String LOG_TAG = NetworkInterfaceHelper.class.getSimpleName();
    private int deviceIdLoopback, deviceIdWlan;

    public NetworkInterfaceHelper() {
        deviceIdLoopback = 1;
        deviceIdWlan = -1;

        LinkedList<NetworkUtils.NetworkDevice> networkDevices;
        try {
            networkDevices = NetworkUtils.getNetworkDevices();
        } catch (ShellExecuteExceptions.CallException e) {
            Log.e(LOG_TAG, "Unable to obtain list of network-devices. Device 1 will be assumed as 'loopback', everything else will be assumed a 'wlan' interface.");
            return;
        }

        // Fetch wifi device for this mobile phone

        Log.v(LOG_TAG, "Searching for device id of wifi-interface...");

        for(NetworkUtils.NetworkDevice device : networkDevices) {
            // device.name is string of form "<device-type><number>", example: wlan0

            for(String wifiDeviceName : NetworkUtils.NetworkInterfacesWifi) { // the wifi-device-names from this list do not contain a number
                if (device.name.startsWith(wifiDeviceName)) {
                    deviceIdWlan = device.id;
                    return;
                }
            }
        }

        Log.v(LOG_TAG, "Could not find id of wifi-interface. Assuming all none-loopback interfaces as wifi.");
    }

    public Packages.NetworkInterface getPackageInterfaceById(int interfaceID) {
        if (interfaceID == deviceIdLoopback)
            return Packages.NetworkInterface.Loopback;

        // In case the wifi device-ID could not be established, all packages which are not from loopback are assumed to be wifi packages
        if (deviceIdWlan < 0)
            return Packages.NetworkInterface.WiFi;

        // if interface has id of wifi-device
        if (deviceIdWlan == interfaceID)
            return Packages.NetworkInterface.WiFi;
        else
            return Packages.NetworkInterface.Umts;
    }
}
