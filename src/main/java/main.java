import ij.IJ;
import ij.ImageJ ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.util.Arrays;


public class main {
    public static void main(String[] args) {
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        nu.pattern.OpenCV.loadLocally();
        DatabaseHandler databaseHandler = new DatabaseHandler();
        DatabaseHandler.clearTable("videos");
        DatabaseHandler.clearTable("frames_raw");
        DatabaseHandler.clearTable("frames_processed");
        DatabaseHandler.clearTable("molecules");
        DatabaseHandler.clearTable("final_images");
        MainFrame mainFrame = new MainFrame();

        //ImageJ ij = new ImageJ();
    }
}