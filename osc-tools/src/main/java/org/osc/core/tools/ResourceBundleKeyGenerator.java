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
package org.osc.core.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.regex.Matcher;

public class ResourceBundleKeyGenerator {

	private static final String COPYRIGHT_AND_LICENSE =
			"/*******************************************************************************\n"+
			" * Copyright (c) Intel Corporation\n"+
			" * Copyright (c) 2017\n"+
			" *\n"+
			" * Licensed under the Apache License, Version 2.0 (the \"License\");\n"+
			" * you may not use this file except in compliance with the License.\n"+
			" * You may obtain a copy of the License at\n"+
			" *\n"+
			" *    http://www.apache.org/licenses/LICENSE-2.0\n"+
			" *\n"+
			" * Unless required by applicable law or agreed to in writing, software\n"+
			" * distributed under the License is distributed on an \"AS IS\" BASIS,\n"+
			" * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"+
			" * See the License for the specific language governing permissions and\n"+
			" * limitations under the License.\n"+
			" *******************************************************************************/\n";

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

            fileContent.append(COPYRIGHT_AND_LICENSE);

            fileContent.append("package ");
            fileContent.append(packageName);
            fileContent.append(";\n\n");

            fileContent.append("public interface ");
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
