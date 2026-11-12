module com.cgaraydev.memory {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;

    opens com.cgaraydev.memory.ui to javafx.fxml;
    exports com.cgaraydev.memory;
}
