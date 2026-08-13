import { useCallback, useEffect, useMemo, useState } from "react";
import {
  UserCircle,
  Edit,
  Bus,
  Wallet,
  Briefcase,
  Phone,
  Mail,
  MapPin,
  CheckCircle2,
  FileText,
  CalendarDays,
  BadgeCheck,
  Loader2,
  AlertTriangle,
  RefreshCw,
  ShieldCheck,
  ContactRound,
} from "lucide-react";
import { toast } from "react-toastify";
import DriverLayout from "../../components/layout/DriverLayout";
import { API_BASE_URL } from "../../utils/api";

export default function DriverProfilePage() {
  const loggedInUser = useMemo(() => {
    try {
      return JSON.parse(
        localStorage.getItem("yatayatUser") || "null"
      );
    } catch (error) {
      console.error("Invalid logged-in user data:", error);
      return null;
    }
  }, []);

  const [driver, setDriver] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  const fetchDriverProfile = useCallback(async (manualRefresh = false) => {
    if (!loggedInUser?.id) {
      setError(
        "Logged-in driver information was not found. Please log in again."
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
        `${API_BASE_URL}/api/drivers/profile/${loggedInUser.id}`,
        { credentials: "include" }
      );

      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(
          data.message || "Unable to load driver profile."
        );
      }

      setDriver(data.driver);

      localStorage.setItem(
        "yatayatUser",
        JSON.stringify({
          ...loggedInUser,
          fullName:
            data.driver?.fullName || loggedInUser.fullName,
          email: data.driver?.email || loggedInUser.email,
          phone: data.driver?.phone || loggedInUser.phone,
        })
      );

      localStorage.setItem(
        "driverApplicationStatus",
        data.driver?.verificationStatus || "NOT_SUBMITTED"
      );
    } catch (profileError) {
      console.error("Driver profile error:", profileError);

      setError(
        profileError.message ||
          "Unable to load driver profile."
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [loggedInUser]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void fetchDriverProfile();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [fetchDriverProfile]);

  if (loading) {
    return (
      <DriverLayout activePage="Profile">
        <div className="flex min-h-64 items-center justify-center rounded-3xl border border-slate-200 bg-white shadow-sm">
          <div className="text-center">
            <Loader2
              size={32}
              className="mx-auto animate-spin text-[#08264a]"
            />

            <h2 className="mt-4 text-lg font-semibold text-slate-900">
              Loading driver profile
            </h2>

            <p className="mt-2 text-sm text-slate-500">
              Please wait while we retrieve your verified details.
            </p>
          </div>
        </div>
      </DriverLayout>
    );
  }

  if (error && !driver) {
    return (
      <DriverLayout activePage="Profile">
        <div className="rounded-3xl border border-red-200 bg-red-50 p-6 text-center">
          <AlertTriangle
            size={36}
            className="mx-auto text-red-600"
          />

          <h2 className="mt-3 text-lg font-semibold text-red-800">
            Profile could not be loaded
          </h2>

          <p className="mt-2 text-sm text-red-700">
            {error}
          </p>

          <button
            type="button"
            onClick={() => fetchDriverProfile(true)}
            className="mt-4 inline-flex h-11 items-center gap-2 rounded-xl bg-red-600 px-4 text-sm font-semibold text-white transition hover:bg-red-700"
          >
            <RefreshCw size={17} />
            Try Again
          </button>
        </div>
      </DriverLayout>
    );
  }

  const verificationStatus =
    driver?.verificationStatus || "NOT_SUBMITTED";

  const isApproved = verificationStatus === "APPROVED";

  return (
    <DriverLayout activePage="Profile">
      <header className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-semibold text-slate-900">
            Driver Profile
          </h1>

          <p className="mt-1 text-sm text-slate-600">
            View your identity, licence, verification and professional
            information.
          </p>
        </div>

        <button
          type="button"
          onClick={() => fetchDriverProfile(true)}
          disabled={refreshing}
          className="inline-flex h-11 w-fit items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
        >
          <RefreshCw
            size={17}
            className={refreshing ? "animate-spin" : ""}
          />

          {refreshing ? "Refreshing..." : "Refresh Profile"}
        </button>
      </header>

      {error && (
        <div className="mb-5 flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm font-semibold text-amber-800">
          <AlertTriangle
            size={19}
            className="mt-0.5 shrink-0"
          />
          <p>{error}</p>
        </div>
      )}

      {/* PROFILE HEADER */}

      <section className="mb-5 overflow-hidden rounded-3xl bg-[#08264a] text-white shadow-sm">
        <div className="p-5 sm:p-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex min-w-0 items-center gap-4">
              <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl border-2 border-white/25 bg-white/10 text-2xl font-semibold">
                {getInitials(driver?.fullName)}
              </div>

              <div className="min-w-0">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-blue-200">
                  Yatayat Driver
                </p>

                <h2 className="mt-1 text-2xl font-semibold">
                  {driver?.fullName || "Driver User"}
                </h2>

                <div className="mt-2.5 flex flex-wrap gap-2">
                  <span className="rounded-full bg-white/10 px-3 py-1 text-xs font-semibold text-slate-200">
                    Driver ID:{" "}
                    {driver?.applicationId
                      ? `DRV-${driver.applicationId}`
                      : "Not assigned"}
                  </span>

                  <VerificationBadge
                    status={verificationStatus}
                  />
                </div>
              </div>
            </div>

            <button
              type="button"
              onClick={() =>
                toast.info(
                  "Profile editing will be connected after the update API is created."
                )
              }
              className="inline-flex h-10 w-fit items-center justify-center gap-2 rounded-xl bg-white px-4 text-sm font-semibold text-[#08264a] transition hover:bg-slate-100 lg:w-auto"
            >
              <Edit size={17} />
              Edit Profile
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 border-t border-white/10 bg-white/5 sm:grid-cols-3">
          <HeaderDetail
            label="Email Address"
            value={driver?.email}
          />

          <HeaderDetail
            label="Phone Number"
            value={driver?.phone}
          />

          <HeaderDetail
            label="Account Status"
            value={formatVerificationStatus(
              verificationStatus
            )}
          />
        </div>
      </section>

      {/* FUTURE OPERATIONAL STATS */}

      <section className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <StatCard
          title="Completed Trips"
          value="0"
          description="Trip records will appear here"
          icon={<Bus size={21} />}
        />

        <StatCard
          title="Trip Revenue"
          value="NPR 0"
          description="Calculated from completed trips"
          icon={<Wallet size={21} />}
          green
        />

        <StatCard
          title="Assigned Bus"
          value="Not Assigned"
          description="Bus assignment comes next"
          icon={<Briefcase size={21} />}
        />
      </section>

      <section className="grid grid-cols-1 gap-5 xl:grid-cols-12">
        <div className="space-y-5 xl:col-span-8">
          {/* PERSONAL INFORMATION */}

          <ProfileSection
            icon={<UserCircle size={22} />}
            title="Personal Information"
          >
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <InformationCard
                icon={<Mail size={19} />}
                label="Email Address"
                value={driver?.email}
              />

              <InformationCard
                icon={<Phone size={19} />}
                label="Phone Number"
                value={driver?.phone}
              />

              <InformationCard
                icon={<CalendarDays size={19} />}
                label="Date of Birth"
                value={formatDate(driver?.dateOfBirth)}
              />

              <InformationCard
                icon={<MapPin size={19} />}
                label="Permanent Address"
                value={driver?.permanentAddress}
              />

              <InformationCard
                icon={<MapPin size={19} />}
                label="Current Address"
                value={driver?.currentAddress}
              />

              <InformationCard
                icon={<ContactRound size={19} />}
                label="Emergency Contact"
                value={
                  driver?.emergencyContactName &&
                  driver?.emergencyContactPhone
                    ? `${driver.emergencyContactName} · ${driver.emergencyContactPhone}`
                    : driver?.emergencyContactName ||
                      driver?.emergencyContactPhone
                }
              />
            </div>
          </ProfileSection>

          {/* VERIFICATION INFORMATION */}

          <ProfileSection
            icon={<ShieldCheck size={22} />}
            title="Verification Information"
          >
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <InformationCard
                icon={<FileText size={19} />}
                label="Citizenship Number"
                value={driver?.citizenshipNumber}
              />

              <InformationCard
                icon={<BadgeCheck size={19} />}
                label="Application Status"
                value={formatVerificationStatus(
                  verificationStatus
                )}
              />

              <InformationCard
                icon={<CalendarDays size={19} />}
                label="Approved Date"
                value={formatDate(driver?.approvedAt)}
              />

              <InformationCard
                icon={<MapPin size={19} />}
                label="Preferred Operating Area"
                value={driver?.preferredOperatingArea}
              />
            </div>
          </ProfileSection>
        </div>

        <aside className="space-y-5 xl:col-span-4">
          {/* LICENCE INFORMATION */}

          <ProfileSection
            icon={<Briefcase size={22} />}
            title="Professional Details"
          >
            <div className="space-y-4">
              <Detail
                label="Licence Number"
                value={driver?.licenseNumber}
              />

              <Detail
                label="Licence Category"
                value={driver?.licenseCategory}
              />

              <Detail
                label="Licence Issue Date"
                value={formatDate(driver?.licenseIssueDate)}
              />

              <Detail
                label="Licence Expiry Date"
                value={formatDate(driver?.licenseExpiryDate)}
              />

              <Detail
                label="Driving Experience"
                value={
                  driver?.yearsOfExperience !== undefined &&
                  driver?.yearsOfExperience !== null
                    ? `${driver.yearsOfExperience} year(s)`
                    : null
                }
              />

              <div className="flex items-center justify-between border-t border-slate-100 pt-4">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Compliance Status
                </span>

                <span
                  className={`flex items-center gap-1 text-sm font-semibold ${
                    isApproved
                      ? "text-emerald-600"
                      : "text-amber-600"
                  }`}
                >
                  {isApproved ? (
                    <CheckCircle2 size={16} />
                  ) : (
                    <AlertTriangle size={16} />
                  )}

                  {isApproved
                    ? "Verified"
                    : formatVerificationStatus(
                        verificationStatus
                      )}
                </span>
              </div>
            </div>
          </ProfileSection>

          {/* DOCUMENT STATUS */}

          <ProfileSection
            icon={<FileText size={22} />}
            title="Submitted Documents"
          >
            <div className="space-y-2.5">
              <DocumentStatusCard
                title="Profile Photo"
                status={verificationStatus}
              />

              <DocumentStatusCard
                title="Citizenship Front"
                status={verificationStatus}
              />

              <DocumentStatusCard
                title="Citizenship Back"
                status={verificationStatus}
              />

              <DocumentStatusCard
                title="Licence Front"
                status={verificationStatus}
              />

              <DocumentStatusCard
                title="Licence Back"
                status={verificationStatus}
              />
            </div>

            <p className="mt-4 text-xs leading-5 text-slate-500">
              Document preview and download will be connected after we
              add a secure document-viewing endpoint.
            </p>
          </ProfileSection>
        </aside>
      </section>
    </DriverLayout>
  );
}

function ProfileSection({ icon, title, children }) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-4 flex items-center gap-3">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#08264a] text-white">
          {icon}
        </div>

        <h2 className="text-lg font-semibold text-slate-900">
          {title}
        </h2>
      </div>

      {children}
    </section>
  );
}

