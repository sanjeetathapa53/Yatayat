import {
    AlertCircle,
    ArrowLeft,
    CheckCircle2,
    Clock3,
    FileCheck2,
    RefreshCw,
    ShieldAlert,
  } from "lucide-react";
  import { useLocation, useNavigate } from "react-router-dom";
  import AuthLayout from "../../components/auth/AuthLayout";
  
  export default function DriverApplicationStatusPage() {
    const navigate = useNavigate();
    const location = useLocation();
  
    const status =
      location.state?.status ||
      localStorage.getItem("driverApplicationStatus") ||
      "PENDING";
  
    const message =
      location.state?.message ||
      "Your driver application is currently under review.";
  
    const application = location.state?.application || null;
  
    const statusConfig = getStatusConfig(status);
  
    return (
      <AuthLayout>
        <div className="w-full max-w-3xl overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-xl">
          <div className={`${statusConfig.headerStyle} px-6 py-8 text-white sm:px-9`}>
            <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-white/15">
                {statusConfig.icon}
              </div>
  
              <div>
                <p className="text-xs font-black uppercase tracking-[0.22em] text-white/75">
                  Driver Verification
                </p>
  
                <h1 className="mt-2 text-2xl font-black sm:text-3xl">
                  {statusConfig.title}
                </h1>
  
                <p className="mt-2 max-w-2xl text-sm leading-6 text-white/80">
                  {message}
                </p>
              </div>
            </div>
          </div>
  
          <div className="px-6 py-7 sm:px-9">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <StatusStep
                label="Application submitted"
                completed
              />
  
              <StatusStep
                label="Admin review"
                completed={status !== "DRAFT"}
                active={status === "PENDING"}
              />
  
              <StatusStep
                label={
                  status === "APPROVED"
                    ? "Approved"
                    : status === "REJECTED"
                    ? "Decision issued"
                    : status === "SUSPENDED"
                    ? "Access suspended"
                    : "Final decision"
                }
                completed={["APPROVED", "REJECTED", "SUSPENDED"].includes(status)}
              />
            </div>
  
            <div className="mt-7 rounded-2xl border border-slate-200 bg-slate-50 p-5">
              <div className="flex items-start gap-3">
                <FileCheck2
                  size={22}
                  className="mt-0.5 shrink-0 text-[#08264a]"
                />
  
                <div>
                  <h2 className="font-black text-slate-900">
                    What happens next?
                  </h2>
  
                  <p className="mt-2 text-sm leading-6 text-slate-600">
                    {statusConfig.nextStep}
                  </p>
                </div>
              </div>
            </div>
  
            {application && (
              <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
                <DetailCard
                  label="Application ID"
                  value={application.id ? `DRV-${application.id}` : "Pending"}
                />
  
                <DetailCard
                  label="Verification Status"
                  value={application.verificationStatus || status}
                />
  
                <DetailCard
                  label="Licence Number"
                  value={application.licenseNumber}
                />
  
                <DetailCard
                  label="Preferred Area"
                  value={application.preferredOperatingArea}
                />
              </div>
            )}
  
            {status === "REJECTED" && (
              <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-5">
                <div className="flex gap-3">
                  <ShieldAlert
                    size={22}
                    className="shrink-0 text-red-600"
                  />
  
                  <div>
                    <h3 className="font-black text-red-800">
                      Application requires correction
                    </h3>
  
                    <p className="mt-2 text-sm leading-6 text-red-700">
                      {application?.rejectionReason ||
                        "The administrator has rejected this application. Review the reason and submit corrected information."}
                    </p>
                  </div>
                </div>
              </div>
            )}
  
            <div className="mt-7 flex flex-col gap-3 border-t border-slate-200 pt-6 sm:flex-row sm:justify-between">
              <button
                type="button"
                onClick={() => navigate("/login")}
                className="flex items-center justify-center gap-2 rounded-2xl border border-slate-300 px-6 py-3 text-sm font-black text-slate-700 transition hover:bg-slate-50"
              >
                <ArrowLeft size={18} />
                Back to Login
              </button>
  
              {status === "REJECTED" ? (
                <button
                  type="button"
                  onClick={() => navigate("/driver/application")}
                  className="flex items-center justify-center gap-2 rounded-2xl bg-[#08264a] px-6 py-3 text-sm font-black text-white transition hover:bg-[#0d3566]"
                >
                  <RefreshCw size={18} />
                  Edit and Resubmit
                </button>
              ) : status === "APPROVED" ? (
                <button
                  type="button"
                  onClick={() => navigate("/driver/dashboard")}
                  className="flex items-center justify-center gap-2 rounded-2xl bg-emerald-600 px-6 py-3 text-sm font-black text-white transition hover:bg-emerald-700"
                >
                  <CheckCircle2 size={18} />
                  Open Driver Dashboard
                </button>
              ) : (
                <button
                  type="button"
                  disabled
                  className="flex cursor-not-allowed items-center justify-center gap-2 rounded-2xl bg-slate-200 px-6 py-3 text-sm font-black text-slate-500"
                >
                  <Clock3 size={18} />
                  Awaiting Admin Review
                </button>
              )}
            </div>
          </div>
        </div>
      </AuthLayout>
    );
  }
  
  function getStatusConfig(status) {
    switch (status) {
      case "APPROVED":
        return {
          title: "Application Approved",
          headerStyle: "bg-emerald-600",
          icon: <CheckCircle2 size={34} />,
          nextStep:
            "Your driver account is verified. You may now access the driver dashboard and continue with bus assignment or bus registration.",
        };
  
      case "REJECTED":
        return {
          title: "Application Rejected",
          headerStyle: "bg-red-700",
          icon: <AlertCircle size={34} />,
          nextStep:
            "Review the administrator's reason, correct your details or documents, and resubmit the application.",
        };
  
      case "SUSPENDED":
        return {
          title: "Driver Access Suspended",
          headerStyle: "bg-slate-800",
          icon: <ShieldAlert size={34} />,
          nextStep:
            "Operational access is temporarily disabled. Contact the Yatayat administrator for clarification.",
        };
  
      case "DRAFT":
        return {
          title: "Application Not Submitted",
          headerStyle: "bg-amber-600",
          icon: <FileCheck2 size={34} />,
          nextStep:
            "Complete all application sections and submit the required documents for administrator review.",
        };
  
      default:
        return {
          title: "Application Under Review",
          headerStyle: "bg-[#08264a]",
          icon: <Clock3 size={34} />,
          nextStep:
            "The administrator will verify your identity, driving licence, and uploaded documents. You will be able to access driver operations after approval.",
        };
    }
  }
  
  function StatusStep({ label, completed, active }) {
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
              <span className="h-2 w-2 rounded-full bg-slate-500" />
            )}
          </span>
  
          <p className="text-sm font-black text-slate-800">
            {label}
          </p>
        </div>
      </div>
    );
  }
  
  function DetailCard({ label, value }) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white p-4">
        <p className="text-xs font-black uppercase tracking-wider text-slate-500">
          {label}
        </p>
  
        <p className="mt-2 wrap-break-word text-sm font-black text-slate-900">
          {value || "Not available"}
        </p>
      </div>
    );
  }