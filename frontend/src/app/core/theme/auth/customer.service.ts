import { Injectable, signal } from '@angular/core';
import { ActivityEvent, Customer, Incident } from '../../../features/customer-search/customer.models';
import { ACTIVITY, CUSTOMERS, INCIDENTS } from '../../../features/customer-search/customers.mock';

/**
 * Customers, their logs, and who has been verified on this call.
 * Tickets live in TicketService.
 * TODO: replace the mock data with HttpClient calls when the APIs are ready.
 */
@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly customers = CUSTOMERS;
  /** CIF → time the caller passed verification. Ahmed starts verified, as in the design. */
  private readonly verifiedAt = signal(new Map<string, Date>([['123456', new Date()]]));

  /** Last search, so going back to Customer search shows the same results. */
  readonly lastQuery = signal('');

  /** Numbers → search by CIF (starts with). Letters → search by name (contains). */
  search(term: string): Customer[] {
    const q = term.trim().toLowerCase();
    if (!q) return [];

    const isCif = /^\d+$/.test(q);
    return this.customers.filter(c =>
      isCif ? c.cif.startsWith(q) : c.name.toLowerCase().includes(q),
    );
  }

  getByCif(cif: string): Customer | undefined {
    return this.customers.find(c => c.cif === cif);
  }

  // ---------- verification ----------
  isVerified(cif: string): boolean {
    return this.verifiedAt().has(cif);
  }

  verifiedTime(cif: string): Date | undefined {
    return this.verifiedAt().get(cif);
  }

  markVerified(cif: string) {
    // New Map, so the signal sees a new reference.
    this.verifiedAt.update(map => new Map(map).set(cif, new Date()));
  }

  endVerification(cif: string) {
    this.verifiedAt.update(map => {
      const next = new Map(map);
      next.delete(cif);
      return next;
    });
  }

  // ---------- logs & incidents ----------
  activityFor(cif: string): ActivityEvent[] {
    return ACTIVITY.filter(e => e.cif === cif).sort((a, b) => b.at.getTime() - a.at.getTime());
  }

  incidentFor(category: string, platform: string): Incident | undefined {
    return INCIDENTS.find(i => i.category === category && i.platform === platform);
  }

  incidentById(id: string): Incident | undefined {
    return INCIDENTS.find(i => i.id === id);
  }

  activeIncidents(): Incident[] {
    return INCIDENTS;
  }
}
