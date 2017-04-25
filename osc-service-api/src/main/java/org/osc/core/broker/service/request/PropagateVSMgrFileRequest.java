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
package org.osc.core.broker.service.request;

import java.util.HashSet;
import java.util.Set;

import org.osc.core.broker.service.annotations.VmidcLogHidden;


public class PropagateVSMgrFileRequest implements Request {

    private String vsName;
    private Set<String> daiList = new HashSet<String>();
    @VmidcLogHidden
    private byte[] mgrFile;
    private String mgrFileName;

    public String getVsName() {
        return vsName;
    }

    public void setVsName(String vsName) {
        this.vsName = vsName;
    }

    public Set<String> getDaiList() {
        return daiList;
    }

    public void setDaiList(Set<String> daiList) {
        this.daiList = daiList;
    }

    public String getMgrFileName() {
        return mgrFileName;
    }

    public void setMgrFileName(String mgrFileName) {
        this.mgrFileName = mgrFileName;
    }

    public byte[] getMgrFile() {
        return mgrFile;
    }

    public void setMgrFile(byte[] mgrFile) {
        this.mgrFile = mgrFile;
    }

    @Override
    public String toString() {
        return "PropagateVSMgrFileRequest [vsName=" + vsName + ", daiList=" + daiList + ", mgrFileName=" + mgrFileName
                + "]";
    }

}