# E2E Tests with Playwright

This directory contains comprehensive end-to-end tests for the admin-dashboard application using Playwright with role-based testing (admin and user roles).

## Structure

```
tests/
├── e2e/                    # E2E test suites
│   ├── auth-flow.spec.ts
│   ├── navigation-exploration.spec.ts
│   ├── list-pages.spec.ts
│   ├── detail-pages.spec.ts
│   ├── interactions.spec.ts
│   └── permissions.spec.ts
├── pages/                  # Page Object Model classes
│   ├── BasePage.ts
│   ├── BaseListPage.ts
│   ├── BaseDetailPage.ts
│   ├── DashboardPage.ts
│   ├── KeycloakLoginPage.ts
│   ├── NavigationPage.ts
│   ├── ApplicationServiceListPage.ts
│   ├── ApplicationServiceDetailPage.ts
│   ├── ServiceInstanceListPage.ts
│   ├── ServiceInstanceDetailPage.ts
│   ├── ConfigListPage.ts
│   ├── ConfigDetailPage.ts
│   ├── DriftEventListPage.ts
│   ├── ApprovalListPage.ts
│   ├── ApprovalDetailPage.ts
│   ├── ServiceShareListPage.ts
│   ├── ServiceRegistryListPage.ts
│   ├── IamUserListPage.ts
│   ├── IamTeamListPage.ts
│   ├── KVStoreListPage.ts
│   └── ProfilePage.ts
├── helpers/                # Test helpers
│   ├── navigation.ts
│   ├── components.ts
│   └── permissions.ts
├── fixtures/              # Test fixtures
│   ├── auth.ts
│   └── role-based.ts
├── constants/             # Test constants
│   ├── credentials.ts
│   ├── routes.ts
│   └── selectors.ts
├── utils/                 # Test utilities
│   └── test-helpers.ts
└── README.md
```

## Page Object Pattern

Tests follow the Page Object Model (POM) pattern:

- **BasePage**: Base class with common methods and selectors
- **KeycloakLoginPage**: Handles Keycloak authentication
- **DashboardPage**: Handles dashboard interactions
- **NavigationPage**: Handles navigation between pages

## Running Tests

### Prerequisites

1. Install dependencies:
```bash
npm install
```

2. Install Playwright browsers:
```bash
npx playwright install
```

3. Ensure services are running:
   - Frontend: `http://localhost:3000`
   - Keycloak: `http://localhost:8080`
   - Backend API: `http://localhost:8081`

### Run Tests

```bash
# Run all tests (both admin and user roles)
npm run test:e2e

# Run tests for specific role
npx playwright test --project=admin
npx playwright test --project=user

# Run specific test file
npx playwright test tests/e2e/navigation-exploration.spec.ts

# Run tests in UI mode
npm run test:e2e:ui

# Run tests in debug mode
npm run test:e2e:debug

# Run tests in headed mode (see browser)
npm run test:e2e:headed

# Show test report
npm run test:e2e:report
```

### Environment Variables

Set these environment variables if different from defaults:

```bash
BASE_URL=http://localhost:3000
KEYCLOAK_URL=http://localhost:8080
```

## Test Configuration

Tests are configured in `playwright.config.ts`:

- **Base URL**: `http://localhost:3000` (configurable via `BASE_URL` env var)
- **Projects**: Two projects configured (`admin` and `user`) for role-based testing
- **Retries**: 2 retries on CI, 0 locally
- **Workers**: 1 on CI, parallel locally
- **Screenshots**: On failure (full page)
- **Video**: On failure
- **Trace**: On retry
- **Viewport**: 1920x1080
- **Timeouts**: 15s action, 30s navigation

## Test Credentials

Test credentials are defined in `tests/constants/credentials.ts`:

- **Admin**: `admin` / `admin123`
- **User**: `user1` / `user123`

## Test Coverage

The test suite covers:

1. **Navigation & Exploration** (`navigation-exploration.spec.ts`)
   - Navigation through all accessible pages for each role
   - Sidebar navigation
   - Unauthorized redirects

