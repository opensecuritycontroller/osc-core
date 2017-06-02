##
##  Copyright (C) 2017 Intel Corporation.
##
## Makefile to create, partition, encrypt, format and mount a raw
##  disk image used for the vmiDC project.
##

## Everthing runs as root with a umask of 0000 so that all permissions
##  are perserved
SHELL = exec /bin/bash --noprofile -c 'umask 0000; shift; eval "$$1"' --

.PHONY: all format-boot clean cleanup grub transfer transfer-boot transfer-sysimage .FORCE

blddir?=build

all: boot.img $(blddir)/boot

##
## Create a raw disk image with three partitions (making the third
##   partition "small" 2G
##
## When modifying the sizes adjust the dd copies of the disk images
##   to match the new size
##
OSC_disk-0.img:
	dd if=/dev/zero of=$@ bs=65536 seek=819200 count=0
	parted -s -a optimal $@ mklabel msdos
	parted -s -a optimal $@ mkpart primary ext2 2048s 999423s
	parted -s -a optimal $@ mkpart primary 999424s 32249855s
	parted -s -a optimal $@ mkpart primary 32249856s 104857599s
	parted -s -a optimal $@ set 1 boot on

##
## Conditionally create mount poinst and mount each
##    of the loopbacked partitions
##
$(blddir) Trees:
	mkdir -p -m 755 $@

$(blddir)/boot: | $(blddir)
	@if [ ! -d $@ ]; then \
		echo mkdir -m 755 $@; \
		mkdir -m 755 $@; \
	fi

boot.img:
	dd if=/dev/null of=$@ bs=1048576 seek=487 count=0
	mkfs -F $@ -U de505a44-50b7-4757-87e5-a1a547de8dd4

transfer: transfer-boot transfer-sysimage

transfer-boot: OSC_disk-0.img
	dd if=boot.img of=OSC_disk-0.img bs=1048576 seek=1 conv=nocreat,notrunc

transfer-sysimage: sysimage.tar.xz OSC_disk-0.img
	tar -cf - sysimage.tar.xz | dd of=OSC_disk-0.img bs=1048576 seek=488 conv=nocreat,notrunc

sysimage.tar.xz: Trees/pxz-4.999.9beta/pxz | .FORCE
	tar -C $(blddir) -cf - . | Trees/pxz-4.999.9beta/pxz -T 4 -9 > sysimage.tar.xz

Trees/pxz-4.999.9beta/pxz: | Trees/pxz-4.999.9beta
	CFLAGS=-mtune=native make -C $(@D)

Trees/pxz-4.999.9beta: Sources/pxz-4.999.9beta.20091201git.tar.xz | Trees
	tar -C Trees -xvf $<

grub:
	echo -e "device (hd0) OSC_disk-0.img\nroot (hd0,0)\nsetup (hd0)" | grub --device-map=/dev/null
	@echo

cleanup:

clean: cleanup
	$(RM) -r $(blddir)
	$(RM) -r Trees
	$(RM) keyfile
	$(RM) sysimage.tar.xz
	$(RM) OSC_disk-0.img
	$(RM) boot.img

.FORCE:
