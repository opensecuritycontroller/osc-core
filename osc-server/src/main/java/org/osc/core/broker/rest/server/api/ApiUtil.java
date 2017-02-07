package org.osc.core.broker.rest.server.api;

import java.rmi.RemoteException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.osc.core.broker.rest.server.exception.VmidcRestServerException;
import org.osc.core.broker.service.ServiceDispatcher;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;
import org.osc.core.broker.service.exceptions.VmidcBrokerValidationException;
import org.osc.core.broker.service.exceptions.VmidcException;
import org.osc.core.broker.service.request.BaseRequest;
import org.osc.core.broker.service.request.Request;
import org.osc.core.broker.service.response.ListResponse;
import org.osc.core.broker.service.response.SetResponse;
import org.osc.core.rest.client.exception.RestClientException;

public class ApiUtil {

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
    public static <R extends Request, O extends org.osc.core.broker.service.response.Response, T extends ServiceDispatcher<R, O>> O submitRequestToService(
            T service, R request) throws VmidcRestServerException {
        try {
            return service.dispatch(request);
        } catch (VmidcBrokerInvalidEntryException | VmidcBrokerInvalidRequestException
                | VmidcBrokerValidationException expectedException) {
            throw new VmidcRestServerException(Response.status(Status.BAD_REQUEST), expectedException.getMessage(),
                    VmidcRestServerException.VMIDC_VALIDATION_EXCEPTION_ERROR_CODE);
        } catch (RestClientException | RemoteException remoteException) {
            throw new VmidcRestServerException(Response.status(Status.BAD_REQUEST), remoteException.getMessage(),
                    VmidcRestServerException.REMOTE_EXCEPTION_ERROR_CODE);
        } catch (VmidcException generalVmidcException) {
            throw new VmidcRestServerException(Response.status(Status.INTERNAL_SERVER_ERROR),
                    generalVmidcException.getMessage(), VmidcRestServerException.VMIDC_EXCEPTION_ERROR_CODE);
        } catch (Exception e) {
            throw new VmidcRestServerException(Response.status(Status.INTERNAL_SERVER_ERROR), e.getMessage());
        }
    }

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
    public static <R extends BaseRequest<?>, O extends org.osc.core.broker.service.response.Response, T extends ServiceDispatcher<R, O>> O submitBaseRequestToService(
            T service, R request) throws VmidcRestServerException {
        request.setApi(true);
        return submitRequestToService(service, request);
    }

    public static <R extends BaseRequest<?>, O extends org.osc.core.broker.service.response.Response, T extends ServiceDispatcher<R, O>> Response getResponseForBaseRequest(
            T service, R request) throws VmidcRestServerException {
        return Response.status(Status.OK).entity(submitBaseRequestToService(service, request)).build();
    }

    public static <R extends Request, O extends org.osc.core.broker.service.response.Response, T extends ServiceDispatcher<R, O>> Response getResponse(
            T service, R request) throws VmidcRestServerException {
        return Response.status(Status.OK).entity(submitRequestToService(service, request)).build();
    }

    public static <R extends Request, O extends ListResponse<?>, T extends ServiceDispatcher<R, O>> ListResponse<?> getListResponse(
            T service, R request) throws VmidcRestServerException {
        return submitRequestToService(service, request);
    }

    public static <R extends Request, O extends SetResponse<?>, T extends ServiceDispatcher<R, O>> SetResponse<?> getSetResponse(
            T service, R request) throws VmidcRestServerException {
        return submitRequestToService(service, request);
    }

    /**
     * Validates and sets ID on the DTO.
     * Throws an exception if the ID specified does not match the ID specified within the POJO object
     */
    public static <T extends BaseDto> void setIdOrThrow(T dto, Long urlId, String objName)
            throws VmidcRestServerException {
        if (dto.getId() != null && !dto.getId().equals(urlId)) {
            throw createIdMismatchException(dto.getId(), objName);
        }

        dto.setId(urlId);
    }

    public static <T extends BaseDto> void setParentIdOrThrow(T dto, Long urlParentId, String objName) {
        if (dto.getParentId() != null && !dto.getParentId().equals(urlParentId)) {
            throw createParentChildMismatchException(dto.getParentId(), objName);
        }

        dto.setParentId(urlParentId);
    }

    /**
     * Validates and sets ID and Parent ID on the DTO.
     * Throws an exception if the ID and Parent ID specified does not match the ID and Parent ID specified within the
     * POJO object
     */
    public static <T extends BaseDto> void setIdAndParentIdOrThrow(T dto, Long urlId, Long urlParentId, String objName)
            throws VmidcRestServerException {
        setIdOrThrow(dto, urlId, objName);
        setParentIdOrThrow(dto, urlParentId, objName);
    }

    /**
     * Validates Parent ID set on the dto matches the Parent ID passed in
     */
    public static <T extends BaseDto> void validateParentIdMatches(T dto, Long parentId, String objName)
            throws VmidcRestServerException {
        if (!parentId.equals(dto.getParentId())) {
            throw createParentChildMismatchException(dto.getParentId(), objName);
        }

    }

    public static <T extends BaseDto> VmidcRestServerException createIdMismatchException(Long id, String objName) {
        return new VmidcRestServerException(Response.status(Status.BAD_REQUEST), String.format(
                "The ID %d specified in the '%s' data does not match the id specified in the URL", id, objName));
    }

    public static <T extends BaseDto> VmidcRestServerException createParentChildMismatchException(Long parentId,
            String objName) {
        return new VmidcRestServerException(Response.status(Status.BAD_REQUEST),
                String.format("The Parent ID %d specified in the '%s' data does not match the id specified in the URL",
                        parentId, objName));
    }
}
