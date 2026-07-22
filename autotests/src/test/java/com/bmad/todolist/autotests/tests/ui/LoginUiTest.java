package com.bmad.todolist.autotests.tests.ui;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.bmad.todolist.autotests.config.AutotestConfig;
import com.bmad.todolist.autotests.config.StackAvailability;
import com.bmad.todolist.autotests.ui.SelenideSetup;
import com.bmad.todolist.autotests.ui.pages.LoginPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginUiTest extends SelenideSetup {

	@BeforeEach
	void requireUi() {
		assumeTrue(StackAvailability.uiReachable(),
				() -> "UI недоступен: поднимите frontend (" + AutotestConfig.uiBaseUrl() + ")");
	}

	@Test
	void loginFormIsVisible() {
		new LoginPage().openPage();

		$("form.auth-card button[type='submit']").shouldBe(visible);
	}
}