2. **List Pages** (`list-pages.spec.ts`)
   - All list pages with role-based access
   - Data presence verification
   - Search, filters, pagination

3. **Detail Pages** (`detail-pages.spec.ts`)
   - Navigation to detail pages from lists
   - Tab interactions
   - Back navigation

4. **Interactions** (`interactions.spec.ts`)
   - Search interactions
   - Filter interactions
   - Pagination
   - Refresh buttons

5. **Permissions** (`permissions.spec.ts`)
   - Admin access to all pages
   - User access restrictions
   - Unauthorized redirects
   - Permission-based UI visibility

## Writing Tests

### Role-Based Testing

Tests automatically run for both admin and user roles using Playwright projects. Use `loginAsAdmin()` or `loginAsUser()` fixtures:

```typescript
import { test, expect } from '@playwright/test';
import { loginAsAdmin, loginAsUser } from '../fixtures/auth';
import { ApplicationServiceListPage } from '../pages/ApplicationServiceListPage';

test.describe('Admin Role', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('should access all pages', async ({ page }) => {
    const listPage = new ApplicationServiceListPage(page);
    await listPage.goto();
    await listPage.verifyListLoaded();
  });
});

test.describe('User Role', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsUser(page);
  });

  test('should access allowed pages only', async ({ page }) => {
    // Test user access
  });
});
```

### Example Test with Page Objects

```typescript
import { test, expect } from '@playwright/test';
import { loginAsAdmin } from '../fixtures/auth';
import { ApplicationServiceListPage } from '../pages/ApplicationServiceListPage';

test('should load and interact with list page', async ({ page }) => {
  await loginAsAdmin(page);
  
  const listPage = new ApplicationServiceListPage(page);
  await listPage.goto();
  await listPage.verifyListLoaded();
  await listPage.verifyTableHasData();
  
  // Test search
  await listPage.interactWithSearch('test');
  await listPage.waitForLoadingComplete();
  
  // Test filters
  await listPage.filterByLifecycle('ACTIVE');
  await listPage.waitForLoadingComplete();
});
```

### Using Page Objects

```typescript
import { KeycloakLoginPage } from '../pages/KeycloakLoginPage';
import { DashboardPage } from '../pages/DashboardPage';

test('should login and navigate', async ({ page }) => {
  const keycloakPage = new KeycloakLoginPage(page);
  await keycloakPage.login('admin', 'admin123');
  
  const dashboardPage = new DashboardPage(page);
  await dashboardPage.goto('/dashboard');
});
```

### Using Helpers

```typescript
import { NavigationHelper } from '../helpers/navigation';

test('should verify URL', async ({ page }) => {
  const helper = new NavigationHelper(page);
  await helper.verifyUrlContains('/dashboard');
  await helper.verifyRoute('/dashboard');
});
```

## Best Practices

1. **Use Page Objects**: Always use Page Object classes instead of direct selectors
2. **Wait for Elements**: Use `waitForElement()` or `waitFor()` before interactions
3. **Use Fixtures**: Use `login()` fixture for authenticated tests
4. **Clear State**: Clear cookies/storage between tests if needed
5. **Error Handling**: Use try-catch for optional elements
6. **Descriptive Names**: Use clear test names that describe the behavior

## Troubleshooting

### Tests Fail with "Timeout"

- Check if services are running
- Verify URLs are correct
- Increase timeout if needed
- Check browser console for errors

### Authentication Fails

- Verify Keycloak is running
- Check Keycloak client configuration
- Verify redirect URIs match
- Check cookies are enabled

### Selectors Not Found

- Use Playwright Inspector: `npm run test:e2e:debug`
- Check element is actually present
- Use more specific selectors
- Wait for element to be visible

## CI/CD Integration

Tests can be run in CI/CD pipelines:

```yaml
- name: Install dependencies
  run: npm ci

- name: Install Playwright
  run: npx playwright install --with-deps

- name: Run E2E tests
  run: npm run test:e2e
```

## References

- [Playwright Documentation](https://playwright.dev/)
- [Page Object Model](https://playwright.dev/docs/pom)
- [Best Practices](https://playwright.dev/docs/best-practices)

