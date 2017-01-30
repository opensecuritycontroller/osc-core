package org.osc.core.broker.window;

import com.vaadin.ui.Button.ClickListener;
import org.osc.core.broker.window.button.ApproveCancelButtonModel;
import org.osc.core.broker.window.button.ComponentModel;

@SuppressWarnings("serial")
public abstract class CRUDBaseApproveWindow extends CRUDBaseWindow<ApproveCancelButtonModel> {

    public CRUDBaseApproveWindow(ComponentModel model) {
        super(new ApproveCancelButtonModel());
    }

    @Override
    public void createWindow(String caption) throws Exception {
        super.createWindow(caption);
        getComponentModel().setApproveClickedListener((ClickListener) event -> submitForm());
        getComponentModel().setCancelClickedListener((ClickListener) event -> {
            cancelForm();
            close();
        });
    }
}