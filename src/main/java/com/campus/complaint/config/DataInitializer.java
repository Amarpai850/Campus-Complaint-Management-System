package com.campus.complaint.config;

import com.campus.complaint.entity.Category;
import com.campus.complaint.entity.Role;
import com.campus.complaint.entity.User;
import com.campus.complaint.repository.CategoryRepository;
import com.campus.complaint.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

/**
 * Initializes default data on first run:
 * - Default admin account (secure in production via ADMIN_INITIAL_PASSWORD)
 * - Default maintenance staff account (secure in production via STAFF_INITIAL_PASSWORD)
 * - Default complaint categories
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
                                      CategoryRepository categoryRepository,
                                      PasswordEncoder passwordEncoder,
                                      Environment environment) {
        return args -> {
            boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

            // --- ADMIN ACCOUNT INITIALIZATION ---
            if (!userRepository.existsByUsername("admin")) {
                String adminPassword = System.getenv("ADMIN_INITIAL_PASSWORD");
                if (adminPassword == null || adminPassword.isBlank()) {
                    if (isProd) {
                        throw new IllegalStateException(
                            "PRODUCTION SECURITY ERROR: The 'ADMIN_INITIAL_PASSWORD' environment variable must be set " +
                            "to initialize the admin account in production. Default development credentials ('admin123') " +
                            "are strictly prohibited in the production profile."
                        );
                    }
                    adminPassword = "admin123";
                    log.warn("DEVELOPMENT NOTICE: Default dev credentials ('admin123') used for admin account.");
                }

                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setEmail("admin@campus.edu");
                admin.setFullName("System Administrator");
                admin.setRole(Role.ADMIN);
                admin.setDepartment("Administration");
                userRepository.save(admin);
                log.info("Admin account initialized (username: admin)");
            }

            // --- MAINTENANCE STAFF ACCOUNT INITIALIZATION ---
            if (!userRepository.existsByUsername("staff1")) {
                String staffPassword = System.getenv("STAFF_INITIAL_PASSWORD");
                if (staffPassword == null || staffPassword.isBlank()) {
                    if (isProd) {
                        throw new IllegalStateException(
                            "PRODUCTION SECURITY ERROR: The 'STAFF_INITIAL_PASSWORD' environment variable must be set " +
                            "to initialize the staff account in production. Default development credentials ('staff123') " +
                            "are strictly prohibited in the production profile."
                        );
                    }
                    staffPassword = "staff123";
                    log.warn("DEVELOPMENT NOTICE: Default dev credentials ('staff123') used for staff account.");
                }

                User staff = new User();
                staff.setUsername("staff1");
                staff.setPassword(passwordEncoder.encode(staffPassword));
                staff.setEmail("staff1@campus.edu");
                staff.setFullName("John Maintenance");
                staff.setRole(Role.MAINTENANCE_STAFF);
                staff.setDepartment("Maintenance");
                userRepository.save(staff);
                log.info("Maintenance staff account initialized (username: staff1)");
            }

            // Create default categories if none exist
            if (categoryRepository.count() == 0) {
                categoryRepository.save(new Category("Plumbing", "Water leaks, pipe issues, drainage problems"));
                categoryRepository.save(new Category("Electrical", "Power outages, faulty wiring, lighting issues"));
                categoryRepository.save(new Category("Cleanliness", "Unclean areas, garbage disposal, sanitation"));
                categoryRepository.save(new Category("Furniture", "Broken desks, chairs, or other furniture"));
                categoryRepository.save(new Category("IT/Network", "Internet issues, computer problems, projector issues"));
                categoryRepository.save(new Category("Civil", "Building cracks, broken windows, wall damage"));
                categoryRepository.save(new Category("Other", "Any other campus-related complaints"));
                log.info("Default complaint categories created");
            }
        };
    }
}
