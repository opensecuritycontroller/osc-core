package org.osc.core.broker.job.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "lockInformation")
@XmlAccessorType(XmlAccessType.FIELD)
public class LockInformationDto {

    private List<ReadWriteLockRecordDto> lockRecordInformation = new ArrayList<>();

    LockInformationDto() {
    }

    public LockInformationDto(Map<LockObjectReference, ReadWriteLockRecord> lockInformation) {
        for (Entry<LockObjectReference, ReadWriteLockRecord> entry : lockInformation.entrySet()) {
            this.lockRecordInformation.add(new ReadWriteLockRecordDto(entry.getValue()));
        }
    }

}
