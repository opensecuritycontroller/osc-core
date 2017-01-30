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
