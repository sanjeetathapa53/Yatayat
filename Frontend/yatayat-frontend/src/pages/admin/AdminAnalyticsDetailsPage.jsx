import { useCallback, useEffect, useMemo, useState } from "react";
import { ArrowLeft, RefreshCw } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import {
  Area, AreaChart, Bar, BarChart, CartesianGrid, Cell,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import AdminLayout from "../../components/layout/AdminLayout";
import { apiFetch } from "../../utils/api";

const validRanges = new Set(["LAST_7_DAYS", "LAST_30_DAYS"]);
const definitions = {
  users: ["Users Analytics", "Registration trends, roles, and authentication providers."],
  operations: ["Operations Analytics", "Fleet, route, local-service, and trip performance."],
  bookings: ["Booking Analytics", "Booking volume, lifecycle status, and recent activity."],
  revenue: ["Revenue Analytics", "Verified ticket payments and credited wallet top-ups."],
};
const colors = ["#0d6b78", "#2563eb", "#7c3aed", "#d97706", "#dc2626", "#059669"];

export default function AdminAnalyticsDetailsPage({ section }) {
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedRange = searchParams.get("range");
  const range = validRanges.has(requestedRange) ? requestedRange : "LAST_7_DAYS";
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [title, subtitle] = definitions[section];

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      const response = await apiFetch(
        `/api/admin/analytics/${section}?range=${encodeURIComponent(range)}`,
      );
      if (!response.ok) throw new Error(`Unable to load ${section} analytics.`);
      setData(await response.json());
    } catch (loadError) {
      setError(loadError.message || `Unable to load ${section} analytics.`);
    } finally {
      setLoading(false);
    }
  }, [range, section]);

  useEffect(() => {
    Promise.resolve().then(load);
  }, [load]);

  const updateRange = (value) => setSearchParams({ range: value }, { replace: true });

  return (
    <AdminLayout title={title} subtitle={subtitle}>
      <div className="space-y-6">
        <section className="flex flex-col gap-4 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center sm:justify-between">
          <Link to={`/admin/dashboard?range=${range}`} className="flex items-center gap-2 text-sm font-black text-[#08264a] hover:underline focus-visible:outline-2">
            <ArrowLeft size={17} /> Back to Dashboard
          </Link>
          <div className="flex gap-2">
            {[["LAST_7_DAYS", "7 Days"], ["LAST_30_DAYS", "30 Days"]].map(([value, label]) => (
              <button key={value} type="button" onClick={() => updateRange(value)} className={`rounded-xl px-4 py-2 text-sm font-black ${range === value ? "bg-[#08264a] text-white" : "bg-slate-100 text-slate-700 hover:bg-slate-200"}`}>{label}</button>
            ))}
            <button type="button" onClick={load} disabled={loading} aria-label="Refresh analytics" className="flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 hover:bg-slate-50 disabled:opacity-50"><RefreshCw size={17} className={loading ? "animate-spin" : ""} /></button>
          </div>
        </section>
        {error && <div className="flex items-center justify-between rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700"><span>{error}</span><button type="button" onClick={load} className="rounded-lg bg-red-700 px-4 py-2 text-white">Retry</button></div>}
        {loading && !data ? <Skeleton /> : data && <SectionContent section={section} data={data} />}
      </div>
    </AdminLayout>
  );
}

function SectionContent({ section, data }) {
  if (section === "users") return <UsersDetails data={data} />;
  if (section === "operations") return <OperationsDetails data={data} />;
  if (section === "bookings") return <BookingsDetails data={data} />;
  return <RevenueDetails data={data} />;
}

function UsersDetails({ data }) {
  return <><CardGrid items={[["Total Users", data.totalUsers], ["Passengers", data.passengers], ["Drivers", data.drivers], ["Operators", data.operators], ["Admins", data.admins], ["Today", data.registrationsToday], ["In Range", data.registrationsInRange]]} /><div className="grid gap-6 xl:grid-cols-2"><Trend title="Daily registrations" data={data.dailyRegistrations} dataKey="count" /><Distribution title="Role distribution" values={data.roleDistribution} /><Distribution title="Authentication providers" values={data.providerDistribution} /><Recent title="Recent registrations" rows={data.recentRegistrations} columns={[["Role", "role"], ["Provider", "provider"], ["Registered", "createdAt"]]} /></div></>;
}

function OperationsDetails({ data }) {
  return <><CardGrid items={[["Total Buses", data.totalBuses], ["Active Buses", data.activeBuses], ["Pending Buses", data.pendingBuses], ["Routes", data.totalRoutes], ["Local In Service", data.activeLocalServices], ["Scheduled Trips", data.scheduledTrips], ["Completed Trips", data.completedTrips]]} /><div className="grid gap-6 xl:grid-cols-2"><Trend title="Daily trip creation" data={data.dailyTrips} dataKey="count" /><Distribution title="Trip type" values={data.tripTypeBreakdown} /><Distribution title="Trip statuses" values={data.tripStatusBreakdown} /><div className="rounded-3xl border border-slate-200 bg-white p-5"><h2 className="text-xl font-black">Management links</h2><div className="mt-4 flex flex-wrap gap-3"><LinkButton to="/admin/buses" label="Manage Buses" /><LinkButton to="/admin/routes" label="Manage Routes" /><LinkButton to="/admin/live-tracking" label="Live Monitoring" /></div></div></div></>;
}

