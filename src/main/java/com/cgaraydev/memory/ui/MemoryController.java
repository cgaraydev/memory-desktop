package com.cgaraydev.memory.ui;

import com.cgaraydev.memory.App;
import com.cgaraydev.memory.application.MemoryGameCoordinator;
import com.cgaraydev.memory.application.GameResult;
import com.cgaraydev.memory.application.SystemMemoryClock;
import com.cgaraydev.memory.records.GameRecord;
import com.cgaraydev.memory.records.HistoryPagination;
import com.cgaraydev.memory.records.JsonRecordStore;
import com.cgaraydev.memory.records.RecordsPaths;
import com.cgaraydev.memory.records.RecordsService;
import com.cgaraydev.memory.records.RecordsSummary;
import com.cgaraydev.memory.records.PlayerStatistics;
import com.cgaraydev.memory.domain.Difficulty;
import com.cgaraydev.memory.domain.MemoryCard;
import com.cgaraydev.memory.domain.MemoryPhase;
import com.cgaraydev.memory.domain.MemorySession;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

public final class MemoryController {

    private static final List<String> PAIR_BANK = List.of(
            "Sol", "Luna", "Estrella", "Bosque", "Río",
            "Monte", "Fuego", "Agua", "Viento", "Tierra",
            "Nube", "Trueno", "Flor", "Hoja", "Roca",
            "Lobo", "Ciervo", "Águila", "Dragón", "Castillo"
    );


