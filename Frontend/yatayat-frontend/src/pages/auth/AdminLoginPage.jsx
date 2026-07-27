import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import {
  ShieldCheck,
  Lock,
  Mail,
  ArrowRight,
  Eye,
  EyeOff,
  Loader2,
  AlertCircle,
} from "lucide-react";
import AuthLayout from "../../components/auth/AuthLayout";
import { apiFetch } from "../../utils/api";
import { consumeAdminLoginMessage } from "../../utils/adminSession";
import { useAuth } from "../../hooks/useAuth";

export default function AdminLoginPage() {
  const navigate = useNavigate();
  const { setAuthenticatedUser } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loggingIn, setLoggingIn] = useState(false);
  const [error, setError] = useState(consumeAdminLoginMessage);

  const handleAdminLogin = async (event) => {
    event.preventDefault();

    if (!email.trim() || !password.trim()) {
      setError("Please enter the admin email and password.");
      return;
    }

    try {
      setLoggingIn(true);
      setError("");

      const response = await apiFetch(
        "/api/admin/auth/login",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email: email.trim(),
            password,
          }),
        }
      );

      const data = await response.json().catch(() => ({}));

      if (!response.ok || !data.success) {
        setError(data.message || "Invalid admin credentials.");
        return;
      }

      setAuthenticatedUser(data.admin);

      navigate("/admin/dashboard", { replace: true });
    } catch (loginError) {
      console.error("Admin login error:", loginError);

      setError(
        "Unable to connect to the backend. Make sure Spring Boot is running."
      );
    } finally {
      setLoggingIn(false);
    }
  };

  return (
    <AuthLayout>
      <div className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-xl">
        <div className="flex justify-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-[#08264a] text-white">
            <ShieldCheck size={28} />
          </div>
        </div>

        <div className="mt-6 text-center">
          <h2 className="text-3xl font-black text-[#08264a]">
            Admin Login
          </h2>

          <p className="mt-2 text-sm text-slate-500">
            Secure access for Yatayat administrators.
          </p>
        </div>

        <form onSubmit={handleAdminLogin} className="mt-6 space-y-4">
          <div>
            <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
              Admin Email
            </label>

            <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 focus-within:border-[#08264a] focus-within:bg-white">
              <Mail size={18} className="text-slate-400" />

              <input
                type="email"
                value={email}
                onChange={(event) => {
                  setEmail(event.target.value);
                  setError("");
                }}
                placeholder="admin@example.com"
                autoComplete="email"
                required
                className="w-full bg-transparent text-sm outline-none"
              />
            </div>
          </div>

          <div>
            <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
              Password
            </label>

            <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 focus-within:border-[#08264a] focus-within:bg-white">
              <Lock size={18} className="text-slate-400" />

              <input
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(event) => {
                  setPassword(event.target.value);
                  setError("");
                }}
                placeholder="Enter admin password"
                autoComplete="current-password"
                required
                className="w-full bg-transparent text-sm outline-none"
              />

              <button
                type="button"
                onClick={() => setShowPassword((current) => !current)}
                className="rounded-lg p-1 text-slate-500 transition hover:bg-slate-200"
                aria-label={
                  showPassword ? "Hide password" : "Show password"
                }
              >
                {showPassword ? (
                  <EyeOff size={18} />
                ) : (
                  <Eye size={18} />
                )}
              </button>
            </div>
          </div>

          {error && (
            <div className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">
              <AlertCircle size={19} className="mt-0.5 shrink-0" />
              <p>{error}</p>
            </div>
          )}

          <button
            type="submit"
            disabled={loggingIn}
            className="flex w-full items-center justify-center gap-2 rounded-2xl bg-[#08264a] py-3 text-sm font-bold text-white transition hover:bg-[#0d3566] disabled:cursor-not-allowed disabled:opacity-70"
          >
            {loggingIn ? (
              <>
                <Loader2 size={18} className="animate-spin" />
                Logging in...
              </>
            ) : (
              <>
                Login as Admin
                <ArrowRight size={18} />
              </>
            )}
          </button>
        </form>

        <p className="mt-5 text-center text-sm text-slate-600">
          Not an admin?{" "}
          <Link to="/login" className="font-bold text-emerald-600">
            Back to user login
          </Link>
        </p>
      </div>
    </AuthLayout>
  );
}
