package com.example.wander.migration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("mysql")
public class H2ToMySqlMigrationRunner implements CommandLineRunner {

    private final DataSource mysqlDataSource;

    @Value("${app.migration.h2.enabled:false}")
    private boolean enabled;

    @Value("${app.migration.h2.url:jdbc:h2:file:./data/wander_coursehub}")
    private String h2Url;

    @Value("${app.migration.h2.username:root}")
    private String h2Username;

    @Value("${app.migration.h2.password:041029}")
    private String h2Password;

    @Value("${app.migration.h2.marker:./data/h2_migrated.flag}")
    private String markerPath;

    public H2ToMySqlMigrationRunner(DataSource mysqlDataSource) {
        this.mysqlDataSource = mysqlDataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!enabled) {
            return;
        }
        Path marker = Path.of(markerPath);
        if (Files.exists(marker)) {
            return;
        }

        try (Connection h2Conn = DriverManager.getConnection(h2Url, h2Username, h2Password);
             Connection mysqlConn = mysqlDataSource.getConnection()) {
            mysqlConn.setAutoCommit(false);

            migrateTable("users", h2Conn, mysqlConn);
            migrateTable("course", h2Conn, mysqlConn);
            migrateTable("course_section", h2Conn, mysqlConn);
            migrateTable("enrollment", h2Conn, mysqlConn);
            migrateTable("learning_progress", h2Conn, mysqlConn);
            migrateTable("section_progress", h2Conn, mysqlConn);
            migrateTable("role_change_request", h2Conn, mysqlConn);
            migrateTable("notification", h2Conn, mysqlConn);
            migrateTable("admin_audit_log", h2Conn, mysqlConn);

            mysqlConn.commit();
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "migrated");
        }
    }

    private void migrateTable(String table, Connection h2Conn, Connection mysqlConn) throws SQLException {
        if (!h2TableExists(table, h2Conn)) {
            return;
        }
        if (mysqlHasRows(table, mysqlConn)) {
            return;
        }
        try (Statement stmt = h2Conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnName(i));
            }
            String insertSql = buildInsertSql(table, columns);
            try (PreparedStatement ps = mysqlConn.prepareStatement(insertSql)) {
                while (rs.next()) {
                    for (int i = 1; i <= colCount; i++) {
                        ps.setObject(i, rs.getObject(i));
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private boolean h2TableExists(String table, Connection h2Conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?";
        try (PreparedStatement ps = h2Conn.prepareStatement(sql)) {
            ps.setString(1, table.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private boolean mysqlHasRows(String table, Connection mysqlConn) throws SQLException {
        try (Statement stmt = mysqlConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            if (rs.next()) {
                return rs.getLong(1) > 0;
            }
        }
        return false;
    }

    private String buildInsertSql(String table, List<String> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(table).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(columns.get(i));
            if (i < columns.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            sb.append("?");
            if (i < columns.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
