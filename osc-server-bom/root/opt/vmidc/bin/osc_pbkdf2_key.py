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
  osc_pbkdf2_key.py 
  
  This file have pbkdf2 operation supported by OSC to generate a pbkdf2/hmac based hashing key
  for osc cli password. If key generating parameters are not tested or if they are not valid ,this 
  file operation class will defaults it to OSC specific defaults. 
"""

import re
import os
import sys
from pbkdf2 import PBKDF2
try:
    from configparser import ConfigParser
except ImportError:
    from ConfigParser import ConfigParser  # ver. < 3.0

PBKDF2_CONFIG_FILE="pbkdf2_key_config.ini"
PBKDF2_KEY_INFO_FILE="pbkdf2_keyinfo.ini"
PBKDF2_KEY_INFO_VMIDC_DIR = "/opt/vmidc/bin"
PBKDF2_KEY_INFO_VMIDC_FILE_PATH= os.path.join(PBKDF2_KEY_INFO_VMIDC_DIR,PBKDF2_KEY_INFO_FILE)

DEFAULT_SALT_LEN=24
DEFAULT_ROUNDS=4000
DEFAULT_KEYLEN=24



class pbkdf2_operations():
  """vmidcshell password key generation operations
     This class has operation to generate a pbkdf2 specific key generation,
     contanst-time algorithm to key comparisions, config read of kye config file  and other debug info
  """
  digest_key = None
  salt_len = DEFAULT_SALT_LEN 
  salt = 0
  rounds = DEFAULT_ROUNDS
  digest_keylen = DEFAULT_KEYLEN
  
  def osc_cli_generate_master_key(self,key_config_fp):
    """pbkdf2 operation class osc_cli_generate_master_key, reads the config param file 
      and generates a master key, and also create a new file with all the data like, master_key,salt,key_len and rounds
      this pbkdf2_keyinfo file will be used by vmidcShell to generate a key with user entered password and will compare with master key
      if the config file is not present, does not generate the key info file hence disabling shell access 
    """
#   At build time look for the info file at current directory
    key_info_fp=os.path.join("./",PBKDF2_KEY_INFO_FILE)
    if os.path.isfile(key_info_fp):
        os.remove(key_info_fp) 
    if key_config_fp and os.path.isfile(key_config_fp):

       config = ConfigParser()
       config_write = ConfigParser()
       config_write.add_section('pbkdf2_key_decode_info')
       # parse pbkdf2 param file
       config.read(key_config_fp)

       salt_temp = config.getint('pbkdf2_key_params', 'salt')
       min_val = config.getint('pbkdf2_key_params', 'MIN_SALT_VAL')
       max_val = config.getint('pbkdf2_key_params', 'MAX_SALT_VAL')
       if min_val <= salt_temp <= max_val:
         self.salt_len = salt_temp
       self.salt = os.urandom(self.salt_len)
       config_write.set('pbkdf2_key_decode_info','salt',self.salt)

	

       keylen_temp = config.getint('pbkdf2_key_params', 'key_len')
       min_val = config.getint('pbkdf2_key_params', 'MIN_KEY_LEN_VAL')
       max_val = config.getint('pbkdf2_key_params', 'MAX_KEY_LEN_VAL')
       if min_val <= keylen_temp  <= max_val:
         self.digest_keylen = keylen_temp
       config_write.set('pbkdf2_key_decode_info','key_len',self.digest_keylen)

       min_val = config.getint('pbkdf2_key_params', 'MIN_ROUNDS_VAL')
       max_val = config.getint('pbkdf2_key_params', 'MAX_ROUNDS_VAL')
       rounds_temp = config.getint('pbkdf2_key_params', 'rounds')
       if min_val <= rounds_temp  <= max_val:
         self.rounds = rounds_temp
       config_write.set('pbkdf2_key_decode_info','rounds',self.rounds)
       
       password_temp = config.get('pbkdf2_key_params', 'password')
       if password_temp and password_temp.strip():
         self.digest_key=self.pbkdf2_generate_keyhash(password_temp)
         config_write.set('pbkdf2_key_decode_info','master_key',self.digest_key)

       with open(key_info_fp, 'w') as configfile:
          config_write.write(configfile)


  def pbkdf2_generate_keyhash(self,password):
    """
      generated a pbkdf2 key has on a given password-string
      The PBKDF2 class is derived from git hub pbkdf2 module: https://github.com/dlitz/python-pbkdf2
      This class take argumners like password phrase,salt ,rounds/iteration and desired key len and generates
       HMAC bases digest key with SHA1
    """
    digest_key_tmp = PBKDF2(password,self.salt,self.rounds).hexread(self.digest_keylen)
    return digest_key_tmp 

  def get_osc_cli_master_key(self):
    """
      reads master_key and other required params from the pbkdf2_keyinfo file and setups the global values
      needed to generate key for user entered password at vmidcShell
    """
# at OSC target the file path for key_info file would be different hence different path
    if os.path.isfile(PBKDF2_KEY_INFO_VMIDC_FILE_PATH):
       config = ConfigParser()
       config.read(PBKDF2_KEY_INFO_VMIDC_FILE_PATH)
       self.salt = config.get('pbkdf2_key_decode_info','salt')
       self.digest_keylen = config.getint('pbkdf2_key_decode_info','key_len')
       self.rounds = config.getint('pbkdf2_key_decode_info','rounds')
       if config.has_option('pbkdf2_key_decode_info','master_key'):
          self.digest_key = config.get('pbkdf2_key_decode_info','master_key')

  def compare_key_with_default(self,pass_key):
    """This a constant-time alogorithm derived from phython 3. Since python release < 3 does not support
     compare-digest, have to adopt to it with an duplicate API. starting python 3 cut short a API in hmac.
    """
    if not self.digest_key:
       return False
    if not (isinstance(self.digest_key, bytes) and isinstance(pass_key, bytes)):
        sys.stdout.writ("both inputs should be instances of bytes")
    if len(self.digest_key) != len(pass_key):
        return False
    result = 0
    for a, b in zip(self.digest_key, pass_key):
        result |= int(a,16) ^ int(b,16)
    return result == 0


if __name__ == '__main__':

  total_arg = len(sys.argv)
  key_config_dir = None
  key_config_fp = None
  if total_arg == 2:
     key_config_dir = sys.argv[1]
     key_config_fp = os.path.join(key_config_dir,PBKDF2_CONFIG_FILE)
  sys.stdout.write("Generating osc cli key with file %s \n" % key_config_fp)
  pbkdf = pbkdf2_operations()
  pbkdf.osc_cli_generate_master_key(key_config_fp)
