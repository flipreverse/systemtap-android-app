A graphical frontend to control SystemTap for Android
=====================================================

Set up the Android project
---------------------------

1. Unpack the Sherlock ActionBar tarball to a directory you want
2. Import it to Eclipse

	File --> Import --> Existing Project into Workspace --> Select root directory
	
 The list below contains three projects. You will just need the last one, which is the actually library.
 Mark it and click on `Finish`

3. You may need to mark it as libraray project

	Right click on the project --> Properties --> Android --> Mark `Is Library` --> OK
	
4. Use the steps descried in 2. to import the real project

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
