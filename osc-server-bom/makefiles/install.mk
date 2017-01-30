##
## Copyright (C) 2014  McAfee Inc.
##
## Makefile to install the vmidc components into
##  the directory $(blddir).
##

## Everything runs as root with a umask of 0000 so that all permissions
##  are perserved
SHELL = exec /bin/bash --noprofile -c 'umask 0000; shift; eval "$$1"' --

.PHONY: all mlos yum-vmidc yum-mlos cleanup rpm-list run-postinstall

blddir?=build

kernelver=$(shell rpm --root=$(blddir) --qf "%{VERSION}" -qa kernel)
kernelverrelease=$(shell rpm --root=$(blddir) --qf "%{VERSION}-%{RELEASE}" -qa kernel)
ldd=$(shell chroot $(blddir) /usr/bin/ldd $(1) | sed -e's/.*=> //' -e's/ \(.*\)//' -e's/\s*//')
lib64targets=$(patsubst /lib64/%, $(blddir)/usr/local/share/initrd/lib64/%, $(filter /lib64/%, $(1)))
usrlib64targets=$(patsubst /usr/lib64/%, $(blddir)/usr/local/share/initrd/lib64/%, $(filter /usr/lib64/%, $(1)))

# List of various file types extracted from the root directory
#
cryptsetup-files:=$(call ldd, /sbin/cryptsetup)
resize2fs-files:=$(call ldd, /sbin/resize2fs)
parted-files:=$(call ldd, /sbin/parted)
mkfs.ext4-files:=$(call ldd, /sbin/mkfs.ext4)

cryptsetup-lib64-targets=$(call lib64targets, $(cryptsetup-files))
resize2fs-lib64-targets=$(call lib64targets, $(resize2fs-files))
parted-lib64-targets=$(call lib64targets, $(parted-files))
mkfs.ext4-lib64-targets=$(call lib64targets, $(mkfs.ext4-files))

cryptsetup-usrlib64-targets=$(call usrlib64targets, $(cryptsetup-files))
resize2fs-usrlib64-targets=$(call usrlib64targets, $(resize2fs-files))
parted-usrlib64-targets=$(call usrlib64targets, $(parted-files))
mkfs.ext4-usrlib64-targets=$(call usrlib64targets, $(mkfs.ext4-files))

cryptsetup-targets:=$(cryptsetup-lib64-targets) $(cryptsetup-usrlib64-targets)
resize2fs-targets:=$(resize2fs-lib64-targets) $(resize2fs-usrlib64-targets)
parted-targets:=$(parted-lib64-targets) $(parted-usrlib64-targets)
mkfs.ext4-targets:=$(mkfs.ext4-lib64-targets) $(mkfs.ext4-usrlib64-targets)

dirs:=$(patsubst root/%, $(blddir)/%, $(shell find root/ -type d))
rdirs:=$(filter-out $(blddir)/ $(blddir)/etc% $(blddir)/boot% $(blddir)/usr $(blddir)/usr/local $(blddir)/usr/local/share, $(patsubst root/%, $(blddir)/%, $(shell find root/ -depth -type d)))
sxfiles:=$(patsubst src/%, $(blddir)/%, $(shell find src -type f -perm -100))
snfiles:=$(patsubst src/%, $(blddir)/%, $(shell find src -type f ! -perm -100))
xfiles:=$(patsubst root/%, $(blddir)/%, $(shell find root -type f -perm -100))
nfiles:=$(patsubst root/%, $(blddir)/%, $(shell find root -type f ! -perm -100))
files:=$(xfiles) $(nfiles) $(sxfiles) $(snfiles)

busybox-files:=$(addprefix $(blddir)/usr/local/share/initrd/bin/, \
  cat \
  dd \
  head \
  less \
  ls \
  mount \
  sh \
  vi \
)

grub-files:=$(patsubst $(blddir)/usr/share/grub/x86_64-unknown/%, $(blddir)/boot/grub/%, $(shell find $(blddir)/usr/share/grub/x86_64-unknown -type f))

all: $(blddir)/boot/initrd-$(kernelverrelease).x86_64.img \
     $(files) \
     $(blddir)/home/admin \
     services \
     sysctl \
     run-postinstall \
     $(grub-files)

