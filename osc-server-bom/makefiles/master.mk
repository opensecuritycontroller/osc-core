##
## Copyright (C) 2014  McAfee Inc.
##
## Makefile to create the vmidc vmdk file
##
## Broken up into two makefiles so that the installation
##  steps can be evaluated after the mount is done.
##  Otherwise the dependencies are evaluated before
##  the disk exists and if the disk is already partially
##  installed the already resolved rules will not
##  be executed
##

.PHONY: all mount install umount cleanup clean

makedir=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
blddir=$(realpath $(makedir)/..)/build

export buildNumber blddir

dowithbindmount= \
     mount -o loop boot.img $(blddir)/boot && \
     trap "umount -d $(blddir)/boot" EXIT &&

all: OSC_disk-0.vmdk

OSC_disk-0.vmdk: OSC_disk-0.img
	./bin/qemu-img convert $< -O vmdk -c -o compat6,subformat=streamOptimized $@
	chmod 0777 $@
        ifeq ($(build-all-images), true)
		./bin/qemu-img convert -f vmdk -O qcow2 $@ OSC_disk-0.qcow2
		chmod 0777 OSC_disk-0.qcow2
		mv -f OSC_disk-0.img OSC_disk-0-raw.img
		chmod 0777 OSC_disk-0-raw.img
        endif

OSC_disk-0.img: make-image

make-image: version-info
	$(MAKE) --no-print-directory -f makefiles/mount.mk
        ifeq ($(image-os), centos)
		$(dowithbindmount) $(MAKE) --no-print-directory -f makefiles/packages_centos.mk
        else
		$(dowithbindmount) $(MAKE) --no-print-directory -f makefiles/packages.mk
        endif
	find root -type f -exec touch {} \;
        ifeq ($(image-os), centos)
		$(dowithbindmount) $(MAKE) --no-print-directory -f makefiles/install_centos.mk
		$(MAKE) --no-print-directory -f makefiles/packages_centos.mk cleanup
        else
		$(dowithbindmount) $(MAKE) --no-print-directory -f makefiles/install.mk
		$(MAKE) --no-print-directory -f makefiles/packages.mk cleanup
        endif
	$(MAKE) --no-print-directory -f makefiles/mount.mk cleanup
	$(MAKE) --no-print-directory -f makefiles/mount.mk transfer
	$(MAKE) --no-print-directory -f makefiles/mount.mk grub

version-info:
	@echo ===================================================================
	@echo BUILD ENVIORNMENT
	@echo ===================================================================
	@echo
	@echo "RPM NAME                                 VERSION"
	@echo ======================================== ==========================
	rpm -qa --qf "%-40{NAME} %{VERSION}\n" | sort
	@echo
	ant -version
	@echo
	$(CC) --version
	@echo
	$(LD) --version
	@echo
	$(MAKE) --version
	@echo
	./bin/qemu-img --version
	@echo

mount:
	$(MAKE) -f makefiles/mount.mk

install:
        ifeq ($(image-os), centos)
		$(dowithbindmount) $(MAKE) -f makefiles/packages_centos.mk
		$(dowithbindmount) $(MAKE) -f makefiles/install_centos.mk
		$(MAKE) -f makefiles/packages_centos.mk cleanup
        else
		$(dowithbindmount) $(MAKE) -f makefiles/packages.mk
		$(dowithbindmount) $(MAKE) -f makefiles/install.mk
		$(MAKE) -f makefiles/packages.mk cleanup
        endif


grub:
	$(MAKE) -f makefiles/mount.mk grub

umount:
	$(MAKE) -f makefiles/mount.mk cleanup

cleanup:
        ifeq ($(image-os), centos)
		$(MAKE) --no-print-directory -f makefiles/install_centos.mk cleanup
		$(MAKE) --no-print-directory -f makefiles/packages_centos.mk cleanup
        else
		$(MAKE) --no-print-directory -f makefiles/install.mk cleanup
		$(MAKE) --no-print-directory -f makefiles/packages.mk cleanup
        endif

	$(MAKE) --no-print-directory -f makefiles/mount.mk cleanup

clean:
        ifeq ($(image-os), centos)
		$(MAKE) --no-print-directory -f makefiles/packages_centos.mk clean
        else
		$(MAKE) --no-print-directory -f makefiles/packages.mk clean
        endif
	$(MAKE) --no-print-directory -f makefiles/mount.mk clean

	-$(RM) OSC_disk-0.vmdk
	-$(RM) OSC_disk-0.qcow2
	-$(RM) OSC_disk-0.img
	-$(RM) OSC_disk-0-raw.img

realclean: clean
        ifeq ($(image-os), centos) 
		$(MAKE) --no-print-directory -f makefiles/packages_centos.mk realclean
        else
		$(MAKE) --no-print-directory -f makefiles/packages.mk realclean
        endif

