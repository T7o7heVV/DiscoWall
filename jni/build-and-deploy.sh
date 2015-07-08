#!/bin/bash

echo 'building...'
ndk-build

echo 'adb push /media/dynamic/Bachelor-Thesis/AndroidStudioProjects/DiscoWall/libs/armeabi/netfilter_bridge /data/data/nfqnltest/'
adb push /media/dynamic/Bachelor-Thesis/AndroidStudioProjects/DiscoWall/libs/armeabi/netfilter_bridge /data/data/nfqnltest/

echo 'adb push to /data/data/de.uni_kl.informatik.disco.discowall/app_bin/'
adb push /media/dynamic/Bachelor-Thesis/AndroidStudioProjects/DiscoWall/libs/armeabi/netfilter_bridge /data/data/de.uni_kl.informatik.disco.discowall/app_bin/

echo 'all done.'

