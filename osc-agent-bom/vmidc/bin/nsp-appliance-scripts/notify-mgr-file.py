import pexpect
import sys

child = pexpect.spawn("/usr/local/bin/idscli\r", timeout=60);
child.timeout = 60
child.logfile = sys.stdout

# Skip first time factory default prompt
index = child.expect( [ ".*>", ".*later:"] )
if index == 1:
    child.sendline("N\r")
    print("Wizard prompt identified")
    child.expect(".*>")

child.sendline("loadconfiguration filesystem " + sys.argv[1] + "\r")

child.expect(".*>")
child.sendline("downloadstatus\r")

child.expect(".*>")
child.close()
