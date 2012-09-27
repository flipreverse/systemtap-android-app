Build busybox
=============

Make sure you have the recent version of android ndk (current r8), created a standalone toolchain (see $ndk/build/tools) and your $PATH contains the path to the toolchain.
The currently used busybox binary is based on commit 73a19908975948154d1a07c3550592059238e9ef (git://git.busybox.net/busybox).
To build it copy the config file (busybox.config) to the busybox source directory as ".config".
Replace the CONFIG_SYSROOT var by the path where you placed the standalone toolchain.
A simple "make" will do the rest. :-)
