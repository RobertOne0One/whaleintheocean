package com.videoformat.videoformat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // 1. Инициализируем SQLite
        Database.init();

        // Безопасная загрузка FXML
        java.net.URL fxmlUrl = HelloApplication.class.getResource("hello-view.fxml");
        if (fxmlUrl == null) {
            // Если папки сдвинулись, пробуем корень папки с точками
            fxmlUrl = HelloApplication.class.getResource("/com.videoformat.videoformat/hello-view.fxml");
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        // 2. Безопасная загрузка темы
        String savedTheme = Database.getSavedTheme();
        String cssPathString = "/com.videoformat.videoformat/styles/" + savedTheme;
        java.net.URL cssUrl = HelloApplication.class.getResource(cssPathString);

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("⚠️ Тема " + savedTheme + " не найдена по пути: " + cssPathString);

            // Пробуем дефолтную тему
            java.net.URL defaultUrl = HelloApplication.class.getResource("/com.videoformat.videoformat/styles/default.css");
            if (defaultUrl != null) {
                scene.getStylesheets().add(defaultUrl.toExternalForm());
            } else {
                System.err.println("❌ Критическая ошибка: Файлы стилей физически отсутствуют в ресурсах!");
                System.err.println("Применяем стандартную тему JavaFX (Modena), чтобы приложение не упало.");

                // Включаем аварийный красивый стиль интерфейса, встроенный в саму JavaFX
                scene.getStylesheets().add("data:text/css,.root{-fx-background-color:#1e1e2e;}.label{-fx-text-fill:white;}");
            }
        }

        stage.setTitle("VideoFormat");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
