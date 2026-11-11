import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { SignInError, SignInRequest } from '../../core/theme/auth/auth.models';
import { AuthService, LOCK_MS } from '../../core/theme/auth/auth.service';
import { ROLE_HOME } from '../../core/theme/auth/role-home';
import { DsStatusPill } from '../../shared/ui';
import { BrandPanel } from './components/brand-panel/brand-panel';
import { SignInForm } from './components/sign-in-form/sign-in-form';

@Component({
  selector: 'app-sign-in',
  imports: [DatePipe, BrandPanel, SignInForm, DsStatusPill],
  templateUrl: './sign-in.html',
  styleUrl: './sign-in.css',
})

export class SignIn {
  private auth = inject(AuthService);
  private router = inject(Router);


  loading = signal(false);
  error = signal<SignInError>(null);

  // ---------- locked account ----------
  /** The lock comes from the service, so it survives leaving and coming back to this page. */
  lock = this.auth.lock;
  /** Ticks every second for the countdown. */
  now = signal(Date.now());

  lockLeft = computed(() => {
    const l = this.lock();
    return l ? Math.max(0, l.until.getTime() - this.now()) : 0;
  });

  /** Share of the 15 minutes already passed, for the bar. */
  lockProgress = computed(() => Math.round(100 - (this.lockLeft() / LOCK_MS) * 100));

  /** 585000 ms → "9:45" */
  lockLabel = computed(() => {
    const seconds = Math.ceil(this.lockLeft() / 1000);
    return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
  });

  constructor() {
    const id = setInterval(() => this.now.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(id));
  }


  onSubmit(req: SignInRequest) {
    

    this.loading.set(true);
    this.error.set(null);

    this.auth.SignIn(req)
    .pipe(finalize(() => this.loading.set(false)))
    .subscribe({
      next: user => this.router.navigateByUrl(ROLE_HOME[user.role]),
      error: (err: SignInError) => {
        this.error.set(err);
      },
    });
  }

  /** The timer ended: back to the form, with "Session expired" and "Please sign in again". */
  backToSignIn() {
    this.auth.clearLock();
    this.error.set('expired');
  }
}
