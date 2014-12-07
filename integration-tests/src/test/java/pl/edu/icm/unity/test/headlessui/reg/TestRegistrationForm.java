/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui.reg;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openqa.selenium.By;

import pl.edu.icm.unity.test.headlessui.SeleniumTestBase;

/**
 * 
 * @author P. Piernik
 */
public class TestRegistrationForm extends SeleniumTestBase
{
	@Test
	public void registrationTest() throws Exception
	{
		driver.get(baseUrl + "/admin/admin");
		driver.findElement(By.id("gwt-uid-15")).click();
		driver.findElement(By.id("IdpSelector.remoteIdp.5.")).click();
		driver.findElement(By.id("AuthenticationUI.authnenticateButton")).click();
		driver.findElement(By.id("AuthenticationUI.username")).clear();
		driver.findElement(By.id("AuthenticationUI.username")).sendKeys("demo-user");
		driver.findElement(By.id("WebPasswordRetrieval.password")).clear();
		driver.findElement(By.id("WebPasswordRetrieval.password")).sendKeys("the!test1");
		driver.findElement(By.id("AuthenticationUI.authnenticateButton")).click();
		driver.findElement(By.id("IdpButtonsBar.confirmButton")).click();
		assertTrue(driver.findElement(By.id("ValueEditor.Email")).getAttribute("value")
				.contains("x"));
		driver.findElement(By.id("ValueEditor.Email")).clear();
		driver.findElement(By.id("ValueEditor.Email")).sendKeys("test@test.com");
		assertTrue(driver.findElement(By.id("ListOfElements")) != null);
		waitForElement(By.id("AbstractDialog.confirm"));
		driver.findElement(By.id("AbstractDialog.confirm")).click();	
		//wait
		simpleWait(1000);
		waitForElement(By.id("AbstractDialog.confirm"));
		driver.findElement(By.id("AbstractDialog.confirm")).click();
		driver.findElement(By.id("AuthenticationUI.authnenticateButton")).click();
		driver.findElement(By.id("IdpButtonsBar.confirmButton")).click();
		assertTrue(driver.findElement(By.id("MainHeader.loggedAs")) != null);
		driver.findElement(By.id("MainHeader.logout"));			
	}	
}
