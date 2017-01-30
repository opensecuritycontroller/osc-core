import pexpect
import sys
import time

child = pexpect.spawn("/usr/local/bin/idscli\r", timeout=60);
child.timeout = 60
child.logfile = sys.stdout

# Skip first time factory default prompt
index = child.expect( [ ".*>", ".*later:"] )
if index == 1:
    child.sendline("N\r")
    print("Wizard prompt identified")
    child.expect(".*>")

child.sendline("debug\r")
child.expect(".*>")

if sys.argv[4] == '1':
    child.sendline("deinstall\r")
    child.expect(".*confirm:")
    child.sendline("Y\r")
    child.expect(".*>")
    time.sleep(3)

child.sendline("set sensor name " + sys.argv[1] + "\r")

child.expect(".*>")
child.sendline("set manager ip " + sys.argv[2] + "\r")

child.expect(".*>")
child.sendline("set sensor sharedsecretkey\r")

child.expect(".*:")
child.sendline(sys.argv[3] + "\r")

child.expect(".*:")
child.sendline(sys.argv[3] + "\r")

child.expect(".*>")
child.sendline("status\r")

child.expect(".*>")
child.close()
