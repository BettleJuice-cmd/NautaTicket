module ni.edu.uam.nautaticket {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.postgresql.jdbc;
    requires java.naming;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires static lombok;
    exports ni.edu.uam.nautaticket.util;
}
