import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { getPassengerNotificationUnreadCount } from "../utils/notifications";

const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
  const { user, restoring } = useAuth();
  const { pathname } = useLocation();
  const [unreadCount, setUnreadCount] = useState(0);

  const refreshUnreadCount = useCallback(async () => {
    if (!user || user.role !== "PASSENGER") {
      setUnreadCount(0);
      return 0;
    }
    try {
      const result = await getPassengerNotificationUnreadCount();
      const count = Number(result?.unreadCount || 0);
      setUnreadCount(count);
      return count;
    } catch {
      return 0;
    }
  }, [user]);

  useEffect(() => {
    if (!restoring) {
      Promise.resolve().then(refreshUnreadCount);
    }
  }, [pathname, restoring, refreshUnreadCount]);

  const value = useMemo(() => ({
    unreadCount,
    setUnreadCount,
    refreshUnreadCount,
  }), [refreshUnreadCount, unreadCount]);

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useNotifications() {
  const context = useContext(NotificationContext);
  if (!context) throw new Error("useNotifications must be used within NotificationProvider");
  return context;
}
