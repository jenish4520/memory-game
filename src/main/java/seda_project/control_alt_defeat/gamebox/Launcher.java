package seda_project.control_alt_defeat.gamebox;

import javafx.application.Application;
import javafx.stage.Stage;


// Entry point.
public class Launcher extends Application {

    @Override
    public void start(Stage primaryStage) {
        GameBox window = new GameBox(primaryStage); // Start GameBox
        window.show();
    }

    // Launch app.
    public static void Launcher(String[] args) {
        launch(args);
    }
}
