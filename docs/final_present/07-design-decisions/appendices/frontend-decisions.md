# Frontend Decisions

## Why Redux Toolkit + React Query Hybrid?

### Decision

Use Redux Toolkit for UI state and React Query for server state, instead of using only one solution.

### Context

The dashboard has two types of state:
- **UI State**: Sidebar open/closed, theme mode, notifications
- **Server State**: API data, caching, synchronization

### Alternatives Considered

1. **Redux Only**
   - Single solution
   - Overkill for server state
   - Manual caching and synchronization

2. **React Query Only**
   - Good for server state
   - Not ideal for UI state
   - No global UI state management

3. **Context API Only**
   - Simple
   - Performance issues with frequent updates
   - No built-in caching

4. **Zustand**
   - Lightweight
   - Less ecosystem
   - Less features than Redux Toolkit

### Trade-offs

| Aspect | Redux + React Query | Redux Only | React Query Only | Context API |
|--------|---------------------|------------|------------------|-------------|
| **UI State Management** | Excellent | Excellent | Poor | Good |
| **Server State Management** | Excellent | Poor | Excellent | Poor |
| **Caching** | Built-in (React Query) | Manual | Built-in | Manual |
| **Complexity** | Medium | High | Low | Low |
| **Bundle Size** | Medium | Large | Small | Small |
| **Developer Experience** | Excellent | Good | Excellent | Good |

### Rationale

1. **Separation of Concerns**: UI state (Redux) vs server state (React Query)
2. **Best Tool for Job**: Each library optimized for its use case
3. **Automatic Caching**: React Query handles API caching automatically
4. **Reduced Boilerplate**: Redux Toolkit reduces Redux boilerplate
5. **Performance**: React Query optimizes re-renders, Redux handles UI updates efficiently

### Implementation

**Redux Toolkit (UI State):**
```typescript
// Sidebar state, theme, notifications
const uiSlice = createSlice({
  name: 'ui',
  initialState: { sidebarOpen: true, theme: 'light' },
  reducers: { toggleSidebar, setTheme }
});
```

**React Query (Server State):**
```typescript
// API data with automatic caching
const { data, isLoading } = useQuery({
  queryKey: ['services'],
  queryFn: fetchServices,
  staleTime: 30000
});
```

**Reference:**
- `admin-dashboard/src/store/uiSlice.ts` - Redux state
- `admin-dashboard/src/lib/api/hooks.ts` - React Query hooks

### When to Reconsider

- If UI state becomes minimal
- If server state becomes simple
- If bundle size becomes critical
- If team prefers single solution

---

## Why Declarative Permission Component?

### Decision

Create a declarative `<CanAccess>` component for permission-based UI rendering instead of imperative checks.

### Context

The dashboard needs to control UI element visibility based on permissions (role-based, service-based, team-based). This is needed throughout the application.

### Alternatives Considered

1. **Imperative Checks Only**
   - Simple
   - Repetitive code
   - Less readable

2. **Higher-Order Component (HOC)**
   - Reusable
   - Less flexible
   - Prop drilling

3. **Custom Hooks Only**
   - Flexible
   - More boilerplate
   - Less declarative

### Trade-offs

| Aspect | Declarative Component | Imperative Checks | HOC | Custom Hooks |
|--------|----------------------|-------------------|-----|--------------|
| **Readability** | High | Medium | Medium | Medium |
| **Reusability** | High | Low | High | High |
| **Flexibility** | High | High | Low | High |
| **Boilerplate** | Low | High | Medium | Medium |
| **Type Safety** | High | Medium | Medium | High |

### Rationale

1. **Declarative Syntax**: Clear intent, readable code
2. **Reusability**: Single component used throughout
3. **Type Safety**: TypeScript enforces permission types
4. **Flexibility**: Supports route, role, service, team checks
5. **Fallback Support**: Can show alternative UI when access denied

### Implementation

**Component API:**
```tsx
<CanAccess 
  permission="edit-service" 
  serviceId={serviceId}
  fallback={<Button disabled>No Permission</Button>}
>
  <Button>Edit Service</Button>
</CanAccess>
```

**Features:**
- Route-based access control
- Role-based checks (SYS_ADMIN, USER)
- Service-based permissions
- Team-based access
- Custom check functions
- Fallback UI support

