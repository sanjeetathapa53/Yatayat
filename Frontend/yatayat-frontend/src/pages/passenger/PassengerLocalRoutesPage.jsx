import { useEffect, useState } from "react";
import { Bus, Clock3, MapPin, Navigation, Route, Search } from "lucide-react";
import { useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { localRouteRequest } from "../../utils/localRoutes";

export default function PassengerLocalRoutesPage() {
  const navigate = useNavigate();
  const [fromQuery, setFromQuery] = useState("");
  const [toQuery, setToQuery] = useState("");
  const [fromStop, setFromStop] = useState(null);
  const [toStop, setToStop] = useState(null);
  const [fromOptions, setFromOptions] = useState([]);
  const [toOptions, setToOptions] = useState([]);
  const [routes, setRoutes] = useState([]);
  const [searched, setSearched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => { loadStopOptions(fromQuery, setFromOptions); }, [fromQuery]);
  useEffect(() => { loadStopOptions(toQuery, setToOptions); }, [toQuery]);

  const search = async (event) => {
    event.preventDefault();
    if (!fromStop || !toStop) {
      setError("Choose both boarding and destination stops from the suggestions.");
      return;
    }
    if (fromStop.id === toStop.id) {
      setError("Boarding and destination stops must be different.");
      return;
    }
    setLoading(true);
    setError("");
    setSearched(true);
    try {
      const query = new URLSearchParams({ fromStopId: fromStop.id, toStopId: toStop.id });
      setRoutes(await localRouteRequest(`/api/passenger/local-routes/search?${query}`));
    } catch (requestError) {
      setRoutes([]);
      if (requestError.status === 401) navigate("/login", { replace: true });
      else setError(requestError.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <PassengerLayout activePage="Find Local Route">
      <div className="space-y-6">
        <header>
          <p className="text-xs font-black uppercase tracking-widest text-emerald-700">Local transport</p>
          <h1 className="safe-wrap mt-1 text-3xl font-black text-slate-900">Find Local Route</h1>
          <p className="mt-1 text-sm text-slate-500">Search direct local routes by boarding and destination bus stop.</p>
        </header>

        <form onSubmit={search} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-[1fr_1fr_auto] lg:items-start">
            <StopPicker label="Boarding Stop" query={fromQuery} setQuery={(value) => { setFromQuery(value); setFromStop(null); }} selected={fromStop} setSelected={(stop) => { setFromStop(stop); setFromQuery(stop.name); }} options={fromOptions} />
            <StopPicker label="Destination Stop" query={toQuery} setQuery={(value) => { setToQuery(value); setToStop(null); }} selected={toStop} setSelected={(stop) => { setToStop(stop); setToQuery(stop.name); }} options={toOptions} />
            <button type="submit" disabled={loading} className="tap-target flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-6 py-3 font-black text-white disabled:opacity-60 lg:mt-6">
              <Search size={18} />{loading ? "Searching..." : "Find Route"}
            </button>
          </div>
        </form>

        <div className="rounded-2xl border border-blue-200 bg-blue-50 p-4 text-sm font-semibold text-blue-900">
          Local travel currently supports direct routes only. Seat booking, wallet payment, QR ticketing and live GPS are not part of this local foundation phase.
        </div>
        {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}
        {!loading && searched && routes.length === 0 && !error && <div className="rounded-3xl border border-slate-200 bg-white py-16 text-center"><Bus className="mx-auto text-slate-300" size={48} /><h2 className="mt-4 text-xl font-black">No direct local route found.</h2><p className="mt-2 text-sm font-semibold text-slate-500">Reverse direction and transfer routes are not included yet.</p></div>}
        <div className="space-y-4">{routes.map((route) => <LocalRouteCard key={`${route.routeId}-${route.boardingStopId}-${route.destinationStopId}`} route={route} onView={() => navigate(`/passenger/local-routes/${route.routeId}`, { state: { fromStopId: route.boardingStopId, toStopId: route.destinationStopId } })} />)}</div>
      </div>
    </PassengerLayout>
  );
}

function StopPicker({ label, query, setQuery, selected, setSelected, options }) {
  return <div className="relative"><label className="text-xs font-black uppercase tracking-wide text-slate-500">{label}<div className="mt-2 flex items-center gap-2 rounded-xl border border-slate-300 px-3 focus-within:border-[#08264a]"><MapPin size={18} className="shrink-0" /><input required value={query} placeholder="Search stop name or landmark" onChange={(event) => setQuery(event.target.value)} className="w-full py-3 font-semibold normal-case outline-none" /></div></label>{query.trim().length >= 2 && !selected && options.length > 0 && <div className="absolute z-20 mt-2 max-h-60 w-full overflow-y-auto rounded-2xl border border-slate-200 bg-white p-2 shadow-xl">{options.map((stop) => <button type="button" key={stop.id} onClick={() => setSelected(stop)} className="w-full rounded-xl p-3 text-left hover:bg-slate-50"><span className="block font-black text-slate-900">{stop.name}</span><span className="safe-wrap block text-xs font-semibold text-slate-500">{stop.landmark || "No landmark"}</span></button>)}</div>}</div>;
}

function LocalRouteCard({ route, onView }) {
  return <article className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6"><div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center"><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><h2 className="safe-wrap text-xl font-black text-slate-900">{route.routeName}</h2><span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-black text-emerald-700">LOCAL</span></div><p className="mt-1 font-bold text-slate-500">{route.routeCode}</p><div className="mt-4 grid gap-2 text-sm md:grid-cols-2"><p className="safe-wrap flex items-center gap-2"><Navigation size={16} />{route.boardingStopName} to {route.destinationStopName}</p><p className="flex items-center gap-2"><Clock3 size={16} />{route.segmentDurationMinutes ?? route.estimatedDurationMinutes} minutes</p><p className="flex items-center gap-2"><Route size={16} />{route.intermediateStopCount ?? 0} intermediate stop(s)</p><p className="font-black text-emerald-700">NPR {route.estimatedFare ?? "--"}</p></div><p className="safe-wrap mt-3 text-sm font-semibold text-slate-600">Operating: {route.operatingStartTime || "--"} to {route.operatingEndTime || "--"}</p></div><div className="flex shrink-0 flex-col gap-2"><button type="button" onClick={onView} className="tap-target rounded-xl bg-[#08264a] px-6 py-3 font-black text-white">View Route</button><button type="button" disabled className="tap-target cursor-not-allowed rounded-xl border border-slate-300 px-6 py-3 font-black text-slate-400">Live GPS — Coming Soon</button></div></div></article>;
}

async function loadStopOptions(query, setter) {
  const safeQuery = query.trim();
  if (safeQuery.length < 2) {
    setter([]);
    return;
  }
  try {
    setter(await localRouteRequest(`/api/passenger/stops?${new URLSearchParams({ query: safeQuery })}`));
  } catch {
    setter([]);
  }
}
