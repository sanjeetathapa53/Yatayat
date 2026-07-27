import { useCallback, useEffect, useMemo, useState } from "react";
import { apiFetch } from "../utils/api";
import { AuthContext } from "./authContextValue";

const USER_KEYS = ["yatayatUser", "loginTime"];
const ADMIN_KEYS = ["yatayatAdmin", "adminAuthenticated"];

function clearIdentity() {
  [...USER_KEYS, ...ADMIN_KEYS].forEach((key) => localStorage.removeItem(key));
}

function storeIdentity(user) {
  clearIdentity();
  if (!user) return;
  if (user.role === "ADMIN") {
    localStorage.setItem("yatayatAdmin", JSON.stringify(user));
    localStorage.setItem("adminAuthenticated", "true");
  } else {
    localStorage.setItem("yatayatUser", JSON.stringify(user));
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [restoring, setRestoring] = useState(true);

  const setAuthenticatedUser = useCallback((authenticatedUser) => {
    storeIdentity(authenticatedUser);
    setUser(authenticatedUser);
  }, []);

  const clearAuthenticatedUser = useCallback(() => {
    clearIdentity();
    setUser(null);
  }, []);

  const restoreSession = useCallback(async () => {
    try {
      const response = await apiFetch("/api/auth/me");
      if (!response.ok) {
        clearAuthenticatedUser();
        return null;
      }
      const authenticatedUser = await response.json();
      setAuthenticatedUser(authenticatedUser);
      return authenticatedUser;
    } catch {
      clearAuthenticatedUser();
      return null;
    } finally {
      setRestoring(false);
    }
  }, [clearAuthenticatedUser, setAuthenticatedUser]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void restoreSession();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [restoreSession]);

  useEffect(() => {
    window.addEventListener("yatayat-auth-cleared", clearAuthenticatedUser);
    return () => window.removeEventListener("yatayat-auth-cleared", clearAuthenticatedUser);
  }, [clearAuthenticatedUser]);

  const value = useMemo(() => ({
    user, restoring, restoreSession, setAuthenticatedUser, clearAuthenticatedUser,
  }), [user, restoring, restoreSession, setAuthenticatedUser, clearAuthenticatedUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
