#!/bin/bash

echo 'building...'
ndk-build

echo 'cp ../libs/armeabi/* ../app/src/main/assets/bin/'
cp ../libs/armeabi/* '../app/src/main/assets/bin/'

echo 'adb push ../libs/armeabi/netfilter_bridge /data/data/nfqnltest/'
adb push ../libs/armeabi/netfilter_bridge /data/data/nfqnltest/

echo 'adb push to /data/data/de.uni_kl.informatik.disco.discowall/app_bin/'
adb push ../libs/armeabi/netfilter_bridge /data/data/de.uni_kl.informatik.disco.discowall/app_bin/

echo 'all done.'

