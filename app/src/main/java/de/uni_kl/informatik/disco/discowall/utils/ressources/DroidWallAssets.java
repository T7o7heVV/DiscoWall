package de.uni_kl.informatik.disco.discowall.utils.ressources;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by tw on 12.05.15.
 */
public enum DroidWallAssets {
    DIR_BINARIES,
    NETFILTER_BRIDGE_BINARY
    ;

    public static class AssetFunctionalityNotImplementedException extends RuntimeException {
        private final DroidWallAssets asset;

        public AssetFunctionalityNotImplementedException(DroidWallAssets asset) {
            super("DroidWall asset " + asset + " does not provide the requested functionality.");
            this.asset = asset;
        }

        public DroidWallAssets getAsset() {
            return asset;
        }
    }

    public InputStream getInputStream(Context context) throws IOException {
        AssetManager assetManager = context.getAssets();

        switch (this) {
            case NETFILTER_BRIDGE_BINARY: return assetManager.open(NETFILTER_BRIDGE_BINARY.getRelativePath());

            default: throw new AssetFunctionalityNotImplementedException(this);
        }
    }

    public BufferedReader getBufferedReader(Context context) throws IOException {
        switch (this) {
            case NETFILTER_BRIDGE_BINARY: return new BufferedReader(new InputStreamReader(NETFILTER_BRIDGE_BINARY.getInputStream(context)));

            default: throw new AssetFunctionalityNotImplementedException(this);
        }
    }

    public String getRelativePath() {
        switch(this) {
            case DIR_BINARIES: return "bin/";

            case NETFILTER_BRIDGE_BINARY: return DIR_BINARIES.getRelativePath() + "netfilter_bridge";

            default: throw new AssetFunctionalityNotImplementedException(this);
        }
    }
}
