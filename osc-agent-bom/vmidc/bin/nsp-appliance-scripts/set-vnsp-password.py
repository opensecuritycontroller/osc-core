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

child.sendline("debug\r")

child.expect(".*>")
child.sendline("passwd\r")

child.expect(".*:")
child.sendline(sys.argv[1] + "\r")

#If we've encountered a password error
index = child.expect( [ ".*:", ".*>"] )
if index == 1:
    sys.exit(-2)

child.sendline(sys.argv[2] + "\r")

child.expect(".*:")
child.sendline(sys.argv[2] + "\r")

child.close()
