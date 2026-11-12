module com.cgaraydev.memory {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.cgaraydev.memory to javafx.fxml;
    exports com.cgaraydev.memory;
}
