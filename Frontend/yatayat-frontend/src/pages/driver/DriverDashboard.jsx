import { useCallback, useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  ArrowRight,
  BadgeCheck,
  BriefcaseBusiness,
  Bus,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Loader2,
  RefreshCw,
  Route,
  ShieldCheck,
  UserCircle,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import DriverLayout from "../../components/layout/DriverLayout";
import { API_BASE_URL, apiFetch } from "../../utils/api";
import { selectCurrentDriverWork } from "../../utils/driverCurrentWork";
import {
  getCurrentDriverTrip,
  tripStatusLabel,
  tripStatusTone,
} from "../../utils/driverTrips";
import {
  getCurrentDriverLocalService,
  serviceStatusLabel,
  serviceStatusTone,
} from "../../utils/localServices";

export default function DriverDashboard() {
  const navigate = useNavigate();
  const loggedInUser = useMemo(() => {
    try {
      return JSON.parse(localStorage.getItem("yatayatUser") || "null");
    } catch (error) {
      console.error("Invalid logged-in user data:", error);
      return null;
    }
  }, []);

  const [driver, setDriver] = useState(null);
  const [operatorAssociation, setOperatorAssociation] = useState(null);
  const [currentWork, setCurrentWork] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  const loadDashboard = useCallback(async (manualRefresh = false) => {
    if (!loggedInUser?.id) {
      setError("Logged-in driver information was not found. Please log in again.");
      setLoading(false);
      return;
    }

    manualRefresh ? setRefreshing(true) : setLoading(true);
    setError("");

    try {
      const profileResponse = await fetch(
        `${API_BASE_URL}/api/drivers/profile/${loggedInUser.id}`,
        { credentials: "include" },
      );
      const profileData = await profileResponse.json();
      if (!profileResponse.ok || !profileData.success) {
        throw new Error(profileData.message || "Unable to load the driver dashboard.");
      }

      const [associationResponse, scheduledTrip, localService] = await Promise.all([
        apiFetch("/api/driver/operator-association"),
        getCurrentDriverTrip(),
        getCurrentDriverLocalService(),
      ]);

      setDriver(profileData.driver);
      setOperatorAssociation(
        associationResponse.ok && associationResponse.status !== 204
          ? await associationResponse.json()
          : null,
      );
      setCurrentWork(selectCurrentDriverWork(scheduledTrip, localService));
    } catch (dashboardError) {
      console.error("Driver dashboard loading error:", dashboardError);
      setError(dashboardError.message || "Unable to load the driver dashboard.");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [loggedInUser]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadDashboard();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [loadDashboard]);

  return (
    <DriverLayout activePage="Dashboard">
      <div className="space-y-5">
        <header className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-blue-700">
              Driver operations
            </p>
            <h1 className="mt-1.5 text-2xl font-semibold tracking-tight text-slate-900 sm:text-3xl">
              Welcome back, {firstName(driver?.fullName || loggedInUser?.fullName)}
            </h1>
            <p className="mt-1.5 max-w-2xl text-sm text-slate-500">
              Review your operator association and current driving assignment.
            </p>
          </div>
          <button
            type="button"
            onClick={() => loadDashboard(true)}
            disabled={loading || refreshing}
            className="inline-flex h-11 w-fit items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-slate-400 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <RefreshCw size={17} className={refreshing ? "animate-spin" : ""} />
            {refreshing ? "Refreshing..." : "Refresh"}
          </button>
        </header>

        {error && (
          <div className="flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-800">
            <AlertTriangle size={19} className="mt-0.5 shrink-0" />
            <div className="flex-1">
              <p>{error}</p>
              {!loading && (
                <button
                  type="button"
                  onClick={() => loadDashboard(true)}
                  className="mt-2 font-black underline underline-offset-2"
                >
                  Try again
                </button>
              )}
            </div>
          </div>
        )}

        {loading ? (
          <DashboardSkeleton />
        ) : (
          <>
            <section className="grid gap-4 md:grid-cols-3">
              <SummaryCard
                icon={<ShieldCheck size={22} />}
                label="Driver status"
                value={formatStatus(driver?.verificationStatus)}
                detail={driver?.licenseExpiryDate
                  ? `Licence valid until ${formatDate(driver.licenseExpiryDate)}`
                  : "Licence information unavailable"}
                tone={driver?.verificationStatus === "APPROVED" ? "green" : "amber"}
              />
              <SummaryCard
                icon={<BriefcaseBusiness size={22} />}
                label="Assigned operator"
                value={operatorAssociation?.operatorName || "No active operator"}
                detail={operatorAssociation
                  ? "Active operator association"
                  : "Accepted operator invitations appear here"}
                tone={operatorAssociation ? "blue" : "slate"}
              />
              <SummaryCard
                icon={<Bus size={22} />}
                label="Assigned bus"
                value={currentWork?.busNumber || "No active assignment"}
                detail={currentWork?.busName || "Your current operation determines the assigned bus"}
                tone={currentWork ? "blue" : "slate"}
              />
            </section>

            <CurrentOperation
              work={currentWork}
              onOpen={() => navigate("/driver/trip")}
            />

            <section className="grid items-start gap-4 xl:grid-cols-5">
              <div className="xl:col-span-3">
                <ProfileSummary
                  driver={driver}
                  onOpen={() => navigate("/driver/profile")}
                />
              </div>
              <div className="xl:col-span-2">
                <OperatorSummary
                  association={operatorAssociation}
                  onOpen={() => navigate("/driver/notifications")}
                />
              </div>
            </section>
          </>
        )}
      </div>
    </DriverLayout>
  );
}

function CurrentOperation({ work, onOpen }) {
  if (!work) {
    return (
      <section className="rounded-3xl border border-dashed border-slate-300 bg-white p-5 text-center shadow-sm">
        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-slate-100 text-slate-400">
          <Route size={24} />
        </div>
        <h2 className="mt-3 text-xl font-semibold text-slate-900">No current operation</h2>
        <p className="mx-auto mt-1.5 max-w-lg text-sm text-slate-500">
          Your next scheduled trip or local service will appear here when it is assigned.
        </p>
        <button
          type="button"
          onClick={onOpen}
          className="mt-4 inline-flex h-11 items-center gap-2 rounded-xl bg-[#08264a] px-4 text-sm font-semibold text-white transition hover:bg-[#0d3566]"
        >
          Open Trip Management <ArrowRight size={17} />
        </button>
      </section>
    );
  }

  const local = work.workType === "LOCAL_SERVICE";
  const statusLabel = local
    ? serviceStatusLabel(work.status)
    : tripStatusLabel(work.status);
  const statusTone = local
    ? serviceStatusTone(work.status)
    : tripStatusTone(work.status);

  return (
    <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
      <div className="flex flex-col gap-4 border-b border-slate-100 p-5 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-blue-700">
            {local ? "Local service" : "Out-of-valley scheduled trip"}
          </p>
          <h2 className="mt-1.5 text-xl font-semibold text-slate-900">
            {work.origin || "Origin unavailable"} <span aria-hidden="true">→</span>{" "}
            {work.destination || "Destination unavailable"}
          </h2>
          <p className="mt-1.5 text-sm font-medium text-slate-500">
            {[work.routeName, work.operatorName].filter(Boolean).join(" · ") || "Route details unavailable"}
          </p>
        </div>
        <span className={`w-fit rounded-full px-3 py-1.5 text-xs font-semibold uppercase tracking-wide ${statusTone}`}>
          {statusLabel}
        </span>
      </div>

      <div className="grid gap-px bg-slate-200 sm:grid-cols-2 lg:grid-cols-4">
        <OperationDetail
          icon={<Bus size={19} />}
          label="Assigned bus"
          value={[work.busNumber, work.busName].filter(Boolean).join(" · ")}
        />
        <OperationDetail
          icon={<Route size={19} />}
          label="Assigned route"
          value={work.routeName || work.routeCode}
        />
        <OperationDetail
          icon={<CalendarDays size={19} />}
          label={local ? "Service date" : "Departure"}
          value={local ? formatDate(work.serviceDate) : formatDateTime(work.departureAt)}
        />
        <OperationDetail
          icon={<Clock3 size={19} />}
          label={local ? "Planned time" : "Estimated arrival"}
          value={local
            ? [work.plannedStartTime, work.plannedEndTime].filter(Boolean).join(" – ")
            : formatDateTime(work.estimatedArrivalAt)}
        />
      </div>

      <div className="flex flex-col gap-3 bg-slate-50 px-5 py-3.5 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-slate-600">
          Continue operational actions and GPS status from Trip Management.
        </p>
        <button
          type="button"
          onClick={onOpen}
          className="inline-flex h-11 w-fit items-center justify-center gap-2 rounded-xl bg-[#08264a] px-4 text-sm font-semibold text-white transition hover:bg-[#0d3566]"
        >
          Open Trip Management <ArrowRight size={17} />
        </button>
      </div>
    </section>
  );
}

function ProfileSummary({ driver, onOpen }) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-[#08264a] text-base font-semibold text-white">
            {initials(driver?.fullName)}
          </div>
          <div className="min-w-0">
            <p className="text-xs font-medium uppercase tracking-[0.18em] text-slate-500">Driver profile</p>
            <h2 className="mt-0.5 truncate text-lg font-semibold text-slate-900">
              {driver?.fullName || "Driver"}
            </h2>
            <p className="truncate text-sm text-slate-500">
              {driver?.email || "Email unavailable"}
            </p>
          </div>
        </div>
        <UserCircle size={22} className="shrink-0 text-slate-400" />
      </div>
      <div className="mt-4 grid gap-2 sm:grid-cols-2">
        <CompactDetail label="Driver ID" value={driver?.applicationId ? `DRV-${driver.applicationId}` : null} />
        <CompactDetail label="Licence category" value={driver?.licenseCategory} />
        <CompactDetail label="Phone" value={driver?.phone} />
        <CompactDetail label="Operating area" value={driver?.preferredOperatingArea} />
      </div>
      <button
        type="button"
        onClick={onOpen}
        className="mt-4 inline-flex h-10 w-fit items-center gap-2 text-sm font-semibold text-blue-700 transition hover:text-blue-900"
      >
        View full profile <ArrowRight size={16} />
      </button>
    </section>
  );
}

function OperatorSummary({ association, onOpen }) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
        <BriefcaseBusiness size={20} />
      </div>
      <p className="mt-3 text-xs font-medium uppercase tracking-[0.18em] text-slate-500">
        Operator association
      </p>
      <h2 className="mt-1 text-lg font-semibold text-slate-900">
        {association?.operatorName || "No active operator"}
      </h2>
      <p className="mt-1.5 text-sm leading-5 text-slate-500">
        {association
          ? "You are actively associated with this transport operator."
          : "Review operator invitations to establish an active association."}
      </p>
      <div className="mt-3 flex items-center gap-2 text-sm font-medium text-slate-700">
        {association ? (
          <>
            <CheckCircle2 size={17} className="text-emerald-600" /> Active association
          </>
        ) : (
          <>
            <BadgeCheck size={17} className="text-slate-400" /> Association unavailable
          </>
        )}
      </div>
      <button
        type="button"
        onClick={onOpen}
        className="mt-4 inline-flex h-10 w-fit items-center gap-2 text-sm font-semibold text-blue-700 transition hover:text-blue-900"
      >
        View invitations <ArrowRight size={16} />
      </button>
    </section>
  );
}

