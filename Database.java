package com.videoformat.videoformat;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String URL = "jdbc:sqlite:settings.db";

    // Инициализация базы данных
    // Инициализация базы данных
    public static void init() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {

            // 1. Таблица конфигурации (темы)
            stmt.execute("CREATE TABLE IF NOT EXISTS config (key TEXT PRIMARY KEY, value TEXT)");
            stmt.execute("INSERT OR IGNORE INTO config (key, value) VALUES ('theme', 'default.css')");

            // 2. Таблица для истории просмотров
            stmt.execute("CREATE TABLE IF NOT EXISTS history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT, " +
                    "url TEXT, " +
                    "view_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // ИСПРАВЛЕНО: Перенесли внутрь блока try, где переменная stmt существует и доступна
            // 3. Таблица лайков для длинных видео (один пользователь - один лайк на видео)
            stmt.execute("CREATE TABLE IF NOT EXISTS long_video_likes (" +
                    "video_url TEXT, " +
                    "user_name TEXT, " +
                    "PRIMARY KEY (video_url, user_name))");

            // 4. Таблица подписок (составной ключ гарантирует уникальность подписки на автора)
            stmt.execute("CREATE TABLE IF NOT EXISTS subscriptions (" +
                    "user_name TEXT, " +
                    "author_tag TEXT, " +
                    "PRIMARY KEY (user_name, author_tag))");

        } catch (SQLException e) {
            e.printStackTrace();
        } // Здесь переменная stmt безопасно закрывается и уничтожается
    }

    // Сохраняем выбранную тему в базу
    public static void saveTheme(String themeFileName) {
        String sql = "INSERT OR REPLACE INTO config (key, value) VALUES ('theme', ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, themeFileName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Читаем сохраненную тему из базы
    public static String getSavedTheme() {
        String sql = "SELECT value FROM config WHERE key = 'theme'";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "default.css";
    }

    // МЕТОД 1: Добавление видео в историю просмотров
    public static void addToHistory(String title, String url) {
        // Удаляем старую запись об этом же видео, чтобы не было дубликатов в истории
        String deleteSql = "DELETE FROM history WHERE title = ?";
        String insertSql = "INSERT INTO history (title, url) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(URL)) {
            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                delStmt.setString(1, title);
                delStmt.executeUpdate();
            }
            try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                insStmt.setString(1, title);
                insStmt.setString(2, url);
                insStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // МЕТОД 2: Получение списка просмотренных видео (последние 10 штук)
    public static List<String[]> getHistory() {
        List<String[]> historyList = new ArrayList<>();
        String sql = "SELECT title, url FROM history ORDER BY id DESC LIMIT 10";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                historyList.add(new String[]{rs.getString("title"), rs.getString("url")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return historyList;
    }

    // Проверить, стоит ли лайк на длинном видео
    public static boolean isLongVideoLiked(String videoUrl, String userName) {
        String sql = "SELECT 1 FROM long_video_likes WHERE video_url = ? AND user_name = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, videoUrl);
            pstmt.setString(2, userName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Переключить лайк (поставить/убрать)
    public static void toggleLongVideoLike(String videoUrl, String userName, boolean shouldLike) {
        new Thread(() -> {
            String sql = shouldLike ?
                    "INSERT OR IGNORE INTO long_video_likes (video_url, user_name) VALUES (?, ?)" :
                    "DELETE FROM long_video_likes WHERE video_url = ? AND user_name = ?";
            try (Connection conn = DriverManager.getConnection(URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, videoUrl);
                pstmt.setString(2, userName);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Проверить подписку на автора
    public static boolean isSubscribed(String userName, String authorTag) {
        String sql = "SELECT 1 FROM subscriptions WHERE user_name = ? AND author_tag = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            pstmt.setString(2, authorTag);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Переключить подписку (подписаться/отписаться)
    public static void toggleSubscription(String userName, String authorTag, boolean shouldSubscribe) {
        new Thread(() -> {
            String sql = shouldSubscribe ?
                    "INSERT OR IGNORE INTO subscriptions (user_name, author_tag) VALUES (?, ?)" :
                    "DELETE FROM subscriptions WHERE user_name = ? AND author_tag = ?";
            try (Connection conn = DriverManager.getConnection(URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userName);
                pstmt.setString(2, authorTag);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }
    // Получить список всех подписок пользователя
    public static List<String> getSubscriptions(String userName) {
        List<String> subs = new ArrayList<>();
        String sql = "SELECT author_tag FROM subscriptions WHERE user_name = ? ORDER BY author_tag ASC";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    subs.add(rs.getString("author_tag"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return subs;
    }

}
