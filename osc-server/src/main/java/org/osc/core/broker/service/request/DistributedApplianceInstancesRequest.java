package org.osc.core.broker.service.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.osc.core.broker.service.dto.DistributedApplianceInstanceDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.util.ValidateUtil;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DistributedApplianceInstancesRequest implements Request {

    public List<Long> dtoIdList = new ArrayList<Long>();

    DistributedApplianceInstancesRequest() {

    }

    public DistributedApplianceInstancesRequest(List<DistributedApplianceInstanceDto> dtoList) {
        if (dtoList != null) {
            for (DistributedApplianceInstanceDto dai : dtoList) {
                this.dtoIdList.add(dai.getId());
            }
        }
    }


    public List<Long> getDtoIdList() {
        return this.dtoIdList;
    }

    public static void checkForNullFields(DistributedApplianceInstancesRequest request) throws Exception {

        // build a map of (field,value) pairs to be checked for null/empty
        // values
        Map<String, Object> notNullFieldsMap = new HashMap<String, Object>();

        notNullFieldsMap.put("dtoIdList", request.getDtoIdList());

        for (Long id : request.getDtoIdList()) {
            if (id == null) {
                notNullFieldsMap.put("dtoId", id);
            }
        }
        ValidateUtil.checkForNullFields(notNullFieldsMap);

        if(request.getDtoIdList().isEmpty()) {
            throw new VmidcBrokerInvalidEntryException("dtoIdList should not be empty.");
        }
    }

    @Override
    public String toString() {
        return "DistributedApplianceInstancesRequest [dtoIdList=" + this.dtoIdList + "]";
    }
}
