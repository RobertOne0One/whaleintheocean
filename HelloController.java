package com.videoformat.videoformat;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class HelloController {

    @FXML private ImageView avatarImageView;
    @FXML private StackPane avatarContainer;
    @FXML private TextField displayNameField;
    @FXML private TextField usernameTagField;
    @FXML private PasswordField passwordField;
    @FXML private Spinner<Integer> ageSpinner;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        // 1. Настраиваем выбор возраста
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(12, 100, 18);
        ageSpinner.setValueFactory(valueFactory);
        ageSpinner.setEditable(true);

        // 2. Ставим стандартную аватарку и делаем круглой
        Image defaultAvatar = new Image("https://dicebear.com", true);
        avatarImageView.setImage(defaultAvatar);
        Circle clip = new Circle(50, 50, 50);
        avatarImageView.setClip(clip);
        avatarContainer.setStyle("-fx-border-color: #cba6f7; -fx-border-width: 3; -fx-border-radius: 50; -fx-padding: 3;");

        // 3. Автоматически ставим '#' в начале тега
        usernameTagField.setText("#");

        usernameTagField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.startsWith("#")) {
                usernameTagField.setText("#" + newValue.replace("#", ""));
            }
            if (newValue.contains(" ")) {
                usernameTagField.setText(newValue.replace(" ", "_"));
            }
        });
    }

    @FXML
    private void handleChangeAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите аватарку");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) avatarImageView.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            Image customAvatar = new Image(selectedFile.toURI().toString());
            avatarImageView.setImage(customAvatar);
        }
    }

    @FXML
    private void handleLogin() {
        String displayName = displayNameField.getText().trim();
        String usernameTag = usernameTagField.getText().trim();
        String password = passwordField.getText().trim();

        // ИСПРАВЛЕНИЕ: Считываем выбранный возраст из Спиннера!
        int age = ageSpinner.getValue();

        errorLabel.setText("");

        if (displayName.isEmpty() || usernameTag.equals("#") || password.isEmpty()) {
            errorLabel.setStyle("-fx-text-fill: #f38ba8;");
            errorLabel.setText("Пожалуйста, заполните все поля и укажите тег!");
            return;
        }

        try {
            javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/videoformat/videoformat/feed-view.fxml"));
            javafx.scene.Parent root = fxmlLoader.load();

            // Передаем все данные (включая age) во второй контроллер
            FeedController feedController = fxmlLoader.getController();
            feedController.setUserData(displayName, usernameTag, age, avatarImageView.getImage());

            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 950, 650);

            currentStage.setResizable(true);
            currentStage.setMinWidth(600);
            currentStage.setMinHeight(400);

            currentStage.setTitle("VideoFormat - Главная🎬");
            currentStage.setScene(scene);
            currentStage.centerOnScreen();

        } catch (IOException e) {
            errorLabel.setStyle("-fx-text-fill: #f38ba8;");
            errorLabel.setText("Ошибка загрузки ленты видео: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
