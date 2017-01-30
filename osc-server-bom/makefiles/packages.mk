##
## Copyright (C) 2014  McAfee Inc.
##
## Makefile to install the vmidc components into
##  the directory $(blddir). By using the
##  mount.mk makefile this will be a raw disk image
##

## Everthing runs as root with a umask of 0000 so that all permissions
##  are perserved
SHELL = exec /bin/bash --noprofile -c 'umask 0000; shift; eval "$$1"' --

.PHONY: all vmidc mlos yum-vmidc yum-mlos clean cleanup rpm-list

blddir?=build

## Use the cache on the installation machine so that it doesn't
##   use up space on the new machine
yumcache?=$(abspath $(blddir)/../var/cache/yum/)
yumbuild?=$(blddir)/var/cache/yum
dowithyummount= \
  mount --bind $(yumcache) $(yumbuild) && \
  trap "umount $(yumbuild)" EXIT &&

##
## Build the image from MLOS and CentOS repos
##
all: vmidc

vmidc: | $(yumcache) $(yumbuild)
	$(dowithyummount) $(MAKE) -f $(lastword $(MAKEFILE_LIST)) vmidc-doit

#
# Have to duplicate the clean as -j with a dependency would clean it before hand
#
vmidc-doit: yum-vmidc-doit
	yum --enablerepo=base --installroot=$(blddir) clean metadata

## yum-vmidc: RPM-GPG-KEY-MLOS.pub mount-cache
yum-vmidc-doit: yum-basic-doit
	yum -y -c repo/mlos.yum.conf --releasever=2.2 --installroot=$(blddir) install `grep -v ^# mlos.packages2`
	yum -y -c repo/centos-6.yum.conf --releasever=2.2 --installroot=$(blddir) install `grep -v ^# centos-6.packages`
	chroot $(blddir) /bin/bash -c "localedef --list-archive | grep -v -i ^en | xargs localedef --delete-from-archive"
	/bin/mv $(blddir)/usr/lib/locale/locale-archive $(blddir)/usr/lib/locale/locale-archive.tmpl
	chroot $(blddir) /usr/sbin/build-locale-archive

RPM-GPG-KEY-MLOS.pub:
	#wget --quiet -O $@ http://mlos.corp.nai.org/mlos/mlosrepo/KEYS/2/RPM-GPG-KEY-MLOS.pub
	#wget --quiet -O $@ http://mlossnc.corp.nai.org/mlos/mlosrepo/KEYS/2/RPM-GPG-KEY-MLOS.pub

yum-basic-doit: $(blddir)/etc/rpm/macros
	yum -y -c repo/mlos.yum.conf --releasever=2.2 --installroot=$(blddir) clean all
	yum -y -c repo/mlos.yum.conf --releasever=2.2 --installroot=$(blddir) clean metadata
	yum -y -c repo/mlos.yum.conf --releasever=2.2 --installroot=$(blddir) install `grep -v ^# mlos.packages1`

$(blddir)/etc/rpm/macros: $(blddir)/etc/rpm
	echo -e "%_install_langs C:en_US\n%_excludedocs 1\n" > $@

##
## Create the rpm and cache directories
##
$(blddir)/etc/rpm $(yumcache):
	mkdir -p -m 755 $@

$(yumbuild): init-rpm
	mkdir -p -m 755 $@

init-rpm:
	rpmdb --root $(blddir) --initdb

clean: cleanup
	$(RM) RPM-GPG-KEY-MLOS.pub

realclean: clean
	$(RM) -r $(yumcache)

##
## Cleanup yum after we are done installing all the packages
##
cleanup:

##
## List packages in the image
##
rpm-list:
	rpm --installroot=$(blddir) -qa
