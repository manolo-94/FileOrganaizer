package com.example.fileorganaizer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AppApplication extends Application {
    private Label labelRootFolderPath;
    private Label labelFilesFolderPath;
    private Label labelTitle;
    private Button btnSelectRootFolder;
    private Button btnSelectFilesFolder;
    private Button btnOrganizeFiles;
    private IntegerProperty filesMoved = new SimpleIntegerProperty(0);

    private File rootFolder;
    private File filesFolder;

    private Label labelLoading;

    public static void main(String[] args) {

        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Organizador de Archivos");

        // Título de la aplicación
        labelTitle = new Label("Organizador de Archivos");
        labelTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Botón y label para seleccionar carpeta raíz
        btnSelectRootFolder = new Button("Seleccione Carpeta Raíz");
        btnSelectRootFolder.setOnAction(e -> selectRootFolder(primaryStage));
        labelRootFolderPath = new Label("Ruta: Ninguna seleccionada");

        // Botón y label para seleccionar carpeta de archivos
        btnSelectFilesFolder = new Button("Seleccione Carpeta de Archivos");
        btnSelectFilesFolder.setOnAction(e -> selectFilesFolder(primaryStage));
        labelFilesFolderPath = new Label("Ruta: Ninguna seleccionada");

        // Botón para organizar archivos
        btnOrganizeFiles = new Button("Organizar Archivos");
        btnOrganizeFiles.setOnAction(e -> organizeFiles());
        btnOrganizeFiles.setDisable(true);

        labelLoading = new Label("Cargando...");
        labelLoading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        labelLoading.setVisible(false);  // Inicialmente no se muestra

        // Contenedor para los controles
        VBox vbox = new VBox(10, labelTitle, btnSelectRootFolder, labelRootFolderPath,
                btnSelectFilesFolder, labelFilesFolderPath,
                btnOrganizeFiles, labelLoading);
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-padding: 20;");



        // Área de texto para el log
        // Disposición de la escena
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(vbox);

        // Configuración de la escena
        Scene scene = new Scene(borderPane, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void selectRootFolder(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleccionar Carpeta Raíz");
        File selectedFolder = directoryChooser.showDialog(stage);
        if (selectedFolder != null) {
            rootFolder = selectedFolder;
            labelRootFolderPath.setText("Ruta: " + rootFolder.getAbsolutePath());
            checkEnableOrganizeButton();
        }
    }

    private void selectFilesFolder(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleccionar Carpeta de Archivos");
        File selectedFolder = directoryChooser.showDialog(stage);
        if (selectedFolder != null) {
            filesFolder = selectedFolder;
            labelFilesFolderPath.setText("Ruta: " + filesFolder.getAbsolutePath());
            checkEnableOrganizeButton();
        }
    }

    private void checkEnableOrganizeButton() {
        btnOrganizeFiles.setDisable(rootFolder == null || filesFolder == null);
    }

    private void organizeFiles() {
        labelLoading.setVisible(true);  // Muestra el label "Cargando..."
        Task<Void> organizeTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                organizeFilesInDirectory(rootFolder, filesFolder);
                return null;
            }
        };

        organizeTask.setOnSucceeded(event -> {
            labelLoading.setVisible(false);  // Ocultar "Cargando..." cuando termine
        });


        organizeTask.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            if (exception != null) {
                exception.printStackTrace();  // Imprimir la traza de la excepción en consola
                showError("Hubo un error al organizar los archivos: " + exception.getMessage());
            } else {
                showError("Error desconocido al organizar los archivos.");
            }
            labelLoading.setVisible(false);  // Ocultar "Cargando..." si ocurre un error
        });

        new Thread(organizeTask).start();
    }

    private void organizeFilesInDirectory(File rootFolder, File filesFolder) throws IOException {
        if (!rootFolder.exists() || !filesFolder.exists() || !rootFolder.isDirectory() || !filesFolder.isDirectory()) {
            writeErrorLog(rootFolder, "Una de las carpetas no existe.");
            showError("Una de las carpetas no existe o no es una carpeta.");
            return;
        }

        // Listar subcarpetas en la carpeta raíz
        File[] subfolders = rootFolder.listFiles(File::isDirectory);
        //System.out.println("Número de subcarpetas: " + (subfolders != null ? subfolders.length : "null"));
        if (subfolders == null || subfolders.length == 0) {
            writeErrorLog(rootFolder, "No se encontraron subcarpetas en la carpeta raíz.");
            showError("No se encontraron subcarpetas en la carpeta raíz.");
            return;
        }

        // Crear lista de nombres de carpetas limpias
        List<String> folderNames = new ArrayList<>();
        for (File subfolder : subfolders) {
            folderNames.add(cleanFileName(subfolder.getName()));
        }

        // Listar archivos en la carpeta de archivos
        File[] files = filesFolder.listFiles();
        if (files == null || files.length == 0) {
            writeErrorLog( rootFolder, "No se encontraron archivos en la carpeta de archivos.");
            showError("No se encontraron archivos en la carpeta de archivos.");
            return;
        }

        int totalFiles = files.length;
        int movedCount = 0;
        int notMovedCount = 0;
        int notFoundFolders = 0;

        //System.out.println("Total de archivos: " + totalFiles);

        // Procesar cada archivo
        for (File file : files) {
            if (file.isFile() && !file.getName().startsWith(".")) { // Omite archivos ocultos

                String originalFileName = file.getName();  // Guardar el nombre original del archivo

                // Limpiar el nombre del archivo para comparar con las carpetas
                String cleanedFileName = cleanFileName(originalFileName);  // Limpiar el nombre del archivo

                // Verificar si el nombre del archivo coincide con alguna carpeta
                if (folderNames.contains(cleanedFileName)) {
                    File targetFolder = new File(rootFolder, cleanedFileName);
                    if (!targetFolder.exists() && !targetFolder.mkdir()) {
                        notMovedCount++;
                        writeErrorLog(rootFolder, "No se pudo crear la carpeta: " + targetFolder.getAbsolutePath());
                        continue;
                    }

                    // Usar el nombre original del archivo para el destino
                    File targetFile = new File(targetFolder, originalFileName);

                    if (targetFile.exists()) {
                        String newFileName = generateDuplicateName(targetFile);
                        targetFile = new File(targetFolder, newFileName);
                        writeActionLog(rootFolder, "Archivo duplicado encontrado. Nuevo nombre: " + newFileName);
                    }

                    try {
                        Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        movedCount++;
                        writeActionLog(rootFolder, "Archivo movido: " + originalFileName + " -> " + targetFile.getAbsolutePath());
                        //System.out.println("Archivo copiado: " + targetFile.getAbsolutePath());
                    } catch (IOException e) {
                        notMovedCount++;
                        writeErrorLog(rootFolder, "Error al mover el archivo: " + originalFileName + " - " + e.getMessage());
                    }
                } else {
                    notFoundFolders++;
                    writeErrorLog(rootFolder, "No se encontró una carpeta para el archivo: " + originalFileName);
                }
            }
        }

        filesMoved.set(movedCount);
        showSuccessMessage(totalFiles, movedCount, notMovedCount, notFoundFolders);
    }

    private String cleanFileName(String name) {
        // Verificar si el nombre es válido
        if (name == null || name.isEmpty()) {
            return name;
        }

        // Eliminar prefijo 'UTM_' si existe
        if (name.startsWith("UTM_")) {
            name = name.substring(4);
        }

        // Verificar si el archivo termina con '_F' seguido de cualquier extensión
        if (name.matches(".*_F\\.[a-zA-Z0-9]+$") || name.endsWith("_F")) {
            // Buscar el penúltimo guion bajo '_'
            int secondLastUnderscoreIndex = name.lastIndexOf('_', name.lastIndexOf('_') - 1);

            // Si se encuentra un guion bajo, eliminar todo después de él
            if (secondLastUnderscoreIndex != -1) {
                name = name.substring(0, secondLastUnderscoreIndex);
            }
        }

        // Si el nombre tiene una extensión y no tiene sufijo '_F' ni prefijo 'UTM_', eliminar la extensión
        if (name.matches(".*\\.[a-zA-Z0-9]+$") && !name.endsWith("_F")) {
            name = name.substring(0, name.lastIndexOf('.'));
        }

        System.out.println("Nombre del archvivo limpio : " + name);

        return name;
    }

    private String generateDuplicateName(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');

        // Nombre base y extensión del archivo
        String baseName = (dotIndex > 0) ? name.substring(0, dotIndex) : name;
        String extension = (dotIndex > 0) ? name.substring(dotIndex) : "";

        File targetFolder = new File(file.getParent());
        int counter = 1;
        String newName;

        // Generar un nuevo nombre con un contador hasta encontrar uno que no exista
        do {
            newName = baseName + "_DUPLICADO_" + counter + extension;
            counter++;
        } while (new File(targetFolder, newName).exists());

        return newName;
    }

    public void showSuccessMessage(int totalFiles, int movedCount, int notMovedCount, int notFoundFolders) {
        String message = String.format("Total de archivos: %d\nArchivos movidos: %d\nArchivos no movidos: %d\nCarpetas no encontradas: %d",
                totalFiles, movedCount, notMovedCount, notFoundFolders);
        //System.out.println(message);
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Archivos organizados correctamente.");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();

            clearForm();
        });
    }

    private void showError(String message) {

        //if (message == null || message.isEmpty()) {
        //    message = "Se ha producido un error desconocido.";
        //}
        //System.err.println("Error: " + message);  // Mostrar el error en consola
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();  // Muestra la alerta y espera a que el usuario la cierre
            clearForm();
        });
    }

    private void clearForm() {
        // Limpiar Labels
        labelRootFolderPath.setText("");
        labelFilesFolderPath.setText("");
        labelTitle.setText("");

        // Resetear variables de archivos
        rootFolder = null;
        filesFolder = null;

        // Si usas un TextArea (descomenta si lo tienes)
        // logArea.clear();
        //System.out.println("Formulario limpiado.");
    }

    private void writeErrorLog(File rootFolder, String errorMessage) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String logFileName = "error_log_" + sdf.format(new Date()) + ".txt";  // Nombre del archivo de log
        File logFile = new File(rootFolder, logFileName);  // Usamos la ruta de la carpeta raíz

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.append(sdf.format(new Date()) + " - ERROR: " + errorMessage + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeActionLog(File rootFolder, String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String logFileName = "action_log_" + sdf.format(new Date()) + ".txt";  // Nombre del archivo de log
        File logFile = new File(rootFolder, logFileName);  // Usamos la ruta de la carpeta raíz

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.append(sdf.format(new Date()) + " - " + message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}