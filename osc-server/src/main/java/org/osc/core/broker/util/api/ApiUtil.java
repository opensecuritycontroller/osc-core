package org.osc.core.broker.util.api;

import org.osc.core.broker.rest.server.exception.VmidcRestServerException;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.service.response.SetResponse;

import javax.ws.rs.core.Response;

public interface ApiUtil {

    /**
     * Submits a plain request to the service and return the response or throws a VmidcRestServerException in case of
     * errors.
     *
     * @param service
     *            the service to submit to
     * @param request
     *            the request to submit
     * @return response in case of successful submission
     * @throws VmidcRestServerException
     *             Exception either in case of any exceptions thrown by the service or in case of exceptions submitting
     *             the request
     */
    <R extends Request, O extends org.osc.core.broker.service.response.Response, T extends ServiceDispatcher<R, O>> O submitRequestToService(
            T service, R request) throws VmidcRestServerException;

    /**
     * Submits a BaseRequest to the service and return the response or throws a VmidcRestServerException in case of
     * errors. Base request allows you to tag whether the request is coming in via API route or through the UI and
     * behave
     * appropriately.
     *
     * @param service
     *            the service to submit to
     * @param request
     *            the request to submit
     * @return response in case of successful submission
     * @throws VmidcRestServerException
     *             Exception either in case of any exceptions thrown by the service or in case of exceptions submitting
     *             the request
     */
    <R extends BaseRequest<?>, O extends org.osc.core.broker.service.response.Response, T extends ServiceDispatcher<R, O>> O submitBaseRequestToService(
            T service, R request) throws VmidcRestServerException;

    <R extends BaseRequest<?>, O extends org.osc.core.broker.service.response.Response, T extends ServiceDispatcher<R, O>> Response getResponseForBaseRequest(
            T service, R request) throws VmidcRestServerException;

    <R extends Request, O extends org.osc.core.broker.service.response.Response, T extends ServiceDispatcher<R, O>> Response getResponse(
            T service, R request) throws VmidcRestServerException;

    <R extends Request, O extends ListResponse<?>, T extends ServiceDispatcher<R, O>> ListResponse<?> getListResponse(
            T service, R request) throws VmidcRestServerException;

    <R extends Request, O extends SetResponse<?>, T extends ServiceDispatcher<R, O>> SetResponse<?> getSetResponse(
            T service, R request) throws VmidcRestServerException;

    /**
     * Validates and sets ID on the DTO.
     * Throws an exception if the ID specified does not match the ID specified within the POJO object
     */
    <T extends BaseDto> void setIdOrThrow(T dto, Long urlId, String objName)
            throws VmidcRestServerException;

    <T extends BaseDto> void setParentIdOrThrow(T dto, Long urlParentId, String objName);

    /**
     * Validates and sets ID and Parent ID on the DTO.
     * Throws an exception if the ID and Parent ID specified does not match the ID and Parent ID specified within the
     * POJO object
     */
    <T extends BaseDto> void setIdAndParentIdOrThrow(T dto, Long urlId, Long urlParentId, String objName)
            throws VmidcRestServerException;

    /**
     * Validates Parent ID set on the dto matches the Parent ID passed in
     */
    <T extends BaseDto> void validateParentIdMatches(T dto, Long parentId, String objName)
            throws VmidcRestServerException;

    <T extends BaseDto> VmidcRestServerException createIdMismatchException(Long id, String objName);

    <T extends BaseDto> VmidcRestServerException createParentChildMismatchException(Long parentId, String objName);
}
