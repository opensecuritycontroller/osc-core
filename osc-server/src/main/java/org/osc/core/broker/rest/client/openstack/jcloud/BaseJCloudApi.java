package org.osc.core.broker.rest.client.openstack.jcloud;

import com.google.common.base.Optional;
import com.google.common.io.Closeables;
import org.osc.core.broker.rest.client.openstack.jcloud.exception.ExtensionNotPresentException;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * Desgined to be a base class for all jcloud API wrappers in the code.
 */
public abstract class BaseJCloudApi implements Closeable, AutoCloseable {

    protected Endpoint endPoint;

    public BaseJCloudApi(Endpoint endPoint) {
        this.endPoint = endPoint;
    }

    @Override
    public void close() throws IOException {
        for (Closeable api : getApis()) {
            Closeables.close(api, true);
        }
    }

    /**
     * Returns the API from the optional parameter passed in. Throws {@link ExtensionNotPresentException} in case
     * the api is not present
     */
    public static <T> T getOptionalOrThrow(Optional<? extends T> optional, String extensionName) {
        if (optional.isPresent()) {
            return optional.get();
        }

        throw new ExtensionNotPresentException(extensionName);
    }

    /**
     * List of API's the subclass uses.
     *
     * @return the API's used by the subclass.
     */
    protected abstract List<? extends Closeable> getApis();

}
