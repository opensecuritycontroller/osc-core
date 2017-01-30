package org.osc.core.tools;



public class Generator {

    public static void main(String[] args) {
        String generatedCodeFolder = args[0];
        String bundleName = args[1];

        ResourceBundleKeyGenerator keygen = new ResourceBundleKeyGenerator(generatedCodeFolder, bundleName);
        keygen.generate();

    }

}
