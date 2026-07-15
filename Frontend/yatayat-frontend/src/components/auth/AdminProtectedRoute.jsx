import { Navigate, Outlet } from "react-router-dom";

export default function AdminProtectedRoute() {
  const adminAuthenticated =
    localStorage.getItem("adminAuthenticated") === "true";

  const admin = JSON.parse(
    localStorage.getItem("yatayatAdmin") || "null"
  );

  const isValidAdmin =
    adminAuthenticated && admin?.role === "ADMIN";

  if (!isValidAdmin) {
    return <Navigate to="/admin/login" replace />;
  }

  return <Outlet />;
}