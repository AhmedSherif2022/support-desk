import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CustomerSearch } from './customer-search';

describe('CustomerSearch', () => {
  let component: CustomerSearch;
  let fixture: ComponentFixture<CustomerSearch>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerSearch],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerSearch);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
