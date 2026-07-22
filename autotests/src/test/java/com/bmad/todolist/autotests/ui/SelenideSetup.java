package com.bmad.todolist.autotests.ui;

import com.bmad.todolist.autotests.config.AutotestConfig;
import com.codeborne.selenide.Configuration;
import org.junit.jupiter.api.BeforeAll;

public abstract class SelenideSetup {

	@BeforeAll
	static void configureSelenide() {
		Configuration.baseUrl = AutotestConfig.uiBaseUrl();
		Configuration.browser = AutotestConfig.selenideBrowser();
		Configuration.headless = AutotestConfig.selenideHeadless();
		Configuration.timeout = 10_000;
		Configuration.pageLoadTimeout = 20_000;
	}
}
