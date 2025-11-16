/**
 * Centralized selectors for common UI elements
 * Using MUI component patterns and common selectors
 */

export const SELECTORS = {
  // Common layout
  SIDEBAR: '[data-testid="sidebar"], nav[aria-label*="navigation"], aside',
  SIDEBAR_TOGGLE: '[data-testid="sidebar-toggle"], button[aria-label*="menu"]',
  HEADER: 'header, [role="banner"]',
  MAIN_CONTENT: 'main, [role="main"]',
  BREADCRUMBS: 'nav[aria-label="breadcrumb"], ol[aria-label="breadcrumb"]',
  
  // Loading indicators
  LOADING: '[data-testid="loading"], .MuiCircularProgress-root, .MuiSkeleton-root',
  LOADING_SPINNER: '.MuiCircularProgress-root',
  SKELETON: '.MuiSkeleton-root',
  
  // Cards
  CARD: '.MuiCard-root, [data-testid="card"]',
  CARD_CONTENT: '.MuiCardContent-root',
  STATS_CARD: '[data-testid="stats-card"], .MuiCard-root',
  
  // Tables and Data Grids
  TABLE: 'table, .MuiDataGrid-root, [data-testid="table"]',
  DATA_GRID: '.MuiDataGrid-root',
  TABLE_ROW: 'tr[data-testid="table-row"], .MuiDataGrid-row',
  TABLE_CELL: 'td, .MuiDataGrid-cell',
  TABLE_HEADER: 'th, .MuiDataGrid-columnHeader',
  TABLE_BODY: 'tbody, .MuiDataGrid-virtualScroller',
  EMPTY_TABLE: '[data-testid="empty-table"], .MuiDataGrid-overlay',
  
  // Search
  SEARCH_INPUT: 'input[type="search"], input[placeholder*="Search" i], input[aria-label*="Search" i]',
  SEARCH_BUTTON: 'button[aria-label*="Search" i], button:has-text("Search")',
  SEARCH_FIELD: '[data-testid="search-field"], .MuiTextField-root:has(input[type="search"])',
  
  // Filters
  FILTER_SELECT: '.MuiSelect-root, select',
  FILTER_DROPDOWN: '.MuiSelect-select',
  FILTER_MENU: '.MuiMenu-paper, .MuiPopover-paper',
  FILTER_CHIP: '.MuiChip-root[data-testid*="filter"]',
  
  // Pagination
  PAGINATION: '.MuiPagination-root, [data-testid="pagination"]',
  PAGINATION_NEXT: 'button[aria-label*="next" i], button:has-text("Next")',
  PAGINATION_PREV: 'button[aria-label*="previous" i], button:has-text("Previous")',
  PAGINATION_PAGE: 'button[aria-label*="page" i]',
  
  // Buttons
  BUTTON: 'button, .MuiButton-root',
  BUTTON_PRIMARY: '.MuiButton-contained, button[type="submit"]',
  BUTTON_SECONDARY: '.MuiButton-outlined',
  BUTTON_TEXT: '.MuiButton-text',
  ADD_BUTTON: 'button:has-text("Add"), button[aria-label*="Add" i], button:has([class*="AddIcon"])',
  REFRESH_BUTTON: 'button[aria-label*="Refresh" i], button:has([class*="RefreshIcon"])',
  DELETE_BUTTON: 'button[aria-label*="Delete" i], button:has-text("Delete")',
  EDIT_BUTTON: 'button[aria-label*="Edit" i], button:has-text("Edit")',
  SAVE_BUTTON: 'button:has-text("Save"), button[type="submit"]',
  CANCEL_BUTTON: 'button:has-text("Cancel")',
  
  // Forms
  TEXT_FIELD: '.MuiTextField-root input, input[type="text"]',
  TEXT_AREA: 'textarea, .MuiTextField-root textarea',
  SELECT: '.MuiSelect-root, select',
  CHECKBOX: 'input[type="checkbox"], .MuiCheckbox-root',
  RADIO: 'input[type="radio"], .MuiRadio-root',
  SWITCH: '.MuiSwitch-root, input[type="checkbox"][role="switch"]',
  
  // Dialogs and Drawers
  DIALOG: '.MuiDialog-root, [role="dialog"]',
  DIALOG_TITLE: '.MuiDialogTitle-root, [role="dialog"] h2, [role="dialog"] h3',
  DIALOG_CONTENT: '.MuiDialogContent-root',
  DIALOG_ACTIONS: '.MuiDialogActions-root',
  DRAWER: '.MuiDrawer-root, [role="presentation"]',
  DRAWER_PAPER: '.MuiDrawer-paper',
  
  // Tabs
  TABS: '.MuiTabs-root, [role="tablist"]',
  TAB: '.MuiTab-root, [role="tab"]',
  TAB_PANEL: '.MuiTabPanel-root, [role="tabpanel"]',
  
  // Alerts and Messages
  ALERT: '.MuiAlert-root, [role="alert"]',
  ALERT_ERROR: '.MuiAlert-error, [role="alert"][class*="error"]',
  ALERT_SUCCESS: '.MuiAlert-success, [role="alert"][class*="success"]',
  SNACKBAR: '.MuiSnackbar-root, [role="alert"]',
  TOAST: '[data-testid="toast"], .sonner-toast',
  
  // Navigation
  NAV_LINK: 'a[href], .MuiListItemButton-root',
  NAV_ITEM: '.MuiListItem-root, nav li',
  SIDEBAR_ITEM: 'nav a, aside a, .MuiListItemButton-root',
  
  // Page specific
  PAGE_HEADER: 'h1, h2, h3, h4, h5, h6, [data-testid="page-header"]',
  PAGE_TITLE: 'h1, [data-testid="page-title"]',
  
  // Charts
  CHART: 'canvas, svg[data-testid="chart"], .recharts-wrapper',
  
  // Keycloak
  KEYCLOAK_USERNAME: '#username',
  KEYCLOAK_PASSWORD: '#password',
  KEYCLOAK_LOGIN_BUTTON: '#kc-login',
  KEYCLOAK_ERROR: '.alert-error',
  
  // Unauthorized
  UNAUTHORIZED_MESSAGE: '[data-testid="unauthorized"], :text("Unauthorized"), :text("Access Denied")',
} as const;

/**
 * Helper to build selectors with text content
 */
export function selectorWithText(selector: string, text: string): string {
  return `${selector}:has-text("${text}")`;
}

/**
 * Helper to build data-testid selector
 */
export function testId(id: string): string {
  return `[data-testid="${id}"]`;
}

