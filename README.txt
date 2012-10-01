Build busybox
=============

Make sure you have the recent version of android ndk (current r8), created a standalone toolchain (see $ndk/build/tools) and your $PATH contains the path to the toolchain.
The currently used busybox binary is based on commit f47ce07b2699134d94dae9320dabc4a91c3c6b83 (git://git.busybox.net/busybox).
To build it copy the config file (busybox.config) to the busybox source directory as ".config".
Replace the CONFIG_SYSROOT var by the path where you placed the standalone toolchain.
A simple "make" will do the rest. :-)
