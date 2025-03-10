package com.iradraconis.shrinkify;


import com.formdev.flatlaf.intellijthemes.FlatMonokaiProIJTheme;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class Shrinkify extends JFrame {

    private File[] pdfFiles;
    private JComboBox<String> qualityComboBox;
    private JComboBox<String> resolutionComboBox;
    private JCheckBox bwCheckBox;
    private JCheckBox overwriteCheckBox;
    private JButton saveButton;
    private JProgressBar progressBar;
    private DefaultListModel<File> fileListModel;
    private JList<File> fileList;
    private JTextArea compressionInfoTextArea;

    public Shrinkify(String[] args) throws IOException {
        setTitle("Shrinkify - PDF Kompressor mit PDFBox");
        setSize(860, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();

        if (args.length > 0) {
            pdfFiles = new File[args.length];
            for (int i = 0; i < args.length; i++) {
                File file = new File(args[i]);
                if (file.exists() && file.getName().toLowerCase().endsWith(".pdf")) {
                    pdfFiles[i] = file;
                    fileListModel.addElement(pdfFiles[i]); // Datei zur Liste hinzufügen
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Ungültige Datei: " + args[i] + "\nBitte nur PDF-Dateien übergeben.",
                            "Fehler",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void initComponents() {
        // Hauptpanel mit GridBagLayout
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(50, 50, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);  // Abstände zwischen den Komponenten

        // Datei-Auswahlbereich
        JPanel filePanel = new JPanel(new BorderLayout());
        filePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        filePanel.setBackground(new Color(50, 50, 50));

        JButton chooseFileButton = new JButton("PDF-Dateien hinzufügen");
        chooseFileButton.addActionListener(e -> chooseFiles());
        filePanel.add(chooseFileButton, BorderLayout.NORTH);

        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        JScrollPane fileScrollPane = new JScrollPane(fileList);
        fileList.setVisibleRowCount(15);
        // set fileList to width of 200
        fileList.setFixedCellWidth(500);
        filePanel.add(fileScrollPane, BorderLayout.CENTER);

        // Hinzufügen von Drag-and-Drop-Funktionalität
        fileList.setDropMode(DropMode.INSERT);
        fileList.setTransferHandler(new FileTransferHandler());

        // MouseListener für Rechtsklick-Menü
        fileList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = fileList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        fileList.setSelectedIndex(index);
                        showContextMenu(e.getX(), e.getY());
                    }
                }
            }
        });

        // Datei-Auswahlbereich hinzufügen (linke Seite)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1; 
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(filePanel, gbc);

        // Einstellungen
        JPanel settingsPanel = new JPanel();
        settingsPanel.setBackground(new Color(50, 50, 50)); 
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Qualitäts-Kombobox
        String[] qualities = {"Sehr niedrige Qualität", "Niedrigste Qualität", "Niedrige Qualität", "Mittlere Qualität", "Hohe Qualität", "Sehr hohe Qualität"};
        qualityComboBox = new JComboBox<>(qualities);
        qualityComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, qualityComboBox.getPreferredSize().height));
        


        qualityComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityComboBox.setSelectedIndex(3);
        JLabel qualityLabel = new JLabel("Qualitätsstufe auswählen:");
        qualityLabel.setForeground(Color.WHITE); 
        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(qualityLabel);
        settingsPanel.add(qualityComboBox);

        // Auflösungs-Kombobox
        String[] resolutions = {"100%", "90%", "80%", "70%", "60%", "50%", "40%", "30%"};
        resolutionComboBox = new JComboBox<>(resolutions);
        resolutionComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, resolutionComboBox.getPreferredSize().height));
        resolutionComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        resolutionComboBox.setSelectedIndex(6); // standard auf 40 % setzen
        JLabel resolutionLabel = new JLabel("Auflösung reduzieren:");
        resolutionLabel.setForeground(Color.WHITE);
        resolutionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(Box.createVerticalStrut(10));
        settingsPanel.add(resolutionLabel);
        settingsPanel.add(resolutionComboBox);

        // Schwarz/Weiß Checkbox
        bwCheckBox = new JCheckBox("In Schwarz/Weiß konvertieren");
        bwCheckBox.setForeground(Color.WHITE);        
        bwCheckBox.setBackground(new Color(50, 50, 50));
        bwCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(Box.createVerticalStrut(10));
        settingsPanel.add(bwCheckBox);

        // Überschreiben Checkbox
        overwriteCheckBox = new JCheckBox("Urspr. Dateien überschreiben");
        overwriteCheckBox.setSelected(true);
        overwriteCheckBox.setForeground(Color.WHITE);
        overwriteCheckBox.setBackground(new Color(50, 50, 50));
        overwriteCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        overwriteCheckBox.addActionListener(e -> {
            if (overwriteCheckBox.isSelected()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Warnung: Die Originaldateien werden überschrieben!\nFortfahren?",
                        "Originaldateien überschreiben",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) {
                    overwriteCheckBox.setSelected(false);
                }
            }
        });
        settingsPanel.add(Box.createVerticalStrut(15));
        settingsPanel.add(overwriteCheckBox);

        

        // Einstellungen-Panel hinzufügen (mittig)
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(settingsPanel, gbc);

        // Panel für die Buttons (Speichern und Vorschau)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(new Color(50, 50, 50)); // Dunkler Hintergrund

        // Test-Button für Vorschau der Komprimierung
        JButton testButton = new JButton("Vorschau");
        testButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, testButton.getPreferredSize().height));

        testButton.addActionListener(e -> previewCompressedFile());
        settingsPanel.add(testButton);

        // Speichern-Button
        saveButton = new JButton("Speichern");
        // stretch button width to panel width
        saveButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, saveButton.getPreferredSize().height));
        
        settingsPanel.add(Box.createVerticalStrut(15));

        saveButton.addActionListener(e -> compressAndSaveFiles());
        settingsPanel.add(saveButton);
        
        

        // Fortschrittsbalken
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsPanel.add(Box.createVerticalStrut(15));
        settingsPanel.add(progressBar);
        
        
        // TextArea für Kompressionsinformationen hinzufügen (ganz unten)
        compressionInfoTextArea = new JTextArea(10, 20); // Vergrößere die Textarea um 5 Zeilen
        compressionInfoTextArea.setEditable(false);
        compressionInfoTextArea.setForeground(Color.WHITE);
        compressionInfoTextArea.setBackground(new Color(50, 50, 50));
        JScrollPane scrollPane = new JScrollPane(compressionInfoTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, gbc);

        add(panel);
    }



    // Neue Klasse für Drag-and-Drop
    private class FileTransferHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            // Überprüfen, ob Dateien importiert werden können
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            // Importieren der Dateien
            try {
                Transferable t = support.getTransferable();
                List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);

                for (File file : files) {
                    if (file.getName().toLowerCase().endsWith(".pdf")) {
                        fileListModel.addElement(file);
                    } else {
                        JOptionPane.showMessageDialog(Shrinkify.this,
                                "Nur PDF-Dateien können hinzugefügt werden:\n" + file.getName(),
                                "Ungültige Datei",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    // Methode zum Anzeigen des Kontextmenüs
    private void showContextMenu(int x, int y) {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem removeItem = new JMenuItem("Datei entfernen");
        removeItem.addActionListener(e -> {
            int selectedIndex = fileList.getSelectedIndex();
            if (selectedIndex != -1) {
                fileListModel.remove(selectedIndex);
            }
        });
        contextMenu.add(removeItem);
        contextMenu.show(fileList, x, y);
    }

    private void chooseFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            pdfFiles = fileChooser.getSelectedFiles();
            for (File file : pdfFiles) {
                fileListModel.addElement(file);
            }
        }
    }
    
        private void previewCompressedFile() {
        if (fileListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie mindestens eine PDF-Datei aus.");
            return;
        }
    
        float imageQuality = getImageQuality();
        boolean convertBW = bwCheckBox.isSelected();
        float resolutionScale = getResolutionScale();
    
        new Thread(() -> {
            StringBuilder compressionInfo = new StringBuilder();
            for (int i = 0; i < fileListModel.size(); i++) {
                File inputFile = fileListModel.getElementAt(i);
    
                try {
                    // Temporäre Datei erstellen
                    File tempFile = File.createTempFile("preview_compressed_", ".pdf");
                    tempFile.deleteOnExit();  // Wird nach dem Schließen des Programms gelöscht
    
                    long originalSize = inputFile.length();  // Ursprüngliche Dateigröße
    
                    // PDF komprimieren
                    compressPDFWithPDFBox(inputFile, tempFile, imageQuality, convertBW, resolutionScale);
    
                    long compressedSize = tempFile.length();  // Komprimierte Dateigröße
    
                    // Kompressionsinformationen berechnen
                    double originalSizeMB = originalSize / (1024.0 * 1024.0);
                    double compressedSizeMB = compressedSize / (1024.0 * 1024.0);
                    double reductionPercent = ((originalSize - compressedSize) / (double) originalSize) * 100;
    
                    // Informationen hinzufügen
                    compressionInfo.append(String.format("Datei: %s\nOriginalgröße: %.2f MB - Komprimiert: %.2f MB - Reduktion: %.2f%%\n\n",
                            inputFile.getName(), originalSizeMB, compressedSizeMB, reductionPercent));
    
                    // Vorschau anzeigen - Öffnet das temporäre PDF mit dem Standard-PDF-Viewer des Systems
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(tempFile);
                    } else {
                        JOptionPane.showMessageDialog(this, "Vorschau nicht unterstützt auf diesem System.");
                    }
    
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Fehler beim Erstellen der Vorschau für Datei: " + inputFile.getName());
                }
            }
            compressionInfoTextArea.setText(compressionInfo.toString());
        }).start();
    }



    private void compressAndSaveFiles() {
        if (fileListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie mindestens eine PDF-Datei aus.");
            return;
        }

        float imageQuality = getImageQuality();
        boolean convertBW = bwCheckBox.isSelected();
        float resolutionScale = getResolutionScale();
        boolean overwriteFiles = overwriteCheckBox.isSelected();

        File outputDir = null;
        if (!overwriteFiles) {
            JFileChooser dirChooser = new JFileChooser();
            dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = dirChooser.showOpenDialog(this); // Hier wurde showSaveDialog zu showOpenDialog geändert
            if (option != JFileChooser.APPROVE_OPTION) {
                return;
            }
            outputDir = dirChooser.getSelectedFile();
        }

        // Listen zur Speicherung der Dateigrößen
        ArrayList<String> results = new ArrayList<>();
        DecimalFormat df = new DecimalFormat("#.##");

        File finalOutputDir = outputDir;

        // Startzeit erfassen
        long startTime = System.currentTimeMillis();

        new Thread(() -> {
            progressBar.setMaximum(fileListModel.size());
            int progress = 0;

            for (int i = 0; i < fileListModel.size(); i++) {
                File inputFile = fileListModel.getElementAt(i);
                File outputFile;

                if (overwriteFiles) {
                    // Temporäre Datei erstellen
                    try {
                        outputFile = File.createTempFile("temp_compressed_", ".pdf");
                        outputFile.deleteOnExit();
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Fehler beim Erstellen der temporären Datei für: " + inputFile.getName());
                        continue;
                    }
                } else {
                    outputFile = new File(finalOutputDir, "komprimiert_" + inputFile.getName());
                }

                try {
                    long originalSize = inputFile.length();
                    compressPDFWithPDFBox(inputFile, outputFile, imageQuality, convertBW, resolutionScale);

                    if (overwriteFiles) {
                        // Originaldatei durch komprimierte Datei ersetzen
                        Files.move(outputFile.toPath(), inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    long compressedSize = overwriteFiles ? inputFile.length() : outputFile.length();

                    double originalSizeMB = originalSize / (1024.0 * 1024.0);
                    double compressedSizeMB = compressedSize / (1024.0 * 1024.0);
                    double reductionPercent = ((originalSize - compressedSize) / (double) originalSize) * 100;

                    String result = String.format("Datei: %s\nOriginalgröße: %s MB\nKomprimiert: %s MB\nReduktion: %s%%\n",
                            inputFile.getName(),
                            df.format(originalSizeMB),
                            df.format(compressedSizeMB),
                            df.format(reductionPercent));
                    results.add(result);

                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Fehler beim Verarbeiten der Datei: " + inputFile.getName());
                }

                progress++;
                progressBar.setValue(progress);
            }

            // Endzeit erfassen und Dauer berechnen
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            long seconds = (duration / 1000) % 60;
            long minutes = (duration / (1000 * 60)) % 60;

            // Zusammenstellen der Ergebnisse
            StringBuilder message = new StringBuilder("Alle Dateien wurden erfolgreich verarbeitet.\n\n");
            for (String result : results) {
                message.append(result).append("\n");
            }

            // Zeit zur Nachricht hinzufügen
            message.append(String.format("Verstrichene Zeit: %d Minuten und %d Sekunden.", minutes, seconds));
            compressionInfoTextArea.setText(message.toString());
            //JOptionPane.showMessageDialog(this, message.toString());
        }).start();
    }

    private float getImageQuality() {
        String quality = (String) qualityComboBox.getSelectedItem();
        switch (quality) {
            case "Sehr niedrige Qualität":
                return 0.05f;
            case "Niedrigste Qualität":
                return 0.1f;
            case "Niedrige Qualität":
                return 0.3f;
            case "Mittlere Qualität":
                return 0.5f;
            case "Hohe Qualität":
                return 0.8f;
            case "Sehr hohe Qualität":
                return 1.0f;
            default:
                return 0.5f;
        }
    }

    private float getResolutionScale() {
        String resolution = (String) resolutionComboBox.getSelectedItem();
        switch (resolution) {
            case "100%":
                return 1.0f;
            case "90%":
                return 0.9f;
            case "80%":
                return 0.8f;
            case "70%":
                return 0.7f;
            case "60%":
                return 0.6f;
            case "50%":
                return 0.5f;
            case "40%":
                return 0.4f;
            case "30%":
                return 0.3f;
            default:
                return 1.0f;
        }
    }

    private void compressPDFWithPDFBox(File inputFile, File outputFile, float imageQuality, boolean convertBW, float resolutionScale) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputFile)) {
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                Iterable<COSName> xObjectNames = resources.getXObjectNames();

                for (COSName xObjectName : xObjectNames) {
                    PDXObject xObject = resources.getXObject(xObjectName);

                    if (xObject instanceof PDImageXObject) {
                        PDImageXObject imageObject = (PDImageXObject) xObject;

                        // Prüfen der Bilddimensionen vor dem Laden des Bildes
                        int imageWidth = imageObject.getWidth();
                        int imageHeight = imageObject.getHeight();

                        // Skip if dimensions are invalid
                        if (imageWidth <= 1 || imageHeight <= 1) {
                            continue; // Bild überspringen
                        }

                        BufferedImage image;
                        try {
                            image = imageObject.getImage();
                        } catch (Exception e) {
                            continue;
                        }

                        // Prüfen der Dimensionen des geladenen BufferedImage
                        int bufferedImageWidth = image.getWidth();
                        int bufferedImageHeight = image.getHeight();

                        if (bufferedImageWidth <= 1 || bufferedImageHeight <= 1) {
                            continue; // Bild überspringen
                        }

                        // Bild nach Bedarf in RGB konvertieren
                        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
                            BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                            Graphics2D g = rgbImage.createGraphics();
                            g.drawImage(image, 0, 0, null);
                            g.dispose();
                            image = rgbImage;
                        }

                        // Bild in Schwarz-Weiß konvertieren, falls ausgewählt
                        if (convertBW) {
                            BufferedImage bwImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                            Graphics2D g = bwImage.createGraphics();
                            g.drawImage(image, 0, 0, null);
                            g.dispose();
                            image = bwImage;
                        }

                        // Bildauflösung skalieren, falls erforderlich
                        if (resolutionScale != 1.0f) {
                            int newWidth = (int) (image.getWidth() * resolutionScale);
                            int newHeight = (int) (image.getHeight() * resolutionScale);

                            // Überprüfen, ob die neuen Dimensionen gültig sind
                            if (newWidth > 1 && newHeight > 1) {
                                BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, image.getType());
                                Graphics2D g = scaledImage.createGraphics();
                                g.drawImage(image, 0, 0, scaledImage.getWidth(), scaledImage.getHeight(), null);
                                g.dispose();
                                image = scaledImage;
                            } else {
                                continue; // Bild überspringen
                            }
                        }

                        // Bild komprimieren und speichern
                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
                            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                            writer.setOutput(ios);

                            ImageWriteParam param = writer.getDefaultWriteParam();
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            param.setCompressionQuality(imageQuality);

                            writer.write(null, new IIOImage(image, null, null), param);
                            writer.dispose();

                            InputStream in = new ByteArrayInputStream(baos.toByteArray());
                            PDImageXObject compressedImage = JPEGFactory.createFromStream(document, in);
                            resources.put(xObjectName, compressedImage);

                            IOUtils.closeQuietly(in);
                            IOUtils.closeQuietly(ios);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            document.save(outputFile);
        }
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatMonokaiProIJTheme.setup();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Shrinkify app = null;
            try {
                app = new Shrinkify(args);
            } catch (IOException ex) {
                Logger.getLogger(Shrinkify.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (app != null) {
                app.setVisible(true);
            }
        });
    }
}