function BookingsDetails({ data }) {
  return <><CardGrid items={[["Total Bookings", data.totalBookings], ["Today", data.bookingsToday], ["Confirmed", data.confirmedBookings], ["Cancelled", data.cancelledBookings]]} /><div className="grid gap-6 xl:grid-cols-2"><Trend title="Daily bookings" data={data.dailyBookings} dataKey="count" /><Distribution title="Booking status" values={data.statusDistribution} /><Distribution title="Trip type" values={data.tripTypeBreakdown} /><Recent title="Recent bookings" rows={data.recentBookings} columns={[["Reference", "bookingReference"], ["Status", "status"], ["Fare", "totalFare"], ["Booked", "bookedAt"]]} /></div></>;
}

function RevenueDetails({ data }) {
  return <><CardGrid money items={[["Ticket Revenue", data.ticketRevenue], ["Wallet Top-ups", data.walletTopUpRevenue], ["Total Verified", data.totalVerifiedRevenue], ["Successful Attempts", data.successfulPayments], ["Pending Attempts", data.pendingPayments], ["Failed Attempts", data.failedPayments]]} /><div className="grid gap-6 xl:grid-cols-2"><Trend title="Daily verified revenue" data={data.dailyRevenue} dataKey="amount" money /><Distribution title="Revenue sources" values={data.sourceBreakdown} money /><Recent title="Recent verified transactions" rows={data.recentTransactions} columns={[["Source", "source"], ["Method", "method"], ["Amount", "amount"], ["Verified", "verifiedAt"]]} /></div></>;
}

function CardGrid({ items, money = false }) {
  return <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{items.map(([label, value]) => <div key={label} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm"><p className="text-xs font-black uppercase tracking-widest text-slate-500">{label}</p><p className="mt-3 text-2xl font-black text-slate-900">{money && label.includes("Revenue") || money && label.includes("Top-up") || money && label.includes("Verified") ? formatMoney(value) : Number(value || 0).toLocaleString()}</p></div>)}</section>;
}

function Trend({ title, data, dataKey, money = false }) {
  return <ChartCard title={title}><ResponsiveContainer width="100%" height="100%"><AreaChart data={data}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="date" tick={{ fontSize: 10 }} minTickGap={18} /><YAxis tick={{ fontSize: 10 }} /><Tooltip formatter={(value) => money ? formatMoney(value) : value} /><Area dataKey={dataKey} type="monotone" stroke="#0d6b78" fill="#99d5d9" strokeWidth={3} /></AreaChart></ResponsiveContainer></ChartCard>;
}

function Distribution({ title, values, money = false }) {
  const data = useMemo(() => Object.entries(values || {}).map(([name, value]) => ({ name, value: Number(value) })), [values]);
  return <ChartCard title={title}>{data.length === 0 ? <Empty /> : <ResponsiveContainer width="100%" height="100%"><BarChart data={data}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="name" tick={{ fontSize: 9 }} /><YAxis tick={{ fontSize: 10 }} /><Tooltip formatter={(value) => money ? formatMoney(value) : value} /><Bar dataKey="value">{data.map((item, index) => <Cell key={item.name} fill={colors[index % colors.length]} />)}</Bar></BarChart></ResponsiveContainer>}</ChartCard>;
}

function Recent({ title, rows, columns }) {
  const safeRows = Array.isArray(rows) ? rows : [];
  const safeColumns = Array.isArray(columns) ? columns : [];
  return <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="text-xl font-black text-slate-900">{title}</h2>{safeRows.length === 0 ? <Empty /> : <div className="mt-4 overflow-x-auto"><table className="w-full min-w-130 text-left text-sm"><thead><tr className="border-b border-slate-200">{safeColumns.map(([label]) => <th key={label} className="px-3 py-3 text-xs font-black uppercase text-slate-500">{label}</th>)}</tr></thead><tbody>{safeRows.map((row, index) => <tr key={index} className="border-b border-slate-100">{safeColumns.map(([label, key]) => <td key={label} className="px-3 py-3 font-semibold text-slate-700">{formatCell(key, row?.[key])}</td>)}</tr>)}</tbody></table></div>}</div>;
}

function ChartCard({ title, children }) {
  return <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="text-xl font-black text-slate-900">{title}</h2><div className="mt-5 h-64">{children}</div></div>;
}
function LinkButton({ to, label }) { return <Link to={to} className="rounded-xl bg-[#08264a] px-4 py-3 text-sm font-black text-white hover:bg-[#0d3566] focus-visible:outline-2">{label}</Link>; }
function Empty() { return <p className="flex h-full items-center justify-center text-sm font-semibold text-slate-500">No data is available for this period.</p>; }
function Skeleton() { return <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{Array.from({ length: 8 }, (_, index) => <div key={index} className="h-32 animate-pulse rounded-3xl bg-slate-200" />)}</div>; }
function formatMoney(value) { return `NPR ${Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`; }
function formatCell(key, value) { if (key === "amount" || key === "totalFare") return formatMoney(value); if (key.endsWith("At")) return new Date(value).toLocaleString(); return String(value ?? "—").replaceAll("_", " "); }
