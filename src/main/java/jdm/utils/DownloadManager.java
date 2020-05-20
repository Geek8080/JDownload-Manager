package jdm.utils;

import java.io.File;
import java.io.Serializable;

public class DownloadManager implements Serializable {

    public static int downloadCount = 0;

    public static Download getDownloadInstance(String url, File directory) {
        return new Download(url, directory, ++downloadCount);
    }
}
