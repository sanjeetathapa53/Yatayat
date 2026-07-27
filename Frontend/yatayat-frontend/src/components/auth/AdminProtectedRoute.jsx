import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";

export default function AdminProtectedRoute() {
  const { user, restoring } = useAuth();

  if (restoring) {
    return <div className="min-h-screen bg-slate-50" aria-label="Restoring session" />;
  }

  if (!user || user.role !== "ADMIN") {
    return <Navigate to="/admin/login" replace />;
  }

  return <Outlet />;
}
