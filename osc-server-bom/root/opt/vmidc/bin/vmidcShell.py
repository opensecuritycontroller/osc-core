#!/usr/bin/python
# Copyright (c) Intel Corporation
# Copyright (c) 2017
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
  vmidcCliShell.py

  Basic command line interface for the vmiDC server. Offers basic command to configure the system.
  Invoke the program then type
   
    list

  to see the list of commands implemented by the shell.


  Uses the python class Cmd (which is part of the default distro) to implement the command line.
  Cmd provides command line completition as well as help functionality. Read the online documentation
  for specifics.

  The ExtendedCmd class provides for chaining together Cmd objects so that multi-level commands
  can be written. For example the following user command

  set network ntp

  is invoked by using two different ExtendedCmd objects, one for "set", one for "network".
  "ntp" is handled by the "do_ntp" method of the SetNetworkPrompt(ExtendedCmd) class.

  ExtendedCmd wires the completition and list functionality, see the documentation
  for that class for specifics.
"""

import atexit
import getpass
import re
import os
import readline
import signal
import socket
import subprocess
import sys
import tempfile
from osc_pbkdf2_key import pbkdf2_operations,PBKDF2_KEY_INFO_VMIDC_FILE_PATH 


from cmd import Cmd

VMIDCSERVICE="securityBroker"
VMIDCLOG="/opt/vmidc/bin/log/securityBroker.log"
IPREGEX="^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
IPCIDRREGEX="^(((?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)/(?:2[0-4]|1[0-9]|[0-9]))|dhcp)$"
#Modified Domain regex to accept null value and hence adopted new regex value for NTP_DOMAIN--which is not changed
DOMAINREGEX="^$|^[^\s]+$"
NTP_DOMAINREGEX="^[^\s]+$"
HOSTNAMEREGEX="^[^\s]+$"


#MMDDhhmm[[CC]YY][.ss]]
TIMEREGEX="^(1[0-2]|0[1-9])(3[0-1]|[12][0-9]|0[1-9])(2[0-4]|1[0-9]|0[1-9])(60|[1-5][0-9]|0[1-9])(20[0-9][0-9])(\.60|[1-5][0-9]|0[1-9])?$"

def handler(signum, frame):
    """Ignore"""

def restart_service(servicename):
  """
    Restart a service

      servicename - the name of the service to restart
  """
  subprocess.call(["/usr/bin/sudo", "/sbin/service", servicename, "restart"])


def stop_service(servicename):
  """
   Stop a service

      servicename - the name of the service to stop
  """
  subprocess.call(["/usr/bin/sudo", "/sbin/service", servicename, "stop"])


def start_service(servicename):
  """
   start a service

      servicename - the name of the service to start
  """
  subprocess.call(["/usr/bin/sudo", "/sbin/service", servicename, "start"])


def status_service(servicename):
  """
   emit the status of a server

      servicename - the name of the service to emit the status for
  """
  subprocess.call(["/usr/bin/sudo", "/sbin/service", servicename, "status"])


def enable_service(servicename):
  """
   enable a service to start on startup

      servicename - the name of the service to start
  """
  subprocess.call(["/usr/bin/sudo", "/sbin/chkconfig", "--add", servicename])
  subprocess.call(["/usr/bin/sudo", "/sbin/chkconfig", "--level", "2345", servicename, "on"])


def disable_service(servicename):
  """
   disable a service from starting on startup

      servicename - the name of the service to stop 
  """
  subprocess.call(["/usr/bin/sudo", "/sbin/chkconfig", "--level", "2345", servicename, "off"])
  subprocess.call(["/usr/bin/sudo", "/sbin/chkconfig", "--del", servicename])


def validate(lines, regex, errmsg):
  """ Validate a series of lines vs a regex

     lines   -  array of strings
     regex   -  a regular expression to match against
     errmsg  -  an error msg to emit if an error is found
                should contain %s to print error string

     returns None on failure, 1 on success
  """

  for line in lines:
   if not re.match(regex, line):
     if errmsg:
       sys.stdout.write(errmsg%(line) + "\n")
     return None

  return 1

def validate2(lines, regexs, errmsg):
  """ Validate a series of lines vs multiple regex

     lines   -  array of strings
     regexs  -  an array of regular expression to match against
     errmsg  -  an error msg to emit if an error is found
                should contain %s to print error string

     returns None on failure, 1 on success
  """

  for line in lines:
   match = None
   for regex in regexs:
     if re.match(regex, line):
       match = 1

   if not match:
     if errmsg:
       sys.stdout.write(errmsg%(line) + "\n")

     return None

  return 1

def emit(lines):
  """ Emit a series of lines to stdout """
  for line in lines:
    sys.stdout.write(line)

def collect(filename, regex = None, negregex = None, start = [], end = []):
  """ Collect the lines from a file into an array, filtering the results
   
      filename  -  the filename to collect lines from
      regex     -  collect lines that only match the given expresssion
      negregex  -  exclude lines that match the given expression
      start     -  additional elements at the start
      end       -  additional elements at the end
  """
  out = start
  with open(filename) as infile:
    for line in infile:
      if not regex or re.search(regex, line):
        if not negregex or not re.search(negregex, line):
          out.append(line[:-1])

  for line in end:
    out.append(line)

  return out

def replace(filename, lines):
  """Replaces a given file with an array of strings, while
     retaining the permissions of the replaced file

      filename   - the file to replace
      lines      - the lines to replace with, newlines will be appended
  """
  tmp = tempfile.NamedTemporaryFile(delete=False)
  for line in lines:
    tmp.write(line + "\n")

  tmp.close()

  # 
  # Preserve the premissions from the old file
  #
  subprocess.call(["/usr/bin/sudo", "/bin/chmod", "--reference=" + filename, tmp.name])
  subprocess.call(["/usr/bin/sudo", "/bin/chown", "--reference=" + filename, tmp.name])
  getfacl = subprocess.Popen(["/usr/bin/sudo", "/usr/bin/getfacl", "-p", filename], stdout=subprocess.PIPE)
  setfacl = subprocess.Popen(["/usr/bin/sudo", "/usr/bin/setfacl", "--set-file=-", tmp.name], stdin=getfacl.stdout)

  getfacl.wait()
  setfacl.wait()

  # 
  # Move the file
  #
  subprocess.call(["/usr/bin/sudo", "/bin/mv", tmp.name, filename])

def cat(filename):
  """Emits a given file to stdout

     filename - the name of the file to emit
  """
  with open(filename) as infile:
    for line in infile:
      sys.stdout.write(line)

def grep(regex, filename):
  """Emits the lines given file to stdout that match a regex

     regex    - regular express to match
     filename - the name of the file to emit
  """
  with open(filename) as infile:
    for line in infile:
      if re.search(regex, line):
        sys.stdout.write(line)

def filter(regex, filename):
  """Emits parts of the lines given file to stdout that match a regex 
     Only emits the groups (e.g. "([0-9]{7])") of the regex

     regex    - regular express to match, should contain groups
     filename - the name of the file to emit
  """
  with open(filename) as infile:
    for line in infile:
      match = re.search(regex, line)
      if match:
        for m in match.groups():
          sys.stdout.write(m + " ")
        sys.stdout.write("\n")

class ExtendedCmd(Cmd):
  """The ExtendCmd class provides a sub-command extension to the python Cmd class.

     For any method named sub_* a sub-command is enabled that allows for multiple
     word commands. sub_* is must return a boject that has the onecmd and do_list
     methods. Basically a do_* is added that will call the onecmd method
     of the Cmd object and list_* is added that will call the do_list method
  """

  def __init__(self):
    """Initialize a MyCmd object. 

       Searches for any methods named sub_* and adds the do_* and list_* methods
    """
    Cmd.__init__(self)

    names = dir(self.__class__)
    for name in names:
      if name[:4] == 'sub_':
        sub = getattr(self, name)()
        subname = name[4:]

        setattr(self, subname, sub)

        def make_sub(asub, doc):
          def do_sub(self, args):
            asub.onecmd(args)

          do_sub.__doc__ = doc
         
          return do_sub

        setattr(self.__class__, "do_" + subname, make_sub(sub, getattr(self, name).__doc__))
        if "list_" + subname not in names:
          def make_list(asub, aname):
            def do_list(self, args):
              asub.do_list(args + aname + " ")

            return do_list

          setattr(self.__class__, "list_" + subname, make_list(sub, subname))

  def completedefault(self, text, line, begidx, endidx):
    """Wire the completenames of an sub-command to the current line"""
    cmd, arg, line = self.parseline(line)
    if cmd in dir(self):
      return getattr(self, cmd).completedefault(arg, arg, begidx, endidx)

    return self.completenames(text, line, begidx, endidx)

  def completenames(self, text, line, begidx, endidex):
    r = Cmd.completenames(self, text, line, begidx, endidex)

    if len(r) == 1 and "sub_" + r[0] in dir(self):
      return [r[0] + " "]

    return r

  def do_list(self, args):
    """Print Comamnd List"""
    names = dir(self.__class__)

    list = {}
    for name in names:
      if name[:5] == 'list_':
        list[name[5:]]=1

    names.sort()

    # There can be duplicates if routines overridden
    prevname = ''
    for name in names:
      if name[:3] == 'do_':
        if name == prevname:
          continue
        prevname = name
        cmd=name[3:]

        if args and cmd == "list":
           continue

        if args and cmd == "help":
           continue

        if getattr(self, name).__doc__:
          doc = str(getattr(self, name).__doc__)
          if ":" in doc:
            pcmd = args + cmd + " " + doc.split(":", 1)[0]
            pdoc = doc.split(":", 1)[1]
          else:
            pcmd = args + cmd
            pdoc = doc

          sys.stdout.write("%-40s %s\n"%(pcmd, pdoc))

        if cmd in list:
          getattr(self, "list_" + cmd)(args)

  def emptyline(self):
    sys.stdout.write("Expected one of\n")
    self.do_list("   ")

class ServerPrompt(ExtendedCmd):
  """Server Prompt

    Expected to be used when the user enters

       server ...
  """

  def do_start(self, args):
    """Start Open Security Controller server"""
    start_service(VMIDCSERVICE)

  def do_stop(self, args):
    """Stop Open Security Controller server"""
    stop_service(VMIDCSERVICE)

  def do_restart(self, args):
    """Restart Open Security Controller server"""
    restart_service(VMIDCSERVICE)

  def do_status(self, args):
    """Show Open Security Controller server Status"""
    status_service(VMIDCSERVICE)

class SetNetworkPrompt(ExtendedCmd):
  """Set Network Prompt class

    Expected to be used when the user enters

       set network ...
  """
   
  def do_domain(self, args):
    """<domainname>:Set DNS domain name"""
    if validate([args], DOMAINREGEX, "Illegal domain name %s"):
      replace("/etc/resolv.conf", collect("/etc/resolv.conf", None, "domain", ["domain " + args]))

  def do_dns(self, args):
    """<IP> [<IP> ...]:Set DNS servers"""
    servers = args.split()
    if validate(servers, IPREGEX, "Illegal IP Address %s"):
      replace("/etc/resolv.conf",
        collect("/etc/resolv.conf",
                None,
                "nameserver",
                [],
                ["nameserver " + s for s in servers]))

  def do_gateway(self, args):
    """<IP>:Set Network Gateway"""
    if validate([args], IPREGEX, "Illegal IP Address %s"):
      replace("/etc/sysconfig/network", collect("/etc/sysconfig/network", None, "^GATEWAY", ["GATEWAY=" + args]))
      restart_service("network")

  def do_ip(self, args):
    """<IP/CIDR> | dhcp:Set Network IP"""
    if validate([args], IPCIDRREGEX, "Illegal IP Address/CIDR %s"):
      newlines = []
      newlines.append("ONBOOT=yes")

      if args == "dhcp":
        replace("/etc/sysconfig/network", collect("/etc/sysconfig/network", None, "^(GATEWAY)=", [], []))
        newlines.append("BOOTPROTO=dhcp")
      else:
        m = re.match("(.*)/(.*)", args)
        ip = m.group(1)

        ipcalc = subprocess.Popen(["/bin/ipcalc", "-4bmn", args], stdout=subprocess.PIPE)
        for line in ipcalc.stdout:
          newlines.append(line[:-1])

        newlines.append("IPADDR=" + ip)
        newlines.append("BOOTPROTO=static")
      
      replace("/etc/sysconfig/network-scripts/ifcfg-eth0", collect("/etc/sysconfig/network-scripts/ifcfg-eth0", None, "^(IPADDR|NETMASK|BROADCAST|NETWORK|BOOTPROTO|ONBOOT)=", [], newlines))
      restart_service("network")

  def do_hostname(self, args):
    """<hostname>:Set Hostname"""
    if validate([args], HOSTNAMEREGEX, "Illegal hostname Address %s"):
      replace("/etc/sysconfig/network", collect("/etc/sysconfig/network", None, "^HOSTNAME", ["HOSTNAME=" + args]))
      replace("/etc/sysconfig/network-scripts/ifcfg-eth0", collect("/etc/sysconfig/network-scripts/ifcfg-eth0", None, "^DHCP_HOSTNAME", ["DHCP_HOSTNAME=" + args]))
      restart_service("network")
      subprocess.call(["/usr/bin/sudo", "/bin/hostname", args]);
      prompt.prompt = socket.gethostname() + '> '

  def do_ntp(self, args):
    """<IP> [<IP> ...]:Set NTP Server(s)"""
    servers = args.split()
    if validate2(servers, [IPREGEX, NTP_DOMAINREGEX], "Illegal ntp server %s"):
      replace("/etc/ntp.conf", collect("/etc/ntp.conf", None, "^server ", [], ["server " + s for s in servers]))
      replace("/etc/ntp/step-tickers", collect("/etc/ntp/step-tickers", None, ".*", [], servers))
      stop_service("ntpd")
      if len(servers) == 0:
        disable_service("ntpd")
        disable_service("ntpdate")
      else:
        start_service("ntpdate")
        start_service("ntpd")
        enable_service("ntpd")
        enable_service("ntpdate")

class TimesyncPrompt(ExtendedCmd):
  """Timesync Prompt
  
     Enables/Disables VMWare timesync

  """

  def do_enabled(self, args):
    """Enable VMWare timesync"""
    subprocess.call(["/usr/bin/sudo", "vmware-toolbox-cmd", "timesync", "enable"])

  def do_disabled(self, args):
    """Disable VMWare timesync"""
    subprocess.call(["/usr/bin/sudo", "vmware-toolbox-cmd", "timesync", "disable"])

class SetPrompt(ExtendedCmd):
  """set Prompt

    Expected to be used when the user enters

       set ...
  """

  def sub_network(self):
    """Set Network"""
    return SetNetworkPrompt()

  def do_passwd(self, args):
    """Set password"""
    subprocess.call(["/usr/bin/passwd"])

  def do_time(self, args):
    """MMDDmmhhCCCC[.ss]:Set Time"""
    if validate([args], TIMEREGEX, "Illegal date/time %s"):
      subprocess.call(["/usr/bin/sudo", "/bin/date", args])
      sys.stdout.write("Local time: ")
      sys.stdout.flush()
      subprocess.call(["/bin/date"])
      sys.stdout.write("UTC   time: ")
      sys.stdout.flush()
      subprocess.call(["/bin/date", "-u"])

  def sub_timesync(self):
    """Control VMWare timesync"""
    return TimesyncPrompt();
    

  def do_timezone(self, args):
    """Set TimeZone"""
    tzSelect = subprocess.Popen("/usr/bin/tzselect", stdout=subprocess.PIPE)
    tzLine = tzSelect.communicate()[0].strip()
    sys.stdout.write(tzLine + '\n')
    sys.stdout.flush()
    pathToTz = "/usr/share/zoneinfo/" + tzLine
    subprocess.call(["/usr/bin/sudo", "/bin/ln", "-sf", pathToTz, "/etc/localtime"])

class ShowLogPrompt(ExtendedCmd):
  """Show log Prompt

    Expected to be used when the user enters

       show log ...
  """

  def do_follow(self, args):
    """Follow Open Security Controller logs"""
    subprocess.call(["/usr/bin/sudo", "/usr/bin/tail", "-f", VMIDCLOG])

  def do_reverse(self, args):
    """Show Open Security Controller log last record first"""
    tac = subprocess.Popen(["/usr/bin/sudo", "/usr/bin/tac", VMIDCLOG], stdout=subprocess.PIPE)
    less = subprocess.Popen(["/usr/bin/less"], stdin=tac.stdout, env=dict(os.environ, LESSSECURE="1"))
    less.communicate()

  def do_last(self, args):
    """Show last NUM log records"""
    if validate([args], "^[0-9]+$", "Not a number %s"):
      subprocess.call(["/usr/bin/sudo", "/usr/bin/tail", "-n", args, VMIDCLOG])

  def emptyline(self):
    less = subprocess.Popen(["/usr/bin/sudo", "-E", "/usr/bin/less", VMIDCLOG], env=dict(os.environ, LESSSECURE="1"))
    less.communicate()

class ShowNetworkPrompt(ExtendedCmd):
  """Show network Prompt

    Expected to be used when the user enters

       show network ...
  """

  def do_dns(self, args):
    """Show DNS servers"""
    filter("^nameserver (.*)", "/etc/resolv.conf")

  def do_domain(self, args):
    """Show DNS domain name"""
    filter("^domain (.*)", "/etc/resolv.conf")

  def do_route(self, args):
    """Show network routing"""
    subprocess.call(["/sbin/ip", "route"])

  def do_ip(self, args):
    """Show network IP"""
    subprocess.call(["/sbin/ip", "addr"])

  def do_hostname(self, args):
    """Show Hostname"""
    sys.stdout.write(socket.gethostname() + "\n")

  def do_ntp(self, args):
    """Show NTP Server(s)"""
    filter("^\s*server (.*)", "/etc/ntp.conf")

class ShowProcessPrompt(ExtendedCmd):
  """Show process Prompt

    Expected to be used when the user enters

       show process ...
  """

  def do_monitor(self, args):
    """Monitor processes"""
    subprocess.call(["/usr/bin/top", "-s"])

  def emptyline(self):
    subprocess.call(["/bin/ps", "aux"])

class ShowSystemPrompt(ExtendedCmd):
  """Show system Prompt

    Expected to be used when the user enters

       show system ...
  """

  def do_memory(self, args):
    """Show system memory usage"""
    cat("/proc/meminfo")

  def do_uptime(self, args):
    """Show system uptime"""
    subprocess.call(["/usr/bin/uptime"])

class ShowPrompt(ExtendedCmd):
  """Show Prompt

    Expected to be used when the user enters

       show ...
  """

  def do_arp(self, args):
    """Show arp table"""
    subprocess.call(["/sbin/arp", "-n"])

  def do_clock(self, args):
    """Show Clock"""
    sys.stdout.write("Local time: ")
    sys.stdout.flush()
    subprocess.call(["/bin/date"])
    sys.stdout.write("UTC   time: ")
    sys.stdout.flush()
    subprocess.call(["/bin/date", "-u"])

  def do_filesystems(self, args):
    """Show filesystems"""
    subprocess.call(["/bin/df", "-H"])

  def sub_log(self):
    """Show Open Security Controller logs"""
    return ShowLogPrompt()

  def sub_network(self):
    """Show Network"""
    return ShowNetworkPrompt()

  def sub_process(self):
    """Show system processes"""
    return ShowProcessPrompt()

  def sub_system(self):
    """Show system information"""
    return ShowSystemPrompt()

  def do_timesync(self, args):
    """Show VMWare timesync status"""
    subprocess.call(["vmware-toolbox-cmd", "timesync", "status"])

  def do_version(self, args):
    """Show version"""
    subprocess.call(["/usr/bin/sudo", "/bin/bash", "-c", "cd /opt/vmidc/bin/; bash ./vmidc.sh --version"])

  def do_vmware(self, args):
    """Show VMWare status"""
    subprocess.call(["/usr/bin/vmware-checkvm"])
  
class CliPrompt(ExtendedCmd):
  """Base prompt
  """

  def do_help(self, args):
    self.do_list(args)

  def do_debug(self, args):
    """Debug Host Connection"""
    pass

  def do_enable(self,args):
    if os.path.isfile(PBKDF2_KEY_INFO_VMIDC_FILE_PATH):
       password = getpass.getpass("Password:")
       dk = pbkdf.pbkdf2_generate_keyhash(password)
       if pbkdf.compare_key_with_default(dk):
          subprocess.call(["/usr/bin/sudo", "/bin/bash"])
       else:
          sys.stdout.write("Invalid password \n")
    else:
       sys.stdout.write("Login disabled \n")
	

  def do_exit(self, args):
    """Exit CLI"""
    raise SystemExit

  def do_shutdown(self, args):
    """Shutdown the system"""
    subprocess.call(["/usr/bin/sudo", "halt"])

  def do_list(self, args):
    """Print command list"""
    ExtendedCmd.do_list(self, "")

  def do_ping(self, args):
    """Send echo messages"""
    subprocess.call(["/bin/ping", "-c", "5", args])

  def do_ping6(self, args):
    """Send echo messages IPV6"""
    subprocess.call(["/bin/ping6", "-c", "5", args])

  def do_reset(self, args):
    """Reboot the system"""
    subprocess.call(["/usr/bin/sudo", "reboot"])

  def sub_server(self):
    """Open Security Controller Server Control"""
    return ServerPrompt();

  def sub_set(self):
    """Set system information"""
    return SetPrompt();

  def sub_show(self):
    """Show running system information"""
    return ShowPrompt()

  def do_traceroute(self, args):
    """Traceroute to host"""
    subprocess.call(["/bin/traceroute", args])

  def do_traceroute6(self, args):
    """Traceroute to host"""
    subprocess.call(["/bin/traceroute6", args])

  def do_history(self,args):
    """Display history of commands"""
    for idx in range(readline.get_current_history_length()):
      sys.stdout.write(readline.get_history_item(idx + 1) + "\n")

  def do_clear(self,args):
    """Clear the screen"""
    subprocess.call(["/usr/bin/clear"])

  def do_EOF(self,args):
    self.do_exit(args)

  def emptyline(self):
    pass


if __name__ == '__main__':

  # Read history file and write the file on exit
  hfile = os.path.join(os.path.expanduser("~"), ".vmidchist")
  try:
    readline.read_history_file(hfile)
  except IOError:
    pass

  atexit.register(readline.write_history_file, hfile)
 
  signal.signal(signal.SIGINT, handler)
  pbkdf = pbkdf2_operations()
  pbkdf.get_osc_cli_master_key()
  prompt = CliPrompt()
  prompt.prompt = socket.gethostname() + '> '
  prompt.cmdloop()

