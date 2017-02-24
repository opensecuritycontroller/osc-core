package org.osc.core.broker.window;

import org.osc.core.broker.window.button.ComponentModel;
import org.osc.core.broker.window.button.OkCancelValidateButtonModel;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

/**
 * Base Windows which provides default functionality for child class to extend
 * 
 */
@SuppressWarnings("serial")
public abstract class CRUDBaseValidateWindow extends CRUDBaseWindow<OkCancelValidateButtonModel> {

    public CRUDBaseValidateWindow(ComponentModel model) {
        super(new OkCancelValidateButtonModel());
    }

    // returns an empty form layout to derived classes with OK and submit in it.
    @Override
    public void createWindow(String caption) throws Exception {
        super.createWindow(caption);
        getComponentModel().setValidateClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                validateSettings();
            }
        });
    }

    public abstract void validateSettings();
}
