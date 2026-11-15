
module ni.edu.uam.nautaticket {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.net.http;
    requires org.postgresql.jdbc;
    requires java.naming;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires jbcrypt;
    requires static lombok;
    requires com.fasterxml.jackson.databind;

    requires net.sf.jasperreports.core;
    requires net.sf.jasperreports.pdf;
    requires net.sf.jasperreports.jdt;

    exports ni.edu.uam.nautaticket to javafx.graphics;
    exports ni.edu.uam.nautaticket.util;

    opens ni.edu.uam.nautaticket.dto
            to net.sf.jasperreports.core;

    opens ni.edu.uam.nautaticket.controller to javafx.fxml;
    opens ni.edu.uam.nautaticket.model to org.hibernate.orm.core;
    opens ni.edu.uam.nautaticket.dto to com.fasterxml.jackson.databind;
}
