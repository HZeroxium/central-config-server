import { Page } from '@playwright/test';
import { BaseListPage } from './BaseListPage';
import { SELECTORS } from '../constants/selectors';

/**
 * Service Instance List Page Object
 */
export class ServiceInstanceListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to service instances list
   */
  async goto(): Promise<void> {
    await this.goto('/service-instances');
    await this.waitForListLoad();
  }

  /**
   * Filter by environment
   */
  async filterByEnvironment(environment: string): Promise<void> {
    await this.interactWithFilter('Environment', environment);
  }

  /**
   * Filter by status
   */
  async filterByStatus(status: 'ONLINE' | 'OFFLINE'): Promise<void> {
    await this.interactWithFilter('Status', status);
  }

  /**
   * Filter by drift
   */
  async filterByDrift(hasDrift: boolean): Promise<void> {
    const filterText = hasDrift ? 'Has Drift' : 'No Drift';
    await this.interactWithFilter('Drift', filterText);
  }
}

