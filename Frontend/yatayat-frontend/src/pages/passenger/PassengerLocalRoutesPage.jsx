import { useState } from "react";
import { Bus, Clock3, MapPin, Navigation, Route, Search } from "lucide-react";
import { useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { localRouteRequest } from "../../utils/localRoutes";

export default function PassengerLocalRoutesPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ origin: "", destination: "" });
  const [routes, setRoutes] = useState([]);
  const [searched, setSearched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const search = async (event) => {
    event.preventDefault();
    const origin = form.origin.trim();
    const destination = form.destination.trim();
    if (!origin || !destination || origin.toLowerCase() === destination.toLowerCase()) {
      setError("Enter different values for origin and destination.");
      return;
    }
    setLoading(true); setError(""); setSearched(true);
    try {
      const query = new URLSearchParams({ origin, destination });
      setRoutes(await localRouteRequest(`/api/passenger/local-routes/search?${query}`));
    } catch (requestError) {
      setRoutes([]);
      if (requestError.status === 401) navigate("/login", { replace: true });
      else setError(requestError.message);
    } finally { setLoading(false); }
  };

  return <PassengerLayout activePage="Find Local Route">
    <div className="space-y-6">
      <header>
        <p className="text-xs font-black uppercase tracking-widest text-emerald-700">Local transport</p>
        <h1 className="mt-1 text-3xl font-black text-slate-900">Find Local Route</h1>
        <p className="mt-1 text-sm text-slate-500">Find active local routes by their registered origin and destination.</p>
      </header>

      <form onSubmit={search} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1fr_1fr_auto] lg:items-end">
          <Field label="Origin / current area" value={form.origin} placeholder="Koteshwor" onChange={(origin) => setForm((current) => ({ ...current, origin }))} />
          <Field label="Destination" value={form.destination} placeholder="Kalanki" onChange={(destination) => setForm((current) => ({ ...current, destination }))} />
          <button disabled={loading} className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-6 py-3 font-black text-white disabled:opacity-60"><Search size={18} />{loading ? "Searching..." : "Find Route"}</button>
        </div>
      </form>

      <div className="rounded-2xl border border-blue-200 bg-blue-50 p-4 text-sm font-semibold text-blue-900">
        Intermediate stop search, live buses, ETAs and stored fare data are not available yet. Results currently match registered route endpoints in travel direction.
      </div>
      {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}
      {!loading && searched && routes.length === 0 && !error && <div className="rounded-3xl border border-slate-200 bg-white py-16 text-center"><Bus className="mx-auto text-slate-300" size={48} /><h2 className="mt-4 text-xl font-black">No local route found between the selected locations.</h2></div>}
      <div className="space-y-4">{routes.map((route) => <LocalRouteCard key={route.routeId} route={route} onView={() => navigate(`/passenger/local-routes/${route.routeId}`)} />)}</div>
    </div>
  </PassengerLayout>;
}

function Field({ label, value, onChange, placeholder }) {
  return <label className="text-xs font-black uppercase tracking-wide text-slate-500">{label}<div className="mt-2 flex items-center gap-2 rounded-xl border border-slate-300 px-3 focus-within:border-[#08264a]"><MapPin size={18} /><input required value={value} placeholder={placeholder} onChange={(event) => onChange(event.target.value)} className="w-full py-3 font-semibold normal-case outline-none" /></div></label>;
}

function LocalRouteCard({ route, onView }) {
  return <article className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"><div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center"><div><div className="flex flex-wrap items-center gap-2"><h2 className="text-xl font-black text-slate-900">{route.routeName}</h2><span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-black text-emerald-700">LOCAL</span></div><p className="mt-1 font-bold text-slate-500">{route.routeCode}</p><div className="mt-4 grid gap-2 text-sm sm:grid-cols-2"><p className="flex items-center gap-2"><Navigation size={16} />{route.origin} → {route.destination}</p><p className="flex items-center gap-2"><Clock3 size={16} />Estimated duration: {route.estimatedDurationMinutes} minutes</p><p className="flex items-center gap-2"><Route size={16} />Distance: {route.distanceKm} km</p><p>{route.fareInformation}</p></div><p className="mt-3 text-sm font-semibold text-slate-600">Stop summary: {route.stopSummary.join(" → ")}</p></div><div className="flex shrink-0 flex-col gap-2"><button onClick={onView} className="rounded-xl bg-[#08264a] px-6 py-3 font-black text-white">View Route</button><button disabled className="cursor-not-allowed rounded-xl border border-slate-300 px-6 py-3 font-black text-slate-400">Track Bus — Coming Soon</button></div></div></article>;
}
