package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("FXMLs/mainUI.fxml"));
        primaryStage.setTitle("MicroProcessor 8085 Kit");
        if (!System.getProperty("os.name").equals("Linux")){
            primaryStage.setScene(new Scene(root, 1180, 690));
        }else{
            primaryStage.setScene(new Scene(root, 1200, 700));
        }
        primaryStage.setResizable(true);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }



}
