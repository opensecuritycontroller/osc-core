package org.osc.core.util;

import java.io.PrintStream;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class LogUtil {

    public static void initLog4j() {
        try {
            PropertyConfigurator.configureAndWatch("./log4j.properties");
            StdOutErrLog.tieSystemOutAndErrToLog();
        } catch (Exception ex) {
            System.out.println("failed to initialize log4j");
            ex.printStackTrace();
        }
    }

    public static class StdOutErrLog {

        private static final Logger logger = Logger.getLogger(StdOutErrLog.class);

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
