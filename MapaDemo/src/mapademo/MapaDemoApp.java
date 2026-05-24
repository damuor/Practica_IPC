/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mapademo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 *
 * @author jose
 */
public class MapaDemoApp extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("views/MainLayout.fxml"));
        Parent root = loader.load();
        mapademo.controllers.MainLayoutController controller = loader.getController();
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/logo.png")));
        Scene scene = new Scene(root, 1440, 820);
        stage.setTitle("Running la Safor - IPC 2026");
        stage.setScene(scene);
        controller.installKeyboardShortcuts(scene);
        stage.setMinWidth(1320);
        stage.setMinHeight(780);
        stage.setOnCloseRequest(event -> controller.shutdown());
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
