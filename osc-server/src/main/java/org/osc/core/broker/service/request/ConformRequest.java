package org.osc.core.broker.service.request;

import org.osc.core.broker.service.dto.BaseDto;

public class ConformRequest extends BaseRequest<BaseDto> {
    private Long daId;

    public ConformRequest() {
    }

    public ConformRequest(Long daId) {
        setDaId(daId);
    }

    public Long getDaId() {
        return this.daId;
    }

    public void setDaId(Long daId) {
        this.daId = daId;
    }

}
