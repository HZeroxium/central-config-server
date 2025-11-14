import { Page } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Dashboard page object
 * Handles dashboard page interactions and assertions
 */
export class DashboardPage extends BasePage {
  // Common selectors
  readonly pageHeader = () => this.page.locator('h1, h2, h3, h4, h5, h6').first();
  readonly sidebar = () => this.page.locator('[data-testid="sidebar"], nav, aside').first();
  readonly loadingIndicator = () => this.page.locator('[data-testid="loading"], .MuiCircularProgress-root, .MuiSkeleton-root');
  
  // Dashboard-specific selectors
  readonly statsCards = () => this.page.locator('[data-testid="stats-card"], .MuiCard-root');
  readonly charts = () => this.page.locator('[data-testid="chart"], canvas, svg');
  readonly dataTable = () => this.page.locator('[data-testid="data-table"], table, .MuiDataGrid-root');
  
  // Navigation links in sidebar
  readonly dashboardLink = () => this.page.locator('a[href="/dashboard"], a[href="/"]');
  readonly applicationServicesLink = () => this.page.locator('a[href="/application-services"]');
  readonly configsLink = () => this.page.locator('a[href="/configs"]');
  readonly serviceInstancesLink = () => this.page.locator('a[href="/service-instances"]');

  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to dashboard
   */
  async gotoDashboard(): Promise<void> {
    await this.goto('/dashboard');
    await this.waitForPageLoad();
  }

  /**
   * Check if dashboard is loaded
   */
  async isLoaded(): Promise<boolean> {
    // Check for common dashboard elements
    const hasHeader = await this.isVisible('h1, h2, h3, h4, h5, h6');
    const hasSidebar = await this.isVisible('[data-testid="sidebar"], nav, aside');
    return hasHeader && hasSidebar;
  }

  /**
   * Wait for dashboard data to load
   */
  async waitForDataLoad(): Promise<void> {
    // Wait for loading indicators to disappear
    await this.page.waitForSelector('[data-testid="loading"], .MuiCircularProgress-root', {
      state: 'hidden',
      timeout: 10000,
    }).catch(() => {
      // Loading indicator might not exist, which is fine
    });
    
    // Wait for at least one stats card or data element
    await this.page.waitForSelector('[data-testid="stats-card"], .MuiCard-root, table, .MuiDataGrid-root', {
      timeout: 15000,
    }).catch(() => {
      // Some pages might not have these elements
    });
  }

  /**
   * Check if stats cards are visible
   */
  async hasStatsCards(): Promise<boolean> {
    const count = await this.statsCards().count();
    return count > 0;
  }

  /**
   * Get number of stats cards
   */
  async getStatsCardCount(): Promise<number> {
    return await this.statsCards().count();
  }

  /**
   * Check if data table is visible
   */
  async hasDataTable(): Promise<boolean> {
    return await this.isVisible('table, .MuiDataGrid-root');
  }

  /**
   * Navigate to application services page
   */
  async navigateToApplicationServices(): Promise<void> {
    await this.applicationServicesLink().click();
    await this.waitForNavigation();
  }

  /**
   * Navigate to configs page
   */
  async navigateToConfigs(): Promise<void> {
    await this.configsLink().click();
    await this.waitForNavigation();
  }

  /**
   * Navigate to service instances page
   */
  async navigateToServiceInstances(): Promise<void> {
    await this.serviceInstancesLink().click();
    await this.waitForNavigation();
  }

  /**
   * Navigate to dashboard
   */
  async navigateToDashboard(): Promise<void> {
    await this.dashboardLink().click();
    await this.waitForNavigation();
  }

  /**
   * Get page header text
   */
  async getPageHeaderText(): Promise<string | null> {
    return await this.pageHeader().textContent();
  }

  /**
   * Check if sidebar navigation is visible
   */
  async isSidebarVisible(): Promise<boolean> {
    return await this.isVisible('[data-testid="sidebar"], nav, aside');
  }
}

