/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.server.control;

/*
 * support old server upgrade, which runs "java -jar vmiDCServer.jar" instead of "vmidc.sh --console".
 */
public class OldUpgrade {
    public static void main(final String[] args) throws Exception {
        if (args.length == 0) {
            Process process = new ProcessBuilder("/bin/sh", "vmidc.sh", "--console").start();
            try {
                Thread.sleep(2000);
                process.exitValue();
                System.err.println("OldUpgrade: ERROR: unexpected 'vmidc.sh --console' exit.");
                System.exit(2);
            } catch (IllegalThreadStateException e) {
                // still running
                System.exit(0);
            }
        }

        if (args[0].equals("-v")) {
            System.out.println("OldUpgrade: dummy version");
            System.exit(0);
        }

        System.err.println("OldUpgrade: Usage: vmiDCServer.jar [-v]");
        System.exit(1);
    }

}
