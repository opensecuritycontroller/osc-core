package org.osc.core.broker.service.response;

import org.osc.core.broker.service.dto.BaseDto;

public class BaseDtoResponse<T extends BaseDto> implements Response {

    private T dto;

    public BaseDtoResponse() {
    }

    public BaseDtoResponse(T dto) {
        this.dto = dto;
    }

    public T getDto() {
        return this.dto;
    }

    public void setDto(T dto) {
        this.dto = dto;
    }

    @Override
    public String toString() {
        return "BaseDtoResponse [dto=" + this.dto + "]";
    }

}
