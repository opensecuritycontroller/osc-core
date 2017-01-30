package org.osc.core.broker.service.request;

import org.osc.core.broker.service.dto.BaseDto;

/**
 * Default Implementation of a request. Any common information for requests can be added
 * in here to avoid code duplication.
 *
 * @param <T>
 *            the type of DTO object
 */
public class BaseRequest<T extends BaseDto> implements Request {

    private T dto;
    private boolean isApi;

    public BaseRequest() {
    }

    public BaseRequest(boolean isApi) {
        this.isApi = isApi;
    }

    public BaseRequest(T dto) {
        this.dto = dto;
    }

    public T getDto() {
        return this.dto;
    }

    public void setDto(T dto) {
        this.dto = dto;
    }

    public boolean isApi() {
        return this.isApi;
    }

    public void setApi(boolean isApi) {
        this.isApi = isApi;
    }

    @Override
    public String toString() {
        return "BaseRequest [dto=" + this.dto + ", isApi=" + this.isApi + "]";
    }
}
