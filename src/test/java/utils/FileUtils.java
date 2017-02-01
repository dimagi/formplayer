package utils;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by willpride on 1/14/16.
 */
public class FileUtils {
    public static String getFile(Class mClass, String fileName){

        String result = "";

        ClassLoader classLoader = mClass.getClassLoader();
        try {
            result = IOUtils.toString(classLoader.getResourceAsStream(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;

    }
}
