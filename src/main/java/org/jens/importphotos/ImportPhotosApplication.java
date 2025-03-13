package org.jens.importphotos;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class ImportPhotosApplication extends Application {
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp", "heic", "cr2", "cr3", "nef", "arw", "dng", "orf", "rw2", "raf", "srw", "pef");

    private File sourceDir;
    private File targetDir1;
    private File targetDir2;
    private ProgressBar progressBar = new ProgressBar(0);
    private Label progressLabel = new Label("Fortschritt: 0%");

    @Override
    public void start(Stage primaryStage) {
        Button chooseSourceButton = new Button("Quellverzeichnis auswählen");
        Button chooseTarget1Button = new Button("Zielverzeichnis 1 auswählen");
        Button chooseTarget2Button = new Button("Zielverzeichnis 2 auswählen");
        Button copyFilesButton = new Button("Fotos sortieren & kopieren");

        Label sourceLabel = new Label("Kein Quellverzeichnis ausgewählt");
        Label target1Label = new Label("Kein Zielverzeichnis 1 ausgewählt");
        Label target2Label = new Label("Kein Zielverzeichnis 2 ausgewählt");

        chooseSourceButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDir = directoryChooser.showDialog(primaryStage);
            if (selectedDir != null) {
                sourceDir = selectedDir;
                sourceLabel.setText("Quelle: " + sourceDir.getAbsolutePath());
            }
        });

        chooseTarget1Button.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDir = directoryChooser.showDialog(primaryStage);
            if (selectedDir != null) {
                targetDir1 = selectedDir;
                target1Label.setText("Ziel 1: " + targetDir1.getAbsolutePath());
            }
        });

        chooseTarget2Button.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDir = directoryChooser.showDialog(primaryStage);
            if (selectedDir != null) {
                targetDir2 = selectedDir;
                target2Label.setText("Ziel 2: " + targetDir2.getAbsolutePath());
            }
        });

        copyFilesButton.setOnAction(e -> {
            if (sourceDir != null && targetDir1 != null && targetDir2 != null) {
                organizeAndCopyPhotos();
            } else {
                showAlert("Bitte wähle zuerst das Quellverzeichnis und beide Zielverzeichnisse aus.");
            }
        });

        VBox layout = new VBox(10, chooseSourceButton, sourceLabel, chooseTarget1Button, target1Label,
                chooseTarget2Button, target2Label, copyFilesButton, progressBar, progressLabel);
        layout.setStyle("-fx-padding: 10;");

        Scene scene = new Scene(layout, 500, 400);
        primaryStage.setTitle("Foto Organizer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void organizeAndCopyPhotos() {
        File[] files = sourceDir.listFiles();
        if (files == null || files.length == 0) {
            showAlert("Keine Fotos zum Verschieben gefunden.");
            return;
        }

        progressBar.setProgress(0);
        progressLabel.setText("Fortschritt: 0%");

        new Thread(() -> {
            int totalFiles = files.length * 2;
            int copiedFiles = 0;

            for (File file : files) {
                if (!isPhoto(file)) continue;
                try {
                    String yearMonth = getPhotoYearMonth(file);
                    if (yearMonth == null) continue;

                    Path targetPath1 = getSortedTargetPath(targetDir1, yearMonth, file);
                    Path targetPath2 = getSortedTargetPath(targetDir2, yearMonth, file);

                    Files.copy(file.toPath(), targetPath1, StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(file.toPath(), targetPath2, StandardCopyOption.REPLACE_EXISTING);
                    copiedFiles += 2;

                    double progress = (double) copiedFiles / totalFiles;
                    int percent = (int) (progress * 100);
                    Platform.runLater(() -> {
                        progressBar.setProgress(progress);
                        progressLabel.setText("Fortschritt: " + percent + "%");
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Platform.runLater(() -> showAlert("Fotos wurden sortiert und kopiert!"));
        }).start();
    }

    public static String getPhotoYearMonth(File file) {
        try {
            // ExifTool aufrufen und Datum im gewünschten Format abrufen
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "/usr/local/bin/exiftool", "-DateTimeOriginal", "-d", "%Y/%m", file.getAbsolutePath()
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Ausgabe von ExifTool lesen
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Date/Time Original")) {
                    return line.split(": ", 2)[1]; // Gibt das Datum im Format yyyy/MM zurück
                }
            }

            // Prozess beenden
            process.waitFor();
        } catch (IOException | InterruptedException e) {

            e.printStackTrace();
        }
        return null;
    }

    private Path getSortedTargetPath(File targetDir, String yearMonth, File file) throws IOException {
        Path targetFolder = Paths.get(targetDir.getAbsolutePath(), yearMonth);
        if (!Files.exists(targetFolder)) {
            Files.createDirectories(targetFolder);
        }
        return targetFolder.resolve(file.getName());
    }

    public static boolean isPhoto(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }

        // 1️⃣ Überprüfung anhand der Dateiendung
        String fileName = file.getName().toLowerCase();
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return true;
        }

        // 2️⃣ Überprüfung anhand des MIME-Typs
        try {
            String mimeType = Files.probeContentType(file.toPath());
            return mimeType != null && mimeType.startsWith("image/");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        Application.launch(ImportPhotosApplication.class, args);
    }
}
