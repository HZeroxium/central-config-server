import { Page } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

/**
 * Service Share List Page Object
 */
export class ServiceShareListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to service shares list
   */
  async goto(): Promise<void> {
    await this.goto('/service-shares');
    await this.waitForListLoad();
  }

  /**
   * Filter by permission
   */
  async filterByPermission(permission: string): Promise<void> {
    await this.interactWithFilter('Permission', permission);
  }
}

