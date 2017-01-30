package org.osc.core.broker.service.request;

import java.util.HashSet;
import java.util.Set;

import org.osc.core.rest.client.annotations.VmidcLogHidden;


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
