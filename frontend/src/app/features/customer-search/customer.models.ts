import type { DsPriority } from '../../shared/ui';

export type DigitalStatus = 'active' | 'locked' | 'inactive';
export type Segment = 'Premier' | 'Retail' | 'Business';
export type Platform = 'mobile' | 'web';

export interface Customer {
  cif: string;
  name: string;
  segment: Segment;
  city: string;
  mobile: string;
  nationalId: string;
  device: string;
  mobileBanking: DigitalStatus;
  webBanking: DigitalStatus;
  openTickets: number;
}

/**
 * new      → created, nobody worked it yet
 * open     → the agent is working it
 * with-sre → escalated to SRE (Azure DevOps work item)
 * fixed    → SRE says it's fixed; the agent must confirm with the customer
 * resolved / closed → done
 */
export type TicketStatus = 'new' | 'open' | 'with-sre' | 'fixed' | 'resolved' | 'closed';

export interface TicketNote {
  at: Date;
  author: string;
  text: string;
}

export interface TicketEvent {
  at: Date;
  actor: string;
  text: string;
}

export interface Ticket {
  id: string;
  cif: string;
  title: string;
  category: string;
  status: TicketStatus;
  priority: DsPriority;
  platform: Platform;
  createdAt: Date;
  /** Username of the agent who owns the ticket. */
  assignee: string;
  /** "monitor" = auto-created by the log monitor job. */
  origin: 'monitor' | 'agent';
  errorCode?: string;
  adoItem?: string;
  sreOwner?: string;
  incidentId?: string;
  transactionRef?: string;
  device?: string;
  report?: string;
  lastSreAskAt?: Date;
  resolvedAt?: Date;
  notes: TicketNote[];
  timeline: TicketEvent[];
}

/** One event from the customer's digital banking logs. */
export interface ActivityEvent {
  cif: string;
  at: Date;
  platform: Platform;
  area: string;
  title: string;
  detail: string;
  failed: boolean;
  errorCode?: string;
}

/** An open incident that new tickets join instead of escalating again. */
export interface Incident {
  id: string;
  adoItem: string;
  category: string;
  platform: Platform;
  summary: string;
  errorCode: string;
  sreState: string;
  /** What the agent can say to callers. */
  customerScript: string;
}

export interface ServiceHealth {
  name: string;
  errorsPerHour: number;
  state: 'ok' | 'warn' | 'bad';
}

/** What the New ticket form sends to the service. */
export interface NewTicketDraft {
  cif: string;
  platform: Platform;
  priority: DsPriority;
  category: string;
  issue: string;
  report: string;
}

/** 01022233344 → 010••••3344 */
export function maskMobile(mobile: string): string {
  return `${mobile.slice(0, 3)}••••${mobile.slice(-4)}`;
}

/** 29001011234567 → ••••••••••4567 */
export function maskNationalId(id: string): string {
  return `${'•'.repeat(id.length - 4)}${id.slice(-4)}`;
}
