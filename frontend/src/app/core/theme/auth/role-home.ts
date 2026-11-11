import { Role } from './auth.models';

export const ROLE_HOME: Record<Role, string> = {
  agent: '/home',
  guide: '/review',
  reviewer: '/review',
};