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
package org.osc.core.rest.client.agent.model.output;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentDpaInfo {

    public static class DpaStaticInfo {
        public String dpaName;
        public String dpaVersion;
        public String ipcVersion;
        public String rawResponse;

        @Override
        public String toString() {
            return "DpaStaticInfo [dpaName=" + dpaName + ", dpaVersion=" + dpaVersion + ", ipcVersion=" + ipcVersion
                    + ", rawResponse=" + rawResponse + "]";
        }
    }

    public static class NetXIpsDpaRuntimeInfo {
        public Long rxInQueue;  // Received processing queue allocated
        public Long rxSize;     // Received queue size
        public Long txInQueue;  // Transmitted processing queue allocated
        public Long txSize;     // Transmitted queue size

        @Override
        public String toString() {
            return "NetXIpsDpaRuntimeInfo [rxInQueue=" + rxInQueue + ", rxSize=" + rxSize + ", txInQueue=" + txInQueue
                    + ", txSize=" + txSize + "]";
        }
    }

    public static class NetXDpaRuntimeInfo {
        // Runtime info applicable for all NetX DPAs
        public String dpaPid;
        public Date statLastUpdate;
        public Date statCurrentTime;

        // Stats applicable for all NetX based DPAs
        public Long workloadInterfaces;
        public Long rx;             // Received packets to security function appliance
        public Long txSva;          // Packet sent out as instructed by security function appliance
        public Long dropSva;        // Dropped as instructed by security function appliance
        public Long txResource;     // Packet sent out due lack of resources and fail-policy is fail-open
        public Long dropResource;   // Dropped due lack of resources and fail-policy is fail-close
        public Long dropError;      // Dropped due some error on packet processing.

        // Additional info/stats applicable only for NetX IPS DPA
        public NetXIpsDpaRuntimeInfo netXIpsDpaRuntimeInfo = new NetXIpsDpaRuntimeInfo();
        public String rawResponse;

        @Override
        public String toString() {
            return "NetXDpaRuntimeInfo [dpaPid=" + dpaPid + ", statLastUpdate=" + statLastUpdate + ", statCurrentTime="
                    + statCurrentTime + ", workloadInterfaces=" + workloadInterfaces + ", rx=" + rx + ", txSva="
                    + txSva + ", dropSva=" + dropSva + ", txResource=" + txResource + ", dropResource=" + dropResource
                    + ", dropError=" + dropError + ", netXIpsDpaRuntimeInfo=" + netXIpsDpaRuntimeInfo
                    + ", rawResponse=" + rawResponse + "]";
        }
    }

    public DpaStaticInfo dpaStaticInfo = new DpaStaticInfo();
    public NetXDpaRuntimeInfo netXDpaRuntimeInfo = new NetXDpaRuntimeInfo();

    @Override
    public String toString() {
        return "AgentDpaInfo [dpaStaticInfo=" + dpaStaticInfo + ", netXDpaRuntimeInfo=" + netXDpaRuntimeInfo + "]";
    }
}
