import { test, expect } from '@playwright/test';
import { loginAsAdmin, loginAsUser } from '../fixtures/auth';
import { ROUTES, ADMIN_ONLY_ROUTES, PUBLIC_ROUTES } from '../constants/routes';
import { PermissionHelper } from '../helpers/permissions';

import { BASE_URL } from '../constants/config';

/**
 * Permission tests
 * Test admin access to all pages, user access restrictions, unauthorized redirects, permission-based UI visibility
 */
test.describe('Permissions', () => {
  test.describe('Admin Role', () => {
    test.beforeEach(async ({ page }) => {
      await loginAsAdmin(page, BASE_URL);
    });

    test('should access all public routes', async ({ page }) => {
      const permHelper = new PermissionHelper(page);
      
      for (const route of PUBLIC_ROUTES) {
        await permHelper.verifyCanAccessRoute(route);
      }
    });

    test('should access admin-only routes', async ({ page }) => {
      const permHelper = new PermissionHelper(page);
      
      for (const route of ADMIN_ONLY_ROUTES) {
        await permHelper.verifyCanAccessRoute(route);
      }
    });

    test('should see admin-only UI elements', async ({ page }) => {
      // Navigate to a page with admin features
      await page.goto(`${BASE_URL}${ROUTES.DASHBOARD}`);
      await page.waitForLoadState('networkidle');
      
      // Check for IAM menu in sidebar (admin only)
      const iamMenu = page.locator('nav:has-text("IAM"), aside:has-text("IAM"), nav a[href*="/iam"]');
      const iamCount = await iamMenu.count();
      
      // Admin should see IAM menu
      expect(iamCount).toBeGreaterThan(0);
    });
  });

  test.describe('User Role', () => {
    test.beforeEach(async ({ page }) => {
      await loginAsUser(page, BASE_URL);
    });

    test('should access public routes', async ({ page }) => {
      const permHelper = new PermissionHelper(page);
      
      for (const route of PUBLIC_ROUTES) {
        await permHelper.verifyCanAccessRoute(route);
      }
    });

    test('should be redirected from admin-only routes', async ({ page }) => {
      const permHelper = new PermissionHelper(page);
      
      for (const route of ADMIN_ONLY_ROUTES) {
        await permHelper.verifyCannotAccessRoute(route);
      }
    });

    test('should not see admin-only UI elements', async ({ page }) => {
      await page.goto(`${BASE_URL}${ROUTES.DASHBOARD}`);
      await page.waitForLoadState('networkidle');
      
      // IAM menu should not be visible or accessible
      const iamLink = page.locator('nav a[href="/iam/users"], nav a[href="/iam/teams"]');
      const iamVisible = await iamLink.count();
      
      // User should not see IAM links
      expect(iamVisible).toBe(0);
    });

    test('should see unauthorized page when accessing admin routes directly', async ({ page }) => {
      await page.goto(`${BASE_URL}${ROUTES.IAM_USERS}`);
      await page.waitForTimeout(2000);
      
      const url = page.url();
      expect(url).toContain('/unauthorized');
      
      // Verify unauthorized message is shown
      const bodyText = await page.locator('body').textContent();
      expect(bodyText).toMatch(/unauthorized|access denied/i);
    });
  });
});

