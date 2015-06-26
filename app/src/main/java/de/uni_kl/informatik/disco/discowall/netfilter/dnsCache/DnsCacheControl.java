package de.uni_kl.informatik.disco.discowall.netfilter.dnsCache;

import java.io.IOException;
import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.utils.NetworkUtils;

public class DnsCacheControl {
    private final LinkedList<DnsCache> caches = new LinkedList<>();

    public DnsCacheControl(int firstLocalDnsCachePort) throws IOException {
        for(String dnsServerAddress : NetworkUtils.readDnsServerConfigFile()) {
            caches.add(new DnsCache(firstLocalDnsCachePort++, dnsServerAddress));
        }
    }

    public LinkedList<DnsCache> getCaches() {
        return new LinkedList<>(caches);
    }

    public void stopAll() {
        for(DnsCache cache : caches) {
            cache.stopCache();
        }
    }
}
