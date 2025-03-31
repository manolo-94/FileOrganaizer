module com.example.fileorganaizer {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.fileorganaizer to javafx.fxml;
    exports com.example.fileorganaizer;
}