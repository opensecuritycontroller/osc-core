package org.osc.core.rest.client.agent.model.input;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.rest.client.annotations.VmidcLogHidden;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentUpdateMgrFileRequest {

    @VmidcLogHidden
    private byte[] mgrFile = null;
    private String mgrFileName = null;

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

}
