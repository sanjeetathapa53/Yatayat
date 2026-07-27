import { useState } from "react";
import {
  ArrowRight,
  Eye,
  EyeOff,
  Loader2,
  Lock,
  Mail,
  Phone,
  User,
} from "lucide-react";
import { FcGoogle } from "react-icons/fc";
import { FaFacebook } from "react-icons/fa";
import {
  Link,
  useNavigate,
} from "react-router-dom";
import { toast } from "react-toastify";

import AuthLayout from "../../components/auth/AuthLayout";
import OtpModal from "../../components/auth/OtpModal";
import RoleTabs from "../../components/auth/RoleTabs";
import { useAuth } from "../../hooks/useAuth";
import { API_BASE_URL } from "../../utils/api";

export default function RegisterPage() {
  const navigate = useNavigate();
  const { restoreSession } = useAuth();

  const [role, setRole] = useState("passenger");
  const [showPassword, setShowPassword] =
    useState(false);

  const [otp, setOtp] = useState("");
  const [showOtpModal, setShowOtpModal] =
    useState(false);
  const [pendingUserData, setPendingUserData] =
    useState(null);

  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");

  const [sendingOtp, setSendingOtp] =
    useState(false);
  const [verifyingOtp, setVerifyingOtp] =
    useState(false);

  const getBackendRole = () => {
    switch (role) {
      case "driver":
        return "DRIVER";

      case "operator":
        return "OPERATOR";

      default:
        return "PASSENGER";
    }
  };

  const getRoleLabel = () => {
    switch (role) {
      case "driver":
        return "Driver";

      case "operator":
        return "Transport Operator";

      default:
        return "Passenger";
    }
  };

  const handleRegister = async (event) => {
    event.preventDefault();

    const normalizedEmail = email
      .trim()
      .toLowerCase();

    if (
      !fullName.trim() ||
      !normalizedEmail ||
      !phone.trim() ||
      !password
    ) {
      toast.error("Please complete all required fields.");
      return;
    }

    if (password.length < 6) {
      toast.error(
        "Password must contain at least 6 characters."
      );
      return;
    }

    const userData = {
      fullName: fullName.trim(),
      email: normalizedEmail,
      phone: phone.trim(),
      password,
      role: getBackendRole(),
    };

    try {
      setSendingOtp(true);

      const response = await fetch(
        `${API_BASE_URL}/api/auth/send-otp`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(userData),
        }
      );

      const message = await response.text();

      if (message !== "OTP sent to email") {
        toast.error(message || "Unable to send OTP.");
        return;
      }

      setPendingUserData(userData);
      setOtp("");
      setShowOtpModal(true);

      toast.success(
        `OTP sent to ${normalizedEmail}`
      );
    } catch (error) {
      console.error("OTP sending error:", error);

      toast.error(
        "Failed to send OTP. Make sure the backend is running."
      );
    } finally {
      setSendingOtp(false);
    }
  };

  const handleVerifyOtp = async () => {
    if (!pendingUserData) {
      toast.error(
        "Registration information was not found."
      );
      return;
    }

    if (!otp.trim()) {
      toast.error("Please enter the OTP.");
      return;
    }

    try {
      setVerifyingOtp(true);

      const verifyResponse = await fetch(
        `${API_BASE_URL}/api/auth/verify-otp`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email: pendingUserData.email,
            otp: otp.trim(),
          }),
        }
      );

      const verifyMessage =
        await verifyResponse.text();

      if (verifyMessage !== "OTP verified") {
        toast.error(
          verifyMessage || "OTP verification failed."
        );
        return;
      }

      const registerResponse = await fetch(
        `${API_BASE_URL}/api/auth/register`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(pendingUserData),
        }
      );

      const registerMessage =
        await registerResponse.text();

      if (
        registerMessage !==
        "Successfully registered"
      ) {
        toast.error(
          registerMessage || "Registration failed."
        );
        return;
      }

      setShowOtpModal(false);

      toast.success(
        `${getRegistrationSuccessMessage(
          pendingUserData.role
        )}`
      );

      if (pendingUserData.role === "DRIVER") {
        await restoreSession();
        navigate("/driver/application", {
          replace: true,
          state: {
            email: pendingUserData.email,
            fullName: pendingUserData.fullName,
          },
        });

        return;
      }

      if (pendingUserData.role === "OPERATOR") {
        /*
         * The operator first logs in so we receive the newly
         * created user ID from /api/auth/login.
         *
         * LoginPage will then check:
         * GET /api/operators/status/{userId}
         *
         * NOT_SUBMITTED redirects to /operator/application.
         */
        navigate(
          "/login?registeredRole=OPERATOR",
          {
            replace: true,
          }
        );

        return;
      }

      navigate("/login", {
        replace: true,
      });
    } catch (error) {
      console.error(
        "OTP verification or registration error:",
        error
      );

      toast.error(
        "Registration failed. Please try again."
      );
    } finally {
      setVerifyingOtp(false);
    }
  };

  const handleRoleChange = (newRole) => {
    setRole(newRole);
    setShowOtpModal(false);
    setOtp("");
    setPendingUserData(null);
  };

  return (
    <AuthLayout>
      <div className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-xl">
        <RoleTabs
          role={role}
          setRole={handleRoleChange}
        />

        <div className="mt-6">
          <h2 className="text-3xl font-black text-[#08264a]">
            Create Account
          </h2>

          <p className="mt-2 text-sm leading-6 text-slate-500">
            Register as{" "}
            <span className="font-bold text-slate-700">
              {getRoleLabel()}
            </span>{" "}
            to start using Yatayat.
          </p>

          {role === "operator" && (
            <div className="mt-4 rounded-2xl border border-blue-200 bg-blue-50 p-4 text-sm leading-6 text-blue-800">
              Register an operator account first. After
              logging in, you can submit your organization
              details for admin approval and then manage your
              buses.
            </div>
          )}

          {role === "driver" && (
            <div className="mt-4 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-800">
              After creating your account, you must submit your
              licence and identity information for admin
              verification.
            </div>
          )}
        </div>

        <form
          onSubmit={handleRegister}
          className="mt-6 space-y-4"
        >
          <div>
            <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
              {role === "operator"
                ? "Account Holder Name"
                : "Full Name"}
            </label>

            <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 focus-within:border-[#08264a] focus-within:bg-white">
              <User
                size={18}
                className="shrink-0 text-slate-400"
              />

              <input
                type="text"
                placeholder={
                  role === "operator"
                    ? "Operations Manager"
                    : "Sanjita Thapa"
                }
                value={fullName}
                onChange={(event) =>
                  setFullName(event.target.value)
                }
                required
                autoComplete="name"
                className="w-full bg-transparent text-sm outline-none"
              />
            </div>
          </div>

          <div>
            <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
              Email Address
            </label>

            <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 focus-within:border-[#08264a] focus-within:bg-white">
              <Mail
                size={18}
                className="shrink-0 text-slate-400"
              />

              <input
                type="email"
                placeholder="name@example.com"
                value={email}
                onChange={(event) =>
                  setEmail(event.target.value)
                }
                required
                autoComplete="email"
                className="w-full bg-transparent text-sm outline-none"
              />
            </div>
          </div>

          <div>
            <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
              Phone Number
            </label>

            <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 focus-within:border-[#08264a] focus-within:bg-white">
              <Phone
                size={18}
                className="shrink-0 text-slate-400"
              />

              <input
                type="tel"
                placeholder="+977 98XXXXXXXX"
                value={phone}
                onChange={(event) =>
                  setPhone(event.target.value)
                }
                required
                autoComplete="tel"
                className="w-full bg-transparent text-sm outline-none"
              />
            </div>
          </div>

          <div>
            <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
              Password
            </label>

            <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 focus-within:border-[#08264a] focus-within:bg-white">
              <Lock
                size={18}
                className="shrink-0 text-slate-400"
              />

              <input
                type={
                  showPassword
                    ? "text"
                    : "password"
                }
                placeholder="Create a strong password"
                value={password}
                onChange={(event) =>
                  setPassword(event.target.value)
                }
                required
                minLength={6}
                autoComplete="new-password"
                className="w-full bg-transparent text-sm outline-none"
              />

              <button
                type="button"
                onClick={() =>
                  setShowPassword(
                    (current) => !current
                  )
                }
                className="rounded-lg p-1 text-slate-500 transition hover:bg-slate-200"
                aria-label={
                  showPassword
                    ? "Hide password"
                    : "Show password"
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

          <label className="flex items-start gap-2 text-xs leading-5 text-slate-500">
            <input
              type="checkbox"
              required
              className="mt-1 h-4 w-4 accent-[#08264a]"
            />

            <span>
              I agree to the Terms of Service and
              Privacy Policy.
            </span>
          </label>

          <button
            type="submit"
            disabled={sendingOtp || verifyingOtp}
            className="flex w-full items-center justify-center gap-2 rounded-2xl bg-[#08264a] py-3 text-sm font-bold text-white transition hover:bg-[#0d3566] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {sendingOtp ? (
              <>
                <Loader2
                  size={18}
                  className="animate-spin"
                />
                Sending OTP...
              </>
            ) : (
              <>
                Register Account
                <ArrowRight size={18} />
              </>
            )}
          </button>
        </form>

        <div className="my-5 flex items-center gap-4">
          <div className="h-px flex-1 bg-slate-200" />

          <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
            OR SIGN UP WITH
          </span>

          <div className="h-px flex-1 bg-slate-200" />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <button
            type="button"
            onClick={() => {
              window.location.href =
                `${API_BASE_URL}/api/auth/google-register`;
            }}
            className="flex items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            <FcGoogle size={21} />
            Google
          </button>

          <button
            type="button"
            onClick={() =>
              toast.info(
                "Facebook registration will be added later."
              )
            }
            className="flex items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white py-3 text-sm font-semibold text-slate-500 transition hover:bg-slate-50"
          >
            <FaFacebook
              size={19}
              className="text-blue-600"
            />
            Facebook
          </button>
        </div>

        {role === "operator" && (
          <p className="mt-4 text-center text-xs leading-5 text-amber-700">
            Google registration currently uses the default
            account role. Use email registration for an
            operator account until Google role selection is
            connected.
          </p>
        )}

        <p className="mt-5 text-center text-sm text-slate-600">
          Already have an account?{" "}
          <Link
            to="/login"
            className="font-bold text-emerald-600"
          >
            Login Now
          </Link>
        </p>
      </div>

      {showOtpModal && (
        <OtpModal
          otp={otp}
          setOtp={setOtp}
          onVerify={handleVerifyOtp}
          onClose={() => {
            if (!verifyingOtp) {
              setShowOtpModal(false);
            }
          }}
          loading={verifyingOtp}
        />
      )}
    </AuthLayout>
  );
}

function getRegistrationSuccessMessage(role) {
  switch (role) {
    case "DRIVER":
      return "Driver account created successfully. Complete your driver application.";

    case "OPERATOR":
      return "Operator account created successfully. Log in to submit your organization application.";

    default:
      return "Passenger account created successfully. You can now log in.";
  }
}
