module ni.edu.uam.nautaticket {
    requires javafx.controls;
    requires javafx.fxml;


    opens ni.edu.uam.nautaticket to javafx.fxml;
    exports ni.edu.uam.nautaticket;
}