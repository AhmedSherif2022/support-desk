import { Injectable } from '@angular/core';
import { Customer } from '../../features/customer-search/customer.models';
import { CUSTOMERS } from '../../features/customer-search/customers.mock';

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly customers = CUSTOMERS; // TODO: replace with HttpClient when the API is ready

  /** Numbers → search by CIF (starts with). Letters → search by name (contains). */
  search(term: string): Customer[] {
    const q = term.trim().toLowerCase();
    if (!q) return [];

    const isCif = /^\d+$/.test(q);
    return this.customers.filter(c =>
      isCif ? c.cif.startsWith(q) : c.name.toLowerCase().includes(q),
    );
  }
}
