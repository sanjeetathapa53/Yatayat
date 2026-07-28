import { useCallback, useEffect, useState } from "react";
import {
  Activity, AlertTriangle, ArrowRight, Banknote, Building2, Bus,
  CalendarDays, CreditCard, MapPinned, RefreshCw, Route, TicketCheck,
  UserCheck, Users,
} from "lucide-react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import AdminLayout from "../../components/layout/AdminLayout";
import { apiFetch } from "../../utils/api";

const ranges = [["LAST_7_DAYS", "7 Days"], ["LAST_30_DAYS", "30 Days"]];

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedRange = searchParams.get("range");
  const [range, setRange] = useState(
    ["LAST_7_DAYS", "LAST_30_DAYS"].includes(requestedRange)
      ? requestedRange
      : "LAST_7_DAYS",
  );
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
  const recentActivity = Array.isArray(analytics?.recentActivity)
    ? analytics.recentActivity
    : [];
  const openAnalytics = (section) => navigate(
    `/admin/analytics/${section}?range=${encodeURIComponent(range)}`,
  );

  return (
    <AdminLayout
      title="Admin Dashboard"
      subtitle="Real-time platform analytics, operations, revenue, and administrative work."
    >
      <div className="space-y-8">
        <section className="flex flex-col gap-6 rounded-3xl bg-[#08264a] px-6 py-7 text-white shadow-md sm:px-8 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.22em] text-blue-200">
              Yatayat Control Centre
            </p>
            <h1 className="mt-3 text-2xl font-black tracking-tight sm:text-3xl">
              Platform performance at a glance
            </h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-300">
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
                className={`rounded-xl px-4 py-2.5 text-sm font-black transition ${
                  range === value
                    ? "bg-white text-[#08264a] shadow-sm"
                    : "bg-white/10 text-white hover:bg-white/20"
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
              className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 transition hover:bg-white/20 disabled:opacity-50"
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

        <section aria-labelledby="quick-actions-title">
          <SectionHeader
            id="quick-actions-title"
            eyebrow="Operations"
            title="Quick Actions"
            description="Open the administrative tools used most often."
          />
          <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
            <QuickAction label="Review Driver Applications" icon={<UserCheck />} onClick={() => navigate("/admin/driver-applications")} />
            <QuickAction label="Transport Operators" icon={<Building2 />} onClick={() => navigate("/admin/operators")} />
            <QuickAction label="Live Monitoring" icon={<MapPinned />} onClick={() => navigate("/admin/live-monitoring")} />
            <QuickAction label="Routes & Stops" icon={<Route />} onClick={() => navigate("/admin/routes")} />
            <QuickAction label="Fleet Management" icon={<Bus />} onClick={() => navigate("/admin/buses")} />
          </div>
        </section>

        {loading && !analytics ? <DashboardSkeleton /> : analytics && (
          <>
            <section className="space-y-4">
              <SectionHeader
                eyebrow="Platform overview"
                title="Key performance indicators"
                description={`Current totals with activity from the selected ${range === "LAST_7_DAYS" ? "7-day" : "30-day"} period.`}
              />
              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                <Metric label="Total Users" value={summary.totalUsers} note={`${summary.usersRegisteredInRange} in selected range`} icon={<Users />} onClick={() => openAnalytics("users")} />
                <Metric label="Passengers" value={summary.totalPassengers} note={`${summary.totalDrivers} drivers • ${summary.totalOperators} operators`} icon={<UserCheck />} onClick={() => openAnalytics("users")} />
                <Metric label="Buses" value={summary.totalBuses} note={`${summary.activeBuses} active`} icon={<Bus />} onClick={() => openAnalytics("operations")} />
                <Metric label="Routes" value={summary.totalRoutes} note={`${summary.activeLocalTrips} local services running`} icon={<Route />} onClick={() => openAnalytics("operations")} />
                <Metric label="Total Bookings" value={summary.totalBookings} note={`${summary.bookingsToday} today`} icon={<TicketCheck />} onClick={() => openAnalytics("bookings")} />
                <Metric label="Confirmed Bookings" value={summary.confirmedBookings} note={`${summary.cancelledBookings} cancelled`} icon={<CalendarDays />} onClick={() => openAnalytics("bookings")} />
                <Metric label="Trips Today" value={summary.scheduledOutOfValleyTripsToday} note={`${summary.completedTripsToday} completed today`} icon={<MapPinned />} onClick={() => openAnalytics("operations")} />
                <Metric label="Pending Reviews" value={summary.pendingOperatorApplications + summary.pendingDriverApplications + summary.pendingBusApprovals} note="Operators, drivers, and buses" icon={<Activity />} />
              </div>
            </section>

            <section className="grid items-stretch gap-4 md:grid-cols-3">
              <MoneyCard label="Verified Ticket Revenue" value={summary.verifiedTicketRevenue} icon={<CreditCard />} onClick={() => openAnalytics("revenue")} />
              <MoneyCard label="Verified Wallet Top-ups" value={summary.verifiedWalletTopUpAmount} icon={<Banknote />} onClick={() => openAnalytics("revenue")} />
              <MoneyCard label="Total Verified Payments" value={summary.totalVerifiedPaymentAmount} icon={<Banknote />} onClick={() => openAnalytics("revenue")} />
            </section>

            <section className="grid items-stretch gap-6 xl:grid-cols-2">
              <DailyChart title="User registrations" points={analytics.userRegistrations} onDetails={() => openAnalytics("users")} />
              <DailyChart title="Bookings" points={analytics.bookings} onDetails={() => openAnalytics("bookings")} />
            </section>

            <section className="grid items-stretch gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
              <div className="h-full rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
                <CardHeader title="Recent activity" description="Latest safe operational events." />
                <div className="mt-5 divide-y divide-slate-100">
                  {recentActivity.length === 0 ? (
                    <EmptyState text="No recent activity is available." />
                  ) : recentActivity.map((item, index) => (
                    <div key={`${item.type}-${item.referenceId}-${index}`} className="flex items-center gap-4 py-4">
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-[#08264a]">
                        <Activity size={18} />
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="truncate font-black text-slate-900">{item.title}</p>
                        <p className="mt-1 text-xs font-bold uppercase tracking-wide text-slate-400">{String(item.type || "ACTIVITY").replaceAll("_", " ")}</p>
                      </div>
                      <time className="shrink-0 text-xs font-semibold text-slate-500">{formatDateTime(item.occurredAt)}</time>
                    </div>
                  ))}
                </div>
              </div>

              <aside className="h-full rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
                <CardHeader title="Trip mix" action={() => openAnalytics("operations")} />
                <Breakdown label="Local services" value={analytics.tripBreakdown.localServices} />
                <Breakdown label="Out-of-valley trips" value={analytics.tripBreakdown.outOfValleyTrips} />
              </aside>
            </section>
          </>
        )}
      </div>
    </AdminLayout>
  );
}

function Metric({ label, value, note, icon, onClick }) {
  const content = <div className="flex h-full flex-col"><div className="flex items-start justify-between gap-3"><p className="text-xs font-black uppercase tracking-widest text-slate-500">{label}</p><div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-blue-50 text-blue-700">{icon}</div></div><p className="mt-4 text-3xl font-black tracking-tight text-slate-900">{Number(value || 0).toLocaleString()}</p><p className="mt-2 min-h-10 text-sm leading-5 text-slate-500">{note}</p>{onClick && <p className="mt-auto flex items-center gap-1 pt-3 text-xs font-black text-[#08264a]">View details <ArrowRight size={13} /></p>}</div>;
  const classes = "min-h-48 rounded-3xl border border-slate-200 bg-white p-5 text-left shadow-sm transition";
  return onClick
    ? <button type="button" onClick={onClick} className={`${classes} hover:-translate-y-0.5 hover:border-[#08264a] hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#08264a]`}>{content}</button>
    : <div className={classes}>{content}</div>;
}

function MoneyCard({ label, value, icon, onClick }) {
  return <button type="button" onClick={onClick} className="min-h-40 rounded-3xl bg-linear-to-br from-emerald-700 to-emerald-900 p-5 text-left text-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700"><div className="flex items-center justify-between gap-3"><p className="text-xs font-black uppercase tracking-widest text-emerald-100">{label}</p><span className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10">{icon}</span></div><p className="mt-5 text-2xl font-black tracking-tight">{formatMoney(value)}</p><p className="mt-3 flex items-center gap-1 text-xs font-bold text-emerald-100">View details <ArrowRight size={13} /></p></button>;
}

function DailyChart({ title, points, onDetails }) {
  return <div className="flex h-full min-h-88 flex-col rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6"><CardHeader title={title} action={onDetails} /><div className="mt-5 min-h-0 flex-1"><ResponsiveContainer width="100%" height="100%"><AreaChart data={points || []} margin={{ top: 8, right: 8, left: -24, bottom: 0 }}><defs><linearGradient id={`chart-${title.replaceAll(" ", "-")}`} x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#0d6b78" stopOpacity={0.4} /><stop offset="95%" stopColor="#0d6b78" stopOpacity={0.04} /></linearGradient></defs><CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" /><XAxis dataKey="date" tickFormatter={(value) => new Date(`${value}T00:00:00`).toLocaleDateString(undefined, { month: "short", day: "numeric" })} tick={{ fontSize: 10 }} minTickGap={18} /><YAxis allowDecimals={false} tick={{ fontSize: 10 }} /><Tooltip labelFormatter={(value) => new Date(`${value}T00:00:00`).toLocaleDateString()} formatter={(value) => [value, title]} /><Area type="monotone" dataKey="count" stroke="#0d6b78" strokeWidth={3} fill={`url(#chart-${title.replaceAll(" ", "-")})`} /></AreaChart></ResponsiveContainer></div></div>;
}

function QuickAction({ label, icon, onClick }) {
  return <button type="button" onClick={onClick} className="group flex min-h-28 items-center gap-4 rounded-2xl border border-slate-200 bg-white p-4 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-[#08264a] hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#08264a]"><span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-blue-700 transition group-hover:bg-[#08264a] group-hover:text-white">{icon}</span><span className="min-w-0 flex-1 text-sm font-black leading-5 text-slate-800">{label}</span><ArrowRight size={16} className="shrink-0 text-slate-400 transition group-hover:translate-x-0.5 group-hover:text-[#08264a]" /></button>;
}

function SectionHeader({ id, eyebrow, title, description }) {
  return <div><p className="text-[11px] font-black uppercase tracking-[0.18em] text-blue-700">{eyebrow}</p><h2 id={id} className="mt-1 text-xl font-black text-slate-900 sm:text-2xl">{title}</h2><p className="mt-1 text-sm text-slate-500">{description}</p></div>;
}

function CardHeader({ title, description, action }) {
  return <div className="flex min-h-12 items-start justify-between gap-3"><div><h2 className="text-xl font-black text-slate-900">{title}</h2>{description && <p className="mt-1 text-sm text-slate-500">{description}</p>}</div>{action && <button type="button" onClick={action} className="flex shrink-0 items-center gap-1 rounded-lg px-2 py-1 text-xs font-black text-[#08264a] hover:bg-blue-50 focus-visible:outline-2">View details <ArrowRight size={14} /></button>}</div>;
}

function Breakdown({ label, value }) {
  return <div className="mt-4 flex items-center justify-between rounded-2xl bg-slate-50 px-4 py-4"><span className="text-sm font-bold text-slate-600">{label}</span><span className="text-lg font-black text-[#08264a]">{Number(value || 0).toLocaleString()}</span></div>;
}

function EmptyState({ text }) {
  return <div className="flex min-h-40 flex-col items-center justify-center rounded-2xl bg-slate-50 px-6 py-10 text-center"><Activity size={24} className="text-slate-300" /><p className="mt-3 text-sm font-semibold text-slate-500">{text}</p></div>;
}

function DashboardSkeleton() {
  return <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{Array.from({ length: 8 }, (_, index) => <div key={index} className="h-48 animate-pulse rounded-3xl bg-slate-200" />)}</div>;
}

function formatMoney(value) {
  return `NPR ${Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatDateTime(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Unknown time" : date.toLocaleString();
}
