
public class main {
    public static void main(String[] args) {
        nu.pattern.OpenCV.loadLocally();
        DatabaseHandler databaseHandler = new DatabaseHandler();
        DatabaseHandler.clearTable("videos");
        DatabaseHandler.clearTable("frames_raw");
        DatabaseHandler.clearTable("frames_processed");
        DatabaseHandler.clearTable("molecules");
        DatabaseHandler.clearTable("final_images");
        MainFrame mainFrame = new MainFrame();
    }
}