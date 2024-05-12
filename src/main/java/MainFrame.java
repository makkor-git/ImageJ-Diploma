import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.SQLOutput;
import java.util.*;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.io.FileSaver;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class MainFrame extends JFrame implements ActionListener, WindowListener {
    JButton chooseFileButton;
    JButton processButton;
    JTextField filePathTextField;
    JComboBox<String> binarizationMethodComboBox;
    JComboBox<String> localizationMethodComboBox;
    JLabel processingLabel;
    JProgressBar progressBar;

    public String filePath;
    public String binarizationMethod;
    public String localizationMethod;
    public ImagePlus binaryImage;
    public String rawFramesPath;
    public String procFramesPath;
    public HashMap<Integer, String> binarizationMethods;
    public ArrayList<double[][]> coords;

    public MainFrame() {
        this.setSize(600, 300);
        this.setLayout(new GridBagLayout());
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(this);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        chooseFileButton = new JButton("Select file");
        chooseFileButton.addActionListener(this);
        gbc.gridx = 0;
        gbc.gridy = 0;
        this.add(chooseFileButton, gbc);

        filePathTextField = new JTextField(30);
        filePathTextField.setEditable(false);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        this.add(filePathTextField, gbc);

        JLabel binarizationMethodLabel = new JLabel("Binarization Method:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        this.add(binarizationMethodLabel, gbc);

        String[] binarizationMethods = {"None", "Default", "Huang", "Intermodes", "IsoData", "Li",
                "MaxEntropy", "Mean", "Minimum", "Otsu", "Percentile", "Yen"};
        binarizationMethodComboBox = new JComboBox<>(binarizationMethods);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        this.add(binarizationMethodComboBox, gbc);

        JLabel localizationMethodLabel = new JLabel("Localization Method:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        this.add(localizationMethodLabel, gbc);

        String[] localizationMethods = {"Default", "Centroids", "Gaussian"};
        localizationMethodComboBox = new JComboBox<>(localizationMethods);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        this.add(localizationMethodComboBox, gbc);

        processButton = new JButton("Process");
        processButton.addActionListener(this);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        this.add(processButton, gbc);

        processingLabel = new JLabel("Processing...");
        processingLabel.setVisible(false); // Initially invisible
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        this.add(processingLabel, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true); // Set to indeterminate mode
        progressBar.setVisible(false); // Initially invisible
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        this.add(progressBar, gbc);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - this.getWidth()) / 2;
        int y = (screenSize.height - this.getHeight()) / 2;
        this.setLocation(x, y);

        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
       /* if(e.getSource() == launchImageJButton) {
            String videoPath = new String("C:\\Users\\makko\\Downloads\\videoplayback.mp4");
            VideoCapture capture = new VideoCapture(videoPath);
            Mat image = new Mat();
            int i = 0;
            while (capture.isOpened()) {
                boolean ret = capture.read(image);
                if (ret) {
                    Imgcodecs.imwrite("OutputIJ\\Frame" + i + ".png", image);
                }
                else
                    break;
                i++;
            }
        }*/

        if(e.getSource() == chooseFileButton) {
//            if (!new File("raw").mkdir() && !new File("raw").exists())
//            {
//                try {
//                    throw new IOException();
//                } catch (IOException ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//
//            try {
//                FileUtils.cleanDirectory(new File("raw"));
//            } catch (IOException ex) {
//                throw new RuntimeException(ex);
//            }


            //Choosing a videofile
            JFileChooser fileChooser = new JFileChooser();
            int resp = fileChooser.showOpenDialog(null);

            if (resp == JFileChooser.APPROVE_OPTION) {
                filePath = new String(fileChooser.getSelectedFile().getAbsolutePath());
                filePathTextField.setText(filePath);
            }
        }
        else if(e.getSource() == processButton) {
            if (filePath != null && !filePath.isEmpty()) {
                // Disable UI components
                setUIEnabled(false);

                // Show processing label and progress bar
                processingLabel.setVisible(true);
                progressBar.setVisible(true);
                // Perform processing asynchronously
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        MainFrame.this.process((String) binarizationMethodComboBox.getSelectedItem(), (String) localizationMethodComboBox.getSelectedItem());
                        return null;
                    }

                    @Override
                    protected void done() {
                        // Enable UI components
                        setUIEnabled(true);

                        // Hide processing label and progress bar
                        processingLabel.setVisible(false);
                        progressBar.setVisible(false);

                        JOptionPane.showMessageDialog(MainFrame.this, "Done");
                    }
                };
                worker.execute();
            }
            else {
                JOptionPane.showMessageDialog(this, "Please select a file first.");
            }
        }


    }

    public void process(String binarizationMethod, String localizationMethod) {
        //Loading video file
        String videoPath = filePath;
        System.out.println("Loading video: " + filePath);
        VideoCapture capture = new VideoCapture(videoPath);
        int vidWidth = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int vidHeight = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        int frameRate = (int) capture.get(Videoio.CAP_PROP_FPS);
        String format = videoPath.substring(videoPath.length() - 3);
        System.out.println("Video height: " + vidHeight);
        System.out.println("Video width: " + vidWidth);
        System.out.println("Frame rate: " + frameRate);
        //adding video info to database
        DatabaseHandler.addVideo(filePath, vidWidth, vidHeight, format);

        //Splitting videofile into frames
        System.out.println("Splitting videofile into frames...");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime now = LocalDateTime.now();
        String processTime = dtf.format(now);
        rawFramesPath = "raw\\" + processTime;
        new File(rawFramesPath).mkdirs();

        //int frameRate = 1000;
        //long prev = System.currentTimeMillis();

        Mat image = new Mat();
        int i = 0;
        while (capture.isOpened()) {
            //long timeElapsed = System.currentTimeMillis() - prev;
            boolean ret = capture.read(image);
            if (ret) {
                String rawFramePath = rawFramesPath + "\\Frame" + i + ".jpg";

                //adding raw frames info to database
                DatabaseHandler.addRawFrame(rawFramePath, videoPath);

                Imgcodecs.imwrite(rawFramePath, image);
                i++;

            } else
                break;

        }


        System.out.println("Splitting into frames complete.");

        //Processing images
        System.out.println("Processing frames...");
        coords = new ArrayList<>();
        int imSizeX = 0, imSizeY = 0;

        procFramesPath = "processed\\" + processTime;
        new File(procFramesPath).mkdirs();

        while(i > 0) {
            String rawFramePath = rawFramesPath + "\\Frame" + (i-1) + ".jpg";
            ImagePlus imp = IJ.openImage(rawFramePath);

            imSizeX = imp.getWidth();
            imSizeY = imp.getHeight();

            imp.show();


            if(binarizationMethod != "None") {
                //binarize the frame
                IJ.setAutoThreshold(imp, binarizationMethod);
                new ImageConverter(imp).convertToGray8();
                binaryImage = IJ.createImage("Binary Image", "8-bit", imp.getWidth(), imp.getHeight(), 1);
                IJ.run(imp, "Convert to Mask", "");
                binaryImage.setProcessor(null, imp.getProcessor().duplicate());
                binaryImage.show();
            }

            String procFramePath = "";
            ResultsTable rt = new ResultsTable();
            double[] xCoords = new double[0];
            double[] yCoords = new double[0];
            if (localizationMethod == "Default") {
                //ImageJ Macro Language
                String macro = "run(\"Gaussian Blur...\", \"sigma=2\");" +
                        "run(\"Find Maxima...\", \"output=[Single Points]\");";
                IJ.runMacro(macro);
                procFramePath = procFramesPath + "\\Frame" + (i - 1) + ".png";
                IJ.saveAs("png", procFramePath);

                DatabaseHandler.addProcessedFrame(procFramePath, rawFramePath, binarizationMethod);

                macro = "run(\"Find Maxima...\", \"output=List\");";
                IJ.runMacro(macro);

                rt = ResultsTable.getResultsTable();
                xCoords = rt.getColumnAsDoubles(0);
                yCoords = rt.getColumnAsDoubles(1);
            } else if (localizationMethod == "Centroids") {
                //IJ.setAutoThreshold(imp, "Default");
                IJ.setThreshold(imp, 200.0, 255.0);
                /*new ImageConverter(imp).convertToGray8();
                binaryImage = IJ.createImage("Binary Image", "8-bit", imp.getWidth(), imp.getHeight(), 1);
                IJ.run(imp, "Convert to Mask", "");
                binaryImage.setProcessor(null, imp.getProcessor().duplicate());
                binaryImage.show();*/

                // Создаем ROI, соответствующую области, содержащей одиночные молекулы
                Roi roi = new Roi(0, 0, imp.getWidth(), imp.getHeight());
                imp.setRoi(roi);

                // Вычисляем центроид для каждой молекулы в ROI
                int measurements = Measurements.CENTROID;
                ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_OUTLINES, measurements, rt, 0, 60, 0, 1);
                pa.setHideOutputImage(true);
                pa.analyze(imp);

                procFramePath = procFramesPath + "\\Frame" + (i - 1) + ".png";
                IJ.saveAs("png", procFramePath);

                DatabaseHandler.addProcessedFrame(procFramePath, rawFramePath, binarizationMethod);

                xCoords = rt.getColumnAsDoubles(ResultsTable.X_CENTROID);
                yCoords = rt.getColumnAsDoubles(ResultsTable.Y_CENTROID);


            } else if (localizationMethod == "Gaussian") {
                /*
                IJ.setAutoThreshold(imp, "Otsu dark");
                new ImageConverter(imp).convertToGray8();
                binaryImage = IJ.createImage("Binary Image", "8-bit", imp.getWidth(), imp.getHeight(), 1);
                IJ.run(imp, "Convert to Mask", "");
                binaryImage.setProcessor(null, imp.getProcessor().duplicate());
                binaryImage.show();*/

                ImageProcessor ip = imp.getProcessor();
                int width = ip.getWidth();
                int height = ip.getHeight();
                double[][] pixels = new double[width][height];

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        pixels[x][y] = ip.getPixelValue(x, y);
                    }
                }

                double threshold = 150; // Порог для локализации
                double sigma = 2.0; // Параметр гауссиана
                double minDistance = 25.0; // Минимальное расстояние между молекулами
                //ResultsTable rt = new ResultsTable();
                Overlay overlay = new Overlay();
                Overlay overlayWithNumbers = new Overlay();
                HashSet<Integer> checkedMolecules = new HashSet<>(); // Хэшсет для отслеживания номеров молекул

                int counter = 1; // Счётчик для нумерации молекул

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        if (pixels[x][y] > threshold) {
                            double totalWeight = 0.0;
                            double sumX = 0.0;
                            double sumY = 0.0;

                            for (int k = -2; k <= 2; k++) {
                                for (int j = -2; j <= 2; j++) {
                                    int newX = x + k;
                                    int newY = y + j;

                                    if (newX >= 0 && newX < width && newY >= 0 && newY < height) {
                                        double intensity = pixels[newX][newY];
                                        double weight = intensity * Math.exp(-((k * k + j * j) / (2 * sigma * sigma)));

                                        sumX += newX * weight;
                                        sumY += newY * weight;
                                        totalWeight += weight;
                                    }
                                }
                            }

                            double centerX = sumX / totalWeight;
                            double centerY = sumY / totalWeight;

                            if (!checkedMolecules.contains(counter) && !isCluster(rt, centerX, centerY, minDistance)) {
                                OvalRoi ovalRoi = new OvalRoi(centerX - 1, centerY - 1, 6, 6);
                                TextRoi textRoi = new TextRoi(centerX, centerY, String.valueOf(counter));
                                textRoi.setStrokeColor(new Color(0, 255, 0)); // Зеленый цвет для текстовой метки
                                overlayWithNumbers.add(textRoi);

                                counter++;

                                // Добавляем текущую молекулу в таблицу результатов
                                rt.incrementCounter();
                                rt.addValue("X", centerX);
                                rt.addValue("Y", centerY);
                                rt.addValue("Molecule Number", counter - 1);
                                checkedMolecules.add(counter - 1);
                            }
                        }
                    }
                }
                for (int k = 0; k < rt.getCounter(); k++) {
                    int moleculeNumber = (int) rt.getValue("Molecule Number", k);
                    if (moleculeNumber == 0) {
                        rt.deleteRow(k);
                        k--; // Уменьшаем индекс, чтобы не пропустить следующую строку после удаления
                    }
                }

                imp.setOverlay(overlay);
                imp.show();

                ImagePlus overlayWithNumbersImage = NewImage.createRGBImage("Localized Molecules with Numbers", width, height, 1, NewImage.FILL_BLACK);
                overlayWithNumbersImage.setOverlay(overlayWithNumbers);
                overlayWithNumbersImage.show();

                procFramePath = procFramesPath + "\\Frame" + (i - 1) + ".png";
                IJ.saveAs("png", procFramePath);

                DatabaseHandler.addProcessedFrame(procFramePath, rawFramePath, binarizationMethod);

                xCoords = rt.getColumnAsDoubles(0);
                yCoords = rt.getColumnAsDoubles(1);

                overlayWithNumbersImage.hide();
            }

            //Molecules coordinates
            //rt = ResultsTable.getResultsTable();
            int numRows = rt.size();
            //double[] xCoords = rt.getColumnAsDoubles(0);
            //double[] yCoords = rt.getColumnAsDoubles(1);

            if (xCoords != null) {
                for (int n = 0; n < xCoords.length; n++) {
                    DatabaseHandler.addMolecule((int) xCoords[n], (int) yCoords[n], procFramePath);
                }
            }

            double[][] A = new double[numRows][2];
            for (int k = 0; k < numRows; k++) {
                A[k][0] = xCoords[k];
                A[k][1] = yCoords[k];
            }

            coords.add(A);

            imp.hide();
            if(binaryImage != null) {
                binaryImage.hide();
            }
            i--;
        }

        System.out.println("Processing complete.");

        //Final result
        System.out.println("Creating final image...");
        ImagePlus finalImage = NewImage.createImage("Final result",
                imSizeX, imSizeY,
                1,
                8,
                NewImage.FILL_BLACK);
        ImageProcessor ip = finalImage.getProcessor();
        ip.setColor(new Color(255, 255, 255));

        for (i = 0; i < coords.size(); i++) {
            for (int j = 0; j < coords.get(i).length; j++) {
                ip.drawDot((int) coords.get(i)[j][0], (int) coords.get(i)[j][1]);
            }
        }

        finalImage.show();



        FileSaver fs = new FileSaver(finalImage);
        String finalImagePath = "final_images\\FinalImage" + processTime + ".png";
        fs.saveAsPng(finalImagePath);

        System.out.println("Final image created: " + finalImagePath);

        DatabaseHandler.addFinalImage(videoPath, frameRate, finalImagePath, localizationMethod);



    }

    private boolean isCluster(ResultsTable centroidTable, double x, double y, double minDistance) {
        if (centroidTable != null) {
            int count = centroidTable.getCounter();
            for (int i = 0; i < count; i++) {
                double xCoord = centroidTable.getValueAsDouble(0, i);
                double yCoord = centroidTable.getValueAsDouble(1, i);
                double distance = Math.sqrt(Math.pow(x - xCoord, 2) + Math.pow(y - yCoord, 2));
                if (distance < minDistance) {
                    return true; // Если найдено скопление, возвращаем true
                }
            }
        }

        // Если не найдено скопление, добавляем текущую молекулу в таблицу координат
        centroidTable.incrementCounter();
        centroidTable.addValue("X", x);
        centroidTable.addValue("Y", y);

        return false; // Возвращаем false для одиночной молекулы
    }

    private void setUIEnabled(boolean enabled) {
        chooseFileButton.setEnabled(enabled);
        binarizationMethodComboBox.setEnabled(enabled);
        localizationMethodComboBox.setEnabled(enabled);
        processButton.setEnabled(enabled);
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        DatabaseHandler.closeConnection();
        System.exit(0);
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
































