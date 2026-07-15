import { Navigate } from "react-router-dom";

export default function ProtectedRoute({ children, allowedRole }) {
  const storedUser = localStorage.getItem("yatayatUser");
  const loginTime = localStorage.getItem("loginTime");

  const user = storedUser ? JSON.parse(storedUser) : null;

  if (!user || !loginTime) {
    return <Navigate to="/login" replace />;
  }

  const sessionLimit = 30 * 60 * 1000;

  if (Date.now() - Number(loginTime) > sessionLimit) {
    localStorage.removeItem("yatayatUser");
    localStorage.removeItem("loginTime");
    return <Navigate to="/login" replace />;
  }

  if (allowedRole && user.role !== allowedRole) {
    return <Navigate to="/login" replace />;
  }

  return children;
}