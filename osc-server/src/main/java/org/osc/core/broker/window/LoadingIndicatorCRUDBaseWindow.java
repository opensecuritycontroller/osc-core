package org.osc.core.broker.window;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.ui.UI;

/**
 * Base Windows which provides default functionality for child class to extend
 * 
 */
@SuppressWarnings("serial")
public abstract class LoadingIndicatorCRUDBaseWindow extends CRUDBaseWindow<OkCancelButtonModel> {

    @Override
    public void createWindow(String caption) throws Exception {
        super.createWindow(caption);

        final ProgressIndicatorWindow progressIndicatorWindow = new ProgressIndicatorWindow();

        UI.getCurrent().addWindow(progressIndicatorWindow);
        progressIndicatorWindow.bringToFront();

        Runnable seviceCall = new Runnable() {
            @Override
            public void run() {
                // Make service calls in the UI thread, since the calls will update the UI components
                UI.getCurrent().access(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            makeServiceCalls(progressIndicatorWindow);
                        } finally {
                            progressIndicatorWindow.close();
                        }
                    }
                });
            }
        };
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        exec.schedule(seviceCall, 1, TimeUnit.MILLISECONDS);
    }

    /**
     * Overridden to populate the form with the text fields and content, but does not make the service calls to fill
     * them
     * with data.
     */
    @Override
    public final void populateForm() throws Exception {
        initForm();
    }

    /**
     * Populates the form with the text fields and content, but does not make the service calls to fill them
     * with data.
     */
    public abstract void initForm();

    /**
     * Makes the required calls to fill the form fields with data. This method will be called immediately after the
     * the loading indicator window is added to the DOM.
     */
    public abstract void makeServiceCalls(ProgressIndicatorWindow progressIndicatorWindow);

}
