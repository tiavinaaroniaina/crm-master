package site.easy.to.build.crm.service.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public void restoreDatabase() throws Exception {
        // Disable foreign key checks
        try {
            // Set foreign key checks to 0 (disable)
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0;");
            
            // Truncate all tables except the ones mentioned
            String[] tablesToExclude = {"oauth_users", "users", "user_profite", "roles", "user_roles"};
            // Get all tables in the database (this query may vary depending on your DBMS)
            String query = "SHOW TABLES";
            jdbcTemplate.query(query, (rs) -> {
                while (rs.next()) {
                    String tableName = rs.getString(1);
                    boolean shouldExclude = false;
                    for (String excludedTable : tablesToExclude) {
                        if (tableName.equals(excludedTable)) {
                            shouldExclude = true;
                            break;
                        }
                    }
                    if (!shouldExclude) {
                        // Truncate table if it's not in the exclusion list
                        jdbcTemplate.execute("TRUNCATE TABLE " + tableName);
                    }
                }
                return null;
            });

            // Your database restore logic here (you can restore from a SQL file, backup, etc.)
            // For example, you can call a method to restore the database from a backup file (SQL dump)
            // Example: restoreFromBackup("path_to_backup_file");

        } catch (Exception e) {
            throw new Exception("Database restore failed: " + e.getMessage());
        } finally {
            // Reset foreign key checks to 1 (enable)
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1;");
        }
    }

}