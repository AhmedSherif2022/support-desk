import { DsSidebarItem } from '../shared/ui';
import { Role } from '../core/theme/auth/auth.models';

export interface RoleNav {
  subtitle: string;
  sectionLabel: string;
  userRole: string;
  /** Sidebar items. An item's `id` is its route path, so the active item follows the URL. */
  items: DsSidebarItem[];
  /** Bottom nav items (3 to 5, shorter labels); when omitted the first five `items` are used. */
  phoneItems?: DsSidebarItem[];
}

/**
 * Navigation per role, as in the Figma sidebars.
 * TODO: badge counts are placeholders until the tickets, review and alerts data exist.
 */
const GUIDE_ITEMS: DsSidebarItem[] = [
  { id: 'review', label: 'Review queue', icon: 'pi pi-check', badge: { count: 1, tone: 'action' } },
  { id: 'library', label: 'Guide library', icon: 'pi pi-book' },
  { id: 'triage', label: 'Triage', icon: 'pi pi-exclamation-triangle', badge: { count: 1, tone: 'action' } },
  { id: 'feedback', label: 'Agent feedback', icon: 'pi pi-flag', badge: { count: 1, tone: 'open' } },
  { id: 'analytics', label: 'Guide analytics', icon: 'pi pi-chart-bar' },
  { id: 'rules', label: 'Error rules & jobs', icon: 'pi pi-align-left' },
  { id: 'audit', label: 'Audit log', icon: 'pi pi-shield' },
];

export const NAV_BY_ROLE: Record<Role, RoleNav> = {
  agent: {
    subtitle: 'Mobile & web banking',
    sectionLabel: 'Customer service',
    userRole: 'Customer Service Agent',
    items: [
      { id: 'home', label: 'Home', icon: 'pi pi-home' },
      { id: 'customer-search', label: 'Customer search', icon: 'pi pi-search' },
      { id: 'tickets', label: 'My tickets', icon: 'pi pi-ticket', badge: { count: 8, tone: 'open' } },
      { id: 'alerts', label: 'Notifications', icon: 'pi pi-bell', badge: { count: 3, tone: 'action' } },
    ],
    phoneItems: [
      { id: 'home', label: 'Home', icon: 'pi pi-home' },
      { id: 'customer-search', label: 'Search', icon: 'pi pi-search' },
      { id: 'tickets', label: 'Tickets', icon: 'pi pi-ticket', badge: { count: 8, tone: 'open' } },
    ],
  },
  guide: {
    subtitle: 'Knowledge layer',
    sectionLabel: 'Guide admin',
    userRole: 'Guide Admin',
    items: GUIDE_ITEMS,
  },
  reviewer: {
    subtitle: 'Knowledge layer',
    sectionLabel: 'Guide admin',
    userRole: 'Guide Admin · reviewer',
    items: GUIDE_ITEMS.filter(i => i.id !== 'feedback' && i.id !== 'rules'),
  },
};
