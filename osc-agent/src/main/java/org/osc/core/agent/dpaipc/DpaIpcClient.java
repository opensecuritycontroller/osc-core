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
package org.osc.core.agent.dpaipc;

import com.google.common.math.IntMath;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.Primitives;
import com.google.gson.stream.JsonReader;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.osc.core.agent.server.Server;
import org.osc.core.rest.client.agent.model.input.EndpointGroupList;
import org.osc.core.rest.client.agent.model.output.AgentDpaInfo;
import org.osc.core.util.ServerUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.ParseException;

public class DpaIpcClient {

    private static final int DPA_RECONNECT_RETRY_SLEEP = 5 * 1000;  // 5 seconds

	private static final String INTERFACE_ENDPOINT_MAP = "interfaceEndpointsMap.json";

    private static Logger log = Logger.getLogger(DpaIpcClient.class);

    private int ipcVersion;
    private Socket socket;
    private Writer writer;
    private Reader reader;

    private InterfaceEndpointMap interfaceEndpointMaps = new InterfaceEndpointMap();

    private boolean connected = false;
    private Thread retryThread = null;

    @SuppressWarnings("serial")
    private class DpaIpcProtocolException extends Exception {
        public DpaIpcProtocolException(int errorCode, String string) {
            super(string + " (" + errorCode + ")");
        }
    }

    public DpaIpcClient() {
        loadInterfaceEndpointMaps();
    }

