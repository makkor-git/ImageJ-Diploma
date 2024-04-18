import java.sql.*;

public class DatabaseHandler {
    private static final String JDBC_URL = "jdbc:sqlserver://LAPTOP-L48QACSK\\SQLEXPRESS:61647;" +
            "databaseName=dSTORM_DB;" +
            "integratedSecurity=false;" +
            "trustServerCertificate=true;" +
            "user=sa;password=changeme";

    private static Connection connection;

    public DatabaseHandler() {
        try {
            connection = DriverManager.getConnection(JDBC_URL);
            if (connection != null) {
                System.out.println("Connected to database");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addVideo(String path, int width, int height, String format) {
        String query = "INSERT INTO videos (path, width, height, format) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, path);
            statement.setInt(2, width);
            statement.setInt(3, height);
            statement.setString(4, format);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void addRawFrame(String rawFramePath, String videoPath) {
        // Получаем айди видео по его пути
        int videoId = getVideoID(videoPath);

        // Запрос на добавление записи в таблицу frames_raw с указанием внешнего ключа video_id
        String query = "INSERT INTO frames_raw (path, video_id) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, rawFramePath);
            statement.setInt(2, videoId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addProcessedFrame(String procFramePath, String rawFramePath) {
        // Получаем айди сырого кадра по его пути
        int rawFrameID = getRawFrameID(rawFramePath);

        // Запрос на добавление записи в таблицу frames_processed с указанием внешнего ключа frames_raw_id
        String query = "INSERT INTO frames_processed (path, frames_raw_id, binarization_method_id) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, procFramePath);
            statement.setInt(2, rawFrameID);
            statement.setInt(3, 5);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addMolecule(int coordX, int coordY, String procFramePath) {
        // Получаем айди сырого кадра по его пути
        int rawFrameID = getProcFrameID(procFramePath);

        // Запрос на добавление записи в таблицу frames_processed с указанием внешнего ключа frames_raw_id
        String query = "INSERT INTO molecules (coord_X, coord_Y, frames_processed_id) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, coordX);
            statement.setInt(2, coordY);
            statement.setInt(3, rawFrameID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addFinalImage(String videoPath, int freq, String finalImagePath, int locMethodId) {
        int videoId = getVideoID(videoPath);

        String query = "INSERT INTO final_images (video_id, frequency, path, localization_method_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, videoId);
            statement.setInt(2, freq);
            statement.setString(3, finalImagePath);
            statement.setInt(4, locMethodId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getVideoID(String videoPath) {
        int videoId = -1; // По умолчанию -1, если видео не найдено

        String query = "SELECT video_id FROM videos WHERE path = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, videoPath);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                videoId = resultSet.getInt("video_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return videoId;
    }

    public static int getRawFrameID(String rawFramePath) {
        int rawFrameID = -1;

        String query = "SELECT frames_raw_id FROM frames_raw WHERE path = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, rawFramePath);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                rawFrameID = resultSet.getInt("frames_raw_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rawFrameID;
    }

    public static int getProcFrameID(String procFramePath) {
        int procFrameID = -1;

        String query = "SELECT frames_processed_id FROM frames_processed WHERE path = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, procFramePath);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                procFrameID = resultSet.getInt("frames_processed_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return procFrameID;
    }

    public static void clearTable(String tableName) {
        String query = "DELETE FROM " + tableName;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.executeUpdate();
            System.out.println("Table " + tableName + " cleared.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
