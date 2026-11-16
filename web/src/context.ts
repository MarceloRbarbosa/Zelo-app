import type { Profile, Role } from './api.ts';

// The logged-in member, loaded once after login; screens use it to show only what the role can do.
let current: Profile | null = null;

export const context = {
  get profile(): Profile {
    if (!current) {
      throw new Error('Profile not loaded');
    }
    return current;
  },
  set(profile: Profile | null): void {
    current = profile;
  },
  is(...roles: Role[]): boolean {
    return current !== null && roles.includes(current.role);
  },
  // Admins and coordinators manage activities, events, beneficiaries and assistance.
  get manages(): boolean {
    return this.is('ROLE_ADMIN', 'ROLE_COORDINATOR');
  },
};

export const ROLE_LABEL: Record<Role, string> = {
  ROLE_ADMIN: 'Administração',
  ROLE_COORDINATOR: 'Coordenação',
  ROLE_VOLUNTEER: 'Voluntariado',
};