$(grub-files): $(blddir)/boot/grub/%: $(blddir)/usr/share/grub/x86_64-unknown/%
	cp -L $< $(@D)

$(blddir)/boot/initrd-$(kernelverrelease).x86_64.img: \
      $(blddir)/etc/fstab \
      $(blddir)/usr/local/share/initrd/bin/cryptsetup \
      $(blddir)/usr/local/share/initrd/bin/mkfs.ext4 \
      $(blddir)/usr/local/share/initrd/bin/parted \
      $(blddir)/usr/local/share/initrd/bin/resize2fs \
      $(blddir)/usr/local/share/initrd/init \
      $(blddir)/usr/local/share/initrd/etc/keyfile \
      $(blddir)/usr/local/share/initrd/etc/rc.local \
      $(busybox-files) \
      $(cryptsetup-targets) \
      $(mkfs.ext4-targets) \
      $(parted-targets) \
      $(resize2fs-targets) \
      $(blddir)/usr/local/share/initrd/lib/dm-mod.ko \
      $(blddir)/usr/local/share/initrd/lib/dm-crypt.ko \
      $(blddir)/usr/local/share/initrd/lib/virtio_mmio.ko \
      $(blddir)/usr/local/share/initrd/lib/virtio.ko \
      $(blddir)/usr/local/share/initrd/lib/virtio_balloon.ko \
      $(blddir)/usr/local/share/initrd/lib/virtio_pci.ko \
      $(blddir)/usr/local/share/initrd/lib/virtio_ring.ko \
      $(blddir)/usr/local/share/initrd/lib/virtio_net.ko \
      $(blddir)/usr/local/share/initrd/lib/virtio-rng.ko \
      $(blddir)/usr/local/share/initrd/lib/virtio_console.ko \
      $(blddir)/usr/local/share/initrd/lib/virtio_blk.ko \
      $(blddir)/usr/local/share/initrd/lib/vmw_pvscsi.ko
	chroot $(blddir) /sbin/mkinitrd --force --fresh --version $(kernelverrelease).x86_64 --output /boot/initrd-$(kernelverrelease).x86_64.img --proto /usr/local/share/initrd

      #$(blddir)/usr/local/share/initrd/lib/virtio_scsi.ko

# May be needed for above rule (Maybe better to place in lib and let the normal initrd do it's thing

$(blddir)/usr/local/share/initrd/lib/dm-mod.ko: $(blddir)/lib/modules/$(kernelverrelease).x86_64/kernel/drivers/md/dm-mod.ko | $(blddir)/usr/local/share/initrd/lib
	install -o root -g root -m 755 $< $@

$(blddir)/usr/local/share/initrd/lib/dm-crypt.ko: $(blddir)/lib/modules/$(kernelverrelease).x86_64/kernel/drivers/md/dm-crypt.ko | $(blddir)/usr/local/share/initrd/lib
	install -o root -g root -m 755 $< $@

$(blddir)/usr/local/share/initrd/lib/virtio_mmio.ko : $(blddir)/lib/modules/$(kernelverrelease).x86_64/kernel/drivers/virtio/virtio_mmio.ko | $(blddir)/usr/local/share/initrd/lib
	install -o root -g root -m 755 $< $@

$(blddir)/usr/local/share/initrd/lib/virtio.ko : $(blddir)/lib/modules/$(kernelverrelease).x86_64/kernel/drivers/virtio/virtio.ko | $(blddir)/usr/local/share/initrd/lib
	install -o root -g root -m 755 $< $@

$(blddir)/usr/local/share/initrd/lib/virtio_balloon.ko : $(blddir)/lib/modules/$(kernelverrelease).x86_64/kernel/drivers/virtio/virtio_balloon.ko | $(blddir)/usr/local/share/initrd/lib
	install -o root -g root -m 755 $< $@

$(blddir)/usr/local/share/initrd/lib/virtio_pci.ko : $(blddir)/lib/modules/$(kernelverrelease).x86_64/kernel/drivers/virtio/virtio_pci.ko | $(blddir)/usr/local/share/initrd/lib
	install -o root -g root -m 755 $< $@

