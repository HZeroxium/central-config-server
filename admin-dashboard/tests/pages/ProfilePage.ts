import { Page } from '@playwright/test';
import { BaseDetailPage } from './BaseDetailPage';

/**
 * Profile Page Object
 */
export class ProfilePage extends BaseDetailPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to profile page
   */
  async gotoProfile(): Promise<void> {
    await this.goto('/profile');
    await this.waitForDetailLoad();
  }
}

