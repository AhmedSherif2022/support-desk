import { computed, Injectable, signal } from "@angular/core";
import { SignInError, SignInRequest, User } from "./auth.models";
import { Observable, switchMap, throwError, timer, map } from 'rxjs';

/** 5 wrong tries within 15 minutes lock the account for 15 minutes. */
const MAX_ATTEMPTS = 5;
const ATTEMPT_WINDOW_MS = 15 * 60_000;
export const LOCK_MS = 15 * 1_000;

/** localStorage keys, so a refresh or a new tab can't skip the lock. */
const LOCK_KEY = 'dsp.signin.lock';
const FAILED_KEY = 'dsp.signin.failedAt';

export interface AccountLock {
    username: string;
    until: Date;
}

@Injectable({ providedIn: 'root'})
export class AuthService {

    private readonly _user = signal<User | null>(null);
    readonly user = this._user.asReadonly();
    readonly isSignIn = computed(() => this._user() !==null);

    /** The account that is locked right now (null = nothing locked). */
    private readonly _lock = signal<AccountLock | null>(this.readLock());
    readonly lock = this._lock.asReadonly();

    /** Times of recent failed attempts, whatever username was typed. */
    private failedAt: number[] = this.read<number[]>(FAILED_KEY) ?? [];

    SignIn(req: SignInRequest): Observable<User> {
        const users: Record<string, User>={
            'rwan.albagory': {
                username: 'rwan.albagory',
                password: 'Rwan@123',
                name: 'Rwan Albagory',
                role: 'agent' 
            },
            'youssef.adel': { 
                username: 'youssef.adel', 
                password: 'Yous@123',
                name: 'Youssef Adel', 
                role: 'guide' 
            },
            'nour.saleh': { 
                username: 'nour.saleh',
                password: 'Nour@123', 
                name: 'Nour Saleh', 
                role: 'reviewer' 
            },
        };

        const key = req.username.trim().toLowerCase();

        // Still locked → refuse without checking the password.
        const lock = this._lock();
        if (lock && lock.until.getTime() > Date.now()) {
            return this.fail('locked');
        }

        const user = users[key];
        if (!user || user.password !== req.password) {
            return this.fail(this.recordFailure(key));
        }

        this.failedAt = [];
        this.save(FAILED_KEY, null);
        this._user.set(user);
        return timer(600).pipe(map(() => user));
    }

    /** Called when the lock timer ended and the user goes back to the form. */
    clearLock() {
        this._lock.set(null);
        this.save(LOCK_KEY, null);
    }

    signOut() {
        this._user.set(null);
    }

    /** Counts a wrong try; the 5th one in 15 minutes locks the account. */
    private recordFailure(key: string): SignInError {
        const now = Date.now();
        this.failedAt = this.failedAt.filter(t => now - t < ATTEMPT_WINDOW_MS);
        this.failedAt.push(now);
    
        this.save(FAILED_KEY, this.failedAt);

        if (this.failedAt.length >= MAX_ATTEMPTS) {
            this.failedAt = [];
            this.save(FAILED_KEY, null);
            const lock = { username: key, until: new Date(now + LOCK_MS) };
            this._lock.set(lock);
            this.save(LOCK_KEY, lock);
            return 'locked';
        }
        return 'invalid';
    }

    /** The saved lock stays until the user presses "Sign in" after the timer, even if it already ran out. */
    private readLock(): AccountLock | null {
        const saved = this.read<{ username: string; until: string }>(LOCK_KEY);
        return saved ? { username: saved.username, until: new Date(saved.until) } : null;
    }

    private read<T>(key: string): T | null {
        try {
            const raw = localStorage.getItem(key);
            return raw ? (JSON.parse(raw) as T) : null;
        } catch {
            return null; // private window or blocked storage
        }
    }

    private save(key: string, value: unknown) {
        try {
            if (value === null) localStorage.removeItem(key);
            else localStorage.setItem(key, JSON.stringify(value));
        } catch {
            // storage unavailable: the lock still works until the page is refreshed
        }
    }

    /** Wait 600ms like a real request, then fail. (delay() does not delay errors, timer + switchMap does.) */
    private fail(error: SignInError): Observable<never> {
        return timer(600).pipe(switchMap(() => throwError(() => error)));
    }
}
