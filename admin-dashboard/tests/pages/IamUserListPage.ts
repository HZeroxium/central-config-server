import { Page } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

/**
 * IAM User List Page Object (Admin only)
 */
export class IamUserListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to IAM users list
   */
  async goto(): Promise<void> {
    await this.goto('/iam/users');
    await this.waitForListLoad();
  }

  /**
   * Search by username
   */
  async searchByUsername(username: string): Promise<void> {
    const usernameInput = this.page.locator('input[placeholder*="username" i], input[label*="Username" i]').first();
    await usernameInput.fill(username);
    await this.page.waitForTimeout(1000); // Wait for debounce
  }

  /**
   * Search by email
   */
  async searchByEmail(email: string): Promise<void> {
    const emailInput = this.page.locator('input[placeholder*="email" i], input[label*="Email" i]').first();
    await emailInput.fill(email);
    await this.page.waitForTimeout(1000); // Wait for debounce
  }

  /**
   * Filter by role
   */
  async filterByRole(role: string): Promise<void> {
    await this.interactWithFilter('Role', role);
  }
}

