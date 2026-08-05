import { useCallback, useEffect, useState } from "react";
import {
  AlertTriangle,
  ArrowRight,
  Bus,
  CalendarDays,
  CheckCircle2,
  Loader2,
  MapPin,
  Play,
  QrCode,
  RefreshCw,
  Users,
  XCircle,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import ConfirmationModal from "../../components/common/ConfirmationModal";
import DriverLayout from "../../components/layout/DriverLayout";
import { GPS_STATUS, useDriverLocationTracking } from "../../hooks/useDriverLocationTracking";
import { useLocalServiceLocationTracking } from "../../hooks/useLocalServiceLocationTracking";
import {
  finishDriverLocalService,
  getCurrentDriverLocalService,
  serviceStatusLabel,
  serviceStatusTone,
  startDriverLocalService,
} from "../../utils/localServices";
import {
  beginDriverTripBoarding,
  finishDriverTrip,
  getCurrentDriverTrip,
  getDriverScheduledTrips,
  startDriverTrip,
  tripStatusLabel,
  tripStatusTone,
} from "../../utils/driverTrips";
import { selectCurrentDriverWork } from "../../utils/driverCurrentWork";

export default function TripManagementPage() {
  const navigate = useNavigate();
  const [trip, setTrip] = useState(null);
  const [loading, setLoading] = useState(true);
  const [operating, setOperating] = useState(false);
  const [error, setError] = useState("");
  const [confirmFinish, setConfirmFinish] = useState(false);
  const [upcoming, setUpcoming] = useState({ content: [], page: 0, last: true });
  const [history, setHistory] = useState({ content: [], page: 0, last: true });
  const [loadingMore, setLoadingMore] = useState("");
  const gps = useDriverLocationTracking(
    trip?.workType === "SCHEDULED_TRIP" ? trip.scheduledTripId : null,
    trip?.workType === "SCHEDULED_TRIP" && trip.status === "IN_PROGRESS",
  );
  const localGps = useLocalServiceLocationTracking(
    trip?.workType === "LOCAL_SERVICE" ? trip.id : null,
    trip?.workType === "LOCAL_SERVICE" && trip.status === "IN_SERVICE",
  );

  const loadTrip = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [scheduledTrip, localService, upcomingTrips, tripHistory] = await Promise.all([
        getCurrentDriverTrip(),
        getCurrentDriverLocalService(),
        getDriverScheduledTrips("UPCOMING"),
        getDriverScheduledTrips("HISTORY"),
      ]);
      setTrip(selectCurrentDriverWork(scheduledTrip, localService));
      setUpcoming(upcomingTrips);
      setHistory(tripHistory);
    } catch (loadError) {
      setError(loadError.message || "Unable to load your assigned trip.");
    } finally {
      setLoading(false);
    }
  }, []);

  const loadMore = async (scope) => {
    const current = scope === "UPCOMING" ? upcoming : history;
    if (current.last || loadingMore) return;
    setLoadingMore(scope);
    try {
      const next = await getDriverScheduledTrips(scope, current.page + 1);
      const merged = { ...next, content: [...current.content, ...next.content] };
      if (scope === "UPCOMING") setUpcoming(merged);
      else setHistory(merged);
    } catch (loadError) {
      const message = loadError.message || "Unable to load more trips.";
      setError(message);
      toast.error(message);
    } finally {
      setLoadingMore("");
    }
  };

  useEffect(() => {
    Promise.resolve().then(loadTrip);
  }, [loadTrip]);

  const operate = async (action) => {
    if (!trip || operating) return;
    setOperating(true);
    setError("");
    try {
      const localService = trip.workType === "LOCAL_SERVICE";
      await (localService
        ? action === "start"
          ? startDriverLocalService(trip.id)
          : finishDriverLocalService(trip.id)
        : action === "boarding"
          ? beginDriverTripBoarding(trip.scheduledTripId)
          : action === "start"
            ? startDriverTrip(trip.scheduledTripId)
            : finishDriverTrip(trip.scheduledTripId));
      setConfirmFinish(false);
      await loadTrip();
      toast.success(localService
        ? action === "start" ? "Local service started." : "Local service completed."
        : {
            boarding: "Boarding started.",
            start: "Trip started. GPS tracking is now active.",
            finish: "Trip completed.",
          }[action]);
    } catch (operationError) {
      const message = operationError.message || "Trip operation could not be completed.";
      setError(message);
      toast.error(message);
    } finally {
      setOperating(false);
    }
  };

  return (
    <DriverLayout activePage="Trip Management">
      <div className="space-y-5">
        <header className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-xl font-semibold text-slate-900 sm:text-2xl">Trip Management</h1>
            <p className="mt-1 text-slate-500">Manage boarding, live operation, and completion for your assigned trip.</p>
          </div>
          <button
            type="button"
            onClick={loadTrip}
            disabled={loading || operating}
            className="inline-flex h-11 w-fit items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
          >
            <RefreshCw size={17} className={loading ? "animate-spin" : ""} />
            Refresh
          </button>
        </header>

        {error && (
          <div className="flex gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-800">
            <AlertTriangle size={19} className="mt-0.5 shrink-0" />
            <p>{error}</p>
          </div>
        )}

        {loading ? (
          <div className="flex min-h-48 items-center justify-center rounded-3xl border border-slate-200 bg-white">
            <Loader2 size={30} className="animate-spin text-[#08264a]" />
          </div>
        ) : !trip ? (
          <div className="rounded-3xl border border-slate-200 bg-white p-5 text-center shadow-sm">
            <Bus size={32} className="mx-auto text-slate-300" />
            <h2 className="mt-3 text-lg font-semibold text-slate-900">No active assignment</h2>
            <p className="mt-1.5 text-sm text-slate-500">
              Your current scheduled trip or local service will appear here.
            </p>
          </div>
        ) : (
          <>
            <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                <div>
                  <p className="text-xs font-medium uppercase tracking-[0.2em] text-slate-500">
                    {trip.workType === "LOCAL_SERVICE" ? "Local service" : trip.routeCode || "Assigned trip"}
                  </p>
                  <h2 className="mt-1.5 text-xl font-semibold text-slate-900">
                    {trip.origin} → {trip.destination}
                  </h2>
                  <p className="mt-1 text-sm font-medium text-slate-500">
                    {trip.routeName}{trip.operatorName ? ` · ${trip.operatorName}` : ""}
                  </p>
                </div>
                <span className={`self-start rounded-full px-3 py-1.5 text-xs font-medium uppercase tracking-wide ${
                  trip.workType === "LOCAL_SERVICE" ? serviceStatusTone(trip.status) : tripStatusTone(trip.status)
                }`}>
                  {trip.workType === "LOCAL_SERVICE" ? serviceStatusLabel(trip.status) : tripStatusLabel(trip.status)}
                </span>
              </div>

              {trip.workType === "SCHEDULED_TRIP" && trip.status === "IN_PROGRESS" && (
                <GpsStatus status={gps.status} message={gps.message} />
              )}
              {trip.workType === "LOCAL_SERVICE" && trip.status === "IN_SERVICE" && (
                <GpsStatus status={localGps.status} message={localGps.message} />
              )}

              <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                <Detail icon={<Bus size={19} />} label="Assigned bus" value={`${trip.busNumber}${trip.busName ? ` · ${trip.busName}` : ""}`} />
                {trip.workType === "LOCAL_SERVICE" ? (
                  <>
                    <Detail icon={<CalendarDays size={19} />} label="Service date" value={formatServiceDate(trip.serviceDate)} />
                    <Detail icon={<MapPin size={19} />} label="Planned time" value={`${trip.plannedStartTime} - ${trip.plannedEndTime}`} />
                    <Detail icon={<Users size={19} />} label="Ordered stops" value={`${trip.orderedStops?.length || 0} stops`} />
                  </>
                ) : (
                  <>
                    <Detail icon={<CalendarDays size={19} />} label="Departure" value={formatDateTime(trip.departureAt)} />
                    <Detail icon={<MapPin size={19} />} label="Estimated arrival" value={formatDateTime(trip.estimatedArrivalAt)} />
                    <Detail icon={<Users size={19} />} label="Boarded passengers" value={`${trip.boardedPassengers || 0}/${trip.confirmedPassengers || 0}`} />
                  </>
                )}
              </div>

              {trip.workType === "LOCAL_SERVICE" && trip.orderedStops?.length > 0 && (
                <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-3">
                  <h3 className="font-semibold text-slate-900">Ordered stops</h3>
                  <div className="mt-2 grid gap-2 sm:grid-cols-2">
                    {trip.orderedStops.map((stop) => (
                      <div key={stop.id} className="flex items-center gap-3 rounded-xl bg-white p-2.5">
                        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#08264a] text-xs font-semibold text-white">
                          {stop.stopOrder}
                        </span>
                        <div className="min-w-0">
                          <p className="truncate font-semibold text-slate-900">{stop.stopName}</p>
                          <p className="truncate text-xs text-slate-500">{stop.landmark || "Local route stop"}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div className="mt-4 flex flex-wrap items-center gap-2">
                {trip.workType === "LOCAL_SERVICE" && trip.status === "PLANNED" && (
                  <ActionButton busy={operating} onClick={() => operate("start")}>
                    <Play size={18} /> Start Local Service
                  </ActionButton>
                )}
                {trip.workType === "LOCAL_SERVICE" && trip.status === "IN_SERVICE" && (
                  <button
                    type="button"
                    disabled={operating}
                    onClick={() => setConfirmFinish(true)}
                    className="inline-flex h-11 w-fit items-center justify-center rounded-xl bg-red-600 px-4 text-sm font-semibold text-white shadow-lg shadow-red-600/20 transition hover:bg-red-700 disabled:opacity-60"
                  >
                    End Local Service
                  </button>
                )}
                {trip.workType === "SCHEDULED_TRIP" && trip.status === "SCHEDULED" && (
                  <ActionButton busy={operating} onClick={() => operate("boarding")}>
                    <Users size={18} /> Begin Boarding
                  </ActionButton>
                )}
                {trip.workType === "SCHEDULED_TRIP" && trip.status === "BOARDING" && (
                  <>
                    <ActionButton busy={operating} onClick={() => operate("start")}>
                      <Play size={18} /> Start Trip
                    </ActionButton>
                    <SecondaryButton onClick={() => navigate("/driver/scanner")}>
                      <QrCode size={18} /> Scan Tickets
                    </SecondaryButton>
                    <SecondaryButton onClick={() => navigate(`/driver/trips/${trip.scheduledTripId}/manifest`)}>
                      <CheckCircle2 size={18} /> View Manifest
                    </SecondaryButton>
                  </>
                )}
                {trip.workType === "SCHEDULED_TRIP" && trip.status === "IN_PROGRESS" && (
                  <>
                    <SecondaryButton onClick={() => navigate("/driver/scanner")}>
                      <QrCode size={18} /> Scan Tickets
                    </SecondaryButton>
                    <SecondaryButton onClick={() => navigate(`/driver/trips/${trip.scheduledTripId}/manifest`)}>
                      <CheckCircle2 size={18} /> View Manifest
                    </SecondaryButton>
                    <button
                      type="button"
                      disabled={operating}
                      onClick={() => setConfirmFinish(true)}
                      className="inline-flex h-11 w-fit items-center justify-center rounded-xl bg-red-600 px-4 text-sm font-semibold text-white shadow-lg shadow-red-600/20 transition hover:bg-red-700 disabled:opacity-60"
                    >
                      Finish Trip
                    </button>
                  </>
                )}
              </div>
            </section>
          </>
        )}

        {!loading && (
          <>
            <ScheduledTripList
              title="Upcoming Scheduled Trips"
              subtitle="All current and future out-of-valley trips assigned to you."
              trips={upcoming.content.filter((item) =>
                trip?.workType !== "SCHEDULED_TRIP"
                || item.scheduledTripId !== trip.scheduledTripId)}
              empty="No additional upcoming scheduled trips."
              grouped
              hasMore={!upcoming.last}
              loadingMore={loadingMore === "UPCOMING"}
              onLoadMore={() => loadMore("UPCOMING")}
            />
            <ScheduledTripList
              title="Trip History"
              subtitle="Completed and cancelled scheduled trips."
              trips={history.content}
              empty="No completed or cancelled scheduled trips."
              history
              hasMore={!history.last}
              loadingMore={loadingMore === "HISTORY"}
              onLoadMore={() => loadMore("HISTORY")}
            />
          </>
        )}
      </div>

      <ConfirmationModal
        open={confirmFinish}
        title={trip?.workType === "LOCAL_SERVICE" ? "End Local Service?" : "Finish Trip?"}
        message={trip?.workType === "LOCAL_SERVICE"
          ? "Confirm that this local service has ended. It will be marked as completed."
          : "Confirm that this trip has reached its destination. GPS tracking will stop and the trip will be marked as completed."}
        confirmLabel={trip?.workType === "LOCAL_SERVICE" ? "End Service" : "Finish Trip"}
        busyLabel="Finishing..."
        destructive
        busy={operating}
        onClose={() => setConfirmFinish(false)}
        onConfirm={() => operate("finish")}
      />
    </DriverLayout>
  );
}

function ScheduledTripList({
  title, subtitle, trips, empty, grouped = false, history = false,
  hasMore, loadingMore, onLoadMore,
}) {
  const sections = grouped ? groupTripsByDate(trips) : [["", trips]];
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
      <h2 className="text-xl font-semibold text-slate-900">{title}</h2>
      <p className="mt-1 text-sm text-slate-500">{subtitle}</p>
      {trips.length === 0 ? (
        <div className="mt-4 rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-5 text-center">
          <CalendarDays className="mx-auto text-slate-300" size={28} />
          <p className="mt-2 text-sm text-slate-600">{empty}</p>
        </div>
      ) : (
        <div className={history ? "mt-3 space-y-3" : "mt-4 space-y-4"}>
          {sections.map(([heading, items]) => (
            <div key={heading || "history"}>
              {heading && <h3 className="mb-2 text-sm font-semibold uppercase tracking-wider text-blue-700">{heading}</h3>}
              <div className={`grid gap-3 ${history ? "lg:grid-cols-2" : "xl:grid-cols-2"}`}>
                {items.map((item) => <ScheduledTripCard key={item.scheduledTripId} trip={item} history={history} />)}
              </div>
            </div>
          ))}
        </div>
      )}
      {hasMore && (
        <button type="button" disabled={loadingMore} onClick={onLoadMore}
          className="mx-auto mt-4 flex h-11 w-fit items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 text-sm font-semibold text-slate-700 disabled:opacity-60">
          {loadingMore && <Loader2 size={17} className="animate-spin" />}
          {loadingMore ? "Loading..." : "Load More"}
        </button>
      )}
    </section>
  );
}

function ScheduledTripCard({ trip, history = false }) {
  if (history) {
    return (
      <article className="min-w-0 rounded-2xl border border-slate-200 bg-slate-100/80 p-4">
        <div className="flex min-w-0 items-start justify-between gap-3">
          <div className="min-w-0 flex-1">
            <h3 className="wrap-break-word text-sm font-semibold leading-5 text-slate-900">{trip.routeName}</h3>
            <p className="mt-0.5 flex min-w-0 items-center gap-1.5 text-sm font-normal leading-5 text-slate-500">
              <span className="min-w-0 wrap-break-word">{trip.origin}</span>
              <ArrowRight aria-hidden="true" className="shrink-0" size={14} strokeWidth={1.8} />
              <span className="min-w-0 wrap-break-word">{trip.destination}</span>
            </p>
          </div>
          <span className={`inline-flex h-6 shrink-0 items-center gap-1 rounded-full px-2.5 text-[11px] font-semibold uppercase leading-none ${
            trip.status === "CANCELLED"
              ? "border border-red-300 bg-red-200 text-red-800"
              : tripStatusTone(trip.status)
          }`}>
            {trip.status === "CANCELLED" && <XCircle aria-hidden="true" size={12} strokeWidth={2.2} />}
            {tripStatusLabel(trip.status)}
          </span>
        </div>

        <dl className="mt-3 grid gap-x-4 gap-y-2.5 sm:grid-cols-2 2xl:grid-cols-3">
          <HistoryTripFact label="Departure" value={formatDateTime(trip.departureAt)} keepTogether />
          <HistoryTripFact label="Estimated arrival" value={formatDateTime(trip.estimatedArrivalAt)} keepTogether />
          <HistoryTripFact label="Bus" value={<BusIdentity number={trip.busNumber} name={trip.busName} />} />
          <HistoryTripFact label="Operator" value={trip.operatorName} />
          <HistoryTripFact label="Fare" value={trip.fare == null ? "Not available" : `NPR ${Number(trip.fare).toFixed(2)}`} />
        </dl>

        {trip.boardingNotes && (
          <p className="mt-3 border-t border-slate-200 pt-2.5 text-sm leading-5 text-slate-600">
            {trip.boardingNotes}
          </p>
        )}
      </article>
    );
  }

  return (
    <article className="min-w-0 rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="wrap-break-word text-base font-semibold text-slate-900">{trip.routeName}</h3>
          <p className="mt-0.5 wrap-break-word text-sm font-medium text-slate-600">{trip.origin} → {trip.destination}</p>
        </div>
        <span className={`rounded-full px-3 py-1 text-xs font-medium uppercase ${tripStatusTone(trip.status)}`}>
          {tripStatusLabel(trip.status)}
        </span>
      </div>
      <dl className="mt-3 grid gap-2 text-sm sm:grid-cols-2">
        <TripFact label="Departure" value={formatDateTime(trip.departureAt)} />
        <TripFact label="Estimated arrival" value={formatDateTime(trip.estimatedArrivalAt)} />
        <TripFact label="Bus" value={`${trip.busNumber}${trip.busName ? ` · ${trip.busName}` : ""}`} />
        <TripFact label="Operator" value={trip.operatorName} />
        <TripFact label="Fare" value={trip.fare == null ? "Not available" : `NPR ${Number(trip.fare).toFixed(2)}`} />
      </dl>
      {trip.boardingNotes && <p className="mt-3 rounded-xl bg-white p-2.5 text-sm text-slate-600">{trip.boardingNotes}</p>}
    </article>
  );
}

function HistoryTripFact({ label, value, keepTogether = false }) {
  return <div className="min-w-0"><dt className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</dt><dd className={`mt-0.5 text-sm font-semibold leading-5 text-slate-700 ${keepTogether ? "whitespace-nowrap" : "wrap-break-word"}`}>{value || "Not available"}</dd></div>;
}

function BusIdentity({ number, name }) {
  return <span className="inline-flex flex-wrap items-center gap-1.5"><span>{number}</span>{name && <><span aria-hidden="true" className="h-1 w-1 rounded-full bg-slate-400" /><span>{name}</span></>}</span>;
}

function TripFact({ label, value }) {
  return <div><dt className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</dt><dd className="mt-0.5 wrap-break-word font-semibold text-slate-700">{value || "Not available"}</dd></div>;
}

function groupTripsByDate(trips) {
  const groups = new Map();
  for (const trip of trips) {
    const label = dateGroupLabel(trip.departureAt);
    groups.set(label, [...(groups.get(label) || []), trip]);
  }
  return [...groups.entries()];
}

function dateGroupLabel(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Date unavailable";
  const today = new Date();
  const tomorrow = new Date(today);
  tomorrow.setDate(today.getDate() + 1);
  const key = (item) => `${item.getFullYear()}-${item.getMonth()}-${item.getDate()}`;
  if (key(date) === key(today)) return "Today";
  if (key(date) === key(tomorrow)) return "Tomorrow";
  return new Intl.DateTimeFormat("en-NP", {
    day: "numeric", month: "long", year: "numeric",
  }).format(date);
}

function ActionButton({ busy, onClick, children }) {
  return (
    <button
      type="button"
      disabled={busy}
      onClick={onClick}
      className="inline-flex h-11 w-fit items-center justify-center gap-2 rounded-xl bg-[#08264a] px-4 text-sm font-semibold text-white shadow-lg shadow-blue-950/20 transition hover:bg-[#0d3566] disabled:opacity-60"
    >
      {children}
    </button>
  );
}

function SecondaryButton({ onClick, children }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="inline-flex h-11 w-fit items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
    >
      {children}
    </button>
  );
}

function Detail({ icon, label, value }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
      <div className="flex items-center gap-2 text-slate-500">{icon}<span className="text-xs font-medium uppercase">{label}</span></div>
      <p className="mt-1.5 text-sm font-semibold text-slate-900">{value || "Not available"}</p>
    </div>
  );
}

function GpsStatus({ status, message }) {
  const tone = {
    [GPS_STATUS.ACTIVE]: "border-emerald-200 bg-emerald-50 text-emerald-800",
    [GPS_STATUS.NETWORK_ERROR]: "border-red-200 bg-red-50 text-red-800",
    [GPS_STATUS.PERMISSION_DENIED]: "border-red-200 bg-red-50 text-red-800",
    [GPS_STATUS.UNAVAILABLE]: "border-amber-200 bg-amber-50 text-amber-800",
    [GPS_STATUS.WAITING]: "border-blue-200 bg-blue-50 text-blue-800",
  }[status] || "border-slate-200 bg-slate-50 text-slate-700";
  return (
    <div className={`mt-4 rounded-xl border p-3 ${tone}`}>
      <p className="font-semibold">{status}</p>
      {message && <p className="mt-0.5 text-sm">{message}</p>}
    </div>
  );
}

function formatDateTime(value) {
  if (!value) return "Not available";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("en-NP", { dateStyle: "medium", timeStyle: "short" }).format(date);
}

function formatServiceDate(value) {
  if (!value) return "Not available";
  const date = new Date(`${value}T00:00:00`);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("en-NP", { dateStyle: "medium" }).format(date);
}
