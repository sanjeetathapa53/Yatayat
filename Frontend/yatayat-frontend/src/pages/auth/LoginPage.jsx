import { useEffect, useState } from "react";
import {
  useNavigate,
  Link,
  useSearchParams,
} from "react-router-dom";
import {
  Eye,
  EyeOff,
  Lock,
  Mail,
  ArrowRight,
  Loader2,
} from "lucide-react";
import { FcGoogle } from "react-icons/fc";
import { toast } from "react-toastify";

import AuthLayout from "../../components/auth/AuthLayout";
import RoleTabs from "../../components/auth/RoleTabs";
import { API_BASE_URL, apiFetch } from "../../utils/api";
import { useAuth } from "../../hooks/useAuth";
import { logoutUser } from "../../services/authService";

export default function LoginPage() {
  const { setAuthenticatedUser } = useAuth();
  const [role, setRole] = useState("passenger");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  useEffect(() => {
    const googleError = searchParams.get("googleError");
    const googleRegistered = searchParams.get("googleRegistered");
    let handledParameter = null;

    if (googleError === "notRegistered") {
      toast.error(
        "No Yatayat account is associated with this Google account. Please register first.",
        { toastId: "oauth-not-registered" },
      );
      handledParameter = "googleError";
    } else if (googleRegistered === "true") {
      toast.success(
        "Account created successfully. Please sign in with Google.",
        { toastId: "oauth-registration-success" },
      );
      handledParameter = "googleRegistered";
    } else if (googleRegistered === "alreadyExists") {
      toast.info(
        "This Google account is already registered. Please sign in.",
        { toastId: "oauth-already-registered" },
      );
      handledParameter = "googleRegistered";
    } else if (googleError === "localAccount") {
      toast.error(
        "This account is registered with email and password. Please sign in using your email.",
        { toastId: "oauth-local-account" },
      );
      handledParameter = "googleError";
    } else if (googleError === "oauth") {
      toast.error(
        "Google sign-in was cancelled or could not be completed.",
        { toastId: "oauth-provider-failure" },
      );
      handledParameter = "googleError";
    } else if (googleError === "session") {
      toast.error(
        "Google sign-in session could not be restored. Please try again.",
        { toastId: "oauth-session-failure" },
      );
      handledParameter = "googleError";
    }

    if (handledParameter) {
      const remainingParams = new URLSearchParams(searchParams);
      remainingParams.delete(handledParameter);
      setSearchParams(remainingParams, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  const redirectDriverByStatus = async (user) => {
    const statusResponse = await apiFetch(
      `/api/drivers/status/${user.id}`
    );

    const statusData = await statusResponse.json();

    if (!statusResponse.ok || !statusData.success) {
      throw new Error(
        statusData.message ||
          "Unable to check driver application status."
      );
    }

    const status = String(
      statusData.status || "NOT_SUBMITTED"
    ).toUpperCase();

    localStorage.setItem("driverApplicationStatus", status);

    if (status === "NOT_SUBMITTED" || status === "DRAFT") {
      toast.info("Please complete your driver application.");

      navigate("/driver/application", {
        replace: true,
        state: {
          userId: user.id,
          email: user.email,
          fullName: user.fullName,
          application: statusData.application || null,
        },
      });

      return;
    }

    if (
      status === "PENDING" ||
      status === "REJECTED" ||
      status === "SUSPENDED"
    ) {
      navigate("/driver/application-status", {
        replace: true,
        state: {
          status,
          message: statusData.message,
          application: statusData.application || null,
        },
      });

      return;
    }

    if (status === "APPROVED") {
      toast.success("Driver login successful.");

      navigate("/driver/dashboard", {
        replace: true,
      });

      return;
    }

    navigate("/driver/application-status", {
      replace: true,
      state: {
        status,
        message: "Unable to identify driver application status.",
        application: statusData.application || null,
      },
    });
  };

  const redirectOperatorByStatus = async (user) => {
    const response = await apiFetch(
      `/api/operators/status/${user.id}`
    );
  
    const data = await response.json();
  
    if (!response.ok || !data.success) {
      throw new Error(
        data.message || "Unable to check operator status."
      );
    }
  
    const status = String(
      data.status || "NOT_SUBMITTED"
    ).toUpperCase();
  
    localStorage.setItem(
      "operatorApplicationStatus",
      status
    );
  
    if (
      status === "NOT_SUBMITTED" ||
      status === "DRAFT"
    ) {
      toast.info(
        "Please complete your operator application."
      );
  
      navigate("/operator/application", {
        replace: true,
        state: {
          userId: user.id,
          email: user.email,
          fullName: user.fullName,
        },
      });
  
      return;
    }
  
    if (
      status === "PENDING" ||
      status === "REJECTED" ||
      status === "SUSPENDED"
    ) {
      navigate("/operator/application-status", {
        replace: true,
        state: {
          status,
        },
      });
  
      return;
    }
  
    if (status === "APPROVED") {
      toast.success("Operator login successful.");
  
      navigate("/operator/dashboard", {
        replace: true,
      });
  
      return;
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();

    if (!email.trim() || !password.trim()) {
      toast.error("Please enter your email and password.");
      return;
    }

    try {
      setLoading(true);

      const response = await apiFetch(
        "/api/auth/login",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email: email.trim().toLowerCase(),
            password,
          }),
        }
      );

      const text = await response.text();

      if (text === "User not found") {
        toast.error("Email not registered");
        return;
      }

      if (text === "Invalid password") {
        toast.error("Incorrect password");
        return;
      }

      if (text === "This account uses Google Sign-In. Please continue with Google.") {
        toast.error(text);
        return;
      }

      if (!response.ok) {
        toast.error(text || "Login failed");
        return;
      }

      let data;

      try {
        data = JSON.parse(text);
      } catch {
        toast.error("Invalid response from server.");
        return;
      }

      if (role === "passenger" && data.role !== "PASSENGER") {
        await logoutUser();
        toast.error("This account is not registered as Passenger");
        return;
      }

      if (role === "driver" && data.role !== "DRIVER") {
        await logoutUser();
        toast.error("This account is not registered as Driver");
        return;
      }

      if (role === "operator" && data.role !== "OPERATOR") {
        await logoutUser();
        toast.error("This account is not registered as Operator");
        return;
      }

      setAuthenticatedUser(data);

      if (data.role === "PASSENGER") {
        toast.success("Login successful");

        navigate("/passenger/dashboard", {
          replace: true,
        });

        return;
      }

      if (data.role === "DRIVER") {
        await redirectDriverByStatus(data);
        return;
      }

      if (data.role === "OPERATOR") {
        await redirectOperatorByStatus(data);
        return;
    }


      toast.error("Unsupported account role.");
      localStorage.removeItem("yatayatUser");
      localStorage.removeItem("loginTime");
    } catch (error) {
      console.error("Login error:", error);

      toast.error(
        error.message ||
          "Login failed. Please check email and password."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout>
      <div className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-xl">
        <RoleTabs role={role} setRole={setRole} />

        <div className="mt-6">
          <h2 className="text-3xl font-black text-[#08264a]">
            Welcome Back
          </h2>

          <p className="mt-2 text-sm text-slate-500">
          Login as{" "}
{role === "passenger"
  ? "Passenger"
  : role === "driver"
  ? "Driver"
  : "Transport Operator"}{" "}
to continue.
          </p>
        </div>

        <form onSubmit={handleLogin} className="mt-6 space-y-4">
          <div>
            <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
              Email Address
            </label>

            <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
              <Mail size={18} className="text-slate-400" />

              <input
                type="email"
                placeholder="name@yatayat.np"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full bg-transparent text-sm outline-none"
              />
            </div>
          </div>

          <div>
            <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
              Password
            </label>

            <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
              <Lock size={18} className="text-slate-400" />

              <input
                type={showPassword ? "text" : "password"}
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="w-full bg-transparent text-sm outline-none"
              />

              <button
                type="button"
                onClick={() =>
                  setShowPassword((current) => !current)
                }
              >
                {showPassword ? (
                  <EyeOff
                    size={18}
                    className="text-slate-500"
                  />
                ) : (
                  <Eye
                    size={18}
                    className="text-slate-500"
                  />
                )}
              </button>
            </div>
          </div>

          <div className="flex items-center justify-between text-sm">
            <label className="flex items-center gap-2 text-slate-600">
              <input type="checkbox" className="h-4 w-4" />
              Remember me
            </label>

            <Link
              to="/forgot-password"
              className="font-semibold text-[#08264a]"
            >
              Forgot password?
            </Link>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="flex w-full items-center justify-center gap-2 rounded-2xl bg-[#08264a] py-3 text-sm font-bold text-white transition hover:bg-[#0d3566] disabled:cursor-not-allowed disabled:opacity-70"
          >
            {loading ? (
              <>
                <Loader2
                  size={18}
                  className="animate-spin"
                />
                Logging in...
              </>
            ) : (
              <>
                Login
                <ArrowRight size={18} />
              </>
            )}
          </button>
        </form>

        <div className="my-5 flex items-center gap-4">
          <div className="h-px flex-1 bg-slate-200" />

          <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
            OR LOGIN WITH
          </span>

          <div className="h-px flex-1 bg-slate-200" />
        </div>

        <div>
          <button
            type="button"
            onClick={() => {
              window.location.href =
                `${API_BASE_URL}/api/auth/google-login`;
            }}
            className="flex w-full items-center justify-center gap-2 rounded-2xl border border-slate-200 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            <FcGoogle size={20} />
            Continue with Google
          </button>

        </div>

        <p className="mt-5 text-center text-xs text-slate-500">
          Admin?{" "}
          <Link
            to="/admin/login"
            className="font-bold text-[#08264a]"
          >
            Go to Admin Login
          </Link>
        </p>

        <p className="mt-5 text-center text-sm text-slate-600">
          Don&apos;t have an account?{" "}
          <Link
            to="/register"
            className="font-bold text-emerald-600"
          >
            Register Now
          </Link>
        </p>
      </div>
    </AuthLayout>
  );
}
