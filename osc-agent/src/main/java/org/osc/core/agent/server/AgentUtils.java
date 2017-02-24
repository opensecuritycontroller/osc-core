package org.osc.core.agent.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.osc.core.util.PKIUtil;
import org.osc.core.util.ServerUtil;

public class AgentUtils {

    private static Logger log = Logger.getLogger(AgentUtils.class);

    public static boolean compareAndPersistBytesToFile(String filename, byte[] newBytes, boolean forced) {

        log.info("Checking and persisting content for file " + filename);
        byte[] existingBytes = null;

        if (!forced) {
            log.info("Loading content for file " + filename);
            existingBytes = PKIUtil.readBytesFromFile(new File(filename));
        }

        log.info("Comparing exiting file " + filename + " content with new bytes.");
        if (existingBytes == null || !Arrays.equals(newBytes, existingBytes)) {
            PKIUtil.writeBytesToFile(newBytes, ".", filename);
            Server.applianceUtils.persistFile(filename);
            return true;
        }

        return false;
    }

    public static int executeScript(String script) {
        return executeScript(script, null, null);
    }

    public static int executeScript(String script, String arguments) {
        return executeScript(script, arguments, null);
    }

    public static int executeScript(String script, List<String> lines) {
        return executeScript(script, null, lines);
    }

    public static String executeScriptWithOutput(String script) {
        List<String> lines = new ArrayList<String>();
        if (executeScript(script, lines) == 0 && !lines.isEmpty()) {
            return lines.get(0);
        }
        return null;
    }

    public static int executeScript(String script, String arguments, List<String> lines) {
        log.info("Executing script: scripts/" + script + (arguments == null ? "" : " " + arguments));

        if (lines == null) {
            return ServerUtil.execWithLog("scripts/" + script + (arguments == null ? "" : " " + arguments));
        } else {
            return ServerUtil.execWithLog("scripts/" + script + (arguments == null ? "" : " " + arguments),
                    lines);
        }
    }

    public static int executePythonScript(String script) {
        return executePythonScript(script, null, null);
    }

    public static int executePythonScript(String script, String arguments) {
        return executePythonScript(script, arguments, null);
    }

    public static int executePythonScript(String script, List<String> lines) {
        return executePythonScript(script, null, lines);
    }

    public static String executePythonScriptWithOutput(String script) {
        List<String> lines = new ArrayList<String>();
        if (executePythonScript("python scripts/" + script, lines) == 0 && !lines.isEmpty()) {
            return lines.get(0);
        }
        return null;
    }

    private static int executePythonScript(String script, String arguments, List<String> lines) {
        log.info("Executing python script: scripts/" + script + (arguments == null ? "" : " " + arguments));

        if (lines == null) {
            return ServerUtil.execWithLog("python scripts/" + script + (arguments == null ? "" : " " + arguments));
        } else {
            return ServerUtil.execWithLog("python scripts/" + script + (arguments == null ? "" : " " + arguments),
                    lines);
        }
    }

}
