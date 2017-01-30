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
