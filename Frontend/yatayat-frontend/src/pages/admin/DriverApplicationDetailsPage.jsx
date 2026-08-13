import { useCallback, useEffect, useState } from "react";
import {
  AlertTriangle,
  ArrowLeft,
  BadgeCheck,
  CalendarDays,
  CheckCircle2,
  FileText,
  Loader2,
  Mail,
  MapPin,
  Phone,
  RefreshCw,
  ShieldCheck,
  User,
  XCircle,
} from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import AdminLayout from "../../components/layout/AdminLayout";
import { API_BASE_URL } from "../../utils/api";

export default function DriverApplicationDetailsPage() {
  const navigate = useNavigate();
  const { id } = useParams();

  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");

  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectionReason, setRejectionReason] = useState("");
  const [successState, setSuccessState] = useState(null);

  const fetchApplication = useCallback(async () => {
    try {
      setLoading(true);
      setError("");

      const response = await fetch(
        `${API_BASE_URL}/api/admin/drivers/${id}`,
        { credentials: "include" }
      );

      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(
          data.message || "Unable to load driver application."
        );
      }

      setApplication(data.application);
    } catch (fetchError) {
      console.error(fetchError);

      setError(
        fetchError.message ||
          "Unable to load driver application."
      );
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void fetchApplication();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [fetchApplication]);

  const approveApplication = async () => {
    try {
      setActionLoading(true);
      setError("");

      const response = await fetch(
        `${API_BASE_URL}/api/admin/drivers/${id}/approve`,
        {
          method: "PUT",
          credentials: "include",
        }
      );

      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(
          data.message || "Unable to approve application."
        );
      }

      setApplication(data.application);
      setSuccessState({
        type: "APPROVED",
        title: "Driver Approved",
        message:
          "The driver application has been approved successfully.",
      });

      setTimeout(() => {
        navigate("/admin/driver-applications", {
          replace: true,
        });
      }, 1800);
    } catch (approveError) {
      console.error(approveError);

      setError(
        approveError.message ||
          "Unable to approve driver application."
      );
    } finally {
      setActionLoading(false);
    }
  };

  const rejectApplication = async () => {
    if (!rejectionReason.trim()) {
      setError("Please enter a rejection reason.");
      return;
    }

    try {
      setActionLoading(true);
      setError("");

      const response = await fetch(
        `${API_BASE_URL}/api/admin/drivers/${id}/reject`,
        {
          method: "PUT",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            reason: rejectionReason.trim(),
          }),
        }
      );

      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(
          data.message || "Unable to reject application."
        );
      }

      setShowRejectModal(false);
      setApplication(data.application);

      setSuccessState({
        type: "REJECTED",
        title: "Application Rejected",
        message:
          "The rejection reason has been saved successfully.",
      });

      setTimeout(() => {
        navigate("/admin/driver-applications", {
          replace: true,
        });
      }, 1800);
    } catch (rejectError) {
      console.error(rejectError);

      setError(
        rejectError.message ||
          "Unable to reject driver application."
      );
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <AdminLayout
        title="Driver Application"
        subtitle="Loading driver verification information."
      >
        <div className="flex min-h-105 items-center justify-center rounded-2xl border border-slate-200 bg-white">
          <div className="text-center">
            <Loader2
              size={40}
              className="mx-auto animate-spin text-[#08264a]"
            />

            <h2 className="mt-4 text-xl font-semibold text-slate-900">
              Loading application
            </h2>

            <p className="mt-2 text-sm text-slate-500">
              Please wait while we load the driver details.
            </p>
          </div>
        </div>
      </AdminLayout>
    );
  }

  if (error && !application) {
    return (
      <AdminLayout
        title="Driver Application"
        subtitle="Review driver verification information."
      >
        <div className="rounded-2xl border border-red-200 bg-red-50 p-8 text-center">
          <AlertTriangle
            size={42}
            className="mx-auto text-red-600"
          />

          <h2 className="mt-4 text-xl font-semibold text-red-800">
            Application could not be loaded
          </h2>

          <p className="mt-2 text-sm text-red-700">
            {error}
          </p>

          <button
            type="button"
            onClick={fetchApplication}
            className="mt-5 inline-flex items-center gap-2 rounded-xl bg-red-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-red-700"
          >
            <RefreshCw size={17} />
            Try Again
          </button>
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout
      title="Driver Application Review"
      subtitle="Review the applicant's identity, licence and submitted information."
    >
      <div className="space-y-5">
        <button
          type="button"
          onClick={() =>
            navigate("/admin/driver-applications")
          }
          className="inline-flex items-center gap-2 text-sm font-semibold text-[#08264a] hover:underline"
        >
          <ArrowLeft size={17} />
          Back to Applications
        </button>

        {error && (
          <div className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700">
            <AlertTriangle size={19} className="mt-0.5 shrink-0" />
            <p>{error}</p>
          </div>
        )}

        <section className="overflow-hidden rounded-2xl bg-[#08264a] px-6 py-7 text-white shadow-sm sm:px-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex items-center gap-4">
              <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-white/15 text-xl font-semibold">
                {getInitials(application.fullName)}
              </div>

              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.22em] text-blue-200">
                  Application DRV-{application.id}
                </p>

                <h1 className="mt-2 text-2xl font-semibold sm:text-3xl">
                  {application.fullName}
                </h1>

                <p className="mt-2 text-sm text-slate-300">
                  Submitted {formatDateTime(application.submittedAt)}
                </p>
              </div>
            </div>

            <span className="w-fit rounded-full bg-amber-100 px-4 py-2 text-xs font-semibold text-amber-700">
              {application.verificationStatus}
            </span>
          </div>
        </section>

        <section className="grid grid-cols-1 gap-4 xl:grid-cols-12">
          <div className="space-y-5 xl:col-span-8">
            <DetailSection
              icon={<User size={21} />}
              title="Personal Information"
            >
              <DetailGrid>
                <DetailCard
                  label="Full Name"
                  value={application.fullName}
                />
                <DetailCard
                  label="Date of Birth"
                  value={formatDate(application.dateOfBirth)}
                />
                <DetailCard
                  label="Email"
                  value={application.email}
                  icon={<Mail size={15} />}
                />
                <DetailCard
                  label="Phone"
                  value={application.phone}
                  icon={<Phone size={15} />}
                />
                <DetailCard
                  label="Permanent Address"
                  value={application.permanentAddress}
                  icon={<MapPin size={15} />}
                />
                <DetailCard
                  label="Current Address"
                  value={application.currentAddress}
                  icon={<MapPin size={15} />}
                />
                <DetailCard
                  label="Emergency Contact"
                  value={application.emergencyContactName}
                />
                <DetailCard
                  label="Emergency Phone"
                  value={application.emergencyContactPhone}
                />
              </DetailGrid>
            </DetailSection>

            <DetailSection
              icon={<BadgeCheck size={21} />}
              title="Licence and Identity"
            >
              <DetailGrid>
                <DetailCard
                  label="Citizenship Number"
                  value={application.citizenshipNumber}
                />
                <DetailCard
                  label="Licence Number"
                  value={application.licenseNumber}
                />
                <DetailCard
                  label="Licence Category"
                  value={application.licenseCategory}
                />
                <DetailCard
                  label="Years of Experience"
                  value={`${application.yearsOfExperience ?? 0} year(s)`}
                />
                <DetailCard
                  label="Licence Issue Date"
                  value={formatDate(application.licenseIssueDate)}
                  icon={<CalendarDays size={15} />}
                />
                <DetailCard
                  label="Licence Expiry Date"
                  value={formatDate(application.licenseExpiryDate)}
                  icon={<CalendarDays size={15} />}
                />
                <DetailCard
                  label="Preferred Operating Area"
                  value={application.preferredOperatingArea}
                />
                <DetailCard
                  label="Application Note"
                  value={application.applicationNote}
                />
              </DetailGrid>
            </DetailSection>

            <DetailSection
              icon={<FileText size={21} />}
              title="Submitted Documents"
            >
              {application.documents?.length ? (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  {application.documents.map((document) => (
                    <DocumentCard
                      key={document.id}
                      document={document}
                    />
                  ))}
                </div>
              ) : (
                <div className="rounded-2xl bg-slate-50 p-6 text-center text-sm text-slate-500">
                  No document metadata was returned.
                </div>
              )}
            </DetailSection>
          </div>

          <aside className="space-y-5 xl:col-span-4">
            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-100 text-[#08264a]">
                <ShieldCheck size={23} />
              </div>

              <h2 className="mt-5 text-xl font-semibold text-slate-900">
                Verification Decision
              </h2>

              <p className="mt-2 text-sm leading-6 text-slate-500">
                Confirm that the licence is valid and the provided
                information is consistent before approving.
              </p>

              <div className="mt-5 rounded-2xl bg-slate-50 p-4 text-sm">
                <InfoRow
                  label="Status"
                  value={application.verificationStatus}
                />

                <InfoRow
                  label="Licence Expiry"
                  value={formatDate(
                    application.licenseExpiryDate
                  )}
                />

                <InfoRow
                  label="Documents"
                  value={`${application.documents?.length || 0} uploaded`}
                />
              </div>

              {application.verificationStatus === "PENDING" && (
                <div className="mt-6 space-y-3">
                  <button
                    type="button"
                    onClick={approveApplication}
                    disabled={actionLoading}
                    className="flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-600 py-2.5 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {actionLoading ? (
                      <Loader2 size={18} className="animate-spin" />
                    ) : (
                      <CheckCircle2 size={18} />
                    )}
                    Approve Driver
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setError("");
                      setShowRejectModal(true);
                    }}
                    disabled={actionLoading}
                    className="flex w-full items-center justify-center gap-2 rounded-xl border border-red-300 bg-red-50 py-2.5 text-sm font-semibold text-red-700 transition hover:bg-red-100 disabled:opacity-60"
                  >
                    <XCircle size={18} />
                    Reject Application
                  </button>
                </div>
              )}
            </div>

            <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5">
              <h3 className="font-semibold text-amber-900">
                Admin checklist
              </h3>

              <div className="mt-4 space-y-3 text-sm text-amber-800">
                <ChecklistItem text="Identity details are complete" />
                <ChecklistItem text="Licence number and category are valid" />
                <ChecklistItem text="Licence is not expired" />
                <ChecklistItem text="Uploaded documents are readable" />
                <ChecklistItem text="Application details are consistent" />
              </div>
            </div>
          </aside>
        </section>
      </div>

      {showRejectModal && (
        <div className="fixed inset-0 z-999 flex items-center justify-center overflow-y-auto bg-black/50 px-4 py-6 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-2xl bg-white shadow-2xl">
            <div className="px-6 pb-3 pt-7 text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-red-100 text-red-600">
                <XCircle size={31} />
              </div>

              <h2 className="mt-5 text-2xl font-semibold text-slate-900">
                Reject application?
              </h2>

              <p className="mt-2 text-sm leading-6 text-slate-500">
                The driver will see your reason and can correct the
                application before resubmitting.
              </p>
            </div>

            <div className="px-4 py-2.5">
              <label className="text-xs font-semibold uppercase tracking-widest text-slate-500">
                Rejection reason
              </label>

              <textarea
                rows={5}
                value={rejectionReason}
                onChange={(event) => {
                  setRejectionReason(event.target.value);
                  setError("");
                }}
                placeholder="Example: Licence image is unclear. Please upload a readable copy."
                className="mt-2 w-full resize-none rounded-2xl border border-slate-300 bg-slate-50 p-4 text-sm outline-none focus:border-red-500 focus:bg-white"
              />

              <p className="mt-2 text-xs text-slate-500">
                Enter a clear and helpful reason.
              </p>
            </div>

            <div className="flex gap-3 border-t border-slate-200 bg-slate-50 p-5">
              <button
                type="button"
                onClick={() => {
                  if (!actionLoading) {
                    setShowRejectModal(false);
                    setRejectionReason("");
                    setError("");
                  }
                }}
                disabled={actionLoading}
                className="flex-1 rounded-xl border border-slate-300 bg-white py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-100 disabled:opacity-60"
              >
                Keep Pending
              </button>

              <button
                type="button"
                onClick={rejectApplication}
                disabled={
                  actionLoading || !rejectionReason.trim()
                }
                className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-red-600 py-2.5 text-sm font-semibold text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {actionLoading ? (
                  <Loader2 size={17} className="animate-spin" />
                ) : (
                  <XCircle size={17} />
                )}
                Reject
              </button>
            </div>
          </div>
        </div>
      )}

      {successState && (
        <div className="fixed inset-0 z-1000 flex items-center justify-center bg-black/50 px-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-2xl bg-white p-8 text-center shadow-2xl">
            <div
              className={`mx-auto flex h-20 w-20 items-center justify-center rounded-full ${
                successState.type === "APPROVED"
                  ? "bg-emerald-100 text-emerald-600"
                  : "bg-red-100 text-red-600"
              }`}
            >
              {successState.type === "APPROVED" ? (
                <CheckCircle2 size={42} />
              ) : (
                <XCircle size={42} />
              )}
            </div>

            <h2 className="mt-5 text-2xl font-semibold text-slate-900">
              {successState.title}
            </h2>

            <p className="mt-2 text-sm leading-6 text-slate-500">
              {successState.message}
            </p>

            <p className="mt-5 text-xs font-semibold text-slate-400">
              Returning to pending applications...
            </p>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}

function DetailSection({ icon, title, children }) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="mb-5 flex items-center gap-3">
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-100 text-[#08264a]">
          {icon}
        </div>

        <h2 className="text-xl font-semibold text-slate-900">
          {title}
        </h2>
      </div>

      {children}
    </section>
  );
}

