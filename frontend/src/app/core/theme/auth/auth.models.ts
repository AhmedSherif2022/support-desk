export type Role = 'agent' | 'guide' | 'reviewer';

export interface User {
    username: string;
    password: string;
    name: string;
    role: Role;
}

export interface SignInRequest {
    username: string;
    password: string;
}

export type SignInError = 'invalid' | 'locked' | 'expired' | null;