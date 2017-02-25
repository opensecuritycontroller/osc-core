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
package org.osc.core.broker.model.entities.appliance;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.ForeignKey;
import org.osc.core.broker.model.entities.BaseEntity;

@Entity
@Table(name = "VIRTUAL_SYSTEM_MGR_FILE", uniqueConstraints = @UniqueConstraint(columnNames = { "file_type", "vs_fk" }))
public class VirtualSystemMgrFile extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public static final String FILE_TYPE_SIGFILE = "sigfile";
    public static final String FILE_TYPE_COMBO_SIGFILE = "combo-sigfile";
    public static final String FILE_TYPE_DATFILE = "dat-sigfile";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vs_fk", nullable = false)
    @ForeignKey(name = "FK_VSMF_VS")
    private VirtualSystem virtualSystem;

    @Column(name = "mgr_file")
    @Basic(fetch = FetchType.LAZY)
    @Lob
    private byte[] mgrFile = null;

    @Column(name = "file_type")
    private String fileType = FILE_TYPE_SIGFILE; // file type. Must be unique string within VSS. Example: ‘sigfile’, ‘combo-sigfile’, ‘dat-file’, etc.

    @Column(name = "location")
    private String location = null; // final location to persist the file after upload. If null, will be persisted in vmiDC agent directory.

    @Column(name = "process_file_cmd")
    private String processFileCmd = null; // command to process the file. If not, no action taken after persistence.

    public VirtualSystemMgrFile() {
        super();
    }

    public VirtualSystemMgrFile(VirtualSystem vs) {
        super();

        this.virtualSystem = vs;
    }

    public VirtualSystem getVirtualSystem() {
        return virtualSystem;
    }

    public void setVirtualSystem(VirtualSystem virtualSystem) {
        this.virtualSystem = virtualSystem;
    }

    public byte[] getMgrFile() {
        return mgrFile;
    }

    public void setMgrFile(byte[] mgrFile) {
        this.mgrFile = mgrFile;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getProcessFileCmd() {
        return processFileCmd;
    }

    public void setProcessFileCmd(String processFileCmd) {
        this.processFileCmd = processFileCmd;
    }

    @Override
    public String toString() {
        return "VirtualSystemMgrFile [virtualSystem=" + virtualSystem + ", mgrType=" + fileType + ", location="
                + location + ", processFileCmd=" + processFileCmd + ", getId()=" + getId() + "]";
    }

}
