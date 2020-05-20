package jdm.utils;

import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Observable;

public class DownloadSegmentThread extends Observable implements Runnable, Serializable {

    public int getSegmentID() {
        return segmentID;
    }

    private int segmentID;

    private static final int MAX_BUFFER_SIZE = 1024;

    public static final String STATUSES[] = {"Downloading",
            "Paused", "Complete", "Cancelled", "Error"};

    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;

    public URL url;
    public long size;
    public long downloaded;
    public int status;

    public long startPos;
    public long endPos;

    private Download parentDownload;

    public DownloadSegmentThread(URL url, int id, Download parentDownload) {

        this.startPos = 0;
        this.endPos = 0;
        this.segmentID = id;
        this.url = url;
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;
        this.parentDownload = parentDownload;

        download();

        System.out.println("Downloading");
    }

    public DownloadSegmentThread(URL url, long startPos, long endPos, int id, Download parentDownload) {

        this.segmentID = id;
        this.url = url;
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;
        this.parentDownload = parentDownload;

        this.startPos = startPos;
        this.endPos = endPos;

        download();
    }

    public String getUrl() {
        return url.toString();
    }

    public long getSize() {
        return size;
    }

    public int getStatus() {
        return status;
    }

    public void pause() {
        status = PAUSED;
        stateChanged();
    }

    public void resume() {
        status = DOWNLOADING;
        stateChanged();
        download();
    }

    public void cancel() {
        status = CANCELLED;
        File file = new File(segmentID + ".part");
        file.delete();
        //stateChanged();
    }


    private void error() {
        status = ERROR;
        System.out.println("Encountered an error");
        stateChanged();
    }

    private void download() {
        Thread thread = new Thread(this);
        thread.start();
    }

    private void stateChanged() {
        setChanged();
        notifyObservers();
    }

    @Override
    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;
        try {

            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            //if(endPos == -1){
            //    connection.setRequestProperty("Range",
            //            "bytes=" + (downloaded) + "-");
            //}else {
                connection.setRequestProperty("Range",
                        "bytes=" + (startPos + downloaded) + "-" + endPos);
            //}

            System.out.println("Assigned range: "+ (startPos + downloaded) + "-" + endPos);

            connection.connect();

            if (connection.getResponseCode() / 100 != 2) {
                error();
            }

            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                error();
            }

            if (size == -1) {
                size = contentLength;
                stateChanged();
            }

            System.out.println("Size: " + this.size);


            file = new RandomAccessFile(segmentID + ".part", "rw");
            file.seek(downloaded);
            stream = connection.getInputStream();

            while (status == DOWNLOADING) {

                byte buffer[];
                if (size - downloaded > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[(int) (size - downloaded)];
                }

                int read = stream.read(buffer);
                if (read == -1)
                    break;

                file.write(buffer, 0, read);
                downloaded += read;
                this.parentDownload.incrementDownloaded(read);
                stateChanged();
            }

            if (status == DOWNLOADING) {
                status = COMPLETE;
                file.close();
                stateChanged();
            }
        } catch (Exception e) {
            error();
            e.printStackTrace();
        } finally {

            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {}
            }

            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {}
            }
        }
    }

    //http://www.blackkat.net/tintin/pdf/15%20-%20Land%20Of%20Black%20Gold.pdf



}

