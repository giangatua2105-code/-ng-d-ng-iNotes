package com.inotes.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Tao ket noi JDBC toi MySQL. */
public final class DBConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/inotes_db?useUnicode=true&characterEncoding=UTF-8"
                    + "&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tìm thấy MySQL JDBC driver (mysql-connector-j)", e);
        }
        String url = AppConfig.get("db.url", DEFAULT_URL);
        String user = AppConfig.get("db.user", "root");
        String password = AppConfig.get("db.password", "");
        return DriverManager.getConnection(url, user, password);
    }
}