$(blddir)/usr/local/share/initrd/lib/virtio_ring.ko : $(blddir)/lib/modules/$(kernelverrelease).x86_64/kernel/drivers/virtio/virtio_ring.ko | $(blddir)/usr/local/share/initrd/lib
	install -o root -g root -m 755 $< $@

$(blddir)/usr/local/share/initrd/lib/virtio_net.ko : $(blddir)/lib/modules/$(kernelverrelease).x86_64/kernel/drivers/net/virtio_net.ko | $(blddir)/usr/local/share/initrd/lib
	install -o root -g root -m 755 $< $@

$(blddir)/usr/local/share/initrd/lib/virtio-rng.ko : $(blddir)/lib/modules/$(kernelverrelease).x86_64/kernel/drivers/char/hw_random/virtio-rng.ko | $(blddir)/usr/local/share/initrd/lib
	install -o root -g root -m 755 $< $@

$(blddir)/usr/local/share/initrd/lib/virtio_console.ko : $(blddir)/lib/modules/$(kernelverrelease).x86_64/kernel/drivers/char/virtio_console.ko | $(blddir)/usr/local/share/initrd/lib
	install -o root -g root -m 755 $< $@

$(blddir)/usr/local/share/initrd/lib/virtio_blk.ko : $(blddir)/lib/modules/$(kernelverrelease).x86_64/kernel/drivers/block/virtio_blk.ko | $(blddir)/usr/local/share/initrd/lib
	install -o root -g root -m 755 $< $@

$(blddir)/usr/local/share/initrd/lib/vmw_pvscsi.ko : $(blddir)/lib/modules/$(kernelverrelease).x86_64/kernel/drivers/scsi/vmw_pvscsi.ko | $(blddir)/usr/local/share/initrd/lib
	install -o root -g root -m 755 $< $@

$(blddir)/usr/local/share/initrd/lib/virtio_scsi.ko : $(blddir)/lib/modules/$(kernelverrelease).x86_64/kernel/drivers/scsi/virtio_scsi.ko | $(blddir)/usr/local/share/initrd/lib
	install -o root -g root -m 755 $< $@

$(blddir)/usr/local/share/initrd/init: $(blddir)/usr/lib/initrd/init $(blddir)/usr/local/share/initrd
	cp $< $@

$(busybox-files): $(blddir)/usr/local/share/initrd/bin/busybox | $(blddir)/usr/local/share/initrd/bin
	ln -fs busybox $@

$(blddir)/usr/local/share/initrd/bin/busybox: $(blddir)/sbin/busybox $(blddir)/usr/local/share/initrd/bin
	cp -L $< $(@D)

$(dirs) $(blddir)/usr/local/share/initrd/lib:
	mkdir -p -m 755 $@

$(xfiles): $(blddir)/%: root/% | $(dirs) .FORCE
	install -o root -g root -m 755 $< $(@D)

$(nfiles): $(blddir)/%: root/% | $(dirs) .FORCE
	install -o root -g root -m 644 $< $(@D)

$(blddir)/usr/local/share/initrd/bin/cryptsetup: $(blddir)/sbin/cryptsetup | $(blddir)/usr/local/share/initrd/bin/
	cp -L $< $(@D)/

$(blddir)/usr/local/share/initrd/bin/mkfs.ext4: $(blddir)/sbin/mkfs.ext4 | $(blddir)/usr/local/share/initrd/bin/
	cp -L $< $(@D)/

$(blddir)/usr/local/share/initrd/bin/parted: $(blddir)/sbin/parted | $(blddir)/usr/local/share/initrd/bin/
	cp -L $< $(@D)/

$(blddir)/usr/local/share/initrd/bin/resize2fs: $(blddir)/sbin/resize2fs | $(blddir)/usr/local/share/initrd/bin/
	cp -L $< $(@D)/

$(sort $(cryptsetup-lib64-targets) $(resize2fs-lib64-targets) $(parted-lib64-targets) $(mkfs.ext4-lib64-targets)): $(blddir)/usr/local/share/initrd/lib64/%: $(blddir)/lib64/% | $(blddir)/usr/local/share/initrd/lib64
	cp -L $< $(@D)/

