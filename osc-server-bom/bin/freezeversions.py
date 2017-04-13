#!/usr/bin/python

#
# Freeze the rpm versions found in the files:
#
#    centos-6.packages1
#    centos-6.packages2
#    centos-6-new.packages
#
# New files are created with the name
#
#    <name>.frozen
#
# Do a diff, move and copy over
#

import os
import sys
import yum
import re
import shutil
from yum.sqlitesack import YumAvailablePackageSqlite
from yum.packageSack import packagesNewestByName, packagesNewestByNameArch, ListPackageSack

def resolved_name(pkg):
    """ Return the resolved name for the given package """
    return "%s %s" % (pkg.name, pkg.arch)


def is_newly_resolved(pkg, pkg_names):
    """ Determine if the given pkg is in the list pkg_names and adds if not there"""

    if not pkg:
        return False

    ui_na = resolved_name(pkg)
    if ui_na in pkg_names:
        return False

    pkg_names.add(ui_na)
    return True


def format_number(number, SI=0, space=' '):
    """Turn numbers into human-readable metric-like numbers"""
    symbols = [ ' ', # (none)
                'k', # kilo
                'M', # mega
                'G', # giga
                'T', # tera
                'P', # peta
                'E', # exa
                'Z', # zetta
                'Y'] # yotta

    if SI: step = 1000.0
    else: step = 1024.0

    thresh = 999
    depth = 0
    max_depth = len(symbols) - 1

    # we want numbers between 0 and thresh, but don't exceed the length
    # of our list.  In that event, the formatting will be screwed up,
    # but it'll still show the right number.
    while number > thresh and depth < max_depth:
        depth  = depth + 1
        number = number / step

    if type(number) == type(1) or type(number) == type(1L):
        format = '%i%s%s'
    elif number < 9.95:
        # must use 9.95 for proper sizing.  For example, 9.99 will be
        # rounded to 10.0 with the .1f format string (which is too long)
        format = '%.1f%s%s'
    else:
        format = '%.0f%s%s'

    return(format % (float(number or 0), space, symbols[depth]))


def most_recent(values, arch):
    """ Returns the most recent version of a package out of list of
        packages, prefering the given archecture. 'x86_64' is preferred
        over 'i686' for 'noarch'. In addtion the repo ('base/update') that
        is first alphabetically will be preferred """

    # Can't have a most recent with an empty list
    if len(values) == 0:
        return None

    # Prefer the repo alphabetically first
    values = sorted(values, key=lambda v: (v.repo, v))

    pkgs = packagesNewestByNameArch(values)

    # Find a package that meets the arch
    for pkg in pkgs:
        if pkg.arch == arch:
            return pkg

    # No arch so see if x86_64 exists
    if arch == 'noarch':
        for pkg in pkgs:
            if pkg.arch == 'x86_64':
                return pkg

    # Must used this version
    return pkgs[0]


def resolve_dependencies(yb, pkgs, resolved_pkgs, resolved_dependencies):
    """ Resolve all the dependecies for a list of pkgs ignoring those
        packages found in resolved_pkgs, or those dependencies found in
        resolved_dependencies"""

    newpkgs = []
    for pkg in pkgs:
        for req in pkg.requires:
            (r, f, v) = req
            if r.startswith('rpmlib('):
                continue

            if req not in resolved_dependencies:
               po = most_recent(yb.whatProvides(r, f, v), pkg.arch)
               if is_newly_resolved(po, resolved_pkgs):
                   newpkgs.append(po)
                   resolved_dependencies |= set(po.provides)

               resolved_dependencies.add(req)

    return newpkgs


def freeze_package_file(file, config, resolved_pkgs, resolved_dependencies):
    """ Free a package file using the given config file, resolved_pkgs and
        resolved_dependencies indicate previous runs that resolved packages"""

    yb = yum.YumBase()
    yb.preconf.fn = config
    yb.conf.cache = 0

    pkgs = []
    names = sorted(list(set([l.rstrip() for l in open(file).readlines() if not re.match("(^#|^$)", l)])))
    for name in names:
        ematch, match, umatch = yb.pkgSack.matchPackageNames([name])
        pkg = most_recent(ematch + match, 'x86_64')
        if pkg:
           resolved_pkgs.add(resolved_name(pkg))
           pkgs.append(pkg)
           resolved_dependencies |= set(pkg.provides)

    deppkgs = []
    newpkgs = resolve_dependencies(yb, pkgs, resolved_pkgs, resolved_dependencies)
    while len(newpkgs) > 0:
        deppkgs += newpkgs
        newpkgs = resolve_dependencies(yb, newpkgs, resolved_pkgs, resolved_dependencies)

    pkgs += sorted(deppkgs)

    print "FOR %s" % (file)
    print "================================================================================"
    print " Package              Arch     Version                  Repository         Size"
    print "================================================================================"
    for p in pkgs:
       if int(p.epoch) > 0:
           verStr = "%s:%s-%s" % (p.epoch, p.ver, p.rel)
       else:
           verStr = "%s-%s" % (p.ver, p.rel)
       print " %-21s%-9s%-25s%-15s%8s" % (p.name, p.arch, verStr, p.repo, format_number(p.size))
    print

    nf = open(file + ".frozen", "w")
    for p in pkgs:
       nf.write(p.ui_nevra)
       nf.write('\n')
    nf.close()


resolved_pkgs = set()
resolved_dependencies = set()
for (file, config) in [("centos-6.packages1", "repo/centos-6-updated.yum.conf"), ("centos-6.packages2", "repo/centos-6-updated.yum.conf"), ("centos-6-new.packages", "repo/centos-6-updated.yum.conf"), ("centos-epel.packages", "repo/centos-epel.yum.conf")]:
    freeze_package_file(file, config, resolved_pkgs, resolved_dependencies)
