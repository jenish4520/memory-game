module seda_project.control_alt_defeat {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;

    exports seda_project.control_alt_defeat.gamebox;
    opens seda_project.control_alt_defeat.gamebox to javafx.graphics, javafx.fxml;
}
