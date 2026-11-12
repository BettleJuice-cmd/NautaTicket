module ni.edu.uam.nautaticket {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.postgresql.jdbc;
    requires java.naming;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires jbcrypt;
    requires static lombok;

    exports ni.edu.uam.nautaticket to javafx.graphics;
    exports ni.edu.uam.nautaticket.util;
    opens ni.edu.uam.nautaticket.controller to javafx.fxml;
    opens ni.edu.uam.nautaticket.model to org.hibernate.orm.core;
}
