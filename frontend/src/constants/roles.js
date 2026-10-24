/**
 * User roles matching backend UserRole enum.
 */
export const USER_ROLES = Object.freeze({
  ADMIN: 'ADMIN',
  PROJECT_MANAGER: 'PROJECT_MANAGER',
  DEVELOPER: 'DEVELOPER',
});

export const USER_ROLE_LABELS = Object.freeze({
  [USER_ROLES.ADMIN]: 'Administrator',
  [USER_ROLES.PROJECT_MANAGER]: 'Project Manager',
  [USER_ROLES.DEVELOPER]: 'Developer',
});
