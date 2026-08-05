import { useEffect, useId, useMemo, useRef, useState } from "react";
import { Bus, CalendarDays, Loader2, MapPin, RotateCcw, Search } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { useLanguage } from "../../hooks/useLanguage";
import { formatPassengerTripDate, handlePassengerSession, passengerTripRequest } from "../../utils/passengerTrips";

export default function PassengerTripSearchPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useLanguage();
  const [form, setForm] = useState({ origin: location.state?.origin || "", destination: location.state?.destination || "", date: location.state?.date || "" });
  const [trips, setTrips] = useState([]);
  const [searched, setSearched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [routeOptions, setRouteOptions] = useState([]);

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(async () => {
      try {
        const data = await passengerTripRequest("/api/passenger/trips/route-options");
        if (active) setRouteOptions(Array.isArray(data) ? data : []);
      } catch (loadError) {
        if (active && !handlePassengerSession(loadError, navigate)) setRouteOptions([]);
      }
    }, 0);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [navigate]);

  const originSuggestions = useMemo(
    () => routeOptions.map((route) => route.origin),
    [routeOptions],
  );
  const destinationSuggestions = useMemo(
    () => routeOptions
      .map((route) => route.destination)
      .filter((destination) => destination?.toLocaleLowerCase() !== form.origin.trim().toLocaleLowerCase()),
    [form.origin, routeOptions],
  );

  const searchTrips = async (event) => {
    event.preventDefault();
    const origin = form.origin.trim(); const destination = form.destination.trim();
    if (!origin || !destination) { setError(t("passenger.booking.enterOriginDestination")); return; }
    setLoading(true); setError(""); setSearched(true);
    const query = new URLSearchParams({ origin, destination });
    if (form.date) query.set("date", form.date);
    try { setTrips(await passengerTripRequest(`/api/passenger/trips/search?${query}`)); }
    catch (loadError) { if (!handlePassengerSession(loadError, navigate)) { setError(loadError.message); setTrips([]); } }
    finally { setLoading(false); }
  };
  const clear = () => { setForm({ origin: "", destination: "", date: "" }); setTrips([]); setSearched(false); setError(""); };

  return <PassengerLayout activePage="Book Out-of-Valley" title={t("passenger.booking.searchTitle")} subtitle={t("passenger.booking.searchSubtitle")}><div className="space-y-6">
    <p className="text-xs font-black uppercase tracking-widest text-violet-700">{t("passenger.booking.outOfValleyEyebrow")}</p>
    <form onSubmit={searchTrips} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm"><div className="grid grid-cols-1 gap-4 lg:grid-cols-[1fr_1fr_0.8fr_auto] lg:items-end"><AutocompleteField id="trip-origin" icon={<MapPin size={18} />} label={t("passenger.booking.fromOrigin")} value={form.origin} onChange={(value) => setForm((current) => ({ ...current, origin: value }))} suggestions={originSuggestions} placeholder="Kathmandu" required /><AutocompleteField id="trip-destination" icon={<MapPin size={18} />} label={t("passenger.booking.toDestination")} value={form.destination} onChange={(value) => setForm((current) => ({ ...current, destination: value }))} suggestions={destinationSuggestions} placeholder="Pokhara" required /><Field icon={<CalendarDays size={18} />} label={t("passenger.booking.departureDateOptional")} type="date" value={form.date} onChange={(value) => setForm((current) => ({ ...current, date: value }))} /><div className="flex gap-2"><button disabled={loading} className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 font-black text-white disabled:opacity-60"><Search size={18} /> {t("passenger.booking.search")}</button><button type="button" onClick={clear} title={t("passenger.booking.clearSearch")} aria-label={t("passenger.booking.clearSearch")} className="rounded-xl border border-slate-300 p-3 text-slate-600"><RotateCcw size={18} /></button></div></div></form>
    {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}
    {loading ? <div className="flex min-h-64 items-center justify-center"><Loader2 className="animate-spin" size={40} /></div> : searched && trips.length === 0 && !error ? <div className="rounded-3xl border border-slate-200 bg-white py-16 text-center"><Bus className="mx-auto text-slate-300" size={48} /><h2 className="mt-4 text-xl font-black">{t("passenger.booking.noTripsTitle")}</h2><p className="mt-2 text-sm text-slate-500">{t("passenger.booking.noTripsDescription")}</p></div> : <div className="space-y-4">{trips.map((trip) => <TripCard key={trip.tripId} trip={trip} onView={() => navigate(`/passenger/trips/${trip.tripId}`)} t={t} />)}</div>}
  </div></PassengerLayout>;
}

function Field({ icon, label, value, onChange, type = "text", ...props }) { return <label className="block text-xs font-black uppercase tracking-wide text-slate-500">{label}<div className="mt-2 flex items-center gap-2 rounded-xl border border-slate-300 px-3 focus-within:border-[#08264a]"><span>{icon}</span><input {...props} type={type} value={value} onChange={(event) => onChange(event.target.value)} className="w-full py-3 font-semibold normal-case text-slate-800 outline-none" /></div></label>; }

