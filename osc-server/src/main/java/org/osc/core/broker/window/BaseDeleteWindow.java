package org.osc.core.broker.window;

import org.osc.core.broker.window.button.OkCancelButtonModel;

import com.vaadin.ui.CheckBox;

/**
 * Base Windows which provides default functionality for child class to extend
 * 
 */

public abstract class BaseDeleteWindow extends CRUDBaseWindow<OkCancelButtonModel> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected CheckBox forceDeletion;

    public BaseDeleteWindow() throws Exception {
        super();
        this.forceDeletion = new CheckBox("Force Delete");

    }
}
