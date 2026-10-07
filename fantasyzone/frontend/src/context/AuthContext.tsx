import { createContext, useCallback, useContext, useEffect, useMemo, useReducer, type ReactNode } from 'react';
import type { AuthUser } from '../types';
import { setToken, setUnauthorizedHandler } from '../services/api';

const STORAGE_KEY = 'fz_user';

interface State { user: AuthUser | null }
type Action = { type: 'login'; user: AuthUser } | { type: 'logout' };

interface AuthContextValue {
  user: AuthUser | null;
  isAdmin: boolean;
  login: (user: AuthUser) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function reducer(_: State, action: Action): State {
  switch (action.type) {
    case 'login': return { user: action.user };
    case 'logout': return { user: null };
  }
}

function init(): State {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const user = JSON.parse(raw) as AuthUser;
      setToken(user.token);
      return { user };
    }
  } catch { /* ignore corrupt storage */ }
  return { user: null };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, undefined, init);

  const login = useCallback((user: AuthUser) => {
    setToken(user.token);
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(user)); } catch { /* storage unavailable */ }
    dispatch({ type: 'login', user });
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    try { localStorage.removeItem(STORAGE_KEY); } catch { /* storage unavailable */ }
    dispatch({ type: 'logout' });
  }, []);

  // If the API says our token is no longer valid, sign the user out.
  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, [logout]);

  const value = useMemo<AuthContextValue>(
    () => ({ user: state.user, isAdmin: state.user?.roles.includes('ROLE_ADMIN') ?? false, login, logout }),
    [state.user, login, logout],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
