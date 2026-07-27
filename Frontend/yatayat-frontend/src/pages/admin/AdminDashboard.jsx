import { useCallback, useEffect, useState } from "react";
import {
  Activity, AlertTriangle, ArrowRight, Banknote, Bus,
  CalendarDays, CreditCard, MapPinned, RefreshCw, Route, TicketCheck,
  UserCheck, Users,
} from "lucide-react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import AdminLayout from "../../components/layout/AdminLayout";
import { apiFetch } from "../../utils/api";

const ranges = [
  ["LAST_7_DAYS", "7 Days"],
  ["LAST_30_DAYS", "30 Days"],
];

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialRange = ["LAST_7_DAYS", "LAST_30_DAYS"].includes(searchParams.get("range"))
    ? searchParams.get("range") : "LAST_7_DAYS";
  const [range, setRange] = useState(initialRange);
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadDashboard = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      const response = await apiFetch(
        `/api/admin/analytics/dashboard?range=${encodeURIComponent(range)}`,
      );
      if (!response.ok) throw new Error("Unable to load admin analytics.");
      setAnalytics(await response.json());
    } catch (loadError) {
      setError(loadError.message || "Unable to load admin analytics.");
    } finally {
      setLoading(false);
    }
  }, [range]);

  useEffect(() => {
    Promise.resolve().then(loadDashboard);
  }, [loadDashboard]);

  const summary = analytics?.summary;
  const openAnalytics = (section) => navigate(
    `/admin/analytics/${section}?range=${encodeURIComponent(range)}`,
  );

  return (
    <AdminLayout
      title="Admin Dashboard"
      subtitle="Real-time platform analytics, operations, revenue, and administrative work."
    >
      <div className="space-y-6">
        <section className="flex flex-col gap-5 rounded-3xl bg-[#08264a] px-6 py-7 text-white shadow-sm sm:px-8 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.22em] text-blue-200">
              Yatayat Control Centre
            </p>
            <h1 className="mt-3 text-2xl font-black sm:text-3xl">
              Platform performance at a glance
            </h1>
            <p className="mt-2 max-w-2xl text-sm text-slate-300">
              Database-backed user, fleet, trip, booking, payment, and approval metrics.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {ranges.map(([value, label]) => (
              <button
                key={value}
                type="button"
                onClick={() => {
                  setRange(value);
                  setSearchParams({ range: value }, { replace: true });
                }}
                className={`rounded-xl px-4 py-2.5 text-sm font-black ${
                  range === value ? "bg-white text-[#08264a]" : "bg-white/10 text-white hover:bg-white/20"
                }`}
              >
                {label}
              </button>
            ))}
            <button
              type="button"
              onClick={loadDashboard}
              disabled={loading}
              aria-label="Refresh analytics"
              className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 hover:bg-white/20 disabled:opacity-50"
            >
              <RefreshCw size={18} className={loading ? "animate-spin" : ""} />
            </button>
          </div>
        </section>

        {error && (
          <div className="flex flex-col gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700 sm:flex-row sm:items-center sm:justify-between">
            <span className="flex items-center gap-2"><AlertTriangle size={19} />{error}</span>
            <button type="button" onClick={loadDashboard} className="rounded-lg bg-red-700 px-4 py-2 text-white">Retry</button>
          </div>
        )}

        {loading && !analytics ? <DashboardSkeleton /> : analytics && (
          <>
            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <Metric label="Total Users" value={summary.totalUsers} note={`${summary.usersRegisteredInRange} in selected range`} icon={<Users />} onClick={() => openAnalytics("users")} />
              <Metric label="Passengers" value={summary.totalPassengers} note={`${summary.totalDrivers} drivers • ${summary.totalOperators} operators`} icon={<UserCheck />} onClick={() => openAnalytics("users")} />
              <Metric label="Buses" value={summary.totalBuses} note={`${summary.activeBuses} active`} icon={<Bus />} onClick={() => openAnalytics("operations")} />
              <Metric label="Routes" value={summary.totalRoutes} note={`${summary.activeLocalTrips} local services running`} icon={<Route />} onClick={() => openAnalytics("operations")} />
              <Metric label="Total Bookings" value={summary.totalBookings} note={`${summary.bookingsToday} today`} icon={<TicketCheck />} onClick={() => openAnalytics("bookings")} />
              <Metric label="Confirmed Bookings" value={summary.confirmedBookings} note={`${summary.cancelledBookings} cancelled`} icon={<CalendarDays />} onClick={() => openAnalytics("bookings")} />
              <Metric label="Trips Today" value={summary.scheduledOutOfValleyTripsToday} note={`${summary.completedTripsToday} completed today`} icon={<MapPinned />} onClick={() => openAnalytics("operations")} />
              <Metric label="Pending Reviews" value={summary.pendingOperatorApplications + summary.pendingDriverApplications + summary.pendingBusApprovals} note="Operators, drivers, and buses" icon={<Activity />} />
            </section>

            <section className="grid gap-4 md:grid-cols-3">
              <MoneyCard label="Verified Ticket Revenue" value={summary.verifiedTicketRevenue} icon={<CreditCard />} onClick={() => openAnalytics("revenue")} />
              <MoneyCard label="Verified Wallet Top-ups" value={summary.verifiedWalletTopUpAmount} icon={<Banknote />} onClick={() => openAnalytics("revenue")} />
              <MoneyCard label="Total Verified Payments" value={summary.totalVerifiedPaymentAmount} icon={<Banknote />} onClick={() => openAnalytics("revenue")} />
            </section>

            <section className="grid gap-6 xl:grid-cols-2">
              <DailyChart title="User registrations" points={analytics.userRegistrations} onDetails={() => openAnalytics("users")} />
              <DailyChart title="Bookings" points={analytics.bookings} onDetails={() => openAnalytics("bookings")} />
            </section>

            <section className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
              <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
                <h2 className="text-xl font-black text-slate-900">Recent activity</h2>
                <p className="mt-1 text-sm text-slate-500">Latest safe operational events.</p>
                <div className="mt-5 divide-y divide-slate-100">
                  {analytics.recentActivity.length === 0 ? (
                    <EmptyState text="No recent activity is available." />
                  ) : analytics.recentActivity.map((item, index) => (
                    <div key={`${item.type}-${item.referenceId}-${index}`} className="flex items-center justify-between gap-4 py-4">
                      <div>
                        <p className="font-black text-slate-900">{item.title}</p>
                        <p className="mt-1 text-xs font-bold uppercase tracking-wide text-slate-400">{item.type.replaceAll("_", " ")}</p>
                      </div>
                      <time className="shrink-0 text-xs font-semibold text-slate-500">{formatDateTime(item.occurredAt)}</time>
                    </div>
                  ))}
                </div>
              </div>

              <aside className="space-y-6">
                <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                  <div className="flex items-center justify-between gap-3"><h2 className="text-xl font-black text-slate-900">Trip mix</h2><button type="button" onClick={() => openAnalytics("operations")} className="flex items-center gap-1 text-xs font-black text-[#08264a] hover:underline focus-visible:outline-2">View details <ArrowRight size={14} /></button></div>
                  <Breakdown label="Local services" value={analytics.tripBreakdown.localServices} />
                  <Breakdown label="Out-of-valley trips" value={analytics.tripBreakdown.outOfValleyTrips} />
                </div>
                <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                  <h2 className="text-xl font-black text-slate-900">Quick links</h2>
                  <div className="mt-4 space-y-2">
                    <QuickLink label={`Operators (${summary.pendingOperatorApplications} pending)`} onClick={() => navigate("/admin/operators")} />
                    <QuickLink label={`Drivers (${summary.pendingDriverApplications} pending)`} onClick={() => navigate("/admin/driver-applications")} />
                    <QuickLink label={`Buses (${summary.pendingBusApprovals} pending)`} onClick={() => navigate("/admin/buses")} />
                    <QuickLink label="Routes" onClick={() => navigate("/admin/routes")} />
                    <QuickLink label="Live Monitoring" onClick={() => navigate("/admin/live-monitoring")} />
                  </div>
                </div>
              </aside>
            </section>
          </>
        )}
      </div>
    </AdminLayout>
  );
}

