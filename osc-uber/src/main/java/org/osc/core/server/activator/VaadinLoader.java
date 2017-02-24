package org.osc.core.server.activator;

/**
 * Vaadin fails to load MainUI when run from {@link UiServletDelegate}.
 * even although both Vaadin and MainUI are in the same uber-bundle:
 * <pre>
 * Caused by: java.lang.ClassNotFoundException: org.osc.core.broker.view.MainUI
 * at java.net.URLClassLoader.findClass(URLClassLoader.java:381)
 * at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
 * at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:331)
 * at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
 * at com.vaadin.server.ServletPortletHelper.verifyUIClass(ServletPortletHelper.java:70)
 * </pre>
 * <p>
 * This is because Vaadin is using the Context ClassLoader, rather than its own ClassLoader.
 * <p>
 * Vaadin will attempt to use the ClassLoader specified in the "ClassLoader" ServletContext property.
 * This trivial ClassLoader delegates to its parent, which is Vaadin's ClassLoader.
 * <p>
 * The "ClassLoader"  property to use this class is set in {@link UiServletContext}.
 */
public class VaadinLoader extends ClassLoader {
    public VaadinLoader(ClassLoader parent) {
        super(parent);
    }
}
