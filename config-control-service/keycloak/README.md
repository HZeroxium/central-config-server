# Keycloak User Profile Configuration

## Custom Attributes

The realm is configured with the following custom user attributes:

### Core Attributes
- **manager_id**: UUID of the user's manager (used for LINE_MANAGER approval gate)
  - Format: UUID (validated)
  - Included in JWT tokens via mapper
  - Read-only for users, writable by admin

- **phone**: Phone number in E.164 international format
  - Format: +[country][number] (e.g., +84912345678)
  - Validated via regex: `^\+[1-9]\d{1,14}$`
  - User-editable in profile

### Enterprise Attributes
- **employee_id**: Unique employee identifier
- **department**: Department name
- **job_title**: Job title/position
- **office_location**: Office location
- **hire_date**: Hire date in ISO 8601 format (YYYY-MM-DD)

## Manager Relationship Setup

Manager relationships are established via two-pass process in `keycloak-init.sh`:
1. Create all users with `manager_username` in attributes
2. Resolve `manager_username` → Keycloak UUID → set `manager_id` attribute

## Token Claims

The following custom attributes are included in JWT tokens:
- `manager_id`: Via manager_id client scope and mapper
- `groups`: Via groups client scope and mapper

Other attributes (phone, employee_id, etc.) are available via Admin API but not in tokens to minimize token size.

## User Profile Configuration

The User Profile is configured programmatically via Admin API in `keycloak-init.sh` with:

- **Validation Rules**: Pattern validation for manager_id (UUID), phone (E.164), hire_date (ISO 8601)
- **Permissions**: Admin-only edit for manager_id, employee_id, department, job_title, office_location, hire_date; User-editable for phone
- **Input Types**: Appropriate HTML input types (text, tel, date)

## Test Users

The following test users are configured with complete attributes:

- **admin**: System Administrator (EMP000)
- **admin2**: System Administrator 2 (EMP001)
- **user1**: Team1 Lead (EMP001) - Manager for user2
- **user2**: Team1 Member (EMP002) - Reports to user1
- **user3**: Team2 Lead (EMP003) - Manager for user4
- **user4**: Team2 Member (EMP004) - Reports to user3
- **user5**: Contractor (EMP005) - No team membership

## Verification

Use the `verify-user-profile.sh` script to validate:
- User Profile configuration
- Custom attribute definitions
- User attribute values
- Manager relationships

```bash
./verify-user-profile.sh
```

## Troubleshooting

### User Profile Not Applied
- Ensure Keycloak version 21+ (User Profile API support)
- Check Admin API permissions
- Verify JSON syntax in User Profile configuration

### Manager Relationships Not Set
- Verify `manager_username` attributes in users.json
- Check that manager users exist before setting relationships
- Review keycloak-init.sh logs for resolution errors

### Token Claims Missing
- Verify client scope assignments
- Check protocol mapper configuration
- Ensure attributes are set on users before token generation
