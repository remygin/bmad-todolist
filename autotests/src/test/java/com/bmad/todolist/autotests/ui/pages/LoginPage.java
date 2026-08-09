package com.bmad.todolist.autotests.ui.pages;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

import com.bmad.todolist.autotests.config.AutotestConfig;
import com.codeborne.selenide.SelenideElement;

public class LoginPage {

	private final SelenideElement username = $("input[autocomplete='username']");
	private final SelenideElement password = $("input[autocomplete='current-password']");
	private final SelenideElement submit = $("form.auth-card button[type='submit']");

	public LoginPage openPage() {
		open(AutotestConfig.uiBaseUrl() + "/login");
		username.shouldBe(visible);
		return this;
	}

	public LoginPage fillCredentials(String user, String pass) {
		username.setValue(user);
		password.setValue(pass);
		return this;
	}

	public void submit() {
		submit.click();
	}
}