function SummaryCard({ icon, label, value, detail, tone }) {
  const tones = {
    green: "bg-emerald-100 text-emerald-700",
    amber: "bg-amber-100 text-amber-700",
    blue: "bg-blue-100 text-blue-700",
    slate: "bg-slate-100 text-slate-600",
  };

  return (
    <article className="group rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md">
      <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${tones[tone] || tones.slate}`}>
        {icon}
      </div>
      <p className="mt-3 text-xs font-medium uppercase tracking-[0.16em] text-slate-500">{label}</p>
      <h2 className="mt-1 wrap-break-word text-lg font-semibold text-slate-900">{value}</h2>
      <p className="mt-1 text-xs leading-4 text-slate-500">{detail}</p>
    </article>
  );
}

function OperationDetail({ icon, label, value }) {
  return (
    <div className="bg-white p-4">
      <div className="flex items-center gap-2 text-blue-700">
        {icon}
        <p className="text-xs font-medium uppercase tracking-[0.14em] text-slate-500">{label}</p>
      </div>
      <p className="mt-2 wrap-break-word text-sm font-semibold text-slate-900">
        {value || "Not available"}
      </p>
    </div>
  );
}

function CompactDetail({ label, value }) {
  return (
    <div className="rounded-xl bg-slate-50 p-2.5">
      <p className="text-[10px] font-medium uppercase tracking-widest text-slate-500">{label}</p>
      <p className="mt-0.5 wrap-break-word text-sm font-semibold text-slate-900">{value || "Not available"}</p>
    </div>
  );
}

function DashboardSkeleton() {
  return (
    <div className="space-y-5" aria-label="Loading driver dashboard">
      <div className="grid gap-4 md:grid-cols-3">
        {[0, 1, 2].map((item) => (
          <div key={item} className="h-32 animate-pulse rounded-2xl bg-slate-200" />
        ))}
      </div>
      <div className="flex min-h-48 items-center justify-center rounded-3xl border border-slate-200 bg-white">
        <div className="text-center">
          <Loader2 size={30} className="mx-auto animate-spin text-[#08264a]" />
          <p className="mt-2 text-sm text-slate-500">Loading current operation...</p>
        </div>
      </div>
    </div>
  );
}

function firstName(value) {
  return value?.trim().split(/\s+/)[0] || "Driver";
}

function initials(value) {
  return value
    ? value.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join("").toUpperCase()
    : "DR";
}

function formatStatus(value) {
  return value?.replaceAll("_", " ") || "Unknown";
}

function formatDate(value) {
  if (!value) return "Not available";
  const date = new Date(`${value}T00:00:00`);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("en-NP", { dateStyle: "medium" }).format(date);
}

function formatDateTime(value) {
  if (!value) return "Not available";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("en-NP", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(date);
}
