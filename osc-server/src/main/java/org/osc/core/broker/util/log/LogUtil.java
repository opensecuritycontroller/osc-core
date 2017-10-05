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
package org.osc.core.broker.util.log;

import java.io.PrintStream;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class LogUtil {

    static ILoggerFactory loggerFactory;

    public static void initLogging(BundleContext context) throws JoranException {
        if (context != null) {
            StdOutErrLog.tieSystemOutAndErrToLog();
            addFactoryToContext(context);
            return;
        }

        throw new IllegalArgumentException("Attempt to init logging with null OSGi context!");
    }

    private static void addFactoryToContext(BundleContext context) throws JoranException {
        LoggerContext provider = new LoggerContext();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(provider);
        provider.reset();
        configurator.doConfigure("./logback.xml");
        context.registerService(ILoggerFactory.class, provider, new Hashtable<>());
        loggerFactory = provider;
    }

    public static class StdOutErrLog {

        private static final Logger logger = LogProvider.getLogger(StdOutErrLog.class);

        public static void tieSystemOutAndErrToLog() {
            System.setOut(createLoggingProxy(System.out, false));
            System.setErr(createLoggingProxy(System.err, true));
        }

        public static PrintStream createLoggingProxy(final PrintStream realPrintStream, final boolean isError) {
            return new PrintStream(realPrintStream) {
                @Override
                public void print(final String string) {
                    realPrintStream.print(string);
                    if (isError) {
                        logger.error(string);
                    } else {
                        logger.info(string);
                    }
                }

                @Override
                public void print(Object obj) {
                    realPrintStream.print(obj);
                    if (isError) {
                        logger.error(String.valueOf(obj));
                    } else {
                        logger.info(String.valueOf(obj));
                    }
                }


            };
        }
    }
}