    private void loadInterfaceEndpointMaps() {

        log.info("Load service profile containers cache.");

        FileInputStream inputStream = null;
        try {
            File file = new File(INTERFACE_ENDPOINT_MAP);
            inputStream = new FileInputStream(file);

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            try {
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
            } finally {
                IOUtils.closeQuietly(bufferedReader);
            }

            String json = sb.toString();
            Gson gson = new Gson();
            this.interfaceEndpointMaps = gson.fromJson(json, InterfaceEndpointMap.class);

        } catch (Exception e) {
            log.error("Fail to load service profile containers mapping.", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void saveInterfaceEndpointMaps() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        String json = gson.toJson(this.interfaceEndpointMaps);
        FileOutputStream outputStream = null;
        try {
            File file = new File(INTERFACE_ENDPOINT_MAP);
            file.createNewFile();

            outputStream = new FileOutputStream(file);
            outputStream.write(json.getBytes());

            Server.applianceUtils.persistFile(INTERFACE_ENDPOINT_MAP);

        } catch (Exception e) {
            log.error("Fail to save interface endpoint mapping.", e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    public void updateProfileServiceContainer(String interfaceTag, EndpointGroupList endpointGroupList) {
        // Update cache
        this.interfaceEndpointMaps.updateProfileServiceContainer(interfaceTag, endpointGroupList);
        saveInterfaceEndpointMaps();

        // Notify DPA
        try {
            cmd(new UpdateInterfaceEndpointMap(interfaceTag, endpointGroupList));

        } catch (Exception e) {
            log.error("Fail to communicate with DPA IPC.", e);
        }
    }

    public void setProfileServiceContainers(InterfaceEndpointMap interfaceEndpointMap) throws IOException {
        // Update cache
        this.interfaceEndpointMaps = interfaceEndpointMap;
        saveInterfaceEndpointMaps();

        // Notify DPA
        try {
            cmd(new SetInterfaceEndpointMap(this.interfaceEndpointMaps.interfaceEndpointMap.values()));

        } catch (Exception e) {
            log.error("Fail to communicate with DPA IPC.", e);
        }
    }

    private void setInterfaceEndpointMap() throws IOException, DpaIpcProtocolException {
        cmd(new SetInterfaceEndpointMap(this.interfaceEndpointMaps.interfaceEndpointMap.values()));
    }

    private void connect() throws IOException, DpaIpcProtocolException {
        connect(false);
    }

    private synchronized void connect(boolean fromReconnectThread) throws IOException, DpaIpcProtocolException {
    	if (isConnected()) {
    		return;
    	}

        if (this.socket == null) {
            this.socket = new Socket();
        }

        try {
            log.debug("Connecting to DPA IPC...");
            this.socket.connect(new InetSocketAddress("127.0.0.1", 10613));
            log.info("Successfully connected to DPA IPC.");

            // Get transport version
            byte[] ver = new byte[2];
            this.socket.getInputStream().read(ver);
            this.ipcVersion = IntMath.checkedAdd(IntMath.checkedMultiply(ver[0], 256), ver[1]);
            log.info("DPA IPC version: " + this.ipcVersion);

            this.writer = new OutputStreamWriter(this.socket.getOutputStream(), "ASCII");
            this.reader = new InputStreamReader(this.socket.getInputStream(), "ASCII");

            // if we're not called from the background reconnect thread is active, and we've reached 
            // this point successfully, we can safely terminate it since we connected successfully.
            if (!fromReconnectThread) {
                stopReconnectThread();
            } else {
                retryThread = null;
            }
            connected = true;

            // After successful connection, propagate the whole list as we cannot 
            // assume what's there and what was missed since last connection.
            try {
            	setInterfaceEndpointMap();
            } catch (Exception ex) {
                log.error("Sync all security group mapping after successful connection failed. " + ex.getMessage());
            }
            
        } catch (Exception ex) {
            disconnect();
            startReconnectThread();
            throw ex;
        }
    }

    private synchronized void stopReconnectThread() {
    	if (this.retryThread == null) {
    		return;
    	}
        log.info("Stops DPA reconnect retry thread");
        this.retryThread.interrupt();
        try {
            this.retryThread.join();
        } catch (InterruptedException ex) {
            log.warn("Waiting for Reconnect thread interupted.");
        }
        this.retryThread = null;
    }

    private synchronized void startReconnectThread() {
        if (retryThread != null) {
        	return;
        }
        log.info("Starts DPA reconnect retry thread");
        this.retryThread = new Thread("DPA-Reconnect-Thread"){
            @Override
            public void run() {
                while (true) {
                    try {
                        connect(true);
                        log.info("Reconnect thread connected successfully.");
                        break;
                    } catch (Exception ex) {
                        log.debug("Fail connecting to DPA IPC. Error: " + ex.getMessage());
                        try {
                            Thread.sleep(DPA_RECONNECT_RETRY_SLEEP);
                        } catch (InterruptedException e) {
                            log.info("Reconnect thread completion interupted.");
                            break;
                        }
                    }
                }
            }
        };
        retryThread.start();
    }

    private synchronized void disconnect() {
    	connected = false;
        if (this.socket == null) {
            return;
        }

        try {
            this.socket.close();
        } catch (Exception ex) {
            log.error("Fail to close DPA IPC socket.", ex);
        }
        this.socket = null;
    }

    private Object cmd(Object cmd) throws DpaIpcProtocolException, IOException {
        Gson gson = new Gson();
        return execCmd(gson.toJson(cmd));
    }

    private synchronized boolean isConnected() {
		return connected;
    }

    private synchronized JsonElement execCmd(String cmd) throws DpaIpcProtocolException, IOException {
        log.info("DpaIpc request: '" + cmd + "'");
        if (!isConnected()) {
            connect();
        }

        /*
         * Will try to resend the command if dpa is not connected. To check if we are actively connected,
         * we need to write something to the socket.
         * {@link Socket#isConnected()} returns true if the socket was connected at some point in the past and not if it
         * is actively connected, so cant rely on that.
         */
        try {
            this.writer.write(cmd);
            this.writer.flush();
        } catch (IOException e) {
            log.error("Exception while trying to execute command. Will re-attempt.", e);
            // Could be the socket connection is already closed. re-open and try again
            log.info("DPA IPC resync.");
            disconnect();
            connect();

            // If this does not work, bail out.
            this.writer.write(cmd);
            this.writer.flush();
        }

        // Read response
        JsonReader jsonReader = new JsonReader(this.reader);
        JsonParser parser = new JsonParser();
        jsonReader.setLenient(true);

        JsonElement je = parser.parse(jsonReader);

        JsonObject obj = je.getAsJsonObject();
        int status = obj.get("status").getAsInt();
        log.info("DpaIpc Status: " + status);
        JsonElement response = obj.get("response");
        log.info("DpaIpc Response: " + response);

        if (status != 200) {
            String error = obj.get("error").getAsString();
            throw new DpaIpcProtocolException(status, "DpaIpc request error - " + error);
        }

        return response;
    }

    public static class DpaInfo {
        @SerializedName("DPA-name")
        String dpaName;

        @SerializedName("DPA-version")
        String dpaVersion;

        @SerializedName("IPC-version")
        String ipcVersion;

        @SerializedName("dpa-pid")
        Long dpaPid;

        @Override
        public String toString() {
            return "DpaInfo [dpaName=" + this.dpaName + ", dpaVersion=" + this.dpaVersion + ", ipcVersion="
                    + this.ipcVersion + ", dpaPid=" + this.dpaPid + "]";
        }
    }

    public static class DpaStats {

        public Long currentTicks;

        public Long workloadInterfaces;

        public Long rx;
        public Long txSva;
        public Long txResource;
        public Long dropSva;
        public Long dropError;
        public Long dropResource;

        public Long rxInQueue;
        public Long rxSize;
        public Long txInQueue;
        public Long txSize;

        @Override
        public String toString() {
            return "DpaStats [currentTicks=" + this.currentTicks + ", workloadInterfaces=" + this.workloadInterfaces
                    + ", rx=" + this.rx + ", txSva=" + this.txSva + ", txResource=" + this.txResource + ", dropSva="
                    + this.dropSva + ", dropError=" + this.dropError + ", dropResource=" + this.dropResource
                    + ", rxInQueue=" + this.rxInQueue + ", rxSize=" + this.rxSize + ", txInQueue=" + this.txInQueue
                    + ", txSize=" + this.txSize + "]";
        }

    }

    private DpaInfo getDpaStaticInfo() throws IOException, DpaIpcProtocolException {
        return cmd1("{\"cmd\":\"dpa-info\"}", DpaInfo.class);
    }

    private DpaStats getDpaRuntimeInfo() throws IOException, DpaIpcProtocolException {
        return cmd1("{\"cmd\":\"get-statistics\"}", DpaStats.class);
    }

    private <T> T cmd1(final String cmd, final Class<T> classOfT) throws IOException, DpaIpcProtocolException {
        JsonElement je = execCmd(cmd);
        Gson gson = new Gson();
        Object object = gson.fromJson(je, classOfT);
        return Primitives.wrap(classOfT).cast(object);
    }

    private void parseNetXDpaRuntimeInfo(AgentDpaInfo agentDpaInfo) throws IOException, DpaIpcProtocolException {

        DpaStats dpaStats = getDpaRuntimeInfo();
        agentDpaInfo.netXDpaRuntimeInfo.rawResponse = dpaStats.toString();

        agentDpaInfo.netXDpaRuntimeInfo.rx = dpaStats.rx;
        agentDpaInfo.netXDpaRuntimeInfo.txSva = dpaStats.txSva;
        agentDpaInfo.netXDpaRuntimeInfo.txResource = dpaStats.txResource;
        agentDpaInfo.netXDpaRuntimeInfo.dropSva = dpaStats.dropSva;
        agentDpaInfo.netXDpaRuntimeInfo.dropResource = dpaStats.dropResource;
        agentDpaInfo.netXDpaRuntimeInfo.dropError = dpaStats.dropError;
        agentDpaInfo.netXDpaRuntimeInfo.workloadInterfaces = dpaStats.workloadInterfaces;
        agentDpaInfo.netXDpaRuntimeInfo.netXIpsDpaRuntimeInfo.rxInQueue = dpaStats.rxInQueue;
        agentDpaInfo.netXDpaRuntimeInfo.netXIpsDpaRuntimeInfo.rxSize = dpaStats.rxSize;
        agentDpaInfo.netXDpaRuntimeInfo.netXIpsDpaRuntimeInfo.txInQueue = dpaStats.txInQueue;
        agentDpaInfo.netXDpaRuntimeInfo.netXIpsDpaRuntimeInfo.txSize = dpaStats.txSize;
    }

    private void parseDpaStaticInfo(AgentDpaInfo agentDpaInfo) throws IOException, DpaIpcProtocolException,
            ParseException {

        DpaInfo dpaInfo = getDpaStaticInfo();
        agentDpaInfo.dpaStaticInfo.rawResponse = dpaInfo.toString();
        agentDpaInfo.dpaStaticInfo.dpaName = dpaInfo.dpaName;
        agentDpaInfo.dpaStaticInfo.dpaVersion = dpaInfo.dpaVersion;
        agentDpaInfo.dpaStaticInfo.ipcVersion = dpaInfo.ipcVersion;

        // TODO: Future. NSP. Once NSP incorporate new CPA, we can get PID through DPAIPC.
        // For now, we hack around it to by trying to get PID for know dpa process on nsp.
        // If not found, such in the case of NGFW, we'll fall back to get it from DPAIPC.
        agentDpaInfo.netXDpaRuntimeInfo.dpaPid = ServerUtil.getPidByProcessName("vmidc_dpa");
        if (agentDpaInfo.netXDpaRuntimeInfo.dpaPid == null && dpaInfo.dpaPid != null) {
            agentDpaInfo.netXDpaRuntimeInfo.dpaPid = dpaInfo.dpaPid.toString();
        }
    }

    public AgentDpaInfo getAgentDpaInfo() {

        AgentDpaInfo agentDpaInfo = new AgentDpaInfo();

        try {
            parseNetXDpaRuntimeInfo(agentDpaInfo);
        } catch (DpaIpcProtocolException e) {
            String msg = "DPA runtime info. message error (" + e.getMessage() + "). "
                    + agentDpaInfo.netXDpaRuntimeInfo.rawResponse;
            log.error(msg);
            agentDpaInfo.netXDpaRuntimeInfo.rawResponse = msg;
        } catch (IOException e) {
            String msg = "DPA runtime info. communication error (" + e.getMessage() + "). "
                    + agentDpaInfo.netXDpaRuntimeInfo.rawResponse;
            log.error(msg);
            agentDpaInfo.netXDpaRuntimeInfo.rawResponse = msg;
        } catch (Exception e) {
            String msg = "DPA runtime info. unknown error (" + e.getMessage() + "). "
                    + agentDpaInfo.netXDpaRuntimeInfo.rawResponse;
            log.error(msg, e);
            agentDpaInfo.netXDpaRuntimeInfo.rawResponse = msg;
        }

        try {
            parseDpaStaticInfo(agentDpaInfo);
        } catch (DpaIpcProtocolException e) {
            String msg = "DPA static message error (" + e.getMessage() + "). " + agentDpaInfo.dpaStaticInfo.rawResponse;
            log.error(msg);
            agentDpaInfo.dpaStaticInfo.rawResponse = msg;
        } catch (IOException e) {
            String msg = "DPA static communication error (" + e.getMessage() + "). "
                    + agentDpaInfo.dpaStaticInfo.rawResponse;
            log.error(msg);
            agentDpaInfo.dpaStaticInfo.rawResponse = msg;
        } catch (Exception e) {
            String msg = "DPA static unknown error (" + e.getMessage() + "). " + agentDpaInfo.dpaStaticInfo.rawResponse;
            log.error(msg, e);
            agentDpaInfo.dpaStaticInfo.rawResponse = msg;
        }

        return agentDpaInfo;
    }

    @Override
    public String toString() {
        return "DpaIpcClient [ipcVersion=" + this.ipcVersion + ", socket=" + this.socket + ", writer=" + this.writer
                + ", reader=" + this.reader + ", interfaceEndpointMaps=" + this.interfaceEndpointMaps + "]";
    }
}
