
bootfs=`findfs UUID="de505a44-50b7-4757-87e5-a1a547de8dd4"`
if [ "$bootfs" == "" ]; then
   echo ERROR: Could not find boot partition!!!!
else
  hd=`echo $bootfs | /bin/busybox sed -e 's/.$//'`

  luksfs="${hd}3"
  swapfs="${hd}2"

  #
  # If the 3rd partition isnn't any encrypted luks file system, must be the first
  #  boot on this image, so finish creating the image by
  #  making a filesystem encrypted and expanding the tarball found
  #  in the swap partition
  #
  echo starting create file system
  if ! /bin/cryptsetup isLuks $luksfs 2> /dev/null; then
    echo Encrypting filesystem $luksfs
    echo YES | /bin/cryptsetup --key-file=/etc/keyfile --uuid=c09a4398-4a0e-4e25-ac2d-85c05ba01b57 luksFormat $luksfs
    /bin/cryptsetup --key-file=/etc/keyfile luksOpen $luksfs root
    echo Formatting filesystem
    /bin/busybox touch /etc/mtab
    /bin/mkfs.ext4 -q /dev/mapper/root -U 053f2742-c607-4a40-a05c-c39cdeeb97f6
    /bin/mount -t ext4 /dev/mapper/root /sysroot
    echo Expanding filesystem
    tar -Oxf $swapfs | tar -C /sysroot -xJf -
    /bin/busybox umount /sysroot
  else
    echo file system created ..open rootfs
    rootfs=` findfs UUID="c09a4398-4a0e-4e25-ac2d-85c05ba01b57"`
    /bin/cryptsetup --key-file=/etc/keyfile luksOpen $rootfs root
  fi
fi