function DetailGrid({ children }) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      {children}
    </div>
  );
}

function DetailCard({ label, value, icon }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <p className="text-xs font-semibold uppercase tracking-widest text-slate-500">
        {label}
      </p>

      <p className="mt-2 flex items-start gap-2 wrap-break-word text-sm font-semibold text-slate-900">
        {icon && (
          <span className="mt-0.5 shrink-0 text-slate-400">
            {icon}
          </span>
        )}
        {value || "Not provided"}
      </p>
    </div>
  );
}

function DocumentCard({ document }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="flex items-start gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white text-[#08264a]">
          <FileText size={20} />
        </div>

        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
            {formatDocumentType(document.documentType)}
          </p>

          <p className="mt-1 truncate font-semibold text-slate-900">
            {document.originalFileName || "Document"}
          </p>

          <p className="mt-2 text-xs font-bold text-amber-700">
            {document.verificationStatus}
          </p>
        </div>
      </div>
    </div>
  );
}

function InfoRow({ label, value }) {
  return (
    <div className="flex justify-between gap-4 border-b border-slate-200 py-2.5 last:border-b-0">
      <span className="text-slate-500">{label}</span>
      <span className="text-right font-semibold text-slate-900">
        {value || "N/A"}
      </span>
    </div>
  );
}

function ChecklistItem({ text }) {
  return (
    <div className="flex items-start gap-2">
      <CheckCircle2 size={16} className="mt-0.5 shrink-0" />
      <span>{text}</span>
    </div>
  );
}

function getInitials(name) {
  return String(name || "Driver")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

function formatDate(value) {
  if (!value) return "Not available";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function formatDateTime(value) {
  if (!value) return "Not available";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatDocumentType(type) {
  return String(type || "DOCUMENT")
    .toLowerCase()
    .split("_")
    .map(
      (part) =>
        part.charAt(0).toUpperCase() + part.slice(1)
    )
    .join(" ");
}
