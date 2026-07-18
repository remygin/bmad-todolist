package com.bmad.todolist.config;

import com.bmad.todolist.user.Role;
import com.bmad.todolist.user.RoleRepository;
import com.bmad.todolist.user.User;
import com.bmad.todolist.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {

	public static final String ADMIN_ROLE = "ADMIN";

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final String adminUsername;
	private final String adminPassword;

	public AdminBootstrap(
			RoleRepository roleRepository,
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.admin.username}") String adminUsername,
			@Value("${app.admin.password}") String adminPassword) {
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		Role adminRole = roleRepository.findByName(ADMIN_ROLE)
				.orElseGet(() -> roleRepository.save(new Role(ADMIN_ROLE)));

		if (userRepository.existsByUsername(adminUsername)) {
			log.info("Admin user '{}' already exists; bootstrap skipped", adminUsername);
			return;
		}

		User admin = new User(adminUsername, passwordEncoder.encode(adminPassword));
		admin.addRole(adminRole);
		userRepository.save(admin);
		log.info("Bootstrapped admin user '{}'", adminUsername);
	}
}
