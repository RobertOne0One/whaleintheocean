package com.videoformat.videoformat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class FeedController {

    // Состояние новой студии загрузки видеороликов
    private Boolean isUploadingShort = null;
    private java.io.File pendingVideoFile = null;

    @FXML private Button selectShortTypeBtn;
    @FXML private Button selectLongTypeBtn;
    @FXML private StackPane dropZoneContainer;
    @FXML private Label dropZoneStatusLabel;
    @FXML private Button publishVideoBtn;

    @FXML private HBox subscriptionsContainer;
    @FXML private VBox downloadedVideoPanel;
    @FXML private HBox downloadDragHandle;
    @FXML private Button menuHomeButton;
    @FXML private Button menuShortsButton;
    @FXML private Button menuProfileButton;

    @FXML private ScrollPane homeSection;
    @FXML private StackPane shortsSection;
    @FXML private ScrollPane profileSection;
    @FXML private VBox longVideoPlayerSection;
    @FXML private VBox rightActionPanel;

    @FXML private GridPane videoGrid;
    @FXML private GridPane myVideoGrid;
    @FXML private GridPane downloadedVideoGrid;

    // Шортсы
    @FXML private MediaView mediaView;
    @FXML private Label authorLabel;
    @FXML private Button likeButton;

    // --- БОКОВАЯ ПАНЕЛЬ КОММЕНТАРИЕМ ШОРТСОВ ---
    @FXML private VBox shortsCommentsPanel;
    @FXML private VBox shortsCommentsContainer;
    @FXML private TextField shortsCommentField;
    @FXML private Button sendShortsCommentButton;

    // --- ПАНЕЛЬ ТЕМ И ПРИВАТНОЙ ИСТОРИИ В ПРОФИЛЕ ---
    @FXML private VBox themePanel;
    @FXML private VBox historyPanel;
    @FXML private GridPane historyVideoGrid;
    // Длинный плеер
    @FXML private MediaView longMediaView;
    @FXML private Label longVideoTitleLabel;
    @FXML private Button playPauseButton;
    @FXML private Slider timeSlider;
    @FXML private Slider volumeSlider;
    @FXML private Button saveVideoButton;

    @FXML private TextField searchField;

    @FXML private ImageView profileAvatarImageView;
    @FXML private StackPane profileAvatarContainer;
    @FXML private Label profileNameLabel;
    @FXML private Label profileTagLabel;
    @FXML private Label profileAgeLabel;

    private MediaPlayer shortMediaPlayer;
    private MediaPlayer longMediaPlayer;
    private boolean isLiked = false;
    private int currentVideoIndex = 0;

    @FXML private Button longVideoLikeButton;
    @FXML private Button longVideoSubscribeButton;
    @FXML private Button shortsSubscribeButton;

    private String currentUserName = "Гость";

    // СЕТЕВЫЕ СПИСКИ И ДИНАМИЧЕСКИЕ ДАННЫЕ С СЕРВЕРА
    private final List<ServerVideo> serverLongVideos = new ArrayList<>();
    private final List<ServerVideo> serverShortsVideos = new ArrayList<>();
    private final List<List<String>> shortsCommentsData = new ArrayList<>();

    private final List<String> downloadedTitles = new ArrayList<>();
    private final List<String> downloadedUrls = new ArrayList<>();

    private String currentPlayingTitle = "";
    private String currentPlayingUrl = "";

    // Модель данных для разбора JSON-ответа бэкенда Spring Boot
    public static class ServerVideo {
        public long id;
        public String title;
        public String url;
        public String authorName;
        public String authorTag;

        public ServerVideo(long id, String title, String url, String authorName, String authorTag) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.authorName = authorName;
            this.authorTag = authorTag;
        }
    }

    @FXML
    public void initialize() {
        showHomeSection();

        // Асинхронно запрашиваем живую ленту у Spring Boot при старте
        loadLongVideosFromServer();
        loadShortsFromServer();

        generateDownloadedVideoGrid();

        // Живой сетевой поиск по ленте рекомендаций с очисткой сетки
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!homeSection.isVisible()) {
                showHomeSection();
            }
            videoGrid.getChildren().clear();
            generateLongVideoGrid(newValue.trim().toLowerCase());
        });

        // ========================================================
        // ПРОГРАММА ДЛЯ ВНЕШНЕЙ РУЧКИ
        // ========================================================
        final double[] startY = new double[1];
        final double[] startHeight = new double[1];

        downloadDragHandle.setOnMousePressed(event -> {
            startY[0] = event.getSceneY();
            startHeight[0] = downloadedVideoPanel.getHeight();
        });
        downloadDragHandle.setOnMouseDragged(event -> {
            double deltaY = event.getSceneY() - startY[0];
            double newHeight = startHeight[0] - deltaY;

            if (newHeight >= 0 && newHeight <= 350) {
                downloadedVideoPanel.setPrefHeight(newHeight);

                if (newHeight == 0) {
                    downloadedVideoPanel.setVisible(false);
                    downloadedVideoPanel.setManaged(false);
                } else {
                    downloadedVideoPanel.setVisible(true);
                    downloadedVideoPanel.setManaged(true);
                }
            }
        });
    }

    // СЕТЕВЫЕ МЕТОДЫ HTTP HttpClient
    private void loadLongVideosFromServer() {
        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/videos/long"))
                        .GET()
                        .build();

                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Platform.runLater(() -> {
                        serverLongVideos.clear();
                        String body = response.body();
                        if (body.startsWith("[") && body.endsWith("]")) {
                            body = body.substring(1, body.length() - 1);
                            if (!body.trim().isEmpty()) {
                                String[] objects = body.split("\\},\\{");
                                for (String obj : objects) {
                                    obj = obj.replace("{", "").replace("}", "").replace("\"", "");
                                    long id = 0; String title = "", url = "", aName = "", aTag = "";
                                    for (String pair : obj.split(",")) {
                                        String[] kv = pair.split(":", 2);
                                        if (kv.length == 2) {
                                            String k = kv[0].trim(), v = kv[1].trim();
                                            if (k.equals("id")) id = Long.parseLong(v);
                                            else if (k.equals("title")) title = v;
                                            else if (k.equals("url")) url = v;
                                            else if (k.equals("authorName")) aName = v;
                                            else if (k.equals("authorTag")) aTag = v;
                                        }
                                    }
                                    serverLongVideos.add(new ServerVideo(id, title, url, aName, aTag));
                                }
                            }
                        }
                        generateLongVideoGrid("");
                    });
                }
            } catch (Exception e) {
                System.err.println("Ошибка загрузки длинных видео: " + e.getMessage());
            }
        }).start();
    }

    private void loadShortsFromServer() {
        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/videos/shorts"))
                        .GET()
                        .build();

                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Platform.runLater(() -> {
                        serverShortsVideos.clear();
                        shortsCommentsData.clear();
                        String body = response.body();
                        if (body.startsWith("[") && body.endsWith("]")) {
                            body = body.substring(1, body.length() - 1);
                            if (!body.trim().isEmpty()) {
                                String[] objects = body.split("\\},\\{");
                                for (String obj : objects) {
                                    obj = obj.replace("{", "").replace("}", "").replace("\"", "");
                                    long id = 0; String title = "", url = "", aName = "", aTag = "";
                                    for (String pair : obj.split(",")) {
                                        String[] kv = pair.split(":", 2);
                                        if (kv.length == 2) {
                                            String k = kv[0].trim(), v = kv[1].trim();
                                            if (k.equals("id")) id = Long.parseLong(v);
                                            else if (k.equals("title")) title = v;
                                            else if (k.equals("url")) url = v;
                                            else if (k.equals("authorName")) aName = v;
                                            else if (k.equals("authorTag")) aTag = v;
                                        }
                                    }
                                    serverShortsVideos.add(new ServerVideo(id, title, url, aName, aTag));
                                    shortsCommentsData.add(new ArrayList<>());
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Ошибка загрузки шортсов: " + e.getMessage());
            }
        }).start();
    }

    private void playLongVideo(String title, String url) {
        stopAllPlayers();
        Database.addToHistory(title, url);

        homeSection.setVisible(false);
        shortsSection.setVisible(false);
        profileSection.setVisible(false);
        longVideoPlayerSection.setVisible(true);
        rightActionPanel.setVisible(false);
        longVideoPlayerSection.requestFocus();

        currentPlayingTitle = title;
        currentPlayingUrl = url;
        longVideoTitleLabel.setText(title);
        saveVideoButton.setText("📥 Сохранить");

        try {
            Media media = new Media(url);
            longMediaPlayer = new MediaPlayer(media);
            longMediaView.setMediaPlayer(longMediaPlayer);
            longMediaPlayer.setVolume(volumeSlider.getValue());

            volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (longMediaPlayer != null) {
                    longMediaPlayer.setVolume(newValue.doubleValue());
                }
            });

            setupTimeSliderLogic();
            longMediaPlayer.play();
        } catch (Exception e) {
            System.out.println("Ошибка длинного видео: " + e.getMessage());
        }

        String currentAuthor = getAuthorForCurrentLongVideo();
        updateUiStatusForCurrentVideo(currentAuthor, url);
    }
    @FXML
    private void handleSaveCurrentVideo() {
        if (currentPlayingUrl.isEmpty()) return;

        downloadedTitles.add("Скачано: " + currentPlayingTitle + ".mp4");
        downloadedUrls.add(currentPlayingUrl);
        saveVideoButton.setText("✅ Добавлено!");
        generateDownloadedVideoGrid();
    }

    private void setupTimeSliderLogic() {
        longMediaPlayer.currentTimeProperty().addListener((observable, oldTime, newTime) -> {
            if (!timeSlider.isValueChanging()) {
                double totalDuration = longMediaPlayer.getTotalDuration().toSeconds();
                if (totalDuration > 0) {
                    timeSlider.setValue((newTime.toSeconds() / totalDuration) * 100.0);
                }
            }
        });

        timeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (timeSlider.isValueChanging() && longMediaPlayer != null) {
                double totalDuration = longMediaPlayer.getTotalDuration().toSeconds();
                double seekToSeconds = (newValue.doubleValue() / 100.0) * totalDuration;
                longMediaPlayer.seek(Duration.seconds(seekToSeconds));
            }
        });

        timeSlider.setOnMousePressed(event -> {
            if (longMediaPlayer != null) {
                double totalDuration = longMediaPlayer.getTotalDuration().toSeconds();
                double mouseX = event.getX();
                double width = timeSlider.getWidth();
                double newValue = (mouseX / width) * 100.0;
                double seekToSeconds = (newValue / 100.0) * totalDuration;
                longMediaPlayer.seek(Duration.seconds(seekToSeconds));
            }
        });
    }

    @FXML
    private void handlePlayPause() {
        if (longMediaPlayer != null) {
            if (longMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                longMediaPlayer.pause();
                playPauseButton.setText("▶ Старт");
            } else {
                longMediaPlayer.play();
                playPauseButton.setText("⏸ Пауза");
            }
        }
    }

    public void setUserData(String name, String tag, int age, Image avatarImage) {
        this.currentUserName = name;
        profileNameLabel.setText(name);
        profileTagLabel.setText(tag);
        profileAgeLabel.setText("Возраст: " + age + " лет");
        profileAvatarImageView.setImage(avatarImage);

        Circle clip = new Circle(50, 50, 50);
        profileAvatarImageView.setClip(clip);
        profileAvatarContainer.setStyle("-fx-border-color: #cba6f7; -fx-border-width: 3; -fx-border-radius: 50; -fx-padding: 2;");

        generateMyVideoGrid();

        // ========================================================
        // ИНИЦИАЛИЗАЦИЯ УЛЬТРА-ПЛАВНЫХ ГОРЯЧИХ КЛАВИШ (БЕЗ ЛАГОВ)
        // ========================================================
        javafx.application.Platform.runLater(() -> {
            Scene scene = profileAvatarContainer.getScene();
            if (scene == null) return;

            scene.setOnKeyPressed(event -> {
                MediaPlayer activePlayer = (longVideoPlayerSection.isVisible()) ? longMediaPlayer : shortMediaPlayer;

                switch (event.getCode()) {
                    case SPACE:
                        if (activePlayer != null) {
                            if (activePlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                                activePlayer.pause();
                                if (longVideoPlayerSection.isVisible()) playPauseButton.setText("▶ Старт");
                            } else {
                                activePlayer.play();
                                if (longVideoPlayerSection.isVisible()) playPauseButton.setText("⏸ Пауза");
                            }
                        } else {
                            if (longVideoPlayerSection.isVisible()) {
                                if (playPauseButton.getText().equals("⏸ Пауза")) playPauseButton.setText("▶ Старт");
                                else playPauseButton.setText("⏸ Пауза");
                            }
                        }
                        event.consume();
                        break;

                    case UP:
                        double newVolUp = Math.min(1.0, volumeSlider.getValue() + 0.05);
                        volumeSlider.setValue(newVolUp);
                        if (activePlayer != null) activePlayer.setVolume(newVolUp);
                        break;

                    case DOWN:
                        double newVolDown = Math.max(0.0, volumeSlider.getValue() - 0.05);
                        volumeSlider.setValue(newVolDown);
                        if (activePlayer != null) activePlayer.setVolume(newVolDown);
                        break;

                    case LEFT:
                        double newTimeLeft = Math.max(0.0, timeSlider.getValue() - 5.0);
                        timeSlider.setValue(newTimeLeft);
                        if (activePlayer != null) activePlayer.seek(activePlayer.getCurrentTime().subtract(Duration.seconds(5)));
                        break;

                    case RIGHT:
                        double newTimeRight = Math.min(100.0, timeSlider.getValue() + 5.0);
                        timeSlider.setValue(newTimeRight);
                        if (activePlayer != null) activePlayer.seek(activePlayer.getCurrentTime().add(Duration.seconds(5)));
                        break;

                    case M:
                        if (activePlayer != null) activePlayer.setMute(!activePlayer.isMute());
                        break;

                    default:
                        break;
                }
            });
        });
    }
    @FXML
    private void showHomeSection() {
        homeSection.setVisible(true);
        shortsSection.setVisible(false);
        profileSection.setVisible(false);
        longVideoPlayerSection.setVisible(false);
        rightActionPanel.setVisible(false);

        setMenuButtonActive(menuHomeButton);
        stopAllPlayers();
    }

    @FXML
    private void showShortsSection() {
        homeSection.setVisible(false);
        shortsSection.setVisible(true);
        profileSection.setVisible(false);
        longVideoPlayerSection.setVisible(false);
        rightActionPanel.setVisible(true);

        setMenuButtonActive(menuShortsButton);
        stopAllPlayers();
        playShortVideo(currentVideoIndex);

        shortsSection.requestFocus();
    }

    @FXML
    private void showProfileSection() {
        homeSection.setVisible(false);
        shortsSection.setVisible(false);
        profileSection.setVisible(true);
        longVideoPlayerSection.setVisible(false);
        rightActionPanel.setVisible(false);

        setMenuButtonActive(menuProfileButton);
        stopAllPlayers();

        generateMyVideoGrid();
        profileSection.requestFocus();
        generateSubscriptionsList();
    }

    @FXML
    private void closeLongVideoPlayer() {
        showHomeSection();
    }

    private void stopAllPlayers() {
        if (shortMediaPlayer != null) {
            shortMediaPlayer.stop();
            shortMediaPlayer.dispose();
            shortMediaPlayer = null;
        }
        if (longMediaPlayer != null) {
            longMediaPlayer.stop();
            longMediaPlayer.dispose();
            longMediaPlayer = null;
        }
    }

    private void setMenuButtonActive(Button activeButton) {
        menuHomeButton.getStyleClass().remove("menu-btn-active");
        menuShortsButton.getStyleClass().remove("menu-btn-active");
        menuProfileButton.getStyleClass().remove("menu-btn-active");

        activeButton.getStyleClass().add("menu-btn-active");
    }

    private void playShortVideo(int index) {
        if (serverShortsVideos.isEmpty()) return;

        if (index >= serverShortsVideos.size()) {
            currentVideoIndex = 0;
            index = 0;
        }

        if (shortMediaPlayer != null) {
            shortMediaPlayer.stop();
            shortMediaPlayer.dispose();
        }

        ServerVideo video = serverShortsVideos.get(index);
        try {
            Media media = new Media(video.url);
            shortMediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(shortMediaPlayer);
            shortMediaPlayer.play();
            authorLabel.setText(video.authorTag);

            loadCommentsForCurrentShort();
        } catch (Exception e) {
            System.out.println("Ошибка шортс: " + e.getMessage());
        }

        updateUiStatusForCurrentVideo(video.authorTag, null);
    }

    @FXML
    private void handleLike() {
        isLiked = !isLiked;
        likeButton.getStyleClass().removeAll("like-btn-active", "like-btn-inactive");

        if (isLiked) {
            likeButton.setText("❤️ Лайкнуто!");
            likeButton.getStyleClass().add("like-btn-active");
        } else {
            likeButton.setText("❤️ Лайк");
            likeButton.getStyleClass().add("like-btn-inactive");
        }
    }

    @FXML
    private void handleNextVideo() {
        if (serverShortsVideos.isEmpty()) return;
        isLiked = false;
        likeButton.setText("❤️ Лайк");
        likeButton.getStyleClass().removeAll("like-btn-active");
        likeButton.getStyleClass().add("like-btn-inactive");

        currentVideoIndex = (currentVideoIndex + 1) % serverShortsVideos.size();
        playShortVideo(currentVideoIndex);
    }
    @FXML
    private void showCommentsPanel() {
        shortsCommentsPanel.setVisible(true);
        shortsCommentsPanel.setManaged(true);
        shortsCommentsPanel.setTranslateX(shortsCommentsPanel.getPrefWidth());

        javafx.animation.TranslateTransition openAnim = new javafx.animation.TranslateTransition(
                Duration.millis(250), shortsCommentsPanel
        );
        openAnim.setToX(0);
        openAnim.play();
    }

    @FXML
    private void hideCommentsPanel() {
        javafx.animation.TranslateTransition closeAnim = new javafx.animation.TranslateTransition(
                Duration.millis(250), shortsCommentsPanel
        );
        closeAnim.setToX(shortsCommentsPanel.getPrefWidth());
        closeAnim.setOnFinished(event -> {
            shortsCommentsPanel.setVisible(false);
            shortsCommentsPanel.setManaged(false);
            shortsCommentField.clear();
        });
        closeAnim.play();
    }

    @FXML
    private void onSendShortsCommentClick() {
        if (serverShortsVideos.isEmpty()) return;
        String text = shortsCommentField.getText().trim();
        if (text.isEmpty()) return;

        shortsCommentsData.get(currentVideoIndex).add(text);
        loadCommentsForCurrentShort();
        shortsCommentField.clear();
    }

    private void loadCommentsForCurrentShort() {
        shortsCommentsContainer.getChildren().clear();
        if (serverShortsVideos.isEmpty()) return;

        List<String> comments = shortsCommentsData.get(currentVideoIndex);
        for (String commentText : comments) {
            Label label = new Label(commentText);
            label.getStyleClass().add("comment-label");
            label.setPrefWidth(210);
            label.setWrapText(true);
            shortsCommentsContainer.getChildren().add(label);
        }
    }

    private void generateLongVideoGrid(String searchText) {
        videoGrid.getChildren().clear();
        if (serverLongVideos.isEmpty()) return;

        int columns = 3;
        int visibleCardCount = 0;

        for (int i = 0; i < serverLongVideos.size(); i++) {
            ServerVideo video = serverLongVideos.get(i);

            if (!searchText.isEmpty() && !video.title.toLowerCase().contains(searchText)) {
                continue;
            }

            VBox videoCard = createVideoCard(video.title, "👤 " + video.authorName + " • 👀 " + (10 + (i * 25)) + " тыс. просмотров");
            videoCard.setOnMouseClicked(event -> playLongVideo(video.title, video.url));

            int row = visibleCardCount / columns;
            int col = visibleCardCount % columns;
            videoGrid.add(videoCard, col, row);
            visibleCardCount++;
        }
    }

    private void generateMyVideoGrid() {
        myVideoGrid.getChildren().clear();
        int columns = 3;
        int cardCount = 0;

        for (ServerVideo video : serverLongVideos) {
            if (video.authorTag.equalsIgnoreCase(profileTagLabel.getText())) {
                VBox videoCard = createVideoCard(video.title, "👀 " + (cardCount * 12 + 5) + " просмотров");
                videoCard.setOnMouseClicked(event -> playLongVideo(video.title, video.url));

                int row = cardCount / columns;
                int col = cardCount % columns;
                myVideoGrid.add(videoCard, col, row);
                cardCount++;
            }
        }

        if (cardCount == 0) {
            Label noVideosLabel = new Label("У вас пока нет опубликованных влогов");
            noVideosLabel.setStyle("-fx-text-fill: #a6adc8; -fx-font-style: italic;");
            myVideoGrid.add(noVideosLabel, 0, 0);
        }
    }
    private void generateDownloadedVideoGrid() {
        downloadedVideoGrid.getChildren().clear();

        for (int i = 0; i < downloadedTitles.size(); i++) {
            final String title = downloadedTitles.get(i);
            final String url = downloadedUrls.get(i);

            VBox videoCard = new VBox();
            videoCard.setSpacing(4);
            videoCard.setPrefWidth(160);
            videoCard.setPrefHeight(135);
            videoCard.getStyleClass().add("download-card");
            videoCard.setPadding(new Insets(6));

            StackPane previewStack = new StackPane();
            previewStack.setPrefSize(148, 65);
            previewStack.getStyleClass().add("download-preview-stack");

            Label playIcon = new Label("💾 Скачано");
            playIcon.getStyleClass().add("download-icon-label");
            playIcon.setFont(Font.font("System", 10));
            previewStack.getChildren().add(playIcon);

            Button deleteButton = new Button("❌");
            deleteButton.getStyleClass().add("download-delete-btn");
            StackPane.setAlignment(deleteButton, javafx.geometry.Pos.TOP_RIGHT);
            StackPane.setMargin(deleteButton, new Insets(4));

            deleteButton.setOnAction(event -> {
                event.consume();
                downloadedTitles.remove(title);
                downloadedUrls.remove(url);
                generateDownloadedVideoGrid();
            });
            previewStack.getChildren().add(deleteButton);

            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("download-title-label");
            titleLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
            titleLabel.setWrapText(true);
            titleLabel.setPrefHeight(25);

            videoCard.getChildren().addAll(previewStack, titleLabel);

            boolean isRealDuplicate = false;
            for (int j = 0; j < i; j++) {
                if (downloadedTitles.get(j).equals(title) && downloadedUrls.get(j).equals(url)) {
                    isRealDuplicate = true;
                    break;
                }
            }

            if (isRealDuplicate) {
                Label duplicateLabel = new Label("⚠️ Дубликат");
                duplicateLabel.getStyleClass().add("duplicate-badge");
                duplicateLabel.setFont(Font.font("System", 9));
                VBox.setMargin(duplicateLabel, new Insets(2, 0, 0, 5));
                videoCard.getChildren().add(duplicateLabel);
            }

            downloadedVideoGrid.add(videoCard, i, 0);
        }
    }

    private VBox createVideoCard(String title, String subtitle) {
        VBox videoCard = new VBox();
        videoCard.setSpacing(8);
        videoCard.setPrefWidth(240);
        videoCard.getStyleClass().add("video-card");
        videoCard.setPadding(new Insets(10));

        StackPane previewBlock = new StackPane();
        previewBlock.setPrefSize(220, 125);
        previewBlock.getStyleClass().add("video-preview-block");

        Label playIcon = new Label("▶ Воспроизвести");
        playIcon.getStyleClass().add("video-play-icon-text");
        previewBlock.getChildren().add(playIcon);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("video-title-label");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        titleLabel.setWrapText(true);
        titleLabel.setPrefHeight(40);

        Label subLabel = new Label(subtitle);
        subLabel.getStyleClass().add("video-subtitle-label");
        subLabel.setFont(Font.font("System", 10));

        videoCard.getChildren().addAll(previewBlock, titleLabel, subLabel);
        return videoCard;
    }

    @FXML
    private void toggleHistoryPanel() {
        if (historyPanel != null) {
            historyPanel.setVisible(!historyPanel.isVisible());
            historyPanel.setManaged(historyPanel.isVisible());
            if (historyPanel.isVisible()) {
                generateHistoryVideoGrid();
            }
        }
    }

    private void generateHistoryVideoGrid() {
        historyVideoGrid.getChildren().clear();
        int columns = 3;

        List<String[]> historyData = Database.getHistory();
        if (historyData == null) return;

        for (int i = 0; i < historyData.size(); i++) {
            String[] videoInfo = historyData.get(i);
            final String title = videoInfo[0];
            final String url = videoInfo[1];

            VBox videoCard = createVideoCard(title, "🕒 Посмотрено ранее");
            videoCard.setOnMouseClicked(event -> playLongVideo(title, url));

            int row = i / columns;
            int col = i % columns;
            historyVideoGrid.add(videoCard, col, row);
        }
    }

    @FXML
    private void toggleThemePanel() {
        if (themePanel != null) {
            themePanel.setVisible(!themePanel.isVisible());
            themePanel.setManaged(themePanel.isVisible());
        }
    }

    private void applyAndSaveTheme(String themeFileName) {
        Database.saveTheme(themeFileName);

        if (themePanel != null) {
            Scene scene = themePanel.getScene();
            if (scene != null) {
                scene.getStylesheets().clear();

                // ИСПРАВЛЕНИЕ: Прямой путь к папке с точками в названии
                String path = "/com.videoformat.videoformat/styles/" + themeFileName;
                var themeUrl = getClass().getResource(path);

                if (themeUrl != null) {
                    scene.getStylesheets().add(themeUrl.toExternalForm());

                    if (menuHomeButton.getStyleClass().contains("menu-btn-active")) setMenuButtonActive(menuHomeButton);
                    else if (menuShortsButton.getStyleClass().contains("menu-btn-active")) setMenuButtonActive(menuShortsButton);
                    else if (menuProfileButton.getStyleClass().contains("menu-btn-active")) setMenuButtonActive(menuProfileButton);

                    if (homeSection.isVisible()) {
                        generateLongVideoGrid(searchField.getText().trim().toLowerCase());
                    } else if (profileSection.isVisible()) {
                        generateMyVideoGrid();
                        if (historyPanel.isVisible()) {
                            generateHistoryVideoGrid();
                        }
                    }
                    generateDownloadedVideoGrid();
                } else {
                    System.err.println("❌ Ошибка: Файл стиля не найден по пути: " + path);
                }
            }
        }
    }

    @FXML
    private void handleClearAllDuplicates() {
        for (int i = downloadedUrls.size() - 1; i >= 0; i--) {
            String currentUrl = downloadedUrls.get(i);
            String currentTitle = downloadedTitles.get(i);
            boolean hasOriginalBefore = false;

            for (int j = 0; j < i; j++) {
                if (downloadedUrls.get(j).equals(currentUrl) && downloadedTitles.get(j).equals(currentTitle)) {
                    hasOriginalBefore = true;
                    break;
                }
            }

            if (hasOriginalBefore) {
                downloadedTitles.remove(i);
                downloadedUrls.remove(i);
            }
        }
        generateDownloadedVideoGrid();
    }

    private void updateUiStatusForCurrentVideo(String authorTag, String videoUrl) {
        boolean subscribed = Database.isSubscribed(currentUserName, authorTag);
        String subText = subscribed ? "✅ Вы подписаны" : "🔔 Подписаться";

        shortsSubscribeButton.setText(subText);
        longVideoSubscribeButton.setText(subText);

        if (subscribed) {
            shortsSubscribeButton.getStyleClass().add("like-btn-active");
            longVideoSubscribeButton.getStyleClass().add("like-btn-active");
        } else {
            shortsSubscribeButton.getStyleClass().removeAll("like-btn-active");
            longVideoSubscribeButton.getStyleClass().removeAll("like-btn-active");
        }

        if (videoUrl != null) {
            boolean liked = Database.isLongVideoLiked(videoUrl, currentUserName);
            longVideoLikeButton.setText(liked ? "❤️ Лайкнуто!" : "❤️ Лайк");
            if (liked) {
                longVideoLikeButton.getStyleClass().add("like-btn-active");
            } else {
                longVideoLikeButton.getStyleClass().removeAll("like-btn-active");
            }
        }
    }
    @FXML
    private void handleLongVideoLike() {
        if (currentPlayingUrl.isEmpty()) return;
        boolean currentlyLiked = longVideoLikeButton.getText().equals("❤️ Лайкнуто!");
        Database.toggleLongVideoLike(currentPlayingUrl, currentUserName, !currentlyLiked);
        updateUiStatusForCurrentVideo(getAuthorForCurrentLongVideo(), currentPlayingUrl);
    }

    @FXML
    private void handleShortsSubscribe() {
        if (serverShortsVideos.isEmpty()) return;
        String currentAuthor = serverShortsVideos.get(currentVideoIndex).authorTag;
        boolean currentlySubscribed = shortsSubscribeButton.getText().equals("✅ Вы подписаны");
        Database.toggleSubscription(currentUserName, currentAuthor, !currentlySubscribed);
        updateUiStatusForCurrentVideo(currentAuthor, null);
    }

    @FXML
    private void handleLongVideoSubscribe() {
        String currentAuthor = getAuthorForCurrentLongVideo();
        boolean currentlySubscribed = longVideoSubscribeButton.getText().equals("✅ Вы подписаны");
        Database.toggleSubscription(currentUserName, currentAuthor, !currentlySubscribed);
        updateUiStatusForCurrentVideo(currentAuthor, currentPlayingUrl);
    }

    private String getAuthorForCurrentLongVideo() {
        for (ServerVideo video : serverLongVideos) {
            if (video.url.equals(currentPlayingUrl)) return video.authorTag;
        }
        return "@unknown";
    }

    private void generateSubscriptionsList() {
        subscriptionsContainer.getChildren().clear();
        List<String> subs = Database.getSubscriptions(currentUserName);

        if (subs.isEmpty()) {
            Label emptyLabel = new Label("Вы пока ни на кого не подписаны");
            emptyLabel.setStyle("-fx-text-fill: #a6adc8; -fx-font-style: italic;");
            subscriptionsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (String authorTag : subs) {
            VBox authorCard = new VBox();
            authorCard.setSpacing(6);
            authorCard.setAlignment(javafx.geometry.Pos.CENTER);
            authorCard.getStyleClass().add("video-card");
            authorCard.setPadding(new Insets(10));
            authorCard.setPrefWidth(110);

            StackPane avatarCircle = new StackPane();
            avatarCircle.setPrefSize(45, 45);
            avatarCircle.setMaxSize(45, 45);
            avatarCircle.getStyleClass().add("video-preview-block");
            avatarCircle.setStyle("-fx-background-radius: 50;");

            String displayLetter = authorTag.length() > 1 ? authorTag.substring(1, 2).toUpperCase() : "A";
            Label letterLabel = new Label(displayLetter);
            letterLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            avatarCircle.getChildren().add(letterLabel);

            Label nameLabel = new Label(authorTag);
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 11));

            Button unsubscribeBtn = new Button("Удалить");
            unsubscribeBtn.getStyleClass().add("download-delete-btn");
            unsubscribeBtn.setOnAction(event -> {
                Database.toggleSubscription(currentUserName, authorTag, false);
                generateSubscriptionsList();
            });

            authorCard.getChildren().addAll(avatarCircle, nameLabel, unsubscribeBtn);
            subscriptionsContainer.getChildren().add(authorCard);
        }
    }

    @FXML
    private void handleSelectShortType() {
        isUploadingShort = true;
        selectShortTypeBtn.setStyle("-fx-background-color: #32CD32; -fx-text-fill: white;");
        selectLongTypeBtn.setStyle("");
        resetDropZone("Перетащите сюда ШОРТС (.mp4)\nЛимит длительности: 4 минуты");
    }

    @FXML
    private void handleSelectLongType() {
        isUploadingShort = false;
        selectLongTypeBtn.setStyle("-fx-background-color: #32CD32; -fx-text-fill: white;");
        selectShortTypeBtn.setStyle("");
        resetDropZone("Перетащите сюда ВЛОГ (.mp4)\nЛимит длительности: 5 часов");
    }

    private void resetDropZone(String message) {
        pendingVideoFile = null;
        publishVideoBtn.setDisable(true);
        dropZoneStatusLabel.setText(message);
        dropZoneStatusLabel.setStyle("-fx-text-fill: #a6adc8;");

        dropZoneContainer.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles() && isUploadingShort != null) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });

        dropZoneContainer.setOnDragDropped(event -> {
            var db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && isUploadingShort != null) {
                java.io.File file = db.getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".mp4")) {
                    success = true;
                    checkVideoDurationAndPrepare(file);
                } else {
                    showValidationAlert("Ошибка формата", "Наша соцсеть поддерживает только файлы формата .MP4!");
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
    private void checkVideoDurationAndPrepare(java.io.File file) {
        dropZoneStatusLabel.setText("⏳ Проверка длительности файла...");
        dropZoneStatusLabel.setStyle("-fx-text-fill: #fff066;");

        try {
            Media media = new Media(file.toURI().toString());
            MediaPlayer tempPlayer = new MediaPlayer(media);

            tempPlayer.setOnReady(() -> {
                double seconds = media.getDuration().toSeconds();
                tempPlayer.dispose();

                boolean isValid = false;
                if (isUploadingShort) {
                    isValid = seconds <= 240.0;
                } else {
                    isValid = seconds <= 18000.0;
                }

                if (isValid) {
                    Platform.runLater(() -> {
                        pendingVideoFile = file;
                        publishVideoBtn.setDisable(false);
                        dropZoneStatusLabel.setText("✅ Файл готов: " + file.getName() + "\nДлительность: " + String.format("%.1f", seconds) + " сек.");
                        dropZoneStatusLabel.setStyle("-fx-text-fill: #32CD32;");
                    });
                } else {
                    Platform.runLater(() -> {
                        publishVideoBtn.setDisable(true);
                        dropZoneStatusLabel.setText("❌ Превышен лимит времени!\nМаксимум: " + (isUploadingShort ? "4 мин" : "5 часов"));
                        dropZoneStatusLabel.setStyle("-fx-text-fill: #f38ba8;");
                    });
                }
            });

            tempPlayer.setOnError(() -> Platform.runLater(() -> {
                tempPlayer.dispose();
                dropZoneStatusLabel.setText("❌ Ошибка чтения медиа-данных файла");
                dropZoneStatusLabel.setStyle("-fx-text-fill: #f38ba8;");
            }));

        } catch (Exception e) {
            dropZoneStatusLabel.setText("❌ Не удалось проанализировать файл");
            dropZoneStatusLabel.setStyle("-fx-text-fill: #f38ba8;");
        }
    }

    @FXML
    private void handlePublishVideo() {
        if (pendingVideoFile == null || isUploadingShort == null) return;

        publishVideoBtn.setDisable(true);
        dropZoneStatusLabel.setText("🚀 Отправка видео на сервер бэкенда...");
        dropZoneStatusLabel.setStyle("-fx-text-fill: #fff066;");

        new Thread(() -> {
            try {
                String boundary = "---JavaFXMultipartBoundary---";
                byte[] multipartBody = createMultipartBody(boundary, pendingVideoFile, isUploadingShort, currentUserName);

                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://localhost:8080/api/videos/upload"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                        .build();

                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        showValidationAlert("Успех 🎬", "Видео успешно опубликовано и добавлено в ленту соцсети!");
                        isUploadingShort = null;
                        pendingVideoFile = null;
                        selectShortTypeBtn.setStyle("");
                        selectLongTypeBtn.setStyle("");
                        dropZoneStatusLabel.setText("Сначала выберите тип видео выше");
                        dropZoneStatusLabel.setStyle("-fx-text-fill: #a6adc8;");

                        loadLongVideosFromServer();
                        loadShortsFromServer();
                    } else {
                        dropZoneStatusLabel.setText("❌ Ошибка сервера: код " + response.statusCode());
                        dropZoneStatusLabel.setStyle("-fx-text-fill: #f38ba8;");
                        publishVideoBtn.setDisable(false);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    dropZoneStatusLabel.setText("❌ Ошибка сети при отправке файла");
                    dropZoneStatusLabel.setStyle("-fx-text-fill: #f38ba8;");
                    publishVideoBtn.setDisable(false);
                });
            }
        }).start();
    }

    private byte[] createMultipartBody(String boundary, java.io.File file, boolean isShort, String authorName) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        String lineSeparator = "\r\n";

        baos.write(("--" + boundary + lineSeparator).getBytes());
        baos.write(("Content-Disposition: form-data; name=\"isShort\"" + lineSeparator + lineSeparator).getBytes());
        baos.write((String.valueOf(isShort) + lineSeparator).getBytes());

        baos.write(("--" + boundary + lineSeparator).getBytes());
        baos.write(("Content-Disposition: form-data; name=\"authorTag\"" + lineSeparator + lineSeparator).getBytes());
        baos.write((profileTagLabel.getText() + lineSeparator).getBytes());

        baos.write(("--" + boundary + lineSeparator).getBytes());
        baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + lineSeparator).getBytes());
        baos.write(("Content-Type: video/mp4" + lineSeparator + lineSeparator).getBytes());
        baos.write(java.nio.file.Files.readAllBytes(file.toPath()));
        baos.write(lineSeparator.getBytes());

        // ИСПРАВЛЕНО: Заменили приватный метод writeInternal на стандартный публичный write
        baos.write(("--" + boundary + "--" + lineSeparator).getBytes());
        return baos.toByteArray();
    }

    private void showValidationAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("VideoFormat Studio");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML private void selectDefaultTheme() { applyAndSaveTheme("default.css"); }
    @FXML private void selectBlueberryTheme() { applyAndSaveTheme("blueberry.css"); }
    @FXML private void selectLemonTheme() { applyAndSaveTheme("lemon.css"); }
    @FXML private void selectLingonberryTheme() { applyAndSaveTheme("lingonberry.css"); }
}
