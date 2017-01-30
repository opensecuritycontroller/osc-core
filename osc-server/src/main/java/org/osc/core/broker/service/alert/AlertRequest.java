package org.osc.core.broker.service.alert;

import java.util.List;

import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.BaseRequest;

public class AlertRequest extends BaseRequest<BaseDto> {

    private List<AlertDto> dtoList = null;
    private boolean acknowledge;

    public List<AlertDto> getDtoList() {
        return this.dtoList;
    }

    public void setDtoList(List<AlertDto> dtoList) {
        this.dtoList = dtoList;
    }

    public boolean isAcknowledge() {
        return this.acknowledge;
    }

    public void setAcknowledge(boolean acknowledge) {
        this.acknowledge = acknowledge;
    }
}
