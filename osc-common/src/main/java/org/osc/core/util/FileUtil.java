package org.osc.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class FileUtil {

    /**
     * Returns file list from given directory
     *
     * @param directory Directory where plugins are held
     * @return File[] File array loaded from specified directory
     */
    public static File[] getFileListFromDirectory(String directory) throws FileNotFoundException {

        if (directory == null) {
            throw new FileNotFoundException("Cannot obtain list of files from directory - null given");
        }

        File fileDir = new File(directory);
        if (fileDir.exists()) {
            File[] listFiles = fileDir.listFiles();
            if (listFiles != null) {
                return listFiles;
            }
        }

        return new File[0];
    }

    /**
     * Loads properties from given path
     * @param propertiesFilePath Path to properties which should be loaded
     * @return Properties object
     * @throws IOException
     */
    public static Properties loadProperties(String propertiesFilePath) throws IOException {
        Properties prop = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(propertiesFilePath)) {
            prop.load(fileInputStream);
        }
        return prop;
    }

}
