/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.window;

import org.osc.core.broker.view.common.VmidcMessages;
import org.osc.core.broker.view.common.VmidcMessages_;
import org.osc.core.broker.view.util.ViewUtil;
import org.osc.core.broker.window.button.OkCancelButtonModel;
import org.osc.core.broker.window.button.OkCancelForceDeleteModel;
import org.osc.core.broker.window.button.OkCancelValidateButtonModel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

/**
 * A utility class for creating windows which can be used generically across the application
 */
public final class WindowUtil {

    private final static long KB = 1024;
    private final static long MB = KB * KB;

    private WindowUtil() {

    }

    /**
     *
     * Returns a Delete window using provided Content and Caption.It will have a Force Delete check box along with
     * Cancel and Ok Buttons.
     * The cancel button defaults to closing the window. You can customize behavior of OK,Cancel and Force Delete
     * components by calling @link {@link VmidcWindow#getComponentModel()} and setting a different
     * listener to that component
     *
     * @param caption
     *            Window's Caption
     * @param content
     *            Window's Content
     * @return
     *         Window Object
     */
    public static VmidcWindow<OkCancelValidateButtonModel> createValidateWindow(String caption, String content) {
        final VmidcWindow<OkCancelValidateButtonModel> validateWindow = new VmidcWindow<OkCancelValidateButtonModel>(
                new OkCancelValidateButtonModel());
        validateWindow.setCaption(caption);
        validateWindow.getComponentModel().setCancelClickedListener(new ClickListener() {
            /**
             *
             */
            private static final long serialVersionUID = -1166844267835596823L;

            @Override
            public void buttonClick(ClickEvent event) {
                validateWindow.close();
            }
        });
        Label contentLabel = new Label(content);
        contentLabel.setContentMode(ContentMode.HTML);

        FormLayout form = new FormLayout();
        form.setMargin(true);
        form.setSizeUndefined();
        form.addComponent(contentLabel);

        validateWindow.setContent(form);
        return validateWindow;
    }

    /**
     *
     * Returns a Delete window using provided Content and Caption.It will have a Force Delete check box along with
     * Cancel and Ok Buttons.
     * The cancel button defaults to closing the window. You can customize behavior of OK,Cancel and Force Delete
     * components by calling @link {@link VmidcWindow#getComponentModel()} and setting a different
     * listener to that component
     *
     * @param caption
     *            Window's Caption
     * @param content
     *            Window's Content
     * @return
     *         Window Object
     */
    public static VmidcWindow<OkCancelForceDeleteModel> createForceDeleteWindow(String caption, String content) {
        final VmidcWindow<OkCancelForceDeleteModel> deleteWindow = new VmidcWindow<OkCancelForceDeleteModel>(
                new OkCancelForceDeleteModel());

        //Creating the Force Delete Warning Window
        ValueChangeListener forceDeleteValueChangeListener = new ValueChangeListener() {

            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                OkCancelForceDeleteModel forceDeleteModel = deleteWindow.getComponentModel();
                if (forceDeleteModel.getForceDeleteCheckBoxValue()) {
                    final VmidcWindow<OkCancelButtonModel> alertWindow = WindowUtil.createAlertWindow("Force Delete",
                            VmidcMessages.getString(VmidcMessages_.FORCE_DELETE_WARNING));
                    ViewUtil.addWindow(alertWindow);
                    alertWindow.getComponentModel().setOkClickedListener(new ClickListener() {

                        /**
                         *
                         */
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void buttonClick(ClickEvent event) {
                            alertWindow.close();
                        }
                    });
                    alertWindow.getComponentModel().setCancelClickedListener(new ClickListener() {

                        /**
                         *
                         */
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void buttonClick(ClickEvent event) {
                            OkCancelForceDeleteModel forceDeleteModel = deleteWindow.getComponentModel();
                            forceDeleteModel.setForceDeleteValue(false);
                            alertWindow.close();
                        }
                    });
                }

            }

        };

        if (deleteWindow.getComponentModel() instanceof OkCancelForceDeleteModel) {
            OkCancelForceDeleteModel forceDeleteModel = deleteWindow.getComponentModel();
            forceDeleteModel.setForceDeleteValueChangeListener(forceDeleteValueChangeListener);
        }

        //Creating the Force Delete Window
        deleteWindow.setCaption(caption);
        deleteWindow.getComponentModel().setCancelClickedListener(new ClickListener() {

            /**
             *
             */
            private static final long serialVersionUID = -651121508110914210L;

            @Override
            public void buttonClick(ClickEvent event) {
                deleteWindow.close();
            }
        });
        Label contentLabel = new Label(content);
        contentLabel.setContentMode(ContentMode.HTML);

        FormLayout form = new FormLayout();
        form.setMargin(true);
        form.setSizeUndefined();
        form.addComponent(contentLabel);

        deleteWindow.setContent(form);
        return deleteWindow;
    }

    /**
     * Returns a simple window with the specified caption and content(which can be HTML) with an OK and Cancel Buttons.
     * The cancel button defaults to closing the window.
     * This behaviour can be modified by calling @link {@link VmidcWindow#getComponentModel()} and setting a different
     * listener to the cancel button
     *
     * @param caption
     *            the caption
     * @param content
     *            the content
     * @return the handle to window object
     */
    public static VmidcWindow<OkCancelButtonModel> createAlertWindow(String caption, String content) {
        final VmidcWindow<OkCancelButtonModel> alertWindow = new VmidcWindow<OkCancelButtonModel>(
                new OkCancelButtonModel());
        alertWindow.setCaption(caption);
        alertWindow.getComponentModel().setCancelClickedListener(new ClickListener() {

            /**
             *
             */
            private static final long serialVersionUID = 98853982893459323L;

            @Override
            public void buttonClick(ClickEvent event) {
                alertWindow.close();
            }
        });
        Label contentLabel = new Label(content);
        contentLabel.setContentMode(ContentMode.HTML);

        FormLayout form = new FormLayout();
        form.setMargin(true);
        form.setSizeUndefined();
        form.addComponent(contentLabel);

        alertWindow.setContent(form);
        return alertWindow;
    }

    /**
     * This method converts Bytes into Kilo Bytes or Mega Bytes based on the given input
     *
     * @param bytes
     *            Bytes needs to be converted into human redeable format
     * @return
     *         String with
     */
    public static String convertBytesToReadableSize(long bytes) {
        String size;

        if (bytes / MB > 0) {
            size = String.valueOf(bytes / MB) + " MB";
        } else if (bytes / KB > 0) {
            size = String.valueOf(bytes / KB) + " KB";
        } else {
            size = String.valueOf(bytes) + " bytes";
        }
        return size;
    }

}