    private final MemoryGameCoordinator coordinator =
            new MemoryGameCoordinator(new SystemMemoryClock());
    private final Map<Integer, Button> cardButtons = new HashMap<>();
    private final Map<Integer, MemoryCard> visualCards = new HashMap<>();
    private final CardAnimationController cardAnimator = new CardAnimationController();
    private final RecordsService recordsService = new RecordsService(
            new JsonRecordStore(RecordsPaths.defaultFile()), Clock.systemUTC());
    private static final DateTimeFormatter RECORD_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter HISTORY_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault());
    private enum ResultsView { RANKING, HISTORY }

    private Timeline updateTimeline;
    private Animation boardEntrance;
    private FadeTransition overlayFade;
    private PauseTransition victoryDelay;
    private Animation victoryEntrance;
    private Animation recordsEntrance;
    private Animation recordsRowsEntrance;
    private Difficulty selectedRecordsDifficulty = Difficulty.EASY;
    private ResultsView selectedResultsView = ResultsView.RANKING;
    private int historyPageIndex;
    private String victoryHandledSessionId;
    private boolean overlayVisible;
    private String renderedSessionId;
    private MemorySession renderedSession;
    private boolean renderedPaused;
    private double cardWidth = BoardSizing.MIN_CARD_WIDTH;

    @FXML private VBox introView;
    @FXML private VBox gameView;
    @FXML private ScrollPane boardScroll;
    @FXML private GridPane board;
    @FXML private StackPane pauseOverlay;
    @FXML private Label timeLabel;
    @FXML private Label movesLabel;
    @FXML private Label pairsLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label statusLabel;
    @FXML private Button pauseButton;
    @FXML private Label pauseTitleLabel;
    @FXML private Label pauseHintLabel;
    @FXML private StackPane victoryOverlay;
    @FXML private VBox victoryPanel;
    @FXML private Label victoryDifficultyLabel;
    @FXML private Label victoryMessageLabel;
    @FXML private Label victoryTimeLabel;
    @FXML private Label victoryMovesLabel;
    @FXML private Label victoryFailedAttemptsLabel;
    @FXML private Button victoryAgainButton;
    @FXML private Label victoryRecordLabel;
    @FXML private StackPane recordsOverlay;
    @FXML private VBox recordsList;
    @FXML private VBox historyList;
    @FXML private VBox recordsRankingView;
    @FXML private VBox recordsHistoryView;
    @FXML private ScrollPane recordsScroll;
    @FXML private ScrollPane historyScroll;
    @FXML private Button recordsRankingButton;
    @FXML private Button recordsHistoryButton;
    @FXML private Button historyPreviousButton;
    @FXML private Button historyNextButton;
    @FXML private Label historyPageLabel;
    @FXML private Button recordsEasyButton;
    @FXML private Button recordsNormalButton;
    @FXML private Button recordsHardButton;
    @FXML private Button recordsCloseButton;
    @FXML private Label recordsDescriptionLabel;
    @FXML private Label recordsCountLabel;
    @FXML private Label recordsBestLabel;
    @FXML private Label recordsAverageMissesLabel;
    @FXML private Label recordsLatestLabel;
    @FXML private Label historyCountLabel;
    @FXML private Label historyTimeLabel;
    @FXML private Label historyMissesLabel;
    @FXML private Label historyNoticeLabel;

    @FXML
    private void initialize() {
        pauseOverlay.setOpacity(0.0);
        pauseOverlay.setMouseTransparent(true);
        victoryOverlay.setOpacity(0.0);
        victoryOverlay.setMouseTransparent(true);
        boardScroll.viewportBoundsProperty().addListener(
                (observable, previous, current) -> resizeCards(current)
        );

        updateTimeline = new Timeline(new KeyFrame(
                Duration.millis(50), event -> {
                    coordinator.update();
                    render();
                }
        ));
        updateTimeline.setCycleCount(Animation.INDEFINITE);
        updateTimeline.play();
        render();
    }

    // La ventana comunica los cambios de foco, sin depender de JavaFX en el coordinador.
    public void onWindowFocusChanged(boolean focused) {
        coordinator.onWindowFocusChanged(focused);
        render();
    }

    // Se invoca al cerrar la ventana para liberar la animación periódica.
    public void dispose() {
        if (updateTimeline != null) {
            updateTimeline.stop();
        }
        cardAnimator.cancelAll();
        if (boardEntrance != null) {
            boardEntrance.stop();
        }
        if (overlayFade != null) {
            overlayFade.stop();
        }
        hideVictory();
        hideRecords();
    }

    @FXML private void startEasy() { startGame(Difficulty.EASY); }
    @FXML private void startNormal() { startGame(Difficulty.NORMAL); }
    @FXML private void startHard() { startGame(Difficulty.HARD); }

    private void startGame(Difficulty difficulty) {
        coordinator.start(difficulty, PAIR_BANK);
        render();
    }

    @FXML
    private void togglePause() {
        if (coordinator.isManuallyPaused()) {
            coordinator.resume();
        } else if (!coordinator.isPaused()) {
            coordinator.pause();
        }
        render();
    }

    @FXML
    private void resumeFromBoard() {
        // Si solo falta el foco, recuperarlo reanuda automáticamente.
        // Un clic sobre la capa no convierte una pausa automática en manual.
        if (coordinator.isManuallyPaused()) {
            coordinator.resume();
            render();
        }
    }

    @FXML
    private void restart() {
        if (!coordinator.hasSession()) {
            return;
        }
        if (!coordinator.isFinished() && !confirm(
                "Reiniciar partida",
                "Se perderá el progreso actual. ¿Quieres empezar de nuevo?",
                "Reiniciar"
        )) {
            return;
        }
        hideVictory();
        coordinator.restart();
        render();
    }

    @FXML
    private void playAgain() {
        if (!coordinator.isFinished()) {
            return;
        }
        hideVictory();
        coordinator.restart();
        render();
    }

    @FXML
    private void returnToMenu() {
        if (coordinator.hasSession() && !coordinator.isFinished() && !confirm(
                "Volver al menú",
                "Se perderá el progreso actual. ¿Quieres volver al menú?",
                "Volver al menú"
        )) {
            return;
        }
        hideVictory();
        coordinator.returnToIntro();
        cardAnimator.cancelAll();
        cardAnimator.setPaused(false);
        if (boardEntrance != null) {
            boardEntrance.stop();
        }
        if (overlayFade != null) {
            overlayFade.stop();
        }
        overlayVisible = false;
        pauseOverlay.setOpacity(0.0);
        pauseOverlay.setVisible(false);
        pauseOverlay.setMouseTransparent(true);
        board.getChildren().clear();
        cardButtons.clear();
        visualCards.clear();
        renderedSessionId = null;
        renderedSession = null;
        render();

        introView.setOpacity(0.0);
        FadeTransition entrance = new FadeTransition(Duration.millis(250), introView);
        entrance.setToValue(1.0);
        entrance.play();
    }

    @FXML
    private void showRecords() {
        openRecords(ResultsView.RANKING);
    }

    private void openRecords(ResultsView view) {
        selectedRecordsDifficulty = coordinator.hasSession()
                ? coordinator.session().difficulty() : Difficulty.EASY;
        selectedResultsView = view;
        historyPageIndex = 0;
        if (!populateRecords()) {
            return;
        }
        recordsScroll.setVvalue(0.0);
        historyScroll.setVvalue(0.0);
        recordsOverlay.setVisible(true);
        recordsOverlay.setManaged(true);
        recordsOverlay.setMouseTransparent(false);
        recordsOverlay.setOpacity(0.0);
        animateRecordsRows();

        if (recordsEntrance != null) {
            recordsEntrance.stop();
        }
        FadeTransition fade = new FadeTransition(Duration.millis(250), recordsOverlay);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_BOTH);
        fade.setOnFinished(event -> recordsCloseButton.requestFocus());
        recordsEntrance = fade;
        fade.play();
    }

    @FXML
    private void selectRankingView() {
        changeResultsView(ResultsView.RANKING);
    }

    @FXML
    private void selectHistoryView() {
        changeResultsView(ResultsView.HISTORY);
    }

    private void changeResultsView(ResultsView view) {
        if (selectedResultsView == view) {
            return;
        }
        selectedResultsView = view;
        historyPageIndex = 0;
        if (populateRecords()) {
            recordsScroll.setVvalue(0.0);
            historyScroll.setVvalue(0.0);
        }
    }

    @FXML private void selectRecordsEasy() { changeRecordsDifficulty(Difficulty.EASY); }
    @FXML private void selectRecordsNormal() { changeRecordsDifficulty(Difficulty.NORMAL); }
    @FXML private void selectRecordsHard() { changeRecordsDifficulty(Difficulty.HARD); }

    private void changeRecordsDifficulty(Difficulty difficulty) {
        selectedRecordsDifficulty = difficulty;
        historyPageIndex = 0;
        if (populateRecords()) {
            recordsScroll.setVvalue(0.0);
            historyScroll.setVvalue(0.0);
        }
    }

    private boolean populateRecords() {
        List<GameRecord> results;
        PlayerStatistics stats;
        List<GameRecord> historyEntries = List.of();
        try {
            results = recordsService.top(selectedRecordsDifficulty);
            stats = recordsService.statistics(selectedRecordsDifficulty);
            if (selectedResultsView == ResultsView.HISTORY) {
                historyEntries = recordsService.history(selectedRecordsDifficulty);
            }
        } catch (IOException exception) {
            showRecordsError("No se pueden leer los datos de las partidas.", exception);
            return false;
        }

        recordsEasyButton.getStyleClass().setAll("record-tab-button");
        recordsNormalButton.getStyleClass().setAll("record-tab-button");
        recordsHardButton.getStyleClass().setAll("record-tab-button");
        Button selected = switch (selectedRecordsDifficulty) {
            case EASY -> recordsEasyButton;
            case NORMAL -> recordsNormalButton;
            case HARD -> recordsHardButton;
        };
        selected.getStyleClass().add("record-tab-selected");
        recordsDescriptionLabel.setText(switch (selectedRecordsDifficulty) {
            case EASY -> "FÁCIL · 6 PAREJAS";
            case NORMAL -> "NORMAL · 10 PAREJAS";
            case HARD -> "DIFÍCIL · 15 PAREJAS";
        });

        boolean showHistory = selectedResultsView == ResultsView.HISTORY;
        recordsRankingView.setVisible(!showHistory);
        recordsRankingView.setManaged(!showHistory);
        recordsHistoryView.setVisible(showHistory);
        recordsHistoryView.setManaged(showHistory);
        recordsRankingButton.getStyleClass().setAll("records-mode-button");
        recordsHistoryButton.getStyleClass().setAll("records-mode-button");
        (showHistory ? recordsHistoryButton : recordsRankingButton)
                .getStyleClass().add("records-mode-selected");

        RecordsSummary summary = RecordsSummary.from(selectedRecordsDifficulty, results);
        recordsCountLabel.setText(summary.rankedResults() + " / " + RecordsService.TOP_LIMIT);
        recordsBestLabel.setText(summary.best()
                .map(record -> record.moves() + " mov.")
                .orElse("—"));
        recordsAverageMissesLabel.setText(summary.rankedResults() == 0
                ? "—"
                : String.format(Locale.forLanguageTag("es-CL"), "%.1f",
                        summary.averageFailedAttempts()));
        recordsLatestLabel.setText(summary.latest()
                .map(record -> "Última marca del top 10: "
                        + RECORD_DATE.format(record.completedAt())
                        + " · " + record.moves() + " movimientos"
                        + " · " + formatTime(record.elapsedMillis()))
                .orElse("Todavía no hay resultados en este ranking."));

        historyCountLabel.setText(Integer.toString(stats.completedGames()));
        historyTimeLabel.setText(formatTotalTime(stats.totalElapsedMillis()));
        historyMissesLabel.setText(stats.completedGames() == 0
                ? "—"
                : String.format(Locale.forLanguageTag("es-CL"), "%.1f",
                        stats.averageFailedAttempts()));
        historyNoticeLabel.setText(stats.partialLegacyHistory()
                ? "Historial parcial: se importaron las antiguas marcas del top 10. "
                        + "Las otras partidas anteriores a v2.0 no se pueden recuperar."
                : "Partidas registradas desde v2.0, de más reciente a más antigua.");

        if (recordsRowsEntrance != null) {
            recordsRowsEntrance.stop();
            recordsRowsEntrance = null;
        }

        if (showHistory) {
            HistoryPagination.Page<GameRecord> page = HistoryPagination.slice(
                    historyEntries, historyPageIndex, HistoryPagination.DEFAULT_PAGE_SIZE);
            historyPageIndex = page.index();
            historyPreviousButton.setDisable(!page.hasPrevious());
            historyNextButton.setDisable(!page.hasNext());
            historyPageLabel.setText("Página " + page.displayPage() + " de "
                    + page.pageCount() + " · " + page.totalItems() + " partidas");
            historyList.getChildren().clear();
            if (page.entries().isEmpty()) {
                Label empty = new Label("Todavía no hay partidas registradas en esta dificultad.");
                empty.getStyleClass().add("record-empty");
                historyList.getChildren().add(empty);
            } else {
                for (int i = 0; i < page.entries().size(); i++) {
                    historyList.getChildren().add(createHistoryRow(
                            page.firstItemIndex() + i + 1, page.entries().get(i)));
                }
            }
        } else {
            recordsList.getChildren().clear();
            if (results.isEmpty()) {
                Label empty = new Label("Todavía no hay récords en esta dificultad.");
                empty.getStyleClass().add("record-empty");
                recordsList.getChildren().add(empty);
            } else {
                for (int i = 0; i < results.size(); i++) {
                    recordsList.getChildren().add(createRecordRow(i + 1, results.get(i)));
                }
            }
        }
        if (recordsOverlay.isVisible()) {
            animateRecordsRows();
        }
        return true;
    }

    @FXML
    private void previousHistoryPage() {
        if (selectedResultsView == ResultsView.HISTORY && historyPageIndex > 0) {
            historyPageIndex--;
            if (populateRecords()) {
                historyScroll.setVvalue(0.0);
            }
        }
    }

    @FXML
    private void nextHistoryPage() {
        if (selectedResultsView == ResultsView.HISTORY) {
            historyPageIndex++;
            if (populateRecords()) {
                historyScroll.setVvalue(0.0);
            }
        }
    }

    /** Entrada escalonada breve para la vista activa. */
    private void animateRecordsRows() {
        ParallelTransition staggered = new ParallelTransition();
        VBox visibleList = selectedResultsView == ResultsView.HISTORY
                ? historyList : recordsList;
        int index = 0;
        for (Node node : visibleList.getChildren()) {
            FadeTransition fade = new FadeTransition(Duration.millis(180), node);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition rise = new TranslateTransition(Duration.millis(180), node);
            rise.setFromY(5.0);
            rise.setToY(0.0);
            rise.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition rowEntrance = new ParallelTransition(fade, rise);
            rowEntrance.setDelay(Duration.millis(index++ * 18L));
            staggered.getChildren().add(rowEntrance);
        }
        recordsRowsEntrance = staggered;
        staggered.play();
    }

    private HBox createRecordRow(int position, GameRecord record) {
        HBox row = new HBox(14);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("record-row");
        if (position == 1) {
            row.getStyleClass().add("record-row-first");
        } else if (position == 2) {
            row.getStyleClass().add("record-row-second");
        } else if (position == 3) {
            row.getStyleClass().add("record-row-third");
        }

        Label rank = new Label(String.format(Locale.ROOT, "%02d", position));
        rank.getStyleClass().add("record-position");

        Label date = new Label(RECORD_DATE.format(record.completedAt()));
        date.getStyleClass().add("record-date");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label moves = new Label(record.moves() + " movimientos");
        moves.getStyleClass().add("record-stat");
        Label time = new Label(formatTime(record.elapsedMillis()));
        time.getStyleClass().add("record-time");
        row.getChildren().addAll(rank, date, spacer, moves, time);
        return row;
    }

    /** Una fila del historial es una partida, no una posición en el ranking. */
    private HBox createHistoryRow(int number, GameRecord record) {
        HBox row = new HBox(14);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("record-row");

        Label index = new Label(String.format(Locale.ROOT, "#%04d", number));
        index.getStyleClass().add("record-position");
        index.setMinWidth(64);

        Label date = new Label(HISTORY_DATE.format(record.completedAt()));
        date.getStyleClass().add("history-row-date");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label moves = new Label(record.moves() + " movimientos");
        moves.getStyleClass().add("record-stat");

        Label misses = new Label(record.failedAttempts() + " desaciertos");
        misses.getStyleClass().add("history-row-misses");

        Label time = new Label(formatTime(record.elapsedMillis()));
        time.getStyleClass().add("record-time");

        row.getChildren().addAll(index, date, spacer, moves, misses, time);
        return row;
    }

    @FXML
    private void clearSelectedRecords() {
        List<GameRecord> current;
        try {
            current = recordsService.top(selectedRecordsDifficulty);
        } catch (IOException exception) {
            showRecordsError("No se pueden consultar los récords.", exception);
            return;
        }
        if (current.isEmpty()) {
            return;
        }
        if (!confirm("Borrar récords",
                "Se borrarán las marcas del top 10 de la dificultad "
                        + switch (selectedRecordsDifficulty) {
                            case EASY -> "Fácil";
                            case NORMAL -> "Normal";
                            case HARD -> "Difícil";
                        } + ". El historial acumulado se conservará. "
                        + "Esta acción no se puede deshacer.",
                "Borrar")) {
            return;
        }
        try {
            recordsService.clear(selectedRecordsDifficulty);
            populateRecords();
        } catch (IOException exception) {
            showRecordsError("No se pudieron borrar los récords.", exception);
        }
    }

    @FXML
    private void clearSelectedHistory() {
        try {
            PlayerStatistics stats = recordsService.statistics(selectedRecordsDifficulty);
            if (stats.completedGames() == 0) {
                return;
            }
            if (!confirm("Borrar datos de partidas",
                    "Se eliminarán permanentemente todas las partidas registradas "
                            + "y las marcas del top 10 de esta dificultad. "
                            + "Esta acción no se puede deshacer.",
                    "Borrar todo")) {
                return;
            }
            recordsService.clearAll(selectedRecordsDifficulty);
            populateRecords();
        } catch (IOException exception) {
            showRecordsError("No se pudo borrar el historial.", exception);
        }
    }

    private static String formatTotalTime(long millis) {
        long minutes = millis / 60_000;
        long hours = minutes / 60;
        long remaining = minutes % 60;
        return hours > 0
                ? String.format(Locale.ROOT, "%d h %02d min", hours, remaining)
                : String.format(Locale.ROOT, "%d min", remaining);
    }

    @FXML
    private void hideRecords() {
        if (recordsRowsEntrance != null) {
            recordsRowsEntrance.stop();
            recordsRowsEntrance = null;
        }
        if (recordsEntrance != null) {
            recordsEntrance.stop();
            recordsEntrance = null;
        }
        recordsOverlay.setVisible(false);
        recordsOverlay.setManaged(false);
        recordsOverlay.setMouseTransparent(true);
        recordsOverlay.setOpacity(0.0);
    }

    private void showRecordsError(String message, IOException exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Récords");
        alert.setHeaderText(message);
        alert.setContentText("Archivo: " + RecordsPaths.defaultFile()
                + "\nDetalle: " + exception.getMessage()
                + "\nNo se sobrescribieron los datos existentes.");
        if (board.getScene() != null) {
            alert.initOwner(board.getScene().getWindow());
        }
        alert.showAndWait();
    }

    @FXML
    private void exit() {
        App.requestExit();
    }

    private void selectCard(String sessionId, int cardId) {
        coordinator.selectCard(sessionId, cardId);
        render();
    }

    private void createBoard(MemorySession session) {
        cardAnimator.cancelAll();
        if (boardEntrance != null) {
            boardEntrance.stop();
        }
        board.setOpacity(1.0);
        board.setTranslateY(0.0);
        board.getChildren().clear();
        cardButtons.clear();
        visualCards.clear();

        int columns = session.difficulty().getColumns();
        for (int index = 0; index < session.cards().size(); index++) {
            MemoryCard card = session.cards().get(index);
            Button button = new Button("✦");
            button.setWrapText(true);
            button.setRotationAxis(Rotate.Y_AXIS);
            button.getStyleClass().add("memory-card");

            String sessionId = session.id();
            int cardId = card.id();
            button.setOnAction(event -> selectCard(sessionId, cardId));

            cardButtons.put(cardId, button);
            board.add(button, index % columns, index / columns);
        }
        resizeCards(boardScroll.getViewportBounds());
        animateBoardEntrance();
    }

    private void animateBoardEntrance() {
        FadeTransition fade = new FadeTransition(Duration.millis(340), board);
        fade.setFromValue(0.35);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition slide = new TranslateTransition(Duration.millis(340), board);
        slide.setFromY(12.0);
        slide.setToY(0.0);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        boardEntrance = new ParallelTransition(fade, slide);
        boardEntrance.play();
    }

    private void resizeCards(Bounds viewport) {
        MemorySession session = coordinator.session();
        if (session == null || cardButtons.isEmpty()
                || viewport.getWidth() <= 0 || viewport.getHeight() <= 0) {
            return;
        }

        cardWidth = BoardSizing.cardWidth(
                session.difficulty(), viewport.getWidth(), viewport.getHeight()
        );
        double height = cardWidth * BoardSizing.CARD_RATIO;
        for (Button button : cardButtons.values()) {
            button.setMinSize(cardWidth, height);
            button.setPrefSize(cardWidth, height);
            button.setMaxSize(cardWidth, height);
        }
        renderCards(session);
    }

    private void renderCards(MemorySession session) {
        boolean selectable = coordinator.canSelectCard();

        for (MemoryCard card : session.cards()) {
            Button button = cardButtons.get(card.id());
            if (button == null) {
                continue;
            }

            MemoryCard previous = visualCards.put(card.id(), card);
            if (previous == null) {
                applyCardAppearance(button, card);
            } else if (previous.faceUp() != card.faceUp()) {
                cardAnimator.flip(card.id(), button,
                        () -> applyCardAppearance(button, card),
                        () -> refreshCardInteractivity(card.id(), button));
            } else if (!previous.matched() && card.matched()) {
                applyCardAppearance(button, card);
                cardAnimator.match(card.id(), button,
                        () -> refreshCardInteractivity(card.id(), button));
            } else if (!cardAnimator.isAnimating(card.id())) {
                applyCardAppearance(button, card);
            }

            button.setDisable(cardAnimator.isAnimating(card.id())
                    || !selectable || card.faceUp() || card.matched());
        }
    }

    private void applyCardAppearance(Button button, MemoryCard card) {
        button.setText(card.faceUp() ? card.pairId() : "✦");
        int fontSize = card.faceUp()
                ? (int) Math.max(11, Math.min(20, cardWidth * 0.13))
                : (int) Math.max(23, Math.min(36, cardWidth * 0.25));
        button.setStyle("-fx-font-size: " + fontSize + "px;");
        String style = card.matched()
                ? "memory-card-matched"
                : card.faceUp() ? "memory-card-revealed" : "memory-card-hidden";
        button.getStyleClass().setAll("memory-card", style);
        button.setAccessibleText(card.faceUp()
                ? card.pairId() + (card.matched() ? ", pareja encontrada" : ", visible")
                : "Carta oculta");
    }

    private void refreshCardInteractivity(int cardId, Button button) {
        if (cardButtons.get(cardId) != button) {
            return;
        }
        MemorySession session = coordinator.session();
        if (session == null) {
            return;
        }
        session.cards().stream()
                .filter(card -> card.id() == cardId)
                .findFirst()
                .ifPresent(card -> button.setDisable(
                        !coordinator.canSelectCard() || card.faceUp() || card.matched()
                ));
    }

    private void updatePauseOverlay(boolean paused) {
        if (paused == overlayVisible) {
            return;
        }
        overlayVisible = paused;
        if (overlayFade != null) {
            overlayFade.stop();
        }

        if (paused) {
            pauseOverlay.setVisible(true);
            pauseOverlay.setMouseTransparent(false);
        }

        overlayFade = new FadeTransition(Duration.millis(190), pauseOverlay);
        overlayFade.setFromValue(pauseOverlay.getOpacity());
        overlayFade.setToValue(paused ? 1.0 : 0.0);
        overlayFade.setInterpolator(Interpolator.EASE_BOTH);
        overlayFade.setOnFinished(event -> {
            if (!overlayVisible) {
                pauseOverlay.setVisible(false);
                pauseOverlay.setMouseTransparent(true);
            }
        });
        overlayFade.play();
    }

    /** Espera a que termine la última pulsación antes de mostrar el resultado. */
    private void scheduleVictory(MemorySession completed) {
        victoryHandledSessionId = completed.id();
        if (victoryDelay != null) {
            victoryDelay.stop();
        }
        victoryDelay = new PauseTransition(Duration.millis(520));
        String sessionId = completed.id();
        victoryDelay.setOnFinished(event -> {
            MemorySession current = coordinator.session();
            if (current != null && current.id().equals(sessionId)
                    && coordinator.isFinished()) {
                showVictory(GameResult.from(current, coordinator.elapsedMillis()));
            }
        });
        victoryDelay.play();
    }

    private void showVictory(GameResult result) {
        try {
            OptionalInt rank = recordsService.submit(result);
            victoryRecordLabel.setText(rank.isPresent()
                    ? "TOP 10 · PUESTO " + rank.getAsInt()
                    : "Resultado fuera del top 10");
        } catch (IOException exception) {
            victoryRecordLabel.setText("No se pudo guardar este resultado");
            showRecordsError("No se pudo guardar la partida.", exception);
        }
        victoryDifficultyLabel.setText(switch (result.difficulty()) {
            case EASY -> "FÁCIL · 6 PAREJAS";
            case NORMAL -> "NORMAL · 10 PAREJAS";
            case HARD -> "DIFÍCIL · 15 PAREJAS";
        });
        victoryTimeLabel.setText(formatTime(result.elapsedMillis()));
        victoryMovesLabel.setText(Integer.toString(result.moves()));
        victoryFailedAttemptsLabel.setText(Integer.toString(result.failedAttempts()));
        victoryMessageLabel.setText("Tablero resuelto. ¡Bien jugado!");

        victoryOverlay.setManaged(true);
        victoryOverlay.setVisible(true);
        victoryOverlay.setMouseTransparent(false);
        victoryOverlay.setOpacity(0.0);
        victoryPanel.setScaleX(0.96);
        victoryPanel.setScaleY(0.96);
        victoryPanel.setTranslateY(20.0);

        FadeTransition fade = new FadeTransition(Duration.millis(380), victoryOverlay);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition scale = new ScaleTransition(Duration.millis(380), victoryPanel);
        scale.setFromX(0.96);
        scale.setFromY(0.96);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(380), victoryPanel);
        slide.setFromY(20.0);
        slide.setToY(0.0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        victoryEntrance = new ParallelTransition(fade, scale, slide);
        victoryEntrance.setOnFinished(event -> victoryAgainButton.requestFocus());
        victoryEntrance.play();
    }

    private void hideVictory() {
        hideRecords();
        if (victoryDelay != null) {
            victoryDelay.stop();
            victoryDelay = null;
        }
        if (victoryEntrance != null) {
            victoryEntrance.stop();
            victoryEntrance = null;
        }
        victoryHandledSessionId = null;
        victoryOverlay.setVisible(false);
        victoryOverlay.setManaged(false);
        victoryOverlay.setMouseTransparent(true);
        victoryOverlay.setOpacity(0.0);
        victoryPanel.setScaleX(1.0);
        victoryPanel.setScaleY(1.0);
        victoryPanel.setTranslateY(0.0);
    }

    private void render() {
        MemorySession session = coordinator.session();
        boolean playing = session != null;

        introView.setVisible(!playing);
        introView.setManaged(!playing);
        gameView.setVisible(playing);
        gameView.setManaged(playing);
        if (!playing) {
            return;
        }

        if (!session.id().equals(renderedSessionId)) {
            renderedSessionId = session.id();
            renderedSession = null;
            createBoard(session);
        }

        boolean paused = coordinator.isPaused();
        if (session != renderedSession || paused != renderedPaused) {
            renderCards(session);
            renderedSession = session;
            renderedPaused = paused;
        }

        cardAnimator.setPaused(paused);
        if (boardEntrance != null) {
            if (paused && boardEntrance.getStatus() == Animation.Status.RUNNING) {
                boardEntrance.pause();
            } else if (!paused && boardEntrance.getStatus() == Animation.Status.PAUSED) {
                boardEntrance.play();
            }
        }
        updatePauseOverlay(paused);
        boolean manualPause = coordinator.isManuallyPaused();
        pauseButton.setText(manualPause ? "Reanudar" : "Pausar");
        pauseButton.setDisable(coordinator.isFinished() || (paused && !manualPause));
        pauseTitleLabel.setText(manualPause ? "EN PAUSA" : "VENTANA INACTIVA");
        pauseHintLabel.setText(manualPause
                ? "Haz clic en el tablero para reanudar"
                : "Regresa a la ventana para continuar");

        timeLabel.setText(formatTime(coordinator.elapsedMillis()));
        movesLabel.setText(Integer.toString(session.moves()));
        pairsLabel.setText(session.matchedPairs() + " / " + session.totalPairs());
        difficultyLabel.setText(switch (session.difficulty()) {
            case EASY -> "FÁCIL";
            case NORMAL -> "NORMAL";
            case HARD -> "DIFÍCIL";
        });

        if (coordinator.isFinished()) {
            if (!session.id().equals(victoryHandledSessionId)) {
                scheduleVictory(session);
            }
            statusLabel.setText("Partida completada. ¡Encontraste todas las parejas!");
        } else if (paused) {
            statusLabel.setText(manualPause
                    ? "Partida en pausa"
                    : "Partida detenida porque la ventana está inactiva");
        } else if (session.phase() == MemoryPhase.RESOLVING) {
            statusLabel.setText("Comprobando pareja...");
        } else if (!session.revealedCards().isEmpty()) {
            statusLabel.setText("Selecciona la segunda carta");
        } else {
            statusLabel.setText("Selecciona dos cartas para encontrar una pareja");
        }
    }

    private static String formatTime(long millis) {
        long seconds = millis / 1_000;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private boolean confirm(String title, String message, String acceptLabel) {
        ButtonType accept = new ButtonType(acceptLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getButtonTypes().setAll(accept, cancel);
        alert.initOwner(board.getScene().getWindow());
        return alert.showAndWait().orElse(cancel) == accept;
    }
}
