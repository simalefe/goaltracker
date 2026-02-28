package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

public class V1__create_users_and_insert_sample_user extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement stmt = context.getConnection().createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "email VARCHAR(255) NOT NULL," +
                    "username VARCHAR(100) NOT NULL," +
                    "password_hash VARCHAR(255) NOT NULL," +
                    "display_name VARCHAR(200)," +
                    "avatar_url VARCHAR(2048)," +
                    "timezone VARCHAR(50) NOT NULL," +
                    "role VARCHAR(20) NOT NULL," +
                    "is_active BOOLEAN NOT NULL," +
                    "email_verified BOOLEAN NOT NULL," +
                    "failed_login_count INT NOT NULL," +
                    "locked_until TIMESTAMP NULL," +
                    "created_at TIMESTAMP NOT NULL," +
                    "updated_at TIMESTAMP NOT NULL," +
                    "CONSTRAINT uq_users_email UNIQUE (email)," +
                    "CONSTRAINT uq_users_username UNIQUE (username)" +
                    ")");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String password = "Password123!";
        String hash = encoder.encode(password);

        try (PreparedStatement ps = context.getConnection().prepareStatement(
                "INSERT INTO users (email, username, password_hash, display_name, avatar_url, timezone, role, is_active, email_verified, failed_login_count, locked_until, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )) {
            ps.setString(1, "demo@goaltracker.local");
            ps.setString(2, "demo");
            ps.setString(3, hash);
            ps.setString(4, "Demo User");
            ps.setString(5, null);
            ps.setString(6, "Europe/Istanbul");
            ps.setString(7, "USER");
            ps.setBoolean(8, true);
            ps.setBoolean(9, true);
            ps.setInt(10, 0);
            ps.setTimestamp(11, null);
            Timestamp now = Timestamp.from(Instant.now());
            ps.setTimestamp(12, now);
            ps.setTimestamp(13, now);
            ps.execute();
        }
    }
}

