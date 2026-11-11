import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Keeps the user on the sign-in page while the account is locked:
 * the browser Back button and links can't leave it until the lock is cleared
 * (the timer ends and the user presses "Sign in").
 */
export const lockedGuard: CanDeactivateFn<unknown> = () => inject(AuthService).lock() === null;
