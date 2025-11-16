/**
 * Test credentials for different user roles
 */
export interface AuthCredentials {
  username: string;
  password: string;
  role: 'admin' | 'user';
}

export const ADMIN_CREDENTIALS: AuthCredentials = {
  username: 'admin',
  password: 'admin123',
  role: 'admin',
};

export const USER_CREDENTIALS: AuthCredentials = {
  username: 'user1',
  password: 'user123',
  role: 'user',
};

export const DEFAULT_CREDENTIALS = ADMIN_CREDENTIALS;

