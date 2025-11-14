import { Page, expect } from '@playwright/test';
import { KeycloakLoginPage } from '../pages/KeycloakLoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { NavigationHelper } from '../helpers/navigation';

/**
 * Authentication fixtures for Playwright tests
 */

export interface AuthCredentials {
  username: string;
  password: string;
}

export const DEFAULT_CREDENTIALS: AuthCredentials = {
  username: 'admin',
  password: 'admin123',
};

/**
 * Perform login flow
 */
export async function login(
  page: Page,
  credentials: AuthCredentials = DEFAULT_CREDENTIALS,
  baseUrl: string = 'http://localhost:3000'
): Promise<DashboardPage> {
  const keycloakLoginPage = new KeycloakLoginPage(page);
  const navigationHelper = new NavigationHelper(page);

  // Navigate to application - should redirect to Keycloak
  await page.goto(baseUrl);
  await navigationHelper.waitForPageReady();

  // Check if redirected to Keycloak login
  const isKeycloakPage = await navigationHelper.isKeycloakLoginPage();
  
  if (isKeycloakPage) {
    // Perform login on Keycloak page
    await keycloakLoginPage.login(credentials.username, credentials.password);
    
    // Wait for redirect back to application
    await navigationHelper.waitForKeycloakRedirect(baseUrl);
  } else {
    // Already logged in or on application page
    await navigationHelper.waitForPageReady();
  }

  // Verify we're on the application (not Keycloak)
  const isOnApplication = await navigationHelper.isApplication(baseUrl);
  expect(isOnApplication).toBe(true);

  // Return dashboard page object
  const dashboardPage = new DashboardPage(page);
  
  // Wait for dashboard to load
  await dashboardPage.waitForPageLoad();
  
  // Wait a bit more for any async data loading
  await page.waitForTimeout(2000);

  return dashboardPage;
}

/**
 * Check if user is authenticated
 */
export async function isAuthenticated(page: Page, baseUrl: string = 'http://localhost:3000'): Promise<boolean> {
  const navigationHelper = new NavigationHelper(page);
  
  // Check if we're not on Keycloak login page
  const isKeycloakPage = await navigationHelper.isKeycloakLoginPage();
  
  if (isKeycloakPage) {
    return false;
  }

  // Check if we're on the application
  const isOnApplication = await navigationHelper.isApplication(baseUrl);
  
  return isOnApplication;
}

/**
 * Wait for authentication to complete
 */
export async function waitForAuth(
  page: Page,
  baseUrl: string = 'http://localhost:3000',
  timeout = 30000
): Promise<void> {
  const navigationHelper = new NavigationHelper(page);
  
  // Wait for redirect from Keycloak to application
  await navigationHelper.waitForKeycloakRedirect(baseUrl, timeout);
  
  // Wait for page to be ready
  await navigationHelper.waitForPageReady();
}

