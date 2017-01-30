package org.osc.core.broker.view.maintenance;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Link;

public class SupportLayout extends FormLayout {

    private static final long serialVersionUID = 1L;

    public SupportLayout() {
        Link link = new Link("Open Security Controller", new ExternalResource("http://www.intel.com/osc"));
        link.setTargetName("_blank");
        addComponent(link);
    }
}
