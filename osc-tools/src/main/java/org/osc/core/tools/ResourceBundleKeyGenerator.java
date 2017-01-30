package org.osc.core.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.regex.Matcher;

public class ResourceBundleKeyGenerator {

    private String generatedCodeFolder;
    private String bundleName;

    public ResourceBundleKeyGenerator(String generatedCodeFolder, String bundleName) {
        this.generatedCodeFolder = generatedCodeFolder;
        this.bundleName = bundleName;
    }

    public void generate() {
        int lastDot = this.bundleName.lastIndexOf(".");
        String className = this.bundleName.substring(lastDot + 1);
        String packageName = this.bundleName.substring(0, lastDot);
        ResourceBundle resourceBundle = ResourceBundle.getBundle(this.bundleName);

        FileOutputStream fos = null;
        File generatedFile;

        try {
            StringBuilder fileContent = new StringBuilder();

            fileContent.append("package ");
            fileContent.append(packageName);
            fileContent.append(";\n\n");

            fileContent.append("public class ");
            fileContent.append(className);
            fileContent.append("_ {\n\n");

            Enumeration<String> keys = resourceBundle.getKeys();

            while (keys.hasMoreElements()) {
                String keyName = keys.nextElement();
                String keyValue = resourceBundle.getString(keyName);
                String generatedFieldName = keyName.trim().replaceAll("\\.", "_").toUpperCase();
                fileContent.append("\t/**\n\t * " + keyValue + "\n\t */\n");
                fileContent.append("\tpublic static final String ");
                fileContent.append(generatedFieldName);
                fileContent.append(" = \"");
                fileContent.append(keyName);
                fileContent.append("\";\n\n");
            }

            fileContent.append("}");

            String seperatorReplacement = Matcher.quoteReplacement(File.separator);
            generatedFile = new File(this.generatedCodeFolder + packageName.replaceAll("\\.", seperatorReplacement) + File.separator + className + "_.java"); //$NON-NLS-1$
            if(!generatedFile.exists()) {
                generatedFile.getParentFile().mkdirs();
                generatedFile.createNewFile();
            }

            fos = new FileOutputStream(generatedFile, false);
            fos.write(fileContent.toString().getBytes());

            fos.flush();

            System.out.println("File written to: " + generatedFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
