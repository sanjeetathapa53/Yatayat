import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Mail, KeyRound, Lock, ArrowLeft } from "lucide-react";
import { toast } from "react-toastify";

import AuthLayout from "../../components/auth/AuthLayout";
import { apiFetch } from "../../utils/api";

export default function ForgotPasswordPage() {
  const navigate = useNavigate();

  const [step, setStep] = useState(1);

  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [resendSeconds, setResendSeconds] = useState(0);

  useEffect(() => {
    if (resendSeconds <= 0) return undefined;
    const timer = window.setInterval(
      () => setResendSeconds((value) => Math.max(0, value - 1)),
      1000,
    );
    return () => window.clearInterval(timer);
  }, [resendSeconds]);

  const sendOtp = async () => {
    if (loading || resendSeconds > 0 || !email.trim()) return;
    try {
      setLoading(true);
      const res = await apiFetch(
        "/api/auth/send-forgot-password-otp",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ email }),
        }
      );

      const text = await readResponseMessage(res);
      if (!res.ok) {
        toast.error(text || "Reset instructions could not be requested.");
        return;
      }
      toast.success(text);
      setStep(2);
      setResendSeconds(60);
    } catch {
      toast.error("Failed to send OTP.");
    } finally {
      setLoading(false);
    }
  };

  const resetPassword = async () => {
    if (loading) return;
    try {
      setLoading(true);
      const res = await apiFetch(
        "/api/auth/reset-password",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email,
            otp,
            newPassword,
          }),
        }
      );

      const text = await readResponseMessage(res);
      if (!res.ok) {
        toast.error(text || "Password reset could not be completed.");
        return;
      }

      toast.success("Password changed successfully!");

      setTimeout(() => {
        navigate("/login");
      }, 1200);
    } catch {
      toast.error("Something went wrong.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout>
      <div className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-7 shadow-xl">

        <button
          onClick={() => navigate("/login")}
          className="mb-5 flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-[#08264a]"
        >
          <ArrowLeft size={18} />
          Back to Login
        </button>

        <h2 className="text-3xl font-black text-[#08264a]">
          Forgot Password
        </h2>

        <p className="mt-2 text-sm text-slate-500">
          {step === 1
            ? "Enter your registered email."
            : "Enter the OTP sent to your email and choose a new password."}
        </p>

        {step === 1 && (
          <div className="mt-7 space-y-5">

            <div>
              <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
                Email
              </label>

              <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <Mail size={18} className="text-slate-400" />

                <input
                  type="email"
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full bg-transparent outline-none"
                />
              </div>
            </div>

            <button
              onClick={sendOtp}
              disabled={loading || !email.trim()}
              className="w-full rounded-2xl bg-[#08264a] py-3 font-bold text-white hover:bg-[#0d3566]"
            >
              {loading ? "Sending..." : "Send OTP"}
            </button>

          </div>
        )}

        {step === 2 && (
          <div className="mt-7 space-y-5">

            <div>
              <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
                OTP
              </label>

              <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <KeyRound size={18} className="text-slate-400" />

                <input
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))}
                  placeholder="Enter OTP"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={6}
                  className="w-full bg-transparent outline-none"
                />
              </div>
            </div>

            <div>
              <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
                New Password
              </label>

              <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <Lock size={18} className="text-slate-400" />

                <input
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="New Password"
                  className="w-full bg-transparent outline-none"
                />
              </div>
            </div>

            <button
              onClick={resetPassword}
              disabled={loading || otp.length !== 6 || newPassword.length < 6}
              className="w-full rounded-2xl bg-emerald-600 py-3 font-bold text-white hover:bg-emerald-700"
            >
              {loading ? "Resetting..." : "Reset Password"}
            </button>

            <button
              type="button"
              onClick={sendOtp}
              disabled={loading || resendSeconds > 0}
              className="w-full text-sm font-bold text-[#08264a] disabled:text-slate-400"
            >
              {resendSeconds > 0 ? `Resend OTP in ${resendSeconds}s` : "Resend OTP"}
            </button>

          </div>
        )}
      </div>
    </AuthLayout>
  );
}

async function readResponseMessage(response) {
  const text = await response.text();
  try {
    const body = JSON.parse(text);
    return body.detail || body.message || text;
  } catch {
    return text;
  }
}