function Metric({ label, value, note, icon, onClick }) {
  const content = <div className="flex justify-between gap-3"><div><p className="text-xs font-black uppercase tracking-widest text-slate-500">{label}</p><p className="mt-3 text-3xl font-black text-slate-900">{Number(value).toLocaleString()}</p><p className="mt-2 text-sm text-slate-500">{note}</p>{onClick && <p className="mt-3 flex items-center gap-1 text-xs font-black text-[#08264a]">View details <ArrowRight size={13} /></p>}</div><div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-blue-100 text-blue-700">{icon}</div></div>;
  return onClick ? <button type="button" onClick={onClick} className="rounded-3xl border border-slate-200 bg-white p-5 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-[#08264a] hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#08264a]">{content}</button> : <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">{content}</div>;
}

function MoneyCard({ label, value, icon, onClick }) {
  return <button type="button" onClick={onClick} className="rounded-3xl bg-linear-to-br from-emerald-700 to-emerald-900 p-5 text-left text-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700"><div className="flex items-center justify-between"><p className="text-xs font-black uppercase tracking-widest text-emerald-100">{label}</p>{icon}</div><p className="mt-4 text-2xl font-black">{formatMoney(value)}</p><p className="mt-2 flex items-center gap-1 text-xs text-emerald-100">View details <ArrowRight size={13} /></p></button>;
}

