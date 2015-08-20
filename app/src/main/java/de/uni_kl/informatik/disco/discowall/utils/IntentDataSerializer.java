package de.uni_kl.informatik.disco.discowall.utils;

import android.content.Intent;

import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;

public class IntentDataSerializer {
    public static void writeIpPortPair(Packages.IpPortPair ipPortPair, Intent intent, String keyPrefix) {
        intent.putExtra(keyPrefix + ".ip", ipPortPair.getIp());
        intent.putExtra(keyPrefix + ".port", ipPortPair.getPort());
    }

    public static void writeConnection(Connections.IConnection connection, Intent intent, String keyPrefix) {
        writeIpPortPair(connection.getSource(), intent, keyPrefix + ".source");
        writeIpPortPair(connection.getDestination(), intent, keyPrefix + ".destination");
    }

    public static Packages.IpPortPair readIpPortPair(Intent intent, String keyPrefix) {
        final String portKey = keyPrefix + ".port";

        if (!intent.hasExtra(portKey))
            throw new RuntimeException("Could not retrieve port. Value not found: " + portKey);

        String ip = intent.getStringExtra(keyPrefix + ".ip");
        int port = intent.getIntExtra(portKey, 1337);

        return new Packages.IpPortPair(ip, port);
    }

    public static Connections.IConnection readConnection(Intent intent, String keyPrefix) {
        Packages.IpPortPair source = readIpPortPair(intent, keyPrefix + ".source");
        Packages.IpPortPair destination = readIpPortPair(intent, keyPrefix + ".destination");

        return new Connections.SimpleConnection(source, destination);
    }
}
