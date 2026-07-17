import { Navigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export default function ProtectedRoute({ children, allowedRole }) {
  const { user, restoring } = useAuth();

  if (restoring) {
    return <div className="min-h-screen bg-slate-50" aria-label="Restoring session" />;
  }

  if (!user || (allowedRole && user.role !== allowedRole)) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
