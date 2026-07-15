import { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  ArrowRight,
  Building2,
  Bus,
  CalendarDays,
  CheckCircle2,
  Clock3,
  CreditCard,
  FileCheck2,
  MapPinned,
  RefreshCw,
  Route,
  Ticket,
  UserCheck,
  Users,
  Wallet,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import AdminLayout from "../../components/layout/AdminLayout";

const initialStats = {
  passengers: 0,
  totalDrivers: 0,
  pendingDrivers: 0,
  pendingOperators: 0,
  approvedDrivers: 0,
  buses: 0,
  activeTrips: 0,
  todayBookings: 0,
  todayRevenue: 0,
};

export default function AdminDashboard() {
  const navigate = useNavigate();

  const [stats, setStats] = useState(initialStats);
  const [pendingDrivers, setPendingDrivers] = useState([]);
  const [pendingOperators, setPendingOperators] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  const loadDashboard = async (manualRefresh = false) => {
    try {
      manualRefresh ? setRefreshing(true) : setLoading(true);
      setError("");

      /*
       * These APIs will be created progressively.
       * The pending-driver API may already exist once we finish admin approval.
       */

      const [pendingResponse, operatorsResponse] = await Promise.all([
        fetch("http://localhost:8080/api/admin/drivers/pending"),
        fetch("http://localhost:8080/api/admin/operators"),
      ]);

      let pendingData = [];

      if (pendingResponse.ok) {
        pendingData = await pendingResponse.json();
      }

      setPendingDrivers(Array.isArray(pendingData) ? pendingData : []);

      let operatorsData = [];

      if (operatorsResponse.ok) {
        operatorsData = await operatorsResponse.json();
      }

      const pendingOperatorData = Array.isArray(operatorsData)
        ? operatorsData.filter(
            (operator) => operator.verificationStatus === "PENDING"
          )
        : [];

      setPendingOperators(pendingOperatorData);

      /*
       * Temporary calculated values.
       * Later these will come from:
       * GET /api/admin/dashboard/summary
       */

      setStats((previous) => ({
        ...previous,
        pendingDrivers: Array.isArray(pendingData)
          ? pendingData.length
          : 0,
        pendingOperators: pendingOperatorData.length,
      }));
    } catch (fetchError) {
      console.error("Admin dashboard loading error:", fetchError);

      setError(
        "Some dashboard information could not be loaded. Make sure the backend is running."
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  const latestApplications = useMemo(
    () => pendingDrivers.slice(0, 5),
    [pendingDrivers]
  );

  const latestOperatorApplications = useMemo(
    () => pendingOperators.slice(0, 5),
    [pendingOperators]
  );

  return (
    <AdminLayout
      title="Admin Dashboard"
      subtitle="Monitor driver applications, users, buses, trips, bookings and platform operations."
    >
      <div className="space-y-6">
        {/* WELCOME SECTION */}

        <section className="overflow-hidden rounded-3xl bg-[#08264a] px-6 py-7 text-white shadow-sm sm:px-8">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <p className="text-xs font-black uppercase tracking-[0.22em] text-blue-200">
                Yatayat Control Centre
              </p>

              <h1 className="mt-3 text-2xl font-black sm:text-3xl">
                Manage the transport platform from one place
              </h1>

              <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-300">
                Review driver applications, manage buses and routes, supervise
                trips, monitor bookings, and track the overall performance of
                the Yatayat system.
              </p>
            </div>

            <button
              type="button"
              onClick={() => loadDashboard(true)}
              disabled={refreshing}
              className="flex w-full items-center justify-center gap-2 rounded-2xl bg-white px-5 py-3 text-sm font-black text-[#08264a] transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-70 lg:w-auto"
            >
              <RefreshCw
                size={18}
                className={refreshing ? "animate-spin" : ""}
              />
              {refreshing ? "Refreshing..." : "Refresh Dashboard"}
            </button>
          </div>
        </section>

        {/* ERROR */}

        {error && (
          <div className="flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm font-semibold text-amber-800">
            <AlertTriangle size={20} className="mt-0.5 shrink-0" />
            <p>{error}</p>
          </div>
        )}

        {/* STAT CARDS */}

        <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard
            label="Registered Passengers"
            value={stats.passengers}
            description="Passenger accounts"
            icon={<Users size={23} />}
            tone="blue"
            loading={loading}
          />

          <StatCard
            label="Total Drivers"
            value={stats.totalDrivers}
            description={`${stats.approvedDrivers} approved`}
            icon={<UserCheck size={23} />}
            tone="emerald"
            loading={loading}
          />

          <StatCard
            label="Pending Applications"
            value={stats.pendingDrivers}
            description="Require admin review"
            icon={<FileCheck2 size={23} />}
            tone="amber"
            loading={loading}
          />

          <StatCard
            label="Registered Buses"
            value={stats.buses}
            description={`${stats.activeTrips} active trips`}
            icon={<Bus size={23} />}
            tone="violet"
            loading={loading}
          />

          <StatCard
            label="Pending Operators"
            value={stats.pendingOperators}
            description="Require admin review"
            icon={<Building2 size={23} />}
            tone="amber"
            loading={loading}
          />

          <StatCard
            label="Today's Bookings"
            value={stats.todayBookings}
            description="Confirmed tickets today"
            icon={<Ticket size={23} />}
            tone="cyan"
            loading={loading}
          />

          <StatCard
            label="Today's Revenue"
            value={`NPR ${Number(stats.todayRevenue).toLocaleString()}`}
            description="Wallet and ticket payments"
            icon={<Wallet size={23} />}
            tone="emerald"
            loading={loading}
          />

          <StatCard
            label="Active Trips"
            value={stats.activeTrips}
            description="Currently operating"
            icon={<MapPinned size={23} />}
            tone="blue"
            loading={loading}
          />

          <StatCard
            label="Approved Drivers"
            value={stats.approvedDrivers}
            description="Operational accounts"
            icon={<CheckCircle2 size={23} />}
            tone="emerald"
            loading={loading}
          />
        </section>

        {/* MAIN CONTENT */}

        <section className="grid grid-cols-1 gap-6 xl:grid-cols-12">
          {/* DRIVER APPLICATIONS */}

          <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm xl:col-span-8">
            <div className="flex flex-col gap-4 border-b border-slate-200 px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-6">
              <div>
                <h2 className="text-xl font-black text-slate-900">
                  Pending Driver Applications
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  Review identity, licence, experience, and uploaded documents.
                </p>
              </div>

              <button
                type="button"
                onClick={() =>
                  navigate("/admin/driver-applications")
                }
                className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-4 py-3 text-sm font-black text-white transition hover:bg-[#0d3566]"
              >
                View All
                <ArrowRight size={17} />
              </button>
            </div>

            {loading ? (
              <ApplicationSkeleton />
            ) : latestApplications.length === 0 ? (
              <div className="px-6 py-12 text-center">
                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
                  <CheckCircle2 size={30} />
                </div>

                <h3 className="mt-4 text-lg font-black text-slate-900">
                  No pending applications
                </h3>

                <p className="mt-2 text-sm text-slate-500">
                  All submitted driver applications have been reviewed.
                </p>
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {latestApplications.map((application) => (
                  <DriverApplicationRow
                    key={application.id}
                    application={application}
                    onView={() =>
                      navigate(
                        `/admin/driver-applications/${application.id}`
                      )
                    }
                  />
                ))}
              </div>
            )}
          </div>

          <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm xl:col-span-8">
            <div className="flex flex-col gap-4 border-b border-slate-200 px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-6">
              <div>
                <h2 className="text-xl font-black text-slate-900">
                  Pending Operator Applications
                </h2>
                <p className="mt-1 text-sm text-slate-500">
                  Review submitted transport operator registrations.
                </p>
              </div>

              <button
                type="button"
                onClick={() => navigate("/admin/operators")}
                className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-4 py-3 text-sm font-black text-white transition hover:bg-[#0d3566]"
              >
                View All
                <ArrowRight size={17} />
              </button>
            </div>

            {loading ? (
              <ApplicationSkeleton />
            ) : latestOperatorApplications.length === 0 ? (
              <div className="px-6 py-12 text-center">
                <CheckCircle2
                  size={30}
                  className="mx-auto text-emerald-700"
                />
                <h3 className="mt-4 text-lg font-black text-slate-900">
                  No pending operator applications
                </h3>
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {latestOperatorApplications.map((operator) => (
                  <OperatorApplicationRow
                    key={operator.id}
                    operator={operator}
                    onView={() => navigate("/admin/operators")}
                  />
                ))}
              </div>
            )}
          </div>

          {/* QUICK ACTIONS */}

          <aside className="space-y-6 xl:col-span-4">
            <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-xl font-black text-slate-900">
                Quick Actions
              </h2>

              <p className="mt-1 text-sm text-slate-500">
                Open frequently used admin modules.
              </p>

              <div className="mt-5 space-y-3">
                <QuickAction
                  icon={<UserCheck size={20} />}
                  label="Review Driver Applications"
                  description={`${stats.pendingDrivers} currently pending`}
                  onClick={() =>
                    navigate("/admin/driver-applications")
                  }
                />

                <QuickAction
                  icon={<Building2 size={20} />}
                  label="Review Operator Applications"
                  description={`${stats.pendingOperators} currently pending`}
                  onClick={() => navigate("/admin/operators")}
                />

                <QuickAction
                  icon={<Bus size={20} />}
                  label="Manage Buses"
                  description="Register, assign, and verify buses"
                  onClick={() => navigate("/admin/buses")}
                />

                <QuickAction
                  icon={<Route size={20} />}
                  label="Manage Routes"
                  description="Create routes, stops, and fares"
                  onClick={() => navigate("/admin/routes")}
                />

                <QuickAction
                  icon={<CalendarDays size={20} />}
                  label="Manage Trips"
                  description="Create and monitor scheduled trips"
                  onClick={() => navigate("/admin/trips")}
                />
              </div>
            </div>

            <div className="rounded-3xl bg-linear-to-br from-[#08264a] to-[#164b82] p-6 text-white shadow-sm">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/15">
                <MapPinned size={24} />
              </div>

              <h2 className="mt-5 text-xl font-black">
                Live Operations
              </h2>

              <p className="mt-2 text-sm leading-6 text-slate-300">
                Monitor running trips, delayed services, driver status, and
                vehicle locations from the live tracking module.
              </p>

              <button
                type="button"
                onClick={() => navigate("/admin/live-tracking")}
                className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-white py-3 text-sm font-black text-[#08264a] transition hover:bg-slate-100"
              >
                Open Live Tracking
                <ArrowRight size={17} />
              </button>
            </div>
          </aside>
        </section>

        {/* SECONDARY OVERVIEW */}

        <section className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <OverviewCard
            icon={<CreditCard size={22} />}
            title="Payments & Refunds"
            text="Monitor wallet recharges, ticket payments, cancellations, and refunds."
            button="View Payments"
            onClick={() => navigate("/admin/payments")}
          />

          <OverviewCard
            icon={<Ticket size={22} />}
            title="Bookings"
            text="Review passenger bookings, booking status, used tickets, and cancellations."
            button="View Bookings"
            onClick={() => navigate("/admin/bookings")}
          />

          <OverviewCard
            icon={<Clock3 size={22} />}
            title="System Activity"
            text="Operational logs and recent platform actions will appear here later."
            button="View Reports"
            onClick={() => navigate("/admin/reports")}
          />
        </section>
      </div>
    </AdminLayout>
  );
}

function StatCard({
  label,
  value,
  description,
  icon,
  tone,
  loading,
}) {
  const toneStyles = {
    blue: "bg-blue-100 text-blue-700",
    emerald: "bg-emerald-100 text-emerald-700",
    amber: "bg-amber-100 text-amber-700",
    violet: "bg-violet-100 text-violet-700",
    cyan: "bg-cyan-100 text-cyan-700",
  };

  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-1 hover:shadow-md">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-xs font-black uppercase tracking-widest text-slate-500">
            {label}
          </p>

          {loading ? (
            <div className="mt-4 h-9 w-24 animate-pulse rounded-lg bg-slate-200" />
          ) : (
            <h3 className="mt-3 wrap-break-word text-3xl font-black text-slate-900">
              {value}
            </h3>
          )}

          <p className="mt-2 text-sm text-slate-500">
            {description}
          </p>
        </div>

        <div
          className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl ${
            toneStyles[tone] || toneStyles.blue
          }`}
        >
          {icon}
        </div>
      </div>
    </div>
  );
}

function DriverApplicationRow({ application, onView }) {
  return (
    <div className="flex flex-col gap-4 px-5 py-5 transition hover:bg-slate-50 sm:flex-row sm:items-center sm:justify-between sm:px-6">
      <div className="flex min-w-0 items-start gap-4">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-blue-100 text-sm font-black text-[#08264a]">
          {getInitials(application.fullName || "Driver")}
        </div>

        <div className="min-w-0">
          <h3 className="truncate font-black text-slate-900">
            {application.fullName || "Driver Applicant"}
          </h3>

          <p className="mt-1 truncate text-sm text-slate-500">
            {application.email || "Email unavailable"}
          </p>

          <div className="mt-2 flex flex-wrap gap-2">
            <SmallBadge>
              Licence {application.licenseCategory || "N/A"}
            </SmallBadge>

            <SmallBadge>
              {application.yearsOfExperience || 0} year(s)
            </SmallBadge>

            <SmallBadge>
              {application.preferredOperatingArea ||
                "Area not specified"}
            </SmallBadge>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-between gap-3 sm:justify-end">
        <span className="rounded-full bg-amber-100 px-3 py-1.5 text-xs font-black text-amber-700">
          PENDING
        </span>

        <button
          type="button"
          onClick={onView}
          className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-black text-slate-700 transition hover:border-[#08264a] hover:bg-[#08264a] hover:text-white"
        >
          Review
        </button>
      </div>
    </div>
  );
}

function QuickAction({
  icon,
  label,
  description,
  onClick,
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full items-center gap-4 rounded-2xl border border-slate-200 p-4 text-left transition hover:border-[#08264a] hover:bg-blue-50"
    >
      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-[#08264a]">
        {icon}
      </div>

      <div className="min-w-0 flex-1">
        <p className="font-black text-slate-900">
          {label}
        </p>

        <p className="mt-1 text-sm text-slate-500">
          {description}
        </p>
      </div>

      <ArrowRight size={18} className="shrink-0 text-slate-400" />
    </button>
  );
}

function OverviewCard({
  icon,
  title,
  text,
  button,
  onClick,
}) {
  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-[#08264a]">
        {icon}
      </div>

      <h2 className="mt-5 text-lg font-black text-slate-900">
        {title}
      </h2>

      <p className="mt-2 text-sm leading-6 text-slate-500">
        {text}
      </p>

      <button
        type="button"
        onClick={onClick}
        className="mt-5 flex items-center gap-2 text-sm font-black text-[#08264a] hover:underline"
      >
        {button}
        <ArrowRight size={16} />
      </button>
    </div>
  );
}

function SmallBadge({ children }) {
  return (
    <span className="rounded-full bg-slate-100 px-3 py-1 text-[11px] font-bold text-slate-600">
      {children}
    </span>
  );
}

function ApplicationSkeleton() {
  return (
    <div className="space-y-1 p-5">
      {[1, 2, 3].map((item) => (
        <div
          key={item}
          className="flex animate-pulse items-center gap-4 rounded-2xl p-4"
        >
          <div className="h-12 w-12 rounded-2xl bg-slate-200" />

          <div className="flex-1">
            <div className="h-4 w-40 rounded bg-slate-200" />
            <div className="mt-2 h-3 w-56 rounded bg-slate-100" />
          </div>

          <div className="h-9 w-20 rounded-xl bg-slate-200" />
        </div>
      ))}
    </div>
  );
}

function getInitials(name) {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

function OperatorApplicationRow({ operator, onView }) {
  return (
    <div className="flex flex-col gap-4 px-5 py-5 transition hover:bg-slate-50 sm:flex-row sm:items-center sm:justify-between sm:px-6">
      <div className="flex min-w-0 items-start gap-4">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-violet-100 text-sm font-black text-violet-700">
          {getInitials(operator.name || "Operator")}
        </div>
        <div className="min-w-0">
          <h3 className="truncate font-black text-slate-900">
            {operator.name || "Transport Operator"}
          </h3>
          <p className="mt-1 truncate text-sm text-slate-500">
            {operator.email || "Email unavailable"}
          </p>
          <div className="mt-2 flex flex-wrap gap-2">
            <SmallBadge>{operator.registrationNumber || "No registration"}</SmallBadge>
            <SmallBadge>{operator.operatorType || "Type unavailable"}</SmallBadge>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-between gap-3 sm:justify-end">
        <span className="rounded-full bg-amber-100 px-3 py-1.5 text-xs font-black text-amber-700">
          PENDING
        </span>
        <button
          type="button"
          onClick={onView}
          className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-black text-slate-700 transition hover:border-[#08264a] hover:bg-[#08264a] hover:text-white"
        >
          Review
        </button>
      </div>
    </div>
  );
}
