package org.osc.core.sample;

import org.osc.core.tools.ResourceBundleKeyGenerator;


public class SampleMessageGenerator {

    public static void main(String[] args) {
        String currentFolder = System.getProperty("user.dir") +"\\src\\main\\java\\";
        System.out.println("Current Folder: " + currentFolder);

        ResourceBundleKeyGenerator generator = new ResourceBundleKeyGenerator(currentFolder, Messages.BUNDLE_NAME);
        generator.generate();

    }

}
