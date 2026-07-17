import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { useAuth } from "../../context/AuthContext";

export default function GoogleSuccessPage() {
  const navigate = useNavigate();
  const { restoreSession } = useAuth();

  useEffect(() => {
    restoreSession().then((user) => {
      if (!user) {
        toast.error("Google sign-in could not be restored. Please try again.");
        navigate("/login?googleError=session", { replace: true });
        return;
      }
      toast.success("Google login successful");
      const destinations = {
        PASSENGER: "/passenger/dashboard",
        DRIVER: "/driver/dashboard",
        OPERATOR: "/operator/dashboard",
        ADMIN: "/admin/dashboard",
      };
      navigate(destinations[user.role] || "/login", { replace: true });
    });
  }, [navigate, restoreSession]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100">
      <h1 className="text-2xl font-black text-[#08264a]">
        Signing you in with Google...
      </h1>
    </div>
  );
}
