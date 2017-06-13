/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
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
package org.osc.core.broker.view.util;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

public class SdkUtil {

    private Logger LOG = Logger.getLogger(SdkUtil.class);

    private static final String SDKPATH = "webapp/SDK/";

    public enum sdkType {MANAGER, SDN_CONTROLLER}

    /**
     * Regex patterns detecting proper file name for SDK - number and last part is more elastic
     */
    private final String sdnControllerPattern = "^sdn-controller-api-(([0-9.]+)[a-zA-Z-]+|([0-9.]+))\\.jar$";
    private final String managerApiPattern = "^security-mgr-api-(([0-9.]+)[a-zA-Z-]+|([0-9.]+))\\.jar$";

    /**
     * Returns newest version of SDK
     *
     * @return sdk bundle name
     */
    public String getSdk(SdkUtil.sdkType sdkType) {
        Path sdkPath = Paths.get(SDKPATH);

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sdkPath)) {
            for (Path path : directoryStream) {

                if(!validateIsJar(path)) {
                    continue;
                }

                String fileName = path.toString().replace("webapp", "").replace("\\","/");
                switch (sdkType) {
                    case MANAGER:
                        if (path.getFileName().toString().matches(this.managerApiPattern)) {
                            return fileName;
                        }
                        break;
                    case SDN_CONTROLLER:
                        if (path.getFileName().toString().matches(this.sdnControllerPattern)) {
                            return fileName;
                        }
                        break;
                    default:
                        return "";
                }
            }
        } catch (IOException ex) {
            this.LOG.error("Cannot find SDK jar in specified folder: " + sdkPath, ex);
        }

        return "";
    }

    private boolean validateIsJar(Path pathToFile){
        try(JarFile jarFile = new JarFile(pathToFile.toFile())) {
            return jarFile.size() > 0;
        } catch (IOException e) {
            this.LOG.warn("Available file: " + pathToFile.toString() + " is not valid jar package", e);
            return false;
        }
    }
}
