package utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 * Created by willpride on 1/14/16.
 */
public class FileUtils {
    public static String getFile(Class mClass, String fileName){
        ClassLoader classLoader = mClass.getClassLoader();
        return getFile(classLoader, fileName);
    }

    public static String getFile(ClassLoader classLoader, String fileName){

        String result = "";
        try {
            result = IOUtils.toString(classLoader.getResourceAsStream(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;

    }
}