$(sort $(cryptsetup-usrlib64-targets) $(resize2fs-usrlib64-targets) $(parted-usrlib64-targets) $(mkfs.ext4-usrlib64-targets)): $(blddir)/usr/local/share/initrd/lib64/%: $(blddir)/usr/lib64/% | $(blddir)/usr/local/share/initrd/lib64
	cp -L $< $(@D)/

$(blddir)/boot/grub/grub.conf: src/boot/grub/grub.conf
	sed -e 's/KERNELVERRELEASE/$(kernelverrelease)/g' $< > $@

$(blddir)/usr/local/share/initrd/etc/keyfile: | $(blddir)/usr/local/share/initrd/etc
	dd if=/dev/urandom of=$@ bs=128 count=1

$(blddir)/home/admin: $(blddir)/opt/vmidc/bin/vmidcShell.py
	chroot $(blddir) adduser admin
	echo admin123 | chroot $(blddir) passwd --stdin admin
	echo admin123 | chroot $(blddir) passwd --stdin root
	chroot $(blddir) chsh -s /opt/vmidc/bin/vmidcShell.py admin
	chroot $(blddir) chsh -s /sbin/nologin root

services: $(blddir)/etc/init.d/securityBroker $(blddir)/etc/init.d/firstconfig
	chroot $(blddir) chkconfig --add securityBroker
	chroot $(blddir) chkconfig --level 2345 securityBroker on
	chroot $(blddir) chkconfig --add firstconfig
	chroot $(blddir) chkconfig --level 2345 firstconfig on
	chroot $(blddir) chkconfig --add ntpd
	chroot $(blddir) chkconfig --level 2345 ntpd on
	chroot $(blddir) chkconfig --add ntpdate
	chroot $(blddir) chkconfig --level 2345 ntpdate on
	chroot $(blddir) chkconfig --add sshd
	chroot $(blddir) chkconfig --level 2345 sshd on
	chroot $(blddir) chkconfig --add vmtoolsd
	chroot $(blddir) chkconfig --level 2345 vmtoolsd on

sysctl:
	sed -i -e '/^kernel.printk/d' $(blddir)/etc/sysctl.conf
	sed -i -e '/^net.ipv6.conf/d' $(blddir)/etc/sysctl.conf
	echo kernel.printk = 4 4 1 7 >> $(blddir)/etc/sysctl.conf
	echo net.ipv6.conf.all.disable_ipv6 = 1 >> $(blddir)/etc/sysctl.conf
	echo net.ipv6.conf.default.disable_ipv6 = 1 >> $(blddir)/etc/sysctl.conf
	echo net.ipv6.conf.lo.disable_ipv6 = 1 >> $(blddir)/etc/sysctl.conf

run-postinstall: $(blddir)/boot/initrd-$(kernelverrelease).x86_64.img \
                 $(files) \
                 $(blddir)/home/admin \
                 services \
                 sysctl
	cat postinstall | chroot $(blddir) bash -x

clean:
	-chroot $(blddir) userdel -r admin
	@for f in $(busybox-files) \
	          $(grub-files) \
	          $(cryptsetup-files) \
	          $(mkfs.ext4-files) \
	          $(parted-files) \
	          $(resize2fs-files) \
	          $(files) \
	          $(blddir)/boot/initrd-$(kernelverrelease).x86_64.img \
	          $(blddir)/usr/local/share/initrd/bin/busybox \
	          $(blddir)/usr/local/share/initrd/bin/cryptsetup \
	          $(blddir)/usr/local/share/initrd/bin/mkfs.ext4 \
	          $(blddir)/usr/local/share/initrd/bin/parted \
	          $(blddir)/usr/local/share/initrd/bin/resize2fs \
	          $(blddir)/usr/local/share/initrd/etc/keyfile \
	          $(blddir)/usr/local/share/initrd/init; \
	do if [ -f $$f ]; then echo $(RM) $$f; $(RM) $$f; fi; done
	@for d in $(rdirs); do if [ -d $$d ]; then echo rmdir $$d; rmdir $$d; fi; done

.FORCE:

.dummy:
#$(blddir)/etc/issue $(blddir)/etc/system-release: $(blddir)/%: src/% | $(dirs) .FORCE
	#sed -e "s/VERSION/$(buildNumber)/" < $< > $@ 

