package jdm.utils;

import card.controller.DownloadCard;
import card.utils.Downloadable;
import javafx.application.Platform;
import javafx.scene.layout.VBox;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

public class Download extends Observable implements Runnable, Downloadable, Observer, Serializable {

    public long contentLength;

    public String contentLengthString;

    LinkedList<DownloadSegmentThread> downloadSegments;

    URL url = null;
    String urlString = "";
    URLConnection connection = null;
    File outFile = null;

    public DownloadCard card = null;

    private int serialNo = -1;
    private String fileName = "";
    private String fileType = "";
    private double progress = 0.0d;
    private long downloadedBit = 0l;
    private boolean isPausable = false;
    private String fileExtension = "";
    private Icon fileTypeIcon = null;
    private int completedDownloads = 0;

    private int runningThreads = 0;
    private File directory;
    private VBox parentContainer;


    @Override
    public int getSerialNo() {
        return serialNo;
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public String getFileType() {
        return this.fileType;
    }

    @Override
    public double getProgress() {
        return this.progress;
    }

    @Override
    public String getProgressValuesAsString() {
        String downloadedString = "";
        double downloaded = ((double) getDownloaded()) / (1024.0);
        if (downloaded > 1024) {
            downloadedString = String.format("%.2f MB", downloaded / 1024);
        } else {
            downloadedString = String.format("%.2f KB", downloaded);
        }

        double progressValue = getProgress() * 100;

        return String.format("%.2f%s (%s/%s)", progressValue, "%", downloadedString, contentLengthString);
    }

    @Override
    public long getDownloaded() {
        return this.downloadedBit;
    }

    @Override
    public long getSize() {
        return this.contentLength;
    }

    @Override
    public Icon getFileTypeIcon() {
        return this.fileTypeIcon;
    }

    @Override
    public void cancel() {
        this.card.setStatus("Cancelling.. Don't click anywhere on the card");
        for (DownloadSegmentThread downloadSegmentThread : this.downloadSegments) {
            downloadSegmentThread.cancel();
        }
        this.card.setVisible(false);
        this.parentContainer.getChildren().remove(this.card);
    }

    @Override
    public void pause() {
        for (DownloadSegmentThread downloadSegmentThread : this.downloadSegments) {
            downloadSegmentThread.pause();
        }
        Platform.runLater(()->{
            this.card.setStatus("Paused");
        });
    }

    @Override
    public void resume() {
        for (DownloadSegmentThread downloadSegmentThread : this.downloadSegments) {
            downloadSegmentThread.resume();
        }
        Platform.runLater(()->{
            this.card.setStatus("Downloading");
        });
    }

    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>Observable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param o   the observable object.
     * @param arg an argument passed to the <code>notifyObservers</code>
     */
    @Override
    public void update(Observable o, Object arg) {

        DownloadSegmentThread downloadSegmentThread = (DownloadSegmentThread) o;
        this.progress = ((double) this.downloadedBit) / ((double) this.contentLength);
        //if(downloadSegmentThread.getStatus()==DownloadSegmentThread.COMPLETE){
        //    downloadSegments.removeFirstOccurrence(downloadSegmentThread);
        //}
        try {
            card.setProgressValue();
        } catch (Exception e) {
        }

        if (downloadSegmentThread.getStatus() == DownloadSegmentThread.COMPLETE) {
            this.completedDownloads++;

            if (this.completedDownloads == this.runningThreads) {

                this.card.getTimer().cancel();

                //System.out.println("Files written");

                Platform.runLater(() -> {
                    this.card.setMerging();
                });

                mergeFiles();

                Platform.runLater(() -> {
                    this.card.setCompleted();
                });


                //Platform.runLater(() -> {
                //    try {
                //        Thread.sleep(200);
                //    } catch (InterruptedException e) {
                //        e.printStackTrace();
                //    }
                //    this.card.hide();
                //});

                //Platform.setImplicitExit(true);
                //Platform.exit();
            }
        }

    }

    private void mergeFiles() {
        try {
            if (!this.outFile.getParentFile().getAbsoluteFile().exists()) {
                this.outFile.mkdirs();
            }
            if (!this.outFile.getAbsoluteFile().exists()) {
                this.outFile.createNewFile();
            }

            OutputStream out = null;

            try {
                out = Files.newOutputStream(Paths.get(this.outFile.getAbsolutePath()), StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);

                for (DownloadSegmentThread downloadSegmentThread : downloadSegments) {
                    String inFileName = downloadSegmentThread.getSegmentID() + ".part";
                    Files.copy(Paths.get(inFileName), out);
                }
            }catch (Exception ex){

                System.out.println("Writing using the slower method..");
                RandomAccessFile outFile = new RandomAccessFile(this.outFile, "rw");

                long len = 0;

                for (DownloadSegmentThread downloadSegmentThread : downloadSegments) {
                    String fileName = downloadSegmentThread.getSegmentID() + ".part";
                    RandomAccessFile inFile = new RandomAccessFile(fileName, "r");

                    int data;

                    outFile.seek(len);

                    while ((data = inFile.read()) != -1) {
                        outFile.writeByte(data);
                    }

                    len += inFile.length();

                    inFile.close();

                    downloadSegmentThread.cancel();
                }


                outFile.close();
            }finally {
                if (out!=null){
                    out.close();;
                }
            }

            /*RandomAccessFile outFile = new RandomAccessFile(this.outFile, "rw");

            long len = 0;

            for (DownloadSegmentThread downloadSegmentThread : downloadSegments) {
                String fileName = downloadSegmentThread.getSegmentID() + ".part";
                RandomAccessFile inFile = new RandomAccessFile(fileName, "r");

                int data;

                outFile.seek(len);

                while ((data = inFile.read()) != -1) {
                    outFile.writeByte(data);
                }

                len += inFile.length();

                inFile.close();

                downloadSegmentThread.cancel();
            }


            outFile.close();

             */



            //this.card.setCompleted();
            //RandomAccessFile inFile1 = new RandomAccessFile("10.part", "r");
            //RandomAccessFile inFile2 = new RandomAccessFile("11.part", "r");
            //RandomAccessFile inFile3 = new RandomAccessFile("12.part", "r");
            //RandomAccessFile inFile4 = new RandomAccessFile("13.part", "r");
            //RandomAccessFile inFile5 = new RandomAccessFile("14.part", "r");
            //RandomAccessFile inFile6 = new RandomAccessFile("15.part", "r");
            //RandomAccessFile inFile7 = new RandomAccessFile("16.part", "r");
            //RandomAccessFile inFile8 = new RandomAccessFile("17.part", "r");
//
            //int data;
            //while ((data = inFile1.read()) != -1) {
            //    outFile.writeByte(data);
            //}
//
            //len += inFile1.length();
            //outFile.seek(len);
//
            //while ((data = inFile2.read()) != -1) {
            //    outFile.writeByte(data);
            //}
//
            //len += inFile2.length();
            //outFile.seek(len);
//
            //while ((data = inFile3.read()) != -1) {
            //    outFile.writeByte(data);
            //}
//
            //len += inFile3.length();
            //outFile.seek(len);
//
            //while ((data = inFile4.read()) != -1) {
            //    outFile.writeByte(data);
            //}
//
//
            //len += inFile4.length();
            //outFile.seek(len);
//
            //while ((data = inFile5.read()) != -1) {
            //    outFile.writeByte(data);
            //}
//
//
            //len += inFile5.length();
            //outFile.seek(len);
//
            //while ((data = inFile6.read()) != -1) {
            //    outFile.writeByte(data);
            //}
//
//
            //len += inFile6.length();
            //outFile.seek(len);
//
            //while ((data = inFile7.read()) != -1) {
            //    outFile.writeByte(data);
            //}
//
//
            //len += inFile7.length();
            //outFile.seek(len);
//
            //while ((data = inFile8.read()) != -1) {
            //    outFile.writeByte(data);
            //}
//
            //inFile1.close();
            //inFile2.close();
            //inFile3.close();
            //inFile4.close();
            //inFile5.close();
            //inFile6.close();
            //inFile7.close();
            //inFile8.close();


            System.out.println("Successfully Merged all Files...");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {

        downloadSegments = new LinkedList<>();

        this.card.setStatus("Downloading");


        //Icon ico = javax.swing.filechooser.FileSystemView.getFileSystemView().getSystemIcon(this.outFile);
        //this.fileTypeIcon = javax.swing.filechooser.FileSystemView.getFileSystemView().getSystemIcon(this.outFile);
        //this.card.updateFileTypeImage();

        if (isPausable) {
            int partLength = (int) contentLength / 8;
            System.out.println(partLength);
            DownloadSegmentThread downloadSegmentThread = null;

            downloadSegmentThread = new DownloadSegmentThread(this.url, 0, partLength, serialNo * 10, this);//this.serialNo*10 + i);
            downloadSegmentThread.addObserver(this);
            downloadSegments.add(downloadSegmentThread);
            runningThreads++;

            downloadSegmentThread = new DownloadSegmentThread(this.url, partLength + 1, partLength * 2, serialNo * 10 + 1, this);//this.serialNo*10 + i);
            downloadSegmentThread.addObserver(this);
            downloadSegments.add(downloadSegmentThread);
            runningThreads++;

            downloadSegmentThread = new DownloadSegmentThread(this.url, partLength * 2 + 1, partLength * 3, serialNo * 10 + 2, this);//this.serialNo*10 + i);
            downloadSegmentThread.addObserver(this);
            downloadSegments.add(downloadSegmentThread);
            runningThreads++;

            downloadSegmentThread = new DownloadSegmentThread(this.url, partLength * 3 + 1, partLength * 4, serialNo * 10 + 3, this);//this.serialNo*10 + i);
            downloadSegmentThread.addObserver(this);
            downloadSegments.add(downloadSegmentThread);
            runningThreads++;

            downloadSegmentThread = new DownloadSegmentThread(this.url, partLength * 4 + 1, partLength * 5, serialNo * 10 + 4, this);//this.serialNo*10 + i);
            downloadSegmentThread.addObserver(this);
            downloadSegments.add(downloadSegmentThread);
            runningThreads++;

            downloadSegmentThread = new DownloadSegmentThread(this.url, partLength * 5 + 1, partLength * 6, serialNo * 10 + 5, this);//this.serialNo*10 + i);
            downloadSegmentThread.addObserver(this);
            downloadSegments.add(downloadSegmentThread);
            runningThreads++;

            downloadSegmentThread = new DownloadSegmentThread(this.url, partLength * 6 + 1, partLength * 7, serialNo * 10 + 6, this);//this.serialNo*10 + i);
            downloadSegmentThread.addObserver(this);
            downloadSegments.add(downloadSegmentThread);
            runningThreads++;

            downloadSegmentThread = new DownloadSegmentThread(this.url, partLength * 7 + 1, contentLength, serialNo * 10 + 7, this);//this.serialNo*10 + i);
            downloadSegmentThread.addObserver(this);
            downloadSegments.add(downloadSegmentThread);
            runningThreads++;

        } else {
            DownloadSegmentThread downloadSegmentThread = null;
            downloadSegmentThread = new DownloadSegmentThread(this.url, 0, (this.contentLength / 2), 1, this);
            downloadSegmentThread.addObserver(this);
            downloadSegments.add(downloadSegmentThread);
            runningThreads++;
        }


        //System.out.println(isPausable);

        //DownloadSegmentThread downloadSegmentThread2 = new DownloadSegmentThread(this.url, (this.contentLength/2)+1, this.contentLength, (serialNo * 10) + 1);
        //downloadSegmentThread2.addObserver(this);


        //downloadSegments.add(downloadSegmentThread2);

        System.out.println("Calling Method");
        //new Thread(downloadSegmentThread1).start();
        //System.out.println("Calling Method");
        //new Thread(downloadSegmentThread2).start();
    }

    public Download(String urlString, File outFolder, int serialNo) {

        this.urlString = urlString;

        this.serialNo = serialNo;

        urlString.replaceFirst("^https", "http");

        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if (isValid(url)) {//url.getProtocol())

            System.out.println("The URL is valid...");

            try {
                connection = url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }

            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36");
            connection.setReadTimeout(5000);

            if (isPausable(connection)) {
                this.isPausable = true;
            } else {
                this.isPausable = false;
            }

            this.fileType = connection.getContentType();
            this.fileName = url.getFile();
            this.fileName = this.fileName.substring(this.fileName.lastIndexOf('/') + 1, this.fileName.lastIndexOf('.'));

            try {
                this.fileName = URLDecoder.decode(this.fileName, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            //String possFileType = URLConnection.guessContentTypeFromStream(new BufferedInputStream(((HttpURLConnection) connection).getInputStream()));

            this.fileExtension = this.fileType.substring(this.fileType.lastIndexOf('/') + 1);
            this.outFile = new File(outFolder.getAbsoluteFile().getAbsolutePath() + "/" + this.fileName + "." + this.fileExtension);
            this.contentLength = connection.getContentLength();

            double cl = ((double) contentLength) / (1024.0);
            if (cl > 1024) {
                this.contentLengthString = String.format("%.2f MB", cl / 1024);
            } else {
                this.contentLengthString = String.format("%.2f MB", cl);
            }

            this.downloadedBit = 0;

            //if(!isPausable(connection)){

            //}else{

            //run();

            //}

//http://www.blackkat.net/tintin/pdf/16%20-%20Destination%20Moon.pdf

        } else {
            //Show error dialog
        }
    }

    public boolean isPausable() {
        return this.isPausable;
    }

    private boolean isPausable(URLConnection connection) {

        try {
            return (connection.getHeaderField("Accept-Ranges") != null);
        } catch (Exception ex) {
            return false;
        }

    }

    public boolean isValid(URL url) {
        try {
            url.toURI();
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public void incrementDownloaded(int read) {
        this.downloadedBit += read;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public void setParent(VBox box) {
        this.parentContainer = box;
    }
}
