package site.easy.to.build.crm.service.db;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseResetService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public void resetDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");

        try {
            List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME NOT IN ('oauth_users', 'users', 'user_profile', 'roles', 'user_roles', 'alerte_rate')",
                String.class
            );

            for (String table : tables) 
            { jdbcTemplate.execute("TRUNCATE TABLE " + table); }

        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }
}