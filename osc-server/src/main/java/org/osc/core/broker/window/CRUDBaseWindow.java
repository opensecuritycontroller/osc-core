package org.osc.core.broker.window;

import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;
import org.apache.log4j.Logger;
import org.osc.core.broker.view.PageInformationComponent;
import org.osc.core.broker.view.common.StyleConstants;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;

/**
 * Base Windows which provides default functionality for child class to extend
 *
 */
@SuppressWarnings("serial")
public abstract class CRUDBaseWindow<T extends OkCancelButtonModel> extends VmidcWindow<T> {

    private static final Logger log = Logger.getLogger(CRUDBaseWindow.class);

    protected FormLayout form = null;
    protected VerticalLayout content;

    private PageInformationComponent infoText;

    @SuppressWarnings("unchecked")
    public CRUDBaseWindow() {
        super((T) new OkCancelButtonModel());
    }

    public CRUDBaseWindow(T componentModel) {
        super(componentModel);
    }

    // returns an empty form layout to derived classes with OK and submit in it.
    public void createWindow(String caption) throws Exception {
        setCaption(caption);
        // creating top level layout for every window
        this.content = new VerticalLayout();

        // creating form layout shared by all derived classes
        this.form = new FormLayout();
        this.form.setMargin(true);
        this.form.setSizeUndefined();

        this.infoText = new PageInformationComponent();
        this.infoText.addStyleName(StyleConstants.PAGE_INFO_COMPONENT_WINDOW);

        this.content.addComponent(this.infoText);
        this.content.addComponent(this.form);

        getComponentModel().setOkClickedListener((ClickListener) event -> submitForm());
        getComponentModel().setCancelClickedListener((ClickListener) event -> {
            cancelForm();
            close();
        });

        // calling populateForm to create window specific content
        populateForm();

        // adding content to this window
        setContent(this.content);
    }

    /**
     * @see PageInformationComponent#setInfoText(String, String)
     */

    public void setInfoText(String title, String content) {
        this.infoText.setInfoText(title, content);
    }

    protected void handleCatchAllException(Throwable e) {
        log.info(e.getMessage());
        ViewUtil.iscNotification(e.getMessage(), Notification.Type.ERROR_MESSAGE);
    }

    public void cancelForm(){ }

    // creating window specific forms
    public abstract void populateForm() throws Exception;

    public abstract boolean validateForm();
    // window specific implementation for submitting a form
    public abstract void submitForm();

}
