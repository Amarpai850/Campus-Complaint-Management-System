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
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Initializes default data on first run:
 * - Default admin account
 * - Default maintenance staff account
 * - Default complaint categories
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
                                      CategoryRepository categoryRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            // Create default admin if not present
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setEmail("admin@campus.edu");
                admin.setFullName("System Administrator");
                admin.setRole(Role.ADMIN);
                admin.setDepartment("Administration");
                userRepository.save(admin);
                log.info("Default admin account created (username: admin, password: admin123)");
            }

            // Create default maintenance staff if not present
            if (!userRepository.existsByUsername("staff1")) {
                User staff = new User();
                staff.setUsername("staff1");
                staff.setPassword(passwordEncoder.encode("staff123"));
                staff.setEmail("staff1@campus.edu");
                staff.setFullName("John Maintenance");
                staff.setRole(Role.MAINTENANCE_STAFF);
                staff.setDepartment("Maintenance");
                userRepository.save(staff);
                log.info("Default maintenance staff account created (username: staff1, password: staff123)");
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
