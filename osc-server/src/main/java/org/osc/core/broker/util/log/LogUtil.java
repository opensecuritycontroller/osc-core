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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LogUtil {

    public static void redirectConsoleMessagesToLog() {
        try {
            StdOutErrLog.tieSystemOutAndErrToLog();
        } catch (Exception ex) {
            System.out.println("failed to initialize logging");
            ex.printStackTrace();
        }
    }

    public static class StdOutErrLog {

        private static final Logger logger = LoggerFactory.getLogger(StdOutErrLog.class);

        public static void tieSystemOutAndErrToLog() {
            synchronized (System.class) {
                System.setOut(createLoggingProxy(System.out, false));
                System.setErr(createLoggingProxy(System.err, true));
            }
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
