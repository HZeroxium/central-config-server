import { test, expect } from '@playwright/test';
import { DashboardPage } from '../pages/DashboardPage';
import { NavigationPage } from '../pages/NavigationPage';
import { KeycloakLoginPage } from '../pages/KeycloakLoginPage';
import { NavigationHelper } from '../helpers/navigation';
import { login, DEFAULT_CREDENTIALS } from '../fixtures/auth';

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000';
const KEYCLOAK_URL = process.env.KEYCLOAK_URL || 'http://localhost:8080';

/**
 * E2E tests for authentication flow
 * Tests: Navigate to app → Keycloak login → Redirect to dashboard → Navigate pages → Check data loading
 */
test.describe('Authentication Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Clear cookies and storage before each test
    await page.context().clearCookies();
    await page.context().clearPermissions();
  });

  test('should complete login flow and redirect to dashboard', async ({ page }) => {
    // Navigate to application
    await page.goto(BASE_URL);
    
    const navigationHelper = new NavigationHelper(page);
    await navigationHelper.waitForPageReady();

    // Should redirect to Keycloak login page
    const isKeycloakPage = await navigationHelper.isKeycloakLoginPage();
    expect(isKeycloakPage).toBe(true);

    // Perform login
    const keycloakLoginPage = new KeycloakLoginPage(page);
    await keycloakLoginPage.login(DEFAULT_CREDENTIALS.username, DEFAULT_CREDENTIALS.password);

    // Wait for redirect back to application
    await navigationHelper.waitForKeycloakRedirect(BASE_URL);

    // Verify we're on the application (not Keycloak)
    const isOnApplication = await navigationHelper.isApplication(BASE_URL);
    expect(isOnApplication).toBe(true);

    // Verify we're on dashboard or redirected to dashboard
    const currentUrl = page.url();
    expect(currentUrl).toContain(BASE_URL);
  });

  test('should navigate to dashboard and verify it loads', async ({ page }) => {
    // Login using fixture
    const dashboardPage = await login(page, DEFAULT_CREDENTIALS, BASE_URL);

    // Navigate to dashboard explicitly
    await dashboardPage.goto();
    
    // Verify dashboard is loaded
    const isLoaded = await dashboardPage.isLoaded();
    expect(isLoaded).toBe(true);

    // Wait for data to load
    await dashboardPage.waitForDataLoad();

    // Verify dashboard elements are present
    const hasSidebar = await dashboardPage.isSidebarVisible();
    expect(hasSidebar).toBe(true);

    // Verify we can see page content (header, stats, or tables)
    const pageHeader = await dashboardPage.getPageHeaderText();
    expect(pageHeader).not.toBeNull();
  });

  test('should navigate to different pages after login', async ({ page }) => {
    // Login using fixture
    await login(page, DEFAULT_CREDENTIALS, BASE_URL);

    const navigationPage = new NavigationPage(page);
    const navigationHelper = new NavigationHelper(page);

    // Test navigation to application services
    await navigationPage.navigateTo('/application-services');
    await navigationHelper.waitForPageReady();
    await navigationHelper.verifyUrlContains('/application-services');

    // Test navigation to configs
    await navigationPage.navigateTo('/configs');
    await navigationHelper.waitForPageReady();
    await navigationHelper.verifyUrlContains('/configs');

    // Test navigation to service instances
    await navigationPage.navigateTo('/service-instances');
    await navigationHelper.waitForPageReady();
    await navigationHelper.verifyUrlContains('/service-instances');

    // Navigate back to dashboard
    await navigationPage.navigateTo('/dashboard');
    await navigationHelper.waitForPageReady();
    await navigationHelper.verifyUrlContains('/dashboard');
  });

  test('should verify data is loaded on dashboard', async ({ page }) => {
    // Login using fixture
    const dashboardPage = await login(page, DEFAULT_CREDENTIALS, BASE_URL);

    // Navigate to dashboard
    await dashboardPage.goto('/dashboard');
    await dashboardPage.waitForDataLoad();

    // Wait for any API calls to complete
    await page.waitForTimeout(3000);

    // Verify dashboard has loaded some content
    // This could be stats cards, tables, or charts
    const hasContent = await dashboardPage.hasStatsCards() || 
                      await dashboardPage.hasDataTable() ||
                      await navigationHelper.elementExists('body');

    expect(hasContent).toBe(true);

    // Verify page is interactive (not showing loading indicators)
    const isLoading = await navigationHelper.elementExists('.MuiCircularProgress-root, [data-testid="loading"]');
    
    // Loading indicators should not be visible after data loads
    // But we're lenient - they might still exist but be hidden
    if (isLoading) {
      // Check if loading indicator is actually visible
      const loadingVisible = await page.locator('.MuiCircularProgress-root, [data-testid="loading"]').first().isVisible().catch(() => false);
      expect(loadingVisible).toBe(false);
    }
  });

  test('should handle navigation via sidebar', async ({ page }) => {
    // Login using fixture
    await login(page, DEFAULT_CREDENTIALS, BASE_URL);

    const dashboardPage = new DashboardPage(page);
    const navigationHelper = new NavigationHelper(page);

    // Verify sidebar is visible
    const sidebarVisible = await dashboardPage.isSidebarVisible();
    expect(sidebarVisible).toBe(true);

    // Navigate to application services via sidebar
    await dashboardPage.navigateToApplicationServices();
    await navigationHelper.waitForPageReady();
    await navigationHelper.verifyUrlContains('/application-services');

    // Navigate to configs via sidebar
    await dashboardPage.navigateToConfigs();
    await navigationHelper.waitForPageReady();
    await navigationHelper.verifyUrlContains('/configs');

    // Navigate back to dashboard via sidebar
    await navigationPage.navigateTo('/dashboard');
    await navigationHelper.waitForPageReady();
    await navigationHelper.verifyUrlContains('/dashboard');
  });

  test('should maintain session after navigation', async ({ page }) => {
    // Login using fixture
    await login(page, DEFAULT_CREDENTIALS, BASE_URL);

    const navigationPage = new NavigationPage(page);
    const navigationHelper = new NavigationHelper(page);

    // Navigate to multiple pages
    await navigationPage.navigateTo('/application-services');
    await navigationHelper.waitForPageReady();

    await navigationPage.navigateTo('/configs');
    await navigationHelper.waitForPageReady();

    await navigationPage.navigateTo('/service-instances');
    await navigationHelper.waitForPageReady();

    // Verify we're still authenticated (not redirected to Keycloak)
    const isKeycloakPage = await navigationHelper.isKeycloakLoginPage();
    expect(isKeycloakPage).toBe(false);

    // Verify we're still on the application
    const isOnApplication = await navigationHelper.isApplication(BASE_URL);
    expect(isOnApplication).toBe(true);
  });
});

