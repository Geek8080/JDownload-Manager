package jdm;

import com.jfoenix.controls.JFXDecorator;
import com.jfoenix.effects.JFXDepthManager;
import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import jdm.controller.DownloadManagerGUI;

import java.io.IOException;


public class GUILauncher extends Application {

    public static String serialisedFileName = "./files/Downloads.ser";

    public static void main(String[] args) {

        //DownloadManagerGUI managerGUI = new DownloadManagerGUI();
        //managerGUI.setMaxSize(managerGUI.getPrefWidth(), managerGUI.getPrefHeight());

        //File file = new File(serialisedFileName);
        //if (!file.getAbsoluteFile().getParentFile().exists()){
        //    file.getAbsoluteFile().getParentFile().mkdirs();
        //}else if(file.getAbsoluteFile().exists()){
        //    /* Inflate the objects from the file
        //     * show loading dialog
        //     * start the application
        //     */
//
        //    /* Task<Integer> task = new Task<Integer>() {
        //     *     @Override
        //     *     protected Integer call() throws Exception {
        //     *         return null;
        //     *     }
        //     * };
//
        //     * task.setOnRunning((e) -> loadingDialog.show());
        //     * task.setOnSucceeded((e) -> {
        //     *     loadingDialog.hide();
        //     *     Boolean returnValue = task.get();
        //     *     // process return value again in JavaFX thread
        //     * });
        //     * task.setOnFailed((e) -> {
        //     *     // eventual error handling by catching exceptions from task.get()
        //     * });
        //     * new Thread(task).start();
        //     */
        //}
//


        launch(args);

    }

    @Override
    public void start(Stage primaryStage) {

        DownloadManagerGUI managerGUI = new DownloadManagerGUI();
        managerGUI.setMaxWidth(managerGUI.getPrefWidth());
        managerGUI.setMaxHeight(managerGUI.getPrefHeight());
        managerGUI.getBox().setMaxHeight(managerGUI.getScroller().getPrefHeight());
        managerGUI.stage = primaryStage;

        StackPane basePane = new StackPane();
        basePane.setAlignment(managerGUI, Pos.CENTER);
        basePane.getChildren().add(managerGUI);


        JFXDecorator decorator = new JFXDecorator(primaryStage,basePane);
        decorator.setTitle("JDownload Manager");
        decorator.setAlignment(Pos.CENTER);
        decorator.setCenterShape(true);
        decorator.setBackground(new Background(new BackgroundImage(new Image("/image/bacg.jpg",basePane.getMaxWidth(),basePane.getMaxHeight(),true,true),
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                BackgroundSize.DEFAULT)));
        decorator.setCustomMaximize(true);


        JFXDepthManager.setDepth(basePane, 1500);

        basePane.setStyle("-fx-blend-mode: multiply");

        //decorator.setMaximized(true);

        primaryStage.setScene(new Scene(decorator, 800, 800));
        primaryStage.setMinHeight(650);
        primaryStage.setMinWidth(720);
        primaryStage.getIcons().add(new Image(GUILauncher.class.getResourceAsStream("/image/icon.png")));
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> System.exit(1));

    }
}
