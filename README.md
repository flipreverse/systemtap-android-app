A graphical frontend to control SystemTap for Android
=====================================================

Since this project moved to Android Studio, it should be sufficient to either load it into Android Studio
or use gradle on commandline.
Either way gradle should automatically fetch and build it including all dependencies.
If gralde complains about a faulty line in an xml file from sherlock actionbar, just comment that line out.

Updating protobuf definition
----------------------------
1.  Just run the following command:

       protoc --java_out=src/ SystemTapMessage.proto

Build busybox
-------------
1. Make sure you have the recent version of android ndk (current r8)
2. Create a standalone toolchain (see $ndk/build/tools) and add it to your $PATH
3. The currently used busybox binary is based on commit f47ce07b2699134d94dae9320dabc4a91c3c6b83 (git://git.busybox.net/busybox).
4. To build it copy the config file (busybox.config) to the busybox source directory as ".config"
5. Replace the content of CONFIG_SYSROOT by the path where you placed the standalone toolchain including `/sysroot`
6. A simple "make" will do the rest. :-)
