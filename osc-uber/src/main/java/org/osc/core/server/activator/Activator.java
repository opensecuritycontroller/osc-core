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
