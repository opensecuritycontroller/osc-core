/*******************************************************************************
 * Copyright (c) 2017 Intel Corporation
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
package org.osc.core.poc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Generates String constants for field names
 */
public class FieldNameGenerator {

    public void generateStringConstants(Class<?> classToInspect) {
        String packageName = classToInspect.getPackage().getName();
        String className = classToInspect.getSimpleName();
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

            for(Field field: classToInspect.getDeclaredFields()) {
                String fieldName = field.getName();
                StringBuilder generatedFieldNameBuilder = new StringBuilder();
                for(char character : fieldName.toCharArray()) {
                    if(Character.isUpperCase(character)) {
                        generatedFieldNameBuilder.append("_");
                    }
                    generatedFieldNameBuilder.append(character);
                }
                String generatedFieldName = generatedFieldNameBuilder.toString().toUpperCase().trim();
                fileContent.append("public static final String ");
                fileContent.append(generatedFieldName);
                fileContent.append(" = \"");
                fileContent.append(fieldName);
                fileContent.append("\";\n\n");
            }

            fileContent.append("}");

            generatedFile = new File(className + "._java"); //$NON-NLS-1$
            fos = new FileOutputStream(generatedFile, false);
            fos.write(fileContent.toString().getBytes());

            fos.flush();

        } catch(Exception e) {

        }finally {
            try {
                if(fos != null) {
                    fos.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

}
