package seda_project.control_alt_defeat.gamebox;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GameBox extends Application {
    private static final Logger logger = LoggerFactory.getLogger(GameBox.class);

    @Override
    public void start(Stage stage) throws IOException {

        stage.setTitle("GameBox");
        stage.centerOnScreen();
        stage.show();
        stage.setOnCloseRequest(_ -> cleanExit());

        final var fxmlUrl = GameBox.class.getResource("/MyView.fxml");
        final var loader = new FXMLLoader(fxmlUrl);
        final var mainMenuScene = new Scene(loader.load(), 800, 600);
        stage.setScene(mainMenuScene);
        logger.debug("Startup completed");
    }

    public static void cleanExit() {
        logger.debug("Shutting down");
        Platform.exit();
        System.exit(0);
    }
}