import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  ArrowLeft,
  Building2,
  CheckCircle2,
  Clock3,
  FileCheck2,
  Loader2,
  RefreshCw,
  ShieldAlert,
} from "lucide-react";
import {
  useLocation,
  useNavigate,
} from "react-router-dom";
import { toast } from "react-toastify";

import AuthLayout from "../../components/auth/AuthLayout";
import { API_BASE_URL } from "../../utils/api";

export default function OperatorApplicationStatusPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const loggedInUser = useMemo(() => {
    try {
      return JSON.parse(
        localStorage.getItem("yatayatUser") || "null"
      );
    } catch {
      return null;
    }
  }, []);

  const [status, setStatus] = useState(
    location.state?.status ||
      localStorage.getItem(
        "operatorApplicationStatus"
      ) ||
      "PENDING"
  );

  const [operator, setOperator] = useState(
    location.state?.operator || null
  );

  const [message, setMessage] = useState(
    location.state?.message ||
      "Your operator application is under review."
  );

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] =
    useState(false);
  const [error, setError] = useState("");

  const fetchStatus = useCallback(async (
    manualRefresh = false
  ) => {
    if (!loggedInUser?.id) {
      setError(
        "Operator account information was not found. Please log in again."
      );
      setLoading(false);
      return;
    }

    try {
      if (manualRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      setError("");

      const response = await fetch(
        `${API_BASE_URL}/api/operators/status/${loggedInUser.id}`,
        { credentials: "include" }
      );

      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(
          data.message ||
            "Unable to load operator application status."
        );
      }

      const latestStatus = String(
        data.status || "NOT_SUBMITTED"
      ).toUpperCase();

      localStorage.setItem(
        "operatorApplicationStatus",
        latestStatus
      );

      setStatus(latestStatus);
      setOperator(data.operator || null);
      setMessage(
        getStatusMessage(
          latestStatus,
          data.message,
          data.operator
        )
      );

      if (
        latestStatus === "NOT_SUBMITTED" ||
        latestStatus === "DRAFT"
      ) {
        navigate("/operator/application", {
          replace: true,
        });

        return;
      }

      if (latestStatus === "APPROVED") {
        toast.success(
          "Your operator account has been approved."
        );
      }
    } catch (statusError) {
      console.error(
        "Operator status loading error:",
        statusError
      );

      setError(
        statusError.message ||
          "Unable to load operator application status."
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [loggedInUser, navigate]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void fetchStatus();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [fetchStatus]);

  const openDashboard = () => {
    if (status !== "APPROVED") {
      toast.info(
        "Your operator account must be approved first."
      );
      return;
    }

    navigate("/operator/dashboard", {
      replace: true,
    });
  };

  const editAndResubmit = () => {
    if (status !== "REJECTED") {
      return;
    }

    navigate("/operator/application", {
      state: {
        application: operator,
        mode: "RESUBMIT",
      },
    });
  };

  const handleLogout = () => {
    localStorage.removeItem("yatayatUser");
    localStorage.removeItem("loginTime");
    localStorage.removeItem(
      "operatorApplicationStatus"
    );

    navigate("/login", {
      replace: true,
    });
  };

  if (loading) {
    return (
      <AuthLayout>
        <div className="w-full max-w-xl rounded-2xl border border-slate-200 bg-white p-6 text-center shadow-xl">
          <Loader2
            size={42}
            className="mx-auto animate-spin text-[#08264a]"
          />

          <h2 className="mt-5 text-xl font-semibold text-slate-900">
            Checking operator status
          </h2>

          <p className="mt-2 text-sm text-slate-500">
            Please wait while we load the latest admin
            decision.
          </p>
        </div>
      </AuthLayout>
    );
  }

  const config = getStatusConfig(status);

  return (
    <AuthLayout>
      <div className="w-full max-w-3xl overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl">
        <div
          className={`${config.headerStyle} px-6 py-8 text-white sm:px-9`}
        >
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
            <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-white/15">
              {config.icon}
            </div>

            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-white/75">
                Transport Operator Verification
              </p>

              <h1 className="mt-2 text-2xl font-semibold sm:text-3xl">
                {config.title}
              </h1>

              <p className="mt-2 max-w-2xl text-sm leading-6 text-white/80">
                {message}
              </p>
            </div>
          </div>
        </div>

        <div className="px-6 py-7 sm:px-9">
          {error && (
            <div className="mb-6 flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700">
              <AlertCircle
                size={19}
                className="mt-0.5 shrink-0"
              />
              <p>{error}</p>
            </div>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <StatusStep
              label="Application submitted"
              completed
            />

            <StatusStep
              label="Admin verification"
              active={status === "PENDING"}
              completed={[
                "APPROVED",
                "REJECTED",
                "SUSPENDED",
              ].includes(status)}
            />

            <StatusStep
              label={
                status === "APPROVED"
                  ? "Approved"
                  : status === "REJECTED"
                  ? "Correction required"
                  : status === "SUSPENDED"
                  ? "Suspended"
                  : "Final decision"
              }
              completed={[
                "APPROVED",
                "REJECTED",
                "SUSPENDED",
              ].includes(status)}
            />
          </div>

          <div className="mt-7 rounded-2xl border border-slate-200 bg-slate-50 p-5">
            <div className="flex items-start gap-3">
              <Building2
                size={23}
                className="mt-0.5 shrink-0 text-[#08264a]"
              />

              <div>
                <h2 className="font-semibold text-slate-900">
                  What happens next?
                </h2>

                <p className="mt-2 text-sm leading-6 text-slate-600">
                  {config.nextStep}
                </p>
              </div>
            </div>
          </div>

          {operator && (
            <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
              <DetailCard
                label="Organization"
                value={operator.name}
              />

              <DetailCard
                label="Operator Type"
                value={formatOperatorType(
                  operator.operatorType
                )}
              />

              <DetailCard
                label="Registration Number"
                value={operator.registrationNumber}
              />

              <DetailCard
                label="Permit Number"
                value={operator.permitNumber}
              />

              <DetailCard
                label="Contact Person"
                value={operator.contactPerson}
              />

              <DetailCard
                label="Verification Status"
                value={
                  operator.verificationStatus || status
                }
              />
            </div>
          )}

          {status === "REJECTED" && (
            <div className="mt-4 rounded-2xl border border-red-200 bg-red-50 p-5">
              <div className="flex items-start gap-3">
                <ShieldAlert
                  size={23}
                  className="mt-0.5 shrink-0 text-red-600"
                />

                <div>
                  <h3 className="font-semibold text-red-800">
                    Application requires correction
                  </h3>

                  <p className="mt-2 text-sm leading-6 text-red-700">
                    {operator?.rejectionReason ||
                      "The administrator rejected this application. Review the information and resubmit it."}
                  </p>
                </div>
              </div>
            </div>
          )}

          <div className="mt-7 flex flex-col gap-3 border-t border-slate-200 pt-6 sm:flex-row sm:justify-between">
            <button
              type="button"
              onClick={handleLogout}
              className="flex items-center justify-center gap-2 rounded-2xl border border-slate-300 px-6 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
            >
              <ArrowLeft size={18} />
              Logout
            </button>

            <div className="flex flex-col gap-3 sm:flex-row">
              <button
                type="button"
                onClick={() => fetchStatus(true)}
                disabled={refreshing}
                className="flex items-center justify-center gap-2 rounded-2xl border border-[#08264a] px-6 py-2.5 text-sm font-semibold text-[#08264a] transition hover:bg-blue-50 disabled:opacity-60"
              >
                <RefreshCw
                  size={18}
                  className={
                    refreshing ? "animate-spin" : ""
                  }
                />

                {refreshing
                  ? "Checking..."
                  : "Refresh Status"}
              </button>

              {status === "REJECTED" && (
                <button
                  type="button"
                  onClick={editAndResubmit}
                  className="rounded-2xl bg-[#08264a] px-6 py-2.5 text-sm font-semibold text-white transition hover:bg-[#0d3566]"
                >
                  Edit and Resubmit
                </button>
              )}

              {status === "APPROVED" && (
                <button
                  type="button"
                  onClick={openDashboard}
                  className="flex items-center justify-center gap-2 rounded-2xl bg-emerald-600 px-6 py-2.5 text-sm font-semibold text-white transition hover:bg-emerald-700"
                >
                  <CheckCircle2 size={18} />
                  Open Operator Dashboard
                </button>
              )}

              {status === "PENDING" && (
                <button
                  type="button"
                  disabled
                  className="flex cursor-not-allowed items-center justify-center gap-2 rounded-2xl bg-slate-200 px-6 py-2.5 text-sm font-semibold text-slate-500"
                >
                  <Clock3 size={18} />
                  Awaiting Admin Review
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </AuthLayout>
  );
}

function getStatusMessage(
  status,
  backendMessage,
  operator
) {
  if (backendMessage) {
    return backendMessage;
  }

  switch (status) {
    case "APPROVED":
      return "Your operator account has been approved. You may now manage buses, drivers, routes and trips.";

    case "REJECTED":
      return (
        operator?.rejectionReason ||
        "Your operator application requires correction."
      );

    case "SUSPENDED":
      return "Your operator account is currently suspended.";

    default:
      return "Your operator application is awaiting admin review.";
  }
}

function getStatusConfig(status) {
  switch (status) {
    case "APPROVED":
      return {
        title: "Operator Approved",
        headerStyle: "bg-emerald-600",
        icon: <CheckCircle2 size={34} />,
        nextStep:
          "Your operator dashboard is unlocked. You can now register buses and manage your transport operations.",
      };

    case "REJECTED":
      return {
        title: "Application Rejected",
        headerStyle: "bg-red-700",
        icon: <AlertCircle size={34} />,
        nextStep:
          "Review the rejection reason, correct your organization information and resubmit the application.",
      };

    case "SUSPENDED":
      return {
        title: "Operator Suspended",
        headerStyle: "bg-slate-800",
        icon: <ShieldAlert size={34} />,
        nextStep:
          "Operator features are disabled. Contact the Yatayat platform administrator.",
      };

    default:
      return {
        title: "Application Under Review",
        headerStyle: "bg-[#08264a]",
        icon: <Clock3 size={34} />,
        nextStep:
          "The platform administrator will verify your registration details, permit information and organization identity.",
      };
  }
}

function StatusStep({
  label,
  completed,
  active,
}) {
  return (
    <div
      className={`rounded-2xl border p-4 ${
        active
          ? "border-blue-300 bg-blue-50"
          : completed
          ? "border-emerald-200 bg-emerald-50"
          : "border-slate-200 bg-white"
      }`}
    >
      <div className="flex items-center gap-3">
        <span
          className={`flex h-9 w-9 items-center justify-center rounded-full ${
            completed
              ? "bg-emerald-600 text-white"
              : active
              ? "bg-[#08264a] text-white"
              : "bg-slate-200 text-slate-500"
          }`}
        >
          {completed ? (
            <CheckCircle2 size={18} />
          ) : active ? (
            <Clock3 size={18} />
          ) : (
            <FileCheck2 size={18} />
          )}
        </span>

        <p className="text-sm font-semibold text-slate-800">
          {label}
        </p>
      </div>
    </div>
  );
}

function DetailCard({ label, value }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4">
      <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
        {label}
      </p>

      <p className="mt-2 wrap-break-word text-sm font-semibold text-slate-900">
        {value || "Not available"}
      </p>
    </div>
  );
}

function formatOperatorType(type) {
  return String(type || "")
    .toLowerCase()
    .split("_")
    .map(
      (part) =>
        part.charAt(0).toUpperCase() +
        part.slice(1)
    )
    .join(" ");
}
