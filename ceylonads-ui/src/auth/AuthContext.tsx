import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import * as authApi from "../api/authApi";
import { clearAuth, getStoredAuth, setAuth, type StoredAuth } from "./authStorage";
import { AUTH_CLEARED_EVENT } from "./authEvents";
import type { LoginRequest, RegisterRequest, Role } from "../types/api";

interface AuthContextValue {
  isAuthenticated: boolean;
  username: string | null;
  role: Role | null;
  login: (payload: LoginRequest) => Promise<void>;
  register: (payload: RegisterRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuthState] = useState<StoredAuth | null>(() => getStoredAuth());

  useEffect(() => {
    const handleCleared = () => setAuthState(null);
    window.addEventListener(AUTH_CLEARED_EVENT, handleCleared);
    return () => window.removeEventListener(AUTH_CLEARED_EVENT, handleCleared);
  }, []);

  const login = useCallback(async (payload: LoginRequest) => {
    const response = await authApi.login(payload);
    const stored: StoredAuth = {
      token: response.accessToken,
      username: response.username,
      role: response.role,
      expiresAt: response.expiresAt,
    };
    setAuth(stored);
    setAuthState(stored);
  }, []);

  const register = useCallback(async (payload: RegisterRequest) => {
    const response = await authApi.register(payload);
    const stored: StoredAuth = {
      token: response.accessToken,
      username: response.username,
      role: response.role,
      expiresAt: response.expiresAt,
    };
    setAuth(stored);
    setAuthState(stored);
  }, []);

  const logout = useCallback(() => {
    clearAuth();
    setAuthState(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      isAuthenticated: auth !== null,
      username: auth?.username ?? null,
      role: auth?.role ?? null,
      login,
      register,
      logout,
    }),
    [auth, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
