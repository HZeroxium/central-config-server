import { test, expect } from '@playwright/test';
import { loginAsUser } from '../fixtures/auth';
import { BASE_URL } from '../constants/config';
import { ADMIN_ONLY_ROUTES } from '../constants/routes';
import { RecordingHelper } from './helpers/recording-helper';
import { MarkdownGenerator } from './helpers/markdown-generator';
import { OutputOrganizer } from './helpers/output-organizer';
import { USER_RECORDING_PLANS } from './data/recording-metadata';
import { MODULE_METADATA } from './constants';
import type { RecordingSession, RecordingModule } from './types';

// Import page objects
import { DashboardPage } from '../pages/DashboardPage';
import { ApplicationServiceListPage } from '../pages/ApplicationServiceListPage';
import { ServiceInstanceListPage } from '../pages/ServiceInstanceListPage';
import { ConfigListPage } from '../pages/ConfigListPage';
import { ProfilePage } from '../pages/ProfilePage';
import { PermissionHelper } from '../helpers/permissions';

/**
 * User tutorial recording script
 * Records user interactions for public routes only
 */
test.describe('User Tutorial Recording', () => {
  test('record user tutorial', async ({ page }) => {
    const startTime = Date.now();
    const recordingHelper = new RecordingHelper(page, 'user');
    const session: RecordingSession = {
      role: 'user',
      startTime,
      modules: [],
      metadata: {
        baseUrl: BASE_URL,
        viewport: { width: 1920, height: 1080 },
        browser: 'Chrome',
      },
    };

    // Login as user
    await loginAsUser(page, BASE_URL);
    await recordingHelper.waitForPageReady();

    // Record login step
    await recordingHelper.recordStepWithScreenshot(
      'login',
      'Login as regular user',
      BASE_URL,
      ['login']
    );

    // Process each module in user plan
    for (const plan of USER_RECORDING_PLANS) {
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

        // Perform interactions
        await performUserInteractions(page, plan.moduleName, stepPlan.name, recordingHelper);
      }

      session.modules.push(module);
    }

    // Record unauthorized access attempts
    await recordUnauthorizedAccess(page, recordingHelper, session);

    // Generate markdown documentation
    session.endTime = Date.now();
    const markdownGenerator = new MarkdownGenerator();
    const markdownPath = await markdownGenerator.generateAndSave(session);
    console.log(`Markdown generated: ${markdownPath}`);

    // Organize output files
    const organizer = new OutputOrganizer();
    await organizer.organizeByModule('user');
    console.log('Output files organized');
  });
});

/**
 * Perform user interactions
 */
async function performUserInteractions(
  page: any,
  moduleName: string,
  stepName: string,
  recordingHelper: RecordingHelper
): Promise<void> {
  await recordingHelper.waitForInteraction();

  try {
    switch (moduleName) {
      case 'application-services':
        if (stepName === 'services-list') {
          const listPage = new ApplicationServiceListPage(page);
          await listPage.verifyListLoaded();
        }
        break;

      case 'service-instances':
        if (stepName === 'instances-list') {
          const listPage = new ServiceInstanceListPage(page);
          await listPage.verifyListLoaded();
        }
        break;

      case 'configs':
        if (stepName === 'configs-search') {
          const listPage = new ConfigListPage(page);
          await listPage.searchConfig('sample-service', 'dev');
          await recordingHelper.waitForPageReady();
          await recordingHelper.takeScreenshot('config-search-results');
        }
        break;
    }
  } catch (error) {
    console.warn(`Interaction failed for ${moduleName}/${stepName}:`, error);
  }
}

/**
 * Record unauthorized access attempts
 */
async function recordUnauthorizedAccess(
  page: any,
  recordingHelper: RecordingHelper,
  session: RecordingSession
): Promise<void> {
  const permHelper = new PermissionHelper(page);

  recordingHelper.setModule('unauthorized');
  const module: RecordingModule = {
    name: 'unauthorized',
    label: 'Unauthorized Access',
    route: '/unauthorized',
    description: 'Demonstration of unauthorized access redirects',
    steps: [],
    screenshots: [],
  };

  for (const route of ADMIN_ONLY_ROUTES) {
    await page.goto(`${BASE_URL}${route}`);
    await recordingHelper.waitForPageReady();

    const step = await recordingHelper.recordStepWithScreenshot(
      `unauthorized-${route.replace(/\//g, '-')}`,
      `Attempt to access ${route} (redirected to unauthorized)`,
      route,
      ['unauthorized-redirect']
    );

    module.steps.push(step);
  }

  if (module.steps.length > 0) {
    session.modules.push(module);
  }
}

