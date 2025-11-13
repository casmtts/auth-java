package com.example.auth_java.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.auth_java.entities.User;
import com.example.auth_java.repositories.UserRepository;

@Configuration
public class ApplicationConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class);

    private final UserRepository userRepository;

    public ApplicationConfiguration(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Development helper: prints configured default Spring Security user
     * credentials (from `spring.security.user.name` and
     * `spring.security.user.password`) to the application log at startup.
     * Useful when you expect to see the password in the console.
     */
    @Bean
    public CommandLineRunner printDefaultUser(Environment env) {
        return args -> {
            String name = env.getProperty("spring.security.user.name");
            String password = env.getProperty("spring.security.user.password");
            if (name != null && password != null) {
                logger.info("Default security user configured -> {} / {}", name, password);
            } else if (name != null) {
                logger.info("Default security user configured -> {} (password not set)", name);
            } else {
                logger.debug("No default Spring Security user configured via properties.");
            }
        };
    }

    /**
     * Dev helper: seed a default user in the database using the properties
     * `spring.security.user.name` and `spring.security.user.password` if it
     * does not already exist. Password is encoded with BCrypt.
     */
    @Bean
    public CommandLineRunner createDefaultUser(UserRepository userRepository, Environment env) {
        return args -> {
            String name = env.getProperty("spring.security.user.name");
            String password = env.getProperty("spring.security.user.password");

            if (name == null || password == null) {
                return; // nothing to do
            }

            var maybe = userRepository.findByEmail(name);
            if (maybe.isPresent()) {
                logger.debug("Default user already exists in database: {}", name);
                return;
            }

            User u = new User();
            u.setEmail(name);
            u.setPassword(passwordEncoder().encode(password));
            u.setFullName("Default User");

            userRepository.save(u);
            logger.info("Seeded default user in database -> {}", name);
        };
    }
}
