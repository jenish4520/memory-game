module seda.project.control.alt.defeat.gamebox {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;
    requires ch.qos.logback.classic;

    opens seda_project.control_alt_defeat.gamebox to javafx.fxml;
    exports seda_project.control_alt_defeat.gamebox to javafx.graphics;
}