function DailyChart({ title, points, onDetails }) {
  return <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6"><div className="flex items-center justify-between gap-3"><h2 className="text-xl font-black text-slate-900">{title}</h2><button type="button" onClick={onDetails} className="flex items-center gap-1 text-xs font-black text-[#08264a] hover:underline focus-visible:outline-2">View details <ArrowRight size={14} /></button></div><div className="mt-5 h-56 w-full"><ResponsiveContainer width="100%" height="100%"><AreaChart data={points} margin={{ top: 8, right: 8, left: -24, bottom: 0 }}><defs><linearGradient id={`chart-${title.replaceAll(" ", "-")}`} x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#0d6b78" stopOpacity={0.4} /><stop offset="95%" stopColor="#0d6b78" stopOpacity={0.04} /></linearGradient></defs><CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" /><XAxis dataKey="date" tickFormatter={(value) => new Date(`${value}T00:00:00`).toLocaleDateString(undefined, { month: "short", day: "numeric" })} tick={{ fontSize: 10 }} minTickGap={18} /><YAxis allowDecimals={false} tick={{ fontSize: 10 }} /><Tooltip labelFormatter={(value) => new Date(`${value}T00:00:00`).toLocaleDateString()} formatter={(value) => [value, title]} /><Area type="monotone" dataKey="count" stroke="#0d6b78" strokeWidth={3} fill={`url(#chart-${title.replaceAll(" ", "-")})`} /></AreaChart></ResponsiveContainer></div></div>;
}

function Breakdown({ label, value }) {
  return <div className="mt-4 flex items-center justify-between rounded-xl bg-slate-50 px-4 py-3"><span className="text-sm font-bold text-slate-600">{label}</span><span className="text-lg font-black text-[#08264a]">{Number(value).toLocaleString()}</span></div>;
}

function QuickLink({ label, onClick }) {
  return <button type="button" onClick={onClick} className="flex w-full items-center justify-between rounded-xl border border-slate-200 px-4 py-3 text-left text-sm font-black text-slate-700 hover:border-[#08264a] hover:bg-blue-50">{label}<ArrowRight size={16} /></button>;
}

function EmptyState({ text }) {
  return <p className="py-10 text-center text-sm font-semibold text-slate-500">{text}</p>;
}

function DashboardSkeleton() {
  return <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{Array.from({ length: 8 }, (_, index) => <div key={index} className="h-36 animate-pulse rounded-3xl bg-slate-200" />)}</div>;
}

function formatMoney(value) {
  return `NPR ${Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatDateTime(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Unknown time" : date.toLocaleString();
}