**Reference:** `admin-dashboard/src/components/auth/CanAccess.tsx`

### When to Reconsider

- If permission logic becomes too complex
- If component overhead becomes significant
- If imperative checks are preferred
- If permission system changes significantly

---

## Why Orval for API Generation?

### Decision

Use Orval to generate TypeScript types and React Query hooks from OpenAPI specification.

### Context

The backend exposes OpenAPI specification. The frontend needs type-safe API clients and React Query hooks.

### Alternatives Considered

1. **Manual Types and Hooks**
   - Full control
   - Time-consuming
   - Error-prone
   - Out of sync risk

2. **GraphQL Code Generator**
   - Good for GraphQL
   - Not applicable (REST API)
   - Different approach

3. **OpenAPI Generator**
   - Comprehensive
   - Less React Query integration
   - More configuration

4. **Swagger Codegen**
   - Mature
   - Less modern
   - Less TypeScript support

### Trade-offs

| Aspect | Orval | Manual | OpenAPI Generator | Swagger Codegen |
|--------|-------|--------|-------------------|-----------------|
| **Type Safety** | High | High | High | Medium |
| **React Query Integration** | Excellent | Manual | Limited | None |
| **Maintenance** | Low (auto-generated) | High | Medium | Medium |
| **Customization** | Medium | High | High | Medium |
| **Setup Complexity** | Low | None | Medium | Medium |

### Rationale

1. **Type Safety**: Generated types match API exactly
2. **Automatic Updates**: Regenerate when API changes
3. **React Query Integration**: Built-in React Query hook generation
4. **Time Savings**: No manual type/hook creation
5. **Consistency**: All API calls use same pattern

### Implementation

**Configuration:**
```javascript
// orval.config.js
export default {
  'config-control-service': {
    input: './openapi.json',
    output: {
      target: './src/lib/api/hooks.ts',
      client: 'react-query',
    },
  },
};
```

**Generated Output:**
- TypeScript types in `src/lib/api/models/`
- React Query hooks in `src/lib/api/hooks.ts`
- Automatic type inference

**Reference:** `admin-dashboard/orval.config.js`

### When to Reconsider

- If API changes infrequently
- If manual control is preferred
- If Orval limitations become blocking
- If API becomes GraphQL

---

## Why Material-UI (MUI) for Components?

### Decision

Use Material-UI as the component library for the admin dashboard.

### Context

The dashboard needs a comprehensive component library with consistent design, accessibility, and theming.

### Alternatives Considered

1. **Ant Design**
   - Comprehensive
   - Less Material Design
   - Different design language

2. **Chakra UI**
   - Modern
   - Less components
   - Different approach

3. **Custom Components**
   - Full control
   - Time-consuming
   - Maintenance burden

4. **Tailwind CSS Only**
   - Flexible
   - No components
   - More development time

### Trade-offs

| Aspect | MUI | Ant Design | Chakra UI | Custom |
|--------|-----|------------|-----------|--------|
| **Component Count** | High | High | Medium | N/A |
| **Design System** | Material Design | Ant Design | Custom | Custom |
| **Theming** | Excellent | Good | Excellent | Manual |
| **Accessibility** | Excellent | Good | Good | Manual |
| **Bundle Size** | Medium | Medium | Small | Variable |

### Rationale

1. **Comprehensive Components**: DataGrid, Stepper, Timeline, etc.
2. **Material Design**: Familiar, professional look
3. **Theming**: Excellent dark/light mode support
4. **Accessibility**: WCAG compliant components
5. **Ecosystem**: Large community and ecosystem

### Implementation

**Components Used:**
- DataGrid for tables
- Stepper for approval workflows
- Timeline for decision history
- Theme provider for dark/light mode

**Reference:** `admin-dashboard/src/theme/theme.ts`

### When to Reconsider

- If Material Design doesn't fit requirements
- If bundle size becomes critical
- If custom design system is required
- If team prefers different library

---

## Summary

Frontend decisions prioritize:
1. **Type Safety** (TypeScript, Orval)
2. **Developer Experience** (React Query, Redux Toolkit)
3. **Maintainability** (Declarative components, code generation)
4. **User Experience** (MUI components, permission-based UI)

These choices enable rapid development while maintaining code quality and user experience.

