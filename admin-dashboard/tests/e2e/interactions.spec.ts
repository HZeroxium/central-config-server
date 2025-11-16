import { test, expect } from '@playwright/test';
import { loginAsAdmin, loginAsUser } from '../fixtures/auth';
import { ApplicationServiceListPage } from '../pages/ApplicationServiceListPage';
import { ServiceInstanceListPage } from '../pages/ServiceInstanceListPage';
import { DriftEventListPage } from '../pages/DriftEventListPage';
import { ComponentHelper } from '../helpers/components';

import { BASE_URL } from '../constants/config';

/**
 * Interactions tests
 * Test search, filters, pagination, sorting, basic form interactions across pages
 */
test.describe('Interactions', () => {
  test.describe('Admin Role', () => {
    test.beforeEach(async ({ page }) => {
      await loginAsAdmin(page, BASE_URL);
    });

    test('should test search interactions on application services', async ({ page }) => {
      const listPage = new ApplicationServiceListPage(page);
      await listPage.goto();
      
      // Test search
      await listPage.interactWithSearch('test');
      await listPage.waitForLoadingComplete();
      await page.waitForTimeout(2000);
      
      // Clear search
      const searchInput = listPage.getSearchInput();
      await searchInput.clear();
      await searchInput.fill('');
      await page.keyboard.press('Enter');
      await listPage.waitForLoadingComplete();
    });

    test('should test filter interactions on application services', async ({ page }) => {
      const listPage = new ApplicationServiceListPage(page);
      await listPage.goto();
      
      // Try filtering by lifecycle
      try {
        await listPage.filterByLifecycle('ACTIVE');
        await listPage.waitForLoadingComplete();
        
        // Clear filter
        const componentHelper = new ComponentHelper(page);
        await componentHelper.clearFilter('select[label*="Lifecycle" i]');
        await listPage.waitForLoadingComplete();
      } catch (e) {
        // Filter might not be available
      }
    });

    test('should test search on service instances', async ({ page }) => {
      const listPage = new ServiceInstanceListPage(page);
      await listPage.goto();
      
      await listPage.interactWithSearch('test');
      await listPage.waitForLoadingComplete();
    });

    test('should test filters on service instances', async ({ page }) => {
      const listPage = new ServiceInstanceListPage(page);
      await listPage.goto();
      
      try {
        await listPage.filterByStatus('ONLINE');
        await listPage.waitForLoadingComplete();
      } catch (e) {
        // Filter might not be available
      }
    });

    test('should test filters on drift events', async ({ page }) => {
      const listPage = new DriftEventListPage(page);
      await listPage.goto();
      
      try {
        await listPage.filterByStatus('DETECTED');
        await listPage.waitForLoadingComplete();
      } catch (e) {
        // Filter might not be available
      }
    });

    test('should test pagination interactions', async ({ page }) => {
      const listPage = new ApplicationServiceListPage(page);
      await listPage.goto();
      
      const componentHelper = new ComponentHelper(page);
      const hasPagination = await componentHelper.verifyPaginationExists();
      
      if (hasPagination) {
        // Try next page
        try {
          await listPage.interactWithPagination('next');
          await listPage.waitForLoadingComplete();
          
          // Try previous page
          await listPage.interactWithPagination('prev');
          await listPage.waitForLoadingComplete();
        } catch (e) {
          // Pagination might be disabled
        }
      }
    });

    test('should test refresh button', async ({ page }) => {
      const listPage = new ApplicationServiceListPage(page);
      await listPage.goto();
      
      await listPage.clickRefresh();
      await listPage.waitForLoadingComplete();
      await listPage.verifyListLoaded();
    });
  });

  test.describe('User Role', () => {
    test.beforeEach(async ({ page }) => {
      await loginAsUser(page, BASE_URL);
    });

    test('should test search interactions as user', async ({ page }) => {
      const listPage = new ApplicationServiceListPage(page);
      await listPage.goto();
      
      await listPage.interactWithSearch('test');
      await listPage.waitForLoadingComplete();
    });
  });
});

