import { Page } from '@playwright/test';
import { BaseListPage } from './BaseListPage';
import { SELECTORS } from '../constants/selectors';

/**
 * Application Service List Page Object
 */
export class ApplicationServiceListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to application services list
   */
  async goto(): Promise<void> {
    await this.goto('/application-services');
    await this.waitForListLoad();
  }

  /**
   * Click create service button
   */
  async clickCreateService(): Promise<void> {
    const createButton = this.page.locator(SELECTORS.ADD_BUTTON).first();
    await createButton.click();
  }

  /**
   * Filter by lifecycle
   */
  async filterByLifecycle(lifecycle: 'ACTIVE' | 'DEPRECATED' | 'RETIRED'): Promise<void> {
    await this.interactWithFilter('Lifecycle', lifecycle);
  }

  /**
   * Filter by owner team
   */
  async filterByOwnerTeam(teamId: string): Promise<void> {
    await this.interactWithFilter('Owner Team', teamId);
  }

  /**
   * Filter by environment
   */
  async filterByEnvironment(environment: string): Promise<void> {
    await this.interactWithFilter('Environment', environment);
  }

  /**
   * Toggle unassigned only
   */
  async toggleUnassignedOnly(): Promise<void> {
    const checkbox = this.page.locator('input[type="checkbox"]').first();
    await checkbox.click();
    await this.waitForLoadingComplete();
  }
}

