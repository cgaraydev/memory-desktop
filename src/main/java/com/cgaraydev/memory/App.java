package com.cgaraydev.memory;

import com.cgaraydev.memory.ui.MemoryController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public final class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                App.class.getResource("/com/cgaraydev/memory/ui/memory.fxml")
        ));

        Parent root = loader.load();
        MemoryController controller = loader.getController();

        Scene scene = new Scene(root, 1050, 790);
        scene.getStylesheets().add(Objects.requireNonNull(
                App.class.getResource("/com/cgaraydev/memory/ui/memory.css")
        ).toExternalForm());

        primaryStage = stage;
        stage.setTitle("Memory Desktop");
        stage.setMinWidth(810);
        stage.setMinHeight(640);
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            if (!confirmExit()) {
                event.consume();
            }
        });
        stage.setOnHidden(event -> controller.dispose());
        // Una ventana inactiva no debe seguir consumiendo tiempo de juego.
        stage.focusedProperty().addListener(
                (observable, oldValue, focused) ->
                        controller.onWindowFocusChanged(focused)
        );
        stage.show();
        controller.onWindowFocusChanged(stage.isFocused());
    }

    public static void requestExit() {
        if (confirmExit()) {
            Platform.exit();
        }
    }

    private static boolean confirmExit() {
        ButtonType exit = new ButtonType("Salir", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Salir del juego");
        alert.setHeaderText("¿Cerrar Memory Desktop?");
        alert.setContentText("La partida actual no se guardará.");
        alert.getButtonTypes().setAll(exit, cancel);
        if (primaryStage != null) {
            alert.initOwner(primaryStage);
        }

        return alert.showAndWait().orElse(cancel) == exit;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
