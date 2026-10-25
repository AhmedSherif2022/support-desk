package com.supportdesk.auth.config;

import com.supportdesk.auth.entity.Role;
import com.supportdesk.auth.entity.User;
import com.supportdesk.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")

public class DevUserSeeder implements ApplicationRunner
{

    private static final Logger log = LoggerFactory.getLogger(DevUserSeeder.class);
    private static final String DEV_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor injection: Spring passes in the two beans (no @Autowired needed with one constructor).
    public DevUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder)
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override                      // Java: this method comes from the ApplicationRunner interface.
    @Transactional                 // Spring: all three saves in ONE database transaction (all or nothing).
    public void run(ApplicationArguments args)
    {
        createIfMissing("mona", "Mona", Role.AGENT);
        createIfMissing("youssef", "Youssef", Role.GUIDE_ADMIN);
        createIfMissing("nour", "Nour", Role.REVIEWER);
    }

    private void createIfMissing(String username, String fullName, Role role)
    {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        userRepository.save(new User(username, passwordEncoder.encode(DEV_PASSWORD), fullName, role));
        log.info("Created dev user '{}' ({})", username, role);
    }
}