function AutocompleteField({ id, icon, label, value, onChange, suggestions, ...props }) {
  const listboxId = `${id}-${useId().replaceAll(":", "")}-suggestions`;
  const rootRef = useRef(null);
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const matches = useMemo(() => {
    const query = value.trim().toLocaleLowerCase();
    if (!query) return [];
    const unique = new Map();
    suggestions.filter(Boolean).forEach((suggestion) => {
      const key = suggestion.trim().toLocaleLowerCase();
      if (key.includes(query) && !unique.has(key)) unique.set(key, suggestion.trim());
    });
    return [...unique.values()]
      .sort((left, right) => left.localeCompare(right))
      .slice(0, 6);
  }, [suggestions, value]);

  useEffect(() => {
    const closeOnOutsideClick = (event) => {
      if (!rootRef.current?.contains(event.target)) setOpen(false);
    };
    document.addEventListener("mousedown", closeOnOutsideClick);
    return () => document.removeEventListener("mousedown", closeOnOutsideClick);
  }, []);

  const selectSuggestion = (suggestion) => {
    onChange(suggestion);
    setOpen(false);
    setActiveIndex(-1);
  };

  const handleKeyDown = (event) => {
    if (event.key === "Escape") {
      setOpen(false);
      setActiveIndex(-1);
      return;
    }
    if (!matches.length) return;
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setOpen(true);
      setActiveIndex((current) => (current + 1) % matches.length);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setOpen(true);
      setActiveIndex((current) => (current <= 0 ? matches.length - 1 : current - 1));
    } else if (event.key === "Enter" && open && activeIndex >= 0) {
      event.preventDefault();
      selectSuggestion(matches[activeIndex]);
    }
  };

  const expanded = open && matches.length > 0;
  return (
    <div ref={rootRef} className="relative min-w-0">
      <label htmlFor={id} className="block text-xs font-black uppercase tracking-wide text-slate-500">{label}</label>
      <div className="mt-2 flex items-center gap-2 rounded-xl border border-slate-300 px-3 focus-within:border-[#08264a]">
        <span aria-hidden="true">{icon}</span>
        <input
          {...props}
          id={id}
          type="text"
          role="combobox"
          aria-autocomplete="list"
          aria-expanded={expanded}
          aria-controls={listboxId}
          aria-activedescendant={activeIndex >= 0 ? `${listboxId}-option-${activeIndex}` : undefined}
          value={value}
          onChange={(event) => {
            onChange(event.target.value);
            setOpen(true);
            setActiveIndex(-1);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
          className="w-full py-3 font-semibold normal-case text-slate-800 outline-none"
        />
      </div>
      {expanded && (
        <ul id={listboxId} role="listbox" className="absolute left-0 right-0 top-full z-30 mt-1 max-h-56 overflow-y-auto rounded-xl border border-slate-200 bg-white p-1 shadow-lg">
          {matches.map((suggestion, index) => (
            <li
              id={`${listboxId}-option-${index}`}
              key={suggestion.toLocaleLowerCase()}
              role="option"
              aria-selected={index === activeIndex}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => selectSuggestion(suggestion)}
              className={`cursor-pointer rounded-lg px-3 py-2.5 text-sm text-slate-700 ${index === activeIndex ? "bg-[#08264a] text-white" : "hover:bg-slate-100"}`}
            >
              {suggestion}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function TripCard({ trip, onView, t }) { const local = trip.tripType === "LOCAL"; return <article className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"><div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center"><div><div className="flex flex-wrap items-center gap-2"><h2 className="text-xl font-black text-slate-900">{trip.routeName}</h2><span className="rounded-full bg-blue-100 px-3 py-1 text-xs font-black text-blue-700">{trip.status}</span><span className={`rounded-full px-3 py-1 text-xs font-black ${local ? "bg-amber-100 text-amber-700" : "bg-violet-100 text-violet-700"}`}>{local ? t("passenger.booking.local") : t("passenger.booking.outsideValley")}</span></div><p className="mt-1 font-bold text-slate-500">{trip.routeCode} - {t("passenger.booking.routeConnector", { origin: trip.origin, destination: trip.destination })}</p><div className="mt-4 grid grid-cols-1 gap-2 text-sm sm:grid-cols-2 lg:grid-cols-3"><p><b>{t("passenger.booking.operator")}:</b> {trip.operatorName}</p><p><b>{t("passenger.booking.bus")}:</b> {trip.busName || trip.busNumber} ({trip.busNumber})</p><p><b>{t("passenger.booking.busCapacity")}:</b> {t("passenger.booking.seatsCount", { count: trip.seatCapacity })}</p><p><b>{t("passenger.booking.departure")}:</b> {formatPassengerTripDate(trip.departureAt)}</p><p><b>{t("passenger.booking.arrival")}:</b> {formatPassengerTripDate(trip.estimatedArrivalAt)}</p><p><b>{t("passenger.booking.fare")}:</b> NPR {Number(trip.fare).toLocaleString()}</p></div></div><div className="flex shrink-0 flex-col gap-2"><button type="button" onClick={onView} className="rounded-xl bg-[#08264a] px-6 py-3 font-black text-white">{t("passenger.booking.viewDetails")}</button><button type="button" onClick={onView} className="rounded-xl border border-[#08264a] px-6 py-3 font-black text-[#08264a]">{local ? t("passenger.booking.viewLocalService") : t("passenger.booking.selectTrip")}</button></div></div></article>; }
