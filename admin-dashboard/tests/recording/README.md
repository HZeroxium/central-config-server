# Playwright Recording Automation

Hệ thống recording automation để tự động record toàn bộ interactions trên website, tạo video, screenshots, traces và markdown documentation tự động.

## Mục đích

Thay vì manual recording tutorial, hệ thống này tự động:
- Navigate qua tất cả các pages/modules
- Record video, screenshots, và traces
- Generate markdown documentation với screenshots
- Organize output files theo module structure

## Cài đặt

Không cần cài đặt thêm dependencies, sử dụng Playwright đã có sẵn.

## Sử dụng

### Record Admin Tutorial

```bash
npm run record:admin
```

Record toàn bộ admin flow với tất cả modules (bao gồm IAM).

### Record User Tutorial

```bash
npm run record:user
```

Record user flow với public routes only.

### Record Cả Hai

```bash
npm run record:all
```

Record cả admin và user tutorials.

### Xem Recordings

```bash
npm run record:view
```

Mở Playwright trace viewer để xem recordings.

Hoặc xem markdown documentation:
```bash
cat test-results/recordings/tutorials/admin-tutorial.md
cat test-results/recordings/tutorials/user-tutorial.md
```

## Output Structure

```
test-results/recordings/
  admin/
    dashboard/
      video.webm
      screenshots/
        step-01-login-*.png
        step-02-dashboard-overview-*.png
      trace.zip
    application-services/
      video.webm
      screenshots/
        ...
    configs/
      ...
    ...
  user/
    dashboard/
      ...
    ...
  tutorials/
    admin-tutorial.md
    user-tutorial.md
  reports/
    index.html
```

## Cấu hình

Recording config được định nghĩa trong `playwright.recording.config.ts`:
- Video: Always on
- Screenshots: Always on
- Trace: Always on
- Slow motion: 100ms delay giữa các actions
- Sequential execution (không parallel)

## Customization

### Thêm Module Mới

1. Thêm module metadata vào `tests/recording/constants.ts`:
```typescript
export const MODULE_METADATA: Record<string, ...> = {
  'new-module': {
    route: '/new-module',
    label: 'New Module',
    description: 'Description',
  },
};
```

2. Thêm recording plan vào `tests/recording/data/recording-metadata.ts`:
```typescript
export const ADMIN_RECORDING_PLANS: ModuleRecordingPlan[] = [
  {
    moduleName: 'new-module',
    steps: [
      {
        name: 'new-module-view',
        description: 'View new module',
        module: 'new-module',
        route: '/new-module',
        interactions: ['view'],
      },
    ],
  },
];
```

3. Thêm interactions vào `admin-tutorial.ts` hoặc `user-tutorial.ts` nếu cần.

### Điều chỉnh Delays

Sửa trong `tests/recording/constants.ts`:
```typescript
export const RECORDING_DELAYS = {
  PAGE_LOAD: 2000,      // Delay sau khi page load
  INTERACTION: 500,     // Delay giữa các interactions
  SCREENSHOT: 300,      // Delay sau screenshot
  NAVIGATION: 1000,     // Delay sau navigation
} as const;
```

## Troubleshooting

### Recording không chạy

- Đảm bảo frontend đang chạy tại `http://localhost:3000`
- Đảm bảo Keycloak đang chạy và accessible
- Check browser console cho errors

### Screenshots không được tạo

- Check permissions cho `test-results/recordings/` directory
- Đảm bảo đủ disk space

### Markdown không có screenshots

- Screenshots phải được tạo trước khi generate markdown
- Check relative paths trong markdown file

## Notes

- Recordings có thể tốn nhiều disk space (video files)
- Có thể cleanup old recordings bằng cách xóa `test-results/recordings/`
- Markdown documentation được generate tự động và có thể commit vào repo

