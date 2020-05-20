package jdm.controller;

import card.controller.DownloadCard;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextField;
import com.sun.javafx.tk.FileChooserType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jdm.GUILauncher;
import jdm.utils.Download;
import jdm.utils.DownloadManager;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Observable;
import java.util.Observer;

import static javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS;


public class DownloadManagerGUI extends AnchorPane{

    public Stage stage = null;

    public DownloadManagerGUI(){
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/DownloadManagerGUI.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private AnchorPane baseGUI;

    @FXML
    private JFXTextField urlTextField;

    @FXML
    private ScrollPane scroller;

    @FXML
    private VBox box;

    @FXML
    void onEnter(ActionEvent event) {
        System.out.println(getUrlTextField().getText());

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select the folder to save the file.");
        File directory = directoryChooser.showDialog(this.stage);

        try{

            DownloadCard downloadCard = new DownloadCard();
            this.box.getChildren().add(downloadCard);

            System.out.println("Added the gui: " + Calendar.getInstance().getTime());

            Platform.runLater(() -> {
                Download download = DownloadManager.getDownloadInstance(getUrlTextField().getText().trim(), directory);
                System.out.println("Initialised the Download object: " + Calendar.getInstance().getTime());
                downloadCard.addDownloadableObject(download);
                download.card = downloadCard;
                new Thread(download).start();
                if (download.isPausable()){
                    downloadCard.setPausabelCard(true);
                }
                download.setDirectory(directory);
                download.setParent(this.box);
                System.out.println("Started the download");
            });
        }catch (Exception ex){

        }
    }

    public AnchorPane getBaseGUI() {
        return baseGUI;
    }

    public void setBaseGUI(AnchorPane baseGUI) {
        this.baseGUI = baseGUI;
    }

    public JFXTextField getUrlTextField() {
        return urlTextField;
    }

    public void setUrlTextField(JFXTextField urlTextField) {
        this.urlTextField = urlTextField;
    }

    public ScrollPane getScroller() {
        return scroller;
    }

    public void setScroller(ScrollPane scroller) {
        this.scroller = scroller;
    }

    public VBox getBox() {
        return box;
    }

    public void setBox(VBox box) {
        this.box = box;
    }

}

