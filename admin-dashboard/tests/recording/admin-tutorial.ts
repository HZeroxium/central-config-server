import { test, expect } from '@playwright/test';
import { loginAsAdmin } from '../fixtures/auth';
import { BASE_URL } from '../constants/config';
import { RecordingHelper } from './helpers/recording-helper';
import { MarkdownGenerator } from './helpers/markdown-generator';
import { OutputOrganizer } from './helpers/output-organizer';
import { ADMIN_RECORDING_PLANS } from './data/recording-metadata';
import { MODULE_METADATA, getModuleOutputPath } from './constants';
import type { RecordingSession, RecordingModule } from './types';

// Import page objects
import { DashboardPage } from '../pages/DashboardPage';
import { ApplicationServiceListPage } from '../pages/ApplicationServiceListPage';
import { ServiceInstanceListPage } from '../pages/ServiceInstanceListPage';
import { ConfigListPage } from '../pages/ConfigListPage';
import { ApprovalListPage } from '../pages/ApprovalListPage';
import { DriftEventListPage } from '../pages/DriftEventListPage';
import { IamUserListPage } from '../pages/IamUserListPage';
import { IamTeamListPage } from '../pages/IamTeamListPage';
import { ProfilePage } from '../pages/ProfilePage';

/**
 * Admin tutorial recording script
 * Records all admin interactions across all modules
 */
test.describe('Admin Tutorial Recording', () => {
  test('record admin tutorial', async ({ page }) => {
    const startTime = Date.now();
    const recordingHelper = new RecordingHelper(page, 'admin');
    const session: RecordingSession = {
      role: 'admin',
      startTime,
      modules: [],
      metadata: {
        baseUrl: BASE_URL,
        viewport: { width: 1920, height: 1080 },
        browser: 'Chrome',
      },
    };

    // Login as admin
    await loginAsAdmin(page, BASE_URL);
    await recordingHelper.waitForPageReady();

    // Record login step
    await recordingHelper.recordStepWithScreenshot(
      'login',
      'Login as admin user',
      BASE_URL,
      ['login']
    );

    // Process each module
    for (const plan of ADMIN_RECORDING_PLANS) {
      const moduleMetadata = MODULE_METADATA[plan.moduleName];
      if (!moduleMetadata) continue;

      recordingHelper.setModule(plan.moduleName);
      const module: RecordingModule = {
        name: plan.moduleName,
        label: moduleMetadata.label,
        route: moduleMetadata.route,
        description: moduleMetadata.description,
        steps: [],
        screenshots: [],
      };

      // Navigate to module
      await page.goto(`${BASE_URL}${moduleMetadata.route}`);
      await recordingHelper.waitForPageReady();

      // Process each step in the plan
      for (const stepPlan of plan.steps) {
        // Record step with screenshot
        const step = await recordingHelper.recordStepWithScreenshot(
          stepPlan.name,
          stepPlan.description,
          stepPlan.route,
          stepPlan.interactions
        );
        module.steps.push(step);

        // Perform interactions based on step name
        await performInteractions(page, plan.moduleName, stepPlan.name, recordingHelper);
      }

      session.modules.push(module);
    }

    // Generate markdown documentation
    session.endTime = Date.now();
    const markdownGenerator = new MarkdownGenerator();
    const markdownPath = await markdownGenerator.generateAndSave(session);
    console.log(`Markdown generated: ${markdownPath}`);

    // Organize output files
    const organizer = new OutputOrganizer();
    await organizer.organizeByModule('admin');
    console.log('Output files organized');
  });
});

/**
 * Perform interactions based on module and step name
 */
async function performInteractions(
  page: any,
  moduleName: string,
  stepName: string,
  recordingHelper: RecordingHelper
): Promise<void> {
  await recordingHelper.waitForInteraction();

  try {
    switch (moduleName) {
      case 'application-services':
        if (stepName === 'services-search') {
          const listPage = new ApplicationServiceListPage(page);
          await listPage.interactWithSearch('test');
          await recordingHelper.waitForPageReady();
          await recordingHelper.takeScreenshot('services-search-results');
        } else if (stepName === 'service-detail') {
          const listPage = new ApplicationServiceListPage(page);
          await listPage.verifyListLoaded();
          // Try to navigate to first row if available
          try {
            await listPage.navigateToDetail(0);
            await recordingHelper.waitForPageReady();
            await recordingHelper.takeScreenshot('service-detail-view');
            // Go back
            await page.goBack();
            await recordingHelper.waitForNavigation();
          } catch (e) {
            // No data available, skip
          }
        }
        break;

      case 'service-instances':
        if (stepName === 'instances-filter') {
          const listPage = new ServiceInstanceListPage(page);
          try {
            await listPage.filterByStatus('ONLINE');
            await recordingHelper.waitForPageReady();
            await recordingHelper.takeScreenshot('instances-filtered');
          } catch (e) {
            // Filter might not be available
          }
        } else if (stepName === 'instance-detail') {
          const listPage = new ServiceInstanceListPage(page);
          await listPage.verifyListLoaded();
          try {
            await listPage.navigateToDetail(0);
            await recordingHelper.waitForPageReady();
            await recordingHelper.takeScreenshot('instance-detail-view');
            await page.goBack();
            await recordingHelper.waitForNavigation();
          } catch (e) {
            // No data available
          }
        }
        break;

      case 'configs':
        if (stepName === 'configs-search') {
          const listPage = new ConfigListPage(page);
          await listPage.searchConfig('sample-service', 'dev');
          await recordingHelper.waitForPageReady();
          await recordingHelper.takeScreenshot('config-search-results');
        } else if (stepName === 'config-detail') {
          // Detail is already shown after search
          await recordingHelper.takeScreenshot('config-detail-view');
        }
        break;

      case 'drift-events':
        if (stepName === 'drift-filter') {
          const listPage = new DriftEventListPage(page);
          try {
            await listPage.filterByStatus('DETECTED');
            await recordingHelper.waitForPageReady();
            await recordingHelper.takeScreenshot('drift-filtered');
          } catch (e) {
            // Filter might not be available
          }
        }
        break;

      case 'approvals':
        if (stepName === 'approval-detail') {
          const listPage = new ApprovalListPage(page);
          await listPage.verifyListLoaded();
          try {
            await listPage.navigateToDetail(0);
            await recordingHelper.waitForPageReady();
            await recordingHelper.takeScreenshot('approval-detail-view');
            await page.goBack();
            await recordingHelper.waitForNavigation();
          } catch (e) {
            // No data available
          }
        }
        break;
    }
  } catch (error) {
    // Continue even if interactions fail
    console.warn(`Interaction failed for ${moduleName}/${stepName}:`, error);
  }
}

