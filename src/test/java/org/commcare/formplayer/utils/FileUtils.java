package org.commcare.formplayer.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by willpride on 1/14/16.
 */
public class FileUtils {

    public static String getFile(Class mClass, String fileName) {
        try {
            return IOUtils.toString(getFileStream(mClass, fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static InputStream getFileStream(Class mClass, String fileName) {
        ClassLoader classLoader = mClass.getClassLoader();
        return classLoader.getResourceAsStream(fileName);
    }
}
