import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";

export default function GoogleSuccessPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const user = {
      id: searchParams.get("id"),
      fullName: searchParams.get("fullName"),
      email: searchParams.get("email"),
      phone: searchParams.get("phone"),
      role: searchParams.get("role"),
    };

    localStorage.setItem("yatayatUser", JSON.stringify(user));
    localStorage.setItem("loginTime", Date.now().toString());

    toast.success("Google login successful");

    navigate("/passenger/dashboard");
  }, [navigate, searchParams]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100">
      <h1 className="text-2xl font-black text-[#08264a]">
        Signing you in with Google...
      </h1>
    </div>
  );
}