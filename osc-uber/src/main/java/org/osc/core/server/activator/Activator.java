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
package org.osc.core.server.activator;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.mcafee.vmidc.server.Server;

public class Activator implements BundleActivator {

    private static final Logger log = Logger.getLogger(Activator.class);
    private Thread thread;

    @Override
    public void start(BundleContext context) throws Exception {
        Runnable server = new Runnable() {
            @Override
            public void run() {
                try {
                    Server.startServer();
                } catch (Exception e) {
                    log.error("startServer failed", e);
                }
            }
        };

        this.thread = new Thread(server, "Uber-Bundle Activator");
        this.thread.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

}
