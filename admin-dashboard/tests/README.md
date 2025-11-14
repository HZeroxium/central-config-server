# E2E Tests with Playwright

This directory contains end-to-end tests for the admin-dashboard application using Playwright.

## Structure

```
tests/
├── e2e/              # E2E test suites
│   └── auth-flow.spec.ts
├── pages/           # Page Object Model classes
│   ├── BasePage.ts
│   ├── KeycloakLoginPage.ts
│   ├── DashboardPage.ts
│   └── NavigationPage.ts
├── helpers/         # Test helpers
│   └── navigation.ts
└── fixtures/        # Test fixtures
    └── auth.ts
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
# Run all tests
npm run test:e2e

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
- **Retries**: 2 retries on CI, 0 locally
- **Workers**: 1 on CI, parallel locally
- **Screenshots**: On failure only
- **Video**: On failure only
- **Trace**: On retry only

## Writing Tests

### Example Test

```typescript
import { test, expect } from '@playwright/test';
import { login, DEFAULT_CREDENTIALS } from '../fixtures/auth';
import { DashboardPage } from '../pages/DashboardPage';

test('should load dashboard', async ({ page }) => {
  const dashboardPage = await login(page, DEFAULT_CREDENTIALS);
  await dashboardPage.goto('/dashboard');
  await dashboardPage.waitForDataLoad();
  expect(await dashboardPage.isLoaded()).toBe(true);
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

