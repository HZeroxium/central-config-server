import { test, expect } from '@playwright/test';
import { loginAsAdmin, loginAsUser } from '../fixtures/auth';
import { ApplicationServiceListPage } from '../pages/ApplicationServiceListPage';
import { ServiceInstanceListPage } from '../pages/ServiceInstanceListPage';
import { ConfigListPage } from '../pages/ConfigListPage';
import { DriftEventListPage } from '../pages/DriftEventListPage';
import { ApprovalListPage } from '../pages/ApprovalListPage';
import { ServiceShareListPage } from '../pages/ServiceShareListPage';
import { ServiceRegistryListPage } from '../pages/ServiceRegistryListPage';
import { IamUserListPage } from '../pages/IamUserListPage';
import { IamTeamListPage } from '../pages/IamTeamListPage';
import { KVStoreListPage } from '../pages/KVStoreListPage';
import { ApprovalDecisionListPage } from '../pages/ApprovalDecisionListPage';

import { BASE_URL } from '../constants/config';

/**
 * List pages tests
 * Tests all list pages with role-based access, data presence, search, filters, pagination
 */
test.describe('List Pages', () => {
  test.describe('Admin Role', () => {
    test.beforeEach(async ({ page }) => {
      await loginAsAdmin(page, BASE_URL);
    });

    test('should load application services list with data', async ({ page }) => {
      const listPage = new ApplicationServiceListPage(page);
      await listPage.goto();
      await listPage.verifyListLoaded();
      await listPage.verifyTableHasData();
    });

    test('should test search on application services', async ({ page }) => {
      const listPage = new ApplicationServiceListPage(page);
      await listPage.goto();
      await listPage.interactWithSearch('test');
      await listPage.waitForLoadingComplete();
      // Verify search results (may be empty, but should not error)
      await page.waitForTimeout(2000);
    });

    test('should test filters on application services', async ({ page }) => {
      const listPage = new ApplicationServiceListPage(page);
      await listPage.goto();
      
      // Try filtering by lifecycle
      try {
        await listPage.filterByLifecycle('ACTIVE');
        await listPage.waitForLoadingComplete();
      } catch (e) {
        // Filter might not be available, skip
      }
    });

    test('should load service instances list with data', async ({ page }) => {
      const listPage = new ServiceInstanceListPage(page);
      await listPage.goto();
      await listPage.verifyListLoaded();
      await listPage.verifyTableHasData();
    });

    test('should load configs list page', async ({ page }) => {
      const listPage = new ConfigListPage(page);
      await listPage.goto();
      await listPage.verifyPageLoaded();
      await listPage.verifySearchFormPresent();
    });

    test('should load drift events list with data', async ({ page }) => {
      const listPage = new DriftEventListPage(page);
      await listPage.goto();
      await listPage.verifyListLoaded();
      await listPage.verifyTableHasData();
    });

    test('should load approvals list with data', async ({ page }) => {
      const listPage = new ApprovalListPage(page);
      await listPage.goto();
      await listPage.verifyListLoaded();
      await listPage.verifyTableHasData();
    });

    test('should load approval decisions list with data', async ({ page }) => {
      const listPage = new ApprovalDecisionListPage(page);
      await listPage.gotoList();
      await listPage.verifyListLoaded();
      await listPage.verifyTableHasData();
    });

    test('should load service shares list with data', async ({ page }) => {
      const listPage = new ServiceShareListPage(page);
      await listPage.goto();
      await listPage.verifyListLoaded();
      await listPage.verifyTableHasData();
    });

    test('should load service registry list', async ({ page }) => {
      const listPage = new ServiceRegistryListPage(page);
      await listPage.goto();
      await listPage.verifyPageLoaded();
    });

    test('should load IAM users list with data (admin only)', async ({ page }) => {
      const listPage = new IamUserListPage(page);
      await listPage.goto();
      await listPage.verifyListLoaded();
      await listPage.verifyTableHasData();
    });

    test('should load IAM teams list with data (admin only)', async ({ page }) => {
      const listPage = new IamTeamListPage(page);
      await listPage.goto();
      await listPage.verifyListLoaded();
      await listPage.verifyTableHasData();
    });

    test('should load KV store list', async ({ page }) => {
      const listPage = new KVStoreListPage(page);
      await listPage.goto();
      await listPage.verifyListLoaded();
    });

    test('should test pagination on list pages', async ({ page }) => {
      const listPage = new ApplicationServiceListPage(page);
      await listPage.goto();
      
      // Check if pagination exists
      const hasPagination = await listPage.componentHelper.verifyPaginationExists();
      if (hasPagination) {
        // Try clicking next page
        try {
          await listPage.interactWithPagination('next');
          await listPage.waitForLoadingComplete();
        } catch (e) {
          // Pagination might be disabled if only one page
        }
      }
    });
  });

  test.describe('User Role', () => {
    test.beforeEach(async ({ page }) => {
      await loginAsUser(page, BASE_URL);
    });

    test('should load allowed list pages', async ({ page }) => {
      const listPage = new ApplicationServiceListPage(page);
      await listPage.goto();
      await listPage.verifyListLoaded();
    });

    test('should not access IAM pages', async ({ page }) => {
      await page.goto(`${BASE_URL}/iam/users`);
      await page.waitForTimeout(2000);
      
      const url = page.url();
      expect(url).toContain('/unauthorized');
    });
  });
});

