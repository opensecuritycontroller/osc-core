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
import java.util.ArrayList;
import java.util.Collections;

public class SdkUtil {

    private Logger LOG = Logger.getLogger(SdkUtil.class);

    private static final String SDKPATH = "webapp/SDK/";

    public enum sdkType {MANAGER, SDN_CONTROLLER}

    private final String managerPrefix = "-mgr-";

    /**
     * Returns newest version of SDK
     *
     * @return sdk bundle name
     */
    public String getSdk(SdkUtil.sdkType sdkType) {
        Path sdkPath = Paths.get(SDKPATH);

        ArrayList<String> sdkList = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sdkPath)) {
            for (Path path : directoryStream) {
                String fileName = path.toString().replace("webapp", "");
                switch (sdkType) {
                    case MANAGER:
                        if (fileName.contains(this.managerPrefix)) {
                            sdkList.add(fileName);
                        }
                        break;
                    case SDN_CONTROLLER:
                        if (!fileName.contains(this.managerPrefix)) {
                            sdkList.add(fileName);
                        }
                        break;
                }
            }
        } catch (IOException ex) {
            this.LOG.error("Cannot find SDK jar in specified folder: " + sdkPath, ex);
        }

        if (sdkList.size() > 0) {
            Collections.sort(sdkList);
            return sdkList.get(sdkList.size() - 1);
        } else {
            return "";
        }
    }
}
