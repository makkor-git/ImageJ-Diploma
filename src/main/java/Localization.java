import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.*;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.*;
import ij.process.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class Localization {
    private JFrame frame;
    private ImagePlus inp;
    private ImagePlus imp;
    private ImagePlus binaryImage;
    private ResultsTable rt;
    private Overlay overlay;
    private File selectedFile;

    public Localization() {
        // Создаем пользовательский интерфейс
        createUI();
    }

    private void createUI() {
        // Создаем окно
        frame = new JFrame("Localization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Создаем панель для кнопок
        JPanel buttonPanel = new JPanel();
        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Создаем кнопки
        JButton openButton = new JButton("Open Image");
        JButton localizeButton = new JButton("Localize");
        JButton saveButton = new JButton("Save");
        JButton gaussianButton = new JButton("Gaussian Localization");

        // Добавляем обработчик событий для кнопки "Open Image"
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Выбираем файл с изображением
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(frame);
                if (result != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                selectedFile = fileChooser.getSelectedFile();
                String imagePath = selectedFile.getAbsolutePath();

                // Открываем изображение
                inp = IJ.openImage(imagePath);
                inp.show();
                imp = IJ.openImage(imagePath);
                imp.show();
                rt = new ResultsTable();
                overlay = new Overlay();
                binaryImage = null;
            }
        });

        // Добавляем обработчик событий для кнопки "Localize"
        localizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (imp == null) {
                    return;
                }
                long startTime = System.nanoTime(); // Засекаем время начала выполнения
                // Бинаризуем изображение с использованием метода Otsu
                IJ.setAutoThreshold(imp, "Otsu dark");
                new ImageConverter(imp).convertToGray8();
                binaryImage = IJ.createImage("Binary Image", "8-bit", imp.getWidth(), imp.getHeight(), 1);
                IJ.run(imp, "Convert to Mask", "");
                binaryImage.setProcessor(null, imp.getProcessor().duplicate());
                binaryImage.show();

                // Создаем ROI, соответствующую области, содержащей одиночные молекулы
                Roi roi = new Roi(0, 0, imp.getWidth(), imp.getHeight());
                imp.setRoi(roi);

                // Вычисляем центроид для каждой молекулы в ROI
                int measurements = Measurements.CENTROID;
                ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_OUTLINES, measurements, rt, 0, 60, 0, 1);
                pa.analyze(imp);
                rt.show("Results");
                double[] xCentroids = rt.getColumnAsDoubles(ResultsTable.X_CENTROID);
                double[] yCentroids = rt.getColumnAsDoubles(ResultsTable.Y_CENTROID);

                // Выводим изображение с обведенными контурами молекул
                for (int i = 0; i < xCentroids.length; i++) {
                    if (isCluster(xCentroids, yCentroids, i)) {
                        continue;
                    }
                    OvalRoi ovalRoi = new OvalRoi(xCentroids[i] - 3, yCentroids[i] - 3, 6, 6);
                    ovalRoi.setStrokeColor(new Color(0, 0, 255));
                    overlay.add(ovalRoi);
                }
                imp.setOverlay(overlay);
                imp.show();


                // Выводим координаты центров молекул
                rt.show("Localization Results");
                long endTime = System.nanoTime(); // Засекаем время окончания выполнения
                long durationInMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime); // Рассчитываем время выполнения в миллисекундах
                System.out.println("Время выполнения центроидной локализации: " + durationInMillis + " мс");
            }
        });
        gaussianButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (imp == null) {
                    return;
                }

                // Применяем трешхолдинг
                IJ.setAutoThreshold(imp, "Otsu dark");
                new ImageConverter(imp).convertToGray8();
                binaryImage = IJ.createImage("Binary Image", "8-bit", imp.getWidth(), imp.getHeight(), 1);
                IJ.run(imp, "Convert to Mask", "");
                binaryImage.setProcessor(null, imp.getProcessor().duplicate());
                binaryImage.show();

                performGaussianLocalization();
            }
        });

        // Добавляем обработчик событий для кнопки "Save"
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (binaryImage == null || imp == null) {
                    return;
                }

                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Save Image");
                int result = chooser.showSaveDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    String path = chooser.getSelectedFile().getAbsolutePath();

                    // Сохраняем изображение
                    String binaryImagePath = path + File.separator + "binary_" + selectedFile.getName();
                    IJ.saveAs(binaryImage, "PNG", binaryImagePath);

                    String localizedImagePath = path + File.separator + "localized_" + selectedFile.getName();
                    IJ.saveAs(imp, "PNG", localizedImagePath);
                }
            }
        });

        // Добавляем кнопки на панель
        buttonPanel.add(openButton);
        buttonPanel.add(localizeButton);
        buttonPanel.add(gaussianButton);
        buttonPanel.add(saveButton);

        // Отображаем окно
        frame.pack();
        frame.setVisible(true);
    }

    // Метод для определения, является ли молекула частью скопления
    private boolean isCluster(double[] xCentroids, double[] yCentroids, int index) {
        int count = 0;
        for (int i = 0; i < xCentroids.length; i++) {
            if (i == index) {
                continue;
            }
            double distance = Math.sqrt(Math.pow(xCentroids[i] - xCentroids[index], 2) + Math.pow(yCentroids[i] - yCentroids[index], 2));
            if (distance < 5) {
                count++;
                if (count >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private void performGaussianLocalization() {
        if (imp == null) {
            return;
        }
        long startTime = System.nanoTime(); // Засекаем время начала выполнения
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
        ResultsTable centroidTable = new ResultsTable();
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

                    for (int i = -2; i <= 2; i++) {
                        for (int j = -2; j <= 2; j++) {
                            int newX = x + i;
                            int newY = y + j;

                            if (newX >= 0 && newX < width && newY >= 0 && newY < height) {
                                double intensity = pixels[newX][newY];
                                double weight = intensity * Math.exp(-((i * i + j * j) / (2 * sigma * sigma)));

                                sumX += newX * weight;
                                sumY += newY * weight;
                                totalWeight += weight;
                            }
                        }
                    }

                    double centerX = sumX / totalWeight;
                    double centerY = sumY / totalWeight;

                    if (!checkedMolecules.contains(counter) && !isCluster(centroidTable, centerX, centerY, minDistance)) {
                        OvalRoi ovalRoi = new OvalRoi(centerX - 1, centerY - 1, 6, 6);
                        TextRoi textRoi = new TextRoi(centerX, centerY, String.valueOf(counter));
                        textRoi.setStrokeColor(new Color(0, 255, 0)); // Зеленый цвет для текстовой метки
                        overlayWithNumbers.add(textRoi);

                        counter++;

                        // Добавляем текущую молекулу в таблицу результатов
                        centroidTable.incrementCounter();
                        centroidTable.addValue("X", centerX);
                        centroidTable.addValue("Y", centerY);
                        centroidTable.addValue("Molecule Number", counter - 1);
                        checkedMolecules.add(counter - 1);
                    }
                }
            }
        }
        for (int i = 0; i < centroidTable.getCounter(); i++) {
            int moleculeNumber = (int) centroidTable.getValue("Molecule Number", i);
            if (moleculeNumber == 0) {
                centroidTable.deleteRow(i);
                i--; // Уменьшаем индекс, чтобы не пропустить следующую строку после удаления
            }
        }

        imp.setOverlay(overlay);
        imp.show();

        ImagePlus overlayWithNumbersImage = NewImage.createRGBImage("Localized Molecules with Numbers", width, height, 1, NewImage.FILL_BLACK);
        overlayWithNumbersImage.setOverlay(overlayWithNumbers);
        overlayWithNumbersImage.show();

        centroidTable.show("Gaussian Localization Results");
        long endTime = System.nanoTime(); // Засекаем время окончания выполнения
        long durationInMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime); // Рассчитываем время выполнения в миллисекундах
        System.out.println("Время выполнения гауссовской аппроксимации: " + durationInMillis + " мс");
    }

    // Метод для определения, является ли молекула частью скопления
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
    public static void main(String[] args) {
        // Создаем экземпляр класса Localization
        new Localization();
    }
}