function HeaderDetail({ label, value }) {
  return (
    <div className="border-b border-white/10 px-5 py-3 last:border-b-0 sm:border-b-0 sm:border-r sm:last:border-r-0">
      <p className="text-[10px] font-semibold uppercase tracking-wider text-blue-200">
        {label}
      </p>

      <p className="mt-1 wrap-break-word text-sm font-semibold text-white">
        {value || "Not available"}
      </p>
    </div>
  );
}

function StatCard({
  title,
  value,
  description,
  icon,
  green,
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            {title}
          </p>

          <h2 className="mt-1.5 text-xl font-semibold text-[#08264a]">
            {value}
          </h2>

          <p className="mt-1.5 text-xs text-slate-500">
            {description}
          </p>
        </div>

        <div
          className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${
            green
              ? "bg-emerald-100 text-emerald-700"
              : "bg-blue-50 text-[#08264a]"
          }`}
        >
          {icon}
        </div>
      </div>
    </div>
  );
}

function InformationCard({ icon, label, value }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
      <div className="flex items-center gap-2 text-slate-500">
        {icon}

        <p className="text-[10px] font-semibold uppercase tracking-wider">
          {label}
        </p>
      </div>

      <p className="mt-1.5 wrap-break-word text-sm font-semibold text-slate-900">
        {value || "Not provided"}
      </p>
    </div>
  );
}

function Detail({ label, value }) {
  return (
    <div>
      <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
        {label}
      </p>

      <p className="mt-1 wrap-break-word text-sm font-semibold text-slate-900">
        {value || "Not available"}
      </p>
    </div>
  );
}

function DocumentStatusCard({ title, status }) {
  const approved = status === "APPROVED";

  return (
    <div className="flex items-center justify-between gap-3 rounded-xl bg-slate-50 p-3">
      <div className="flex min-w-0 items-center gap-3">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-white text-[#08264a]">
          <FileText size={18} />
        </div>

        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-slate-900">
            {title}
          </p>

          <p className="mt-0.5 text-xs text-slate-500">
            Submitted during driver application
          </p>
        </div>
      </div>

      <span
        className={`shrink-0 rounded-full px-2.5 py-1 text-[10px] font-semibold ${
          approved
            ? "bg-emerald-100 text-emerald-700"
            : "bg-amber-100 text-amber-700"
        }`}
      >
        {approved ? "VERIFIED" : "SUBMITTED"}
      </span>
    </div>
  );
}

function VerificationBadge({ status }) {
  const styles = {
    APPROVED: "bg-emerald-500/20 text-emerald-200",
    PENDING: "bg-amber-500/20 text-amber-200",
    REJECTED: "bg-red-500/20 text-red-200",
    SUSPENDED: "bg-slate-500/30 text-slate-200",
  };

  return (
    <span
      className={`flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold ${
        styles[status] || "bg-white/10 text-slate-200"
      }`}
    >
      {status === "APPROVED" && (
        <CheckCircle2 size={16} />
      )}

      {formatVerificationStatus(status)}
    </span>
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
  if (!value) {
    return "Not available";
  }

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

function formatVerificationStatus(status) {
  switch (status) {
    case "APPROVED":
      return "Verified Professional";
    case "PENDING":
      return "Approval Pending";
    case "REJECTED":
      return "Application Rejected";
    case "SUSPENDED":
      return "Driver Suspended";
    case "DRAFT":
      return "Application Draft";
    case "NOT_SUBMITTED":
      return "Profile Incomplete";
    default:
      return "Driver Account";
  }
}
