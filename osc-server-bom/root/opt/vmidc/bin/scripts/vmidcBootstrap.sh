#!/bin/sh

#Remove dhcp client
sed -i -e '/dhclient/d' /etc/rc.local
sed -i -e '/dhclient/d' /etc/rc.d/rc.local

echo "`date` - Editing rc.local script"  > /tmp/vmidcBootstrap.log
sed "$ a\\echo \"\`date\` - Starting vmiDC Server\" >> /tmp/vmidcBootstrap.log \\
if [ ! -d /mnt/media ]; then \\
    mkdir /mnt/media \\
fi \\
mount /dev/cdrom1 /mnt/media \\
cd /opt/vmidc/bin \\
./vmidc.sh > /dev/null 2>&1 &" /etc/rc.d/rc.local  > tmp
mv tmp /etc/rc.d/rc.local
chmod +x /etc/rc.d/rc.local

chmod +x /opt/vmidc/bin/vmidc.sh
chmod +x /opt/vmidc/bin/vmidcShell.py
chmod +x /opt/vmidc/jre/bin/java
chmod +x /opt/vmidc/jre/bin/keytool
#chmod 755 /etc/init.d/securityBroker
#chmod 755 /etc/init.d/firstconfig

chmod 750 /etc/sudoers.d
chmod 640 /etc/sudoers.d/*

#chkconfig --add firstconfig
#chkconfig --level 2345 firstconfig on
#chkconfig --add securityBroker
#chkconfig --level 2345 securityBroker on

# User admin with restrict shell
#set default password to admin123
adduser admin
chsh -s /opt/vmidc/bin/vmidcShell.py admin
echo admin123 | passwd --stdin admin

# admin sudoer is now handled by /etc/sudoers.d/admin
sed -i -e '/^admin/d' /etc/sudoers

# No root nologon / Still set the password
chsh -s /sbin/nologin root
echo admin123 | passwd --stdin root

yum install -y acl

#install graphviz
cp /opt/vmidc/bin/scripts/centos-6.repo /etc/yum.repos.d/centos-6.repo

yum clean all
yum repolist all
yum-config-manager --disable base updates updates-testing
yum-config-manager --enable 'centos*'
yum repolist all

yum install -y graphviz
dot -V > /tmp/dot.out 2>&1

yum repolist all
yum-config-manager --enable base updates updates-testing
yum-config-manager --disable 'centos*'
yum repolist all
yum clean all

#Remove hard coded settings used to build appliance
sed -i -e '/10.71.86/d' /etc/rc.local
sed -i -e '/10.71.86/d' /etc/rc.d/rc.local
rm -rf /etc/resolv.conf
touch /etc/resolv.conf

rm -f /opt/vmidc/bin/scripts/vmidcBootstrap.sh
rm -f /opt/vmidc/bin/scripts/centos-6.repo

nohup dhclient -r > /tmp/vmidcDeploy.out 2> /tmp/vmidcDeploy.err < /dev/null &
