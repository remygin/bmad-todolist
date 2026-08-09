package com.bmad.todolist.autotests.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AutotestConfig {

	private static final Properties PROPS = load();

	private AutotestConfig() {
	}

	public static String uiBaseUrl() {
		return value("UI_BASE_URL", "ui.baseUrl");
	}

	public static String apiBaseUrl() {
		return value("API_BASE_URL", "api.baseUrl");
	}

	public static String adminUsername() {
		return value("ADMIN_USERNAME", "admin.username");
	}

	public static String adminPassword() {
		return value("ADMIN_PASSWORD", "admin.password");
	}

	public static boolean selenideHeadless() {
		return Boolean.parseBoolean(value("SELENIDE_HEADLESS", "selenide.headless"));
	}

	public static String selenideBrowser() {
		return value("SELENIDE_BROWSER", "selenide.browser");
	}

	private static String value(String envKey, String propKey) {
		String fromEnv = System.getenv(envKey);
		if (fromEnv != null && !fromEnv.isBlank()) {
			return fromEnv.trim();
		}
		String fromSys = System.getProperty(propKey);
		if (fromSys != null && !fromSys.isBlank()) {
			return fromSys.trim();
		}
		return PROPS.getProperty(propKey);
	}

	private static Properties load() {
		Properties properties = new Properties();
		try (InputStream in = AutotestConfig.class.getClassLoader().getResourceAsStream("autotest.properties")) {
			if (in == null) {
				throw new IllegalStateException("autotest.properties not found on classpath");
			}
			properties.load(in);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to load autotest.properties", e);
		}
		return properties;
	}
}
