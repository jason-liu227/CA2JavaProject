package com.example.ca2realrealsproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the FXML layout
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/ca2realrealsproject/main_app.fxml")
        );
        Parent root = loader.load();

        // Set up the scene and stage
        Scene scene = new Scene(root);
        primaryStage.setTitle("Vienna U-Bahn Route Finder");
        primaryStage.setScene(scene);
        primaryStage.setWidth(1024);
        primaryStage.setHeight(768);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}