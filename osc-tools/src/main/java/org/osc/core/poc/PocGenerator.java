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
package org.osc.core.poc;



public class PocGenerator {

    public static final String basePath = "F:\\Software\\Eclipse\\workspaces\\01\\vmidcTools\\src\\main\\java\\";

    /*public static void main(String[] args) {
        FieldNameGenerator fieldNameGenerator = new FieldNameGenerator();

        Collection<File> filesToGenerateFor = FileUtils.listFiles(new File(basePath), new String[] { "java" }, true);

        for (File toGenerate : filesToGenerateFor) {
            try {
                fieldNameGenerator.generateStringConstants(ClassLoader.getSystemClassLoader().loadClass(
                        getClassName(toGenerate)));
            } catch (ClassNotFoundException e) {
                System.out.println(toGenerate.getName() + " Class Not Found");
            }
        }

    }

    private static String getClassName(File toGenerate) {
        return toGenerate.getPath().replace(basePath + "\\", "").replace(".java", "").replace("\\", ".");
    }*/

}
