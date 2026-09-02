import type { Role } from "../types/api";

const TOKEN_KEY = "ceylonads.token";
const USERNAME_KEY = "ceylonads.username";
const ROLE_KEY = "ceylonads.role";
const EXPIRES_KEY = "ceylonads.expiresAt";

export interface StoredAuth {
  token: string;
  username: string;
  role: Role;
  expiresAt: string;
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredAuth(): StoredAuth | null {
  const token = localStorage.getItem(TOKEN_KEY);
  const username = localStorage.getItem(USERNAME_KEY);
  const role = localStorage.getItem(ROLE_KEY) as Role | null;
  const expiresAt = localStorage.getItem(EXPIRES_KEY);

  if (!token || !username || !role || !expiresAt) {
    return null;
  }

  if (new Date(expiresAt).getTime() <= Date.now()) {
    clearAuth();
    return null;
  }

  return { token, username, role, expiresAt };
}

export function setAuth(auth: StoredAuth): void {
  localStorage.setItem(TOKEN_KEY, auth.token);
  localStorage.setItem(USERNAME_KEY, auth.username);
  localStorage.setItem(ROLE_KEY, auth.role);
  localStorage.setItem(EXPIRES_KEY, auth.expiresAt);
}

export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USERNAME_KEY);
  localStorage.removeItem(ROLE_KEY);
  localStorage.removeItem(EXPIRES_KEY);
}
