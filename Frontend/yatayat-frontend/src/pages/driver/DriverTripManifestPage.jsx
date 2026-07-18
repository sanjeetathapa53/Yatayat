import { useEffect, useMemo, useState } from "react";
import { AlertTriangle, Bus, CheckCircle2, Loader2, Search, Users } from "lucide-react";
import { useParams } from "react-router-dom";
import DriverLayout from "../../components/layout/DriverLayout";
import { getDriverTripManifest } from "../../utils/driverTickets";

const filters = ["ALL", "BOARDED", "NOT_BOARDED"];

export default function DriverTripManifestPage() {
  const { scheduledTripId } = useParams();
  const [manifest, setManifest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("ALL");

  useEffect(() => {
    let active = true;
    async function loadManifest() {
      try {
        setLoading(true);
        setError("");
        const data = await getDriverTripManifest(scheduledTripId);
        if (active) setManifest(data);
      } catch (manifestError) {
        if (active) setError(manifestError.message || "Unable to load trip manifest.");
      } finally {
        if (active) setLoading(false);
      }
    }
    loadManifest();
    return () => { active = false; };
  }, [scheduledTripId]);

  const passengers = useMemo(() => {
    const rows = manifest?.passengers || [];
    const needle = query.trim().toLowerCase();
    return rows.filter((row) => {
      const boarded = row.ticketStatus === "USED";
      if (filter === "BOARDED" && !boarded) return false;
      if (filter === "NOT_BOARDED" && boarded) return false;
      if (!needle) return true;
      return [
        row.passengerName,
        row.passengerPhone,
        row.bookingReference,
        row.ticketNumber,
        ...(row.seats || []),
      ].some((value) => String(value || "").toLowerCase().includes(needle));
    });
  }, [manifest, query, filter]);

  if (loading) {
    return (
      <DriverLayout activePage="Scanner">
        <div className="flex min-h-120 items-center justify-center rounded-3xl border border-slate-200 bg-white shadow-sm">
          <div className="text-center">
            <Loader2 size={44} className="mx-auto animate-spin text-[#08264a]" />
            <h2 className="mt-4 text-xl font-black text-slate-900">Loading trip manifest</h2>
          </div>
        </div>
      </DriverLayout>
    );
  }

  if (error) {
    return (
      <DriverLayout activePage="Scanner">
        <div className="rounded-3xl border border-red-200 bg-red-50 p-8 text-center">
          <AlertTriangle size={44} className="mx-auto text-red-600" />
          <h2 className="mt-4 text-xl font-black text-red-800">Manifest unavailable</h2>
          <p className="mt-2 text-sm font-semibold text-red-700">{error}</p>
        </div>
      </DriverLayout>
    );
  }

  const trip = manifest.trip;
  const summary = manifest.summary;

  return (
    <DriverLayout activePage="Scanner">
      <header className="mb-6 rounded-3xl bg-[#08264a] p-6 text-white shadow-sm">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-xs font-black uppercase tracking-widest text-blue-200">Trip Manifest</p>
            <h1 className="mt-2 text-2xl font-black sm:text-3xl">
              {trip.origin} → {trip.destination}
            </h1>
            <p className="mt-2 text-sm font-semibold text-blue-100">
              {trip.busName} · {trip.busNumber} · {formatDateTime(trip.departureAt)}
            </p>
          </div>
          <span className="rounded-full bg-white/10 px-4 py-2 text-xs font-black uppercase tracking-widest">
            {trip.status}
          </span>
        </div>
      </header>

      <section className="mb-6 grid gap-4 md:grid-cols-3">
        <StatCard icon={<Users />} label="Confirmed" value={summary.totalConfirmedPassengers} />
        <StatCard icon={<CheckCircle2 />} label="Boarded" value={summary.boardedPassengers} green />
        <StatCard icon={<Bus />} label="Remaining" value={summary.notYetBoardedPassengers} />
      </section>

      <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex flex-1 items-center gap-3 rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3">
            <Search size={18} className="text-slate-500" />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search passenger, ticket, booking or seat..."
              className="w-full bg-transparent text-sm font-bold outline-none"
            />
          </div>
          <div className="grid grid-cols-3 gap-2">
            {filters.map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => setFilter(item)}
                className={`rounded-2xl px-4 py-3 text-xs font-black transition ${
                  filter === item ? "bg-[#08264a] text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}
              >
                {item.replace("_", " ")}
              </button>
            ))}
          </div>
        </div>

        <div className="mt-5 grid gap-4">
          {passengers.length === 0 ? (
            <div className="rounded-2xl bg-slate-50 p-8 text-center text-sm font-bold text-slate-500">
              No passengers match this view.
            </div>
          ) : passengers.map((row) => (
            <PassengerCard key={row.ticketNumber} row={row} />
          ))}
        </div>
      </section>
    </DriverLayout>
  );
}

function PassengerCard({ row }) {
  const boarded = row.ticketStatus === "USED";
  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-lg font-black text-slate-900">{row.passengerName}</h2>
            <span className={`rounded-full px-3 py-1 text-[10px] font-black ${boarded ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"}`}>
              {boarded ? "BOARDED" : "NOT BOARDED"}
            </span>
          </div>
          <p className="mt-1 text-sm font-semibold text-slate-500">{row.passengerPhone}</p>
        </div>
        <div className="text-left md:text-right">
          <p className="text-sm font-black text-slate-900">{(row.seats || []).join(", ") || "No seats"}</p>
          <p className="mt-1 text-xs font-semibold text-slate-500">{formatDateTime(row.boardedAt)}</p>
        </div>
      </div>
      <div className="mt-4 grid gap-3 rounded-2xl bg-slate-50 p-4 text-sm sm:grid-cols-2">
        <Detail label="Booking" value={row.bookingReference} />
        <Detail label="Ticket" value={row.ticketNumber} />
      </div>
    </article>
  );
}

function StatCard({ icon, label, value, green }) {
  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className={`flex h-12 w-12 items-center justify-center rounded-2xl ${green ? "bg-emerald-100 text-emerald-700" : "bg-blue-50 text-[#08264a]"}`}>
        {icon}
      </div>
      <p className="mt-4 text-xs font-black uppercase tracking-widest text-slate-500">{label}</p>
      <h2 className="mt-1 text-3xl font-black text-slate-900">{value}</h2>
    </div>
  );
}

function Detail({ label, value }) {
  return (
    <div>
      <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">{label}</p>
      <p className="mt-1 break-words font-black text-slate-900">{value || "Not available"}</p>
    </div>
  );
}

function formatDateTime(value) {
  if (!value) return "Not boarded yet";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}
