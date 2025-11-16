import { test as base, Page } from '@playwright/test';
import { loginAsAdmin, loginAsUser } from './auth';
import { DashboardPage } from '../pages/DashboardPage';

/**
 * Role-based test fixtures
 * Provides authenticated page context for admin and user roles
 */

type Role = 'admin' | 'user';

interface RoleBasedFixtures {
  authenticatedPage: Page;
  role: Role;
  dashboardPage: DashboardPage;
}

/**
 * Base test with role support
 */
export const test = base.extend<RoleBasedFixtures>({
  // Default role from project name
  role: ['admin', { option: true }] as any,
  
  // Authenticated page based on role
  authenticatedPage: async ({ page, role }, use) => {
    const { BASE_URL: baseUrl } = await import('../constants/config');
    
    // Login based on role
    if (role === 'admin') {
      await loginAsAdmin(page, baseUrl);
    } else {
      await loginAsUser(page, baseUrl);
    }
    
    await use(page);
  },
  
  // Dashboard page object
  dashboardPage: async ({ authenticatedPage }, use) => {
    const dashboardPage = new DashboardPage(authenticatedPage);
    await use(dashboardPage);
  },
});

export { expect } from '@playwright/test';

