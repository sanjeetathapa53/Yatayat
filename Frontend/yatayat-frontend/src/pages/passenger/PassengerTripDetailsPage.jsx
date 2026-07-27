import { useEffect, useState } from "react";
import { ArrowLeft, Bus, Loader2 } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { useLanguage } from "../../hooks/useLanguage";
import { formatPassengerTripDate, handlePassengerSession, passengerTripRequest } from "../../utils/passengerTrips";

export default function PassengerTripDetailsPage() {
  const { tripId } = useParams();
  const navigate = useNavigate();
  const { t } = useLanguage();
  const [trip, setTrip] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    passengerTripRequest(`/api/passenger/trips/${tripId}`)
      .then((data) => { if (active) setTrip(data); })
      .catch((loadError) => { if (active && !handlePassengerSession(loadError, navigate)) setError(loadError.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [navigate, tripId]);

  const rows = trip ? [
    [t("passenger.booking.tripType"), trip.tripType === "LOCAL" ? t("passenger.booking.local") : t("passenger.booking.outsideValley")],
    [t("passenger.booking.operator"), trip.operatorName], [t("passenger.booking.bus"), `${trip.busName || t("passenger.booking.genericBus")} (${trip.busNumber})`],
    [t("passenger.booking.departure"), formatPassengerTripDate(trip.departureAt)],
    [t("passenger.booking.estimatedArrival"), formatPassengerTripDate(trip.estimatedArrivalAt)],
    [t("passenger.booking.fare"), `NPR ${Number(trip.fare).toLocaleString()}`], [t("passenger.booking.busCapacity"), t("passenger.booking.seatsCount", { count: trip.seatCapacity })],
    [t("passenger.booking.status"), trip.status], [t("passenger.booking.estimatedDuration"), trip.estimatedDurationMinutes ? t("passenger.booking.durationMinutes", { minutes: trip.estimatedDurationMinutes }) : t("passenger.booking.notSpecified")],
    [t("passenger.booking.boardingNotes"), trip.boardingNotes || t("passenger.booking.noBoardingNotes")],
  ] : [];

  return <PassengerLayout activePage="Find Trips" title={trip?.routeName || "Trip details"} subtitle={trip ? t("passenger.booking.routeConnector", { origin: trip.origin, destination: trip.destination }) : "View schedule, bus, fare, and route information."}><div className="mx-auto max-w-5xl space-y-6">
    <button type="button" onClick={() => navigate("/passenger/trips")} className="flex items-center gap-2 text-sm font-black"><ArrowLeft size={17} /> {t("passenger.booking.backToTripSearch")}</button>
    {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-5 font-bold text-red-700">{error}</div>}
    {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin" size={40} /></div> : trip && <>
      <div className="rounded-3xl bg-[#08264a] p-7 text-white"><div className="flex items-center gap-4"><Bus size={34} /><p className="text-sm font-bold text-blue-200">{trip.routeCode}</p></div></div>
      <section className="grid grid-cols-1 gap-4 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-2">{rows.map(([label, value]) => <div key={label} className="rounded-2xl bg-slate-50 p-4"><p className="text-xs font-black uppercase tracking-wide text-slate-500">{label}</p><p className="mt-2 font-bold text-slate-800">{value}</p></div>)}</section>
      {trip.tripType === "LOCAL" ? <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5"><p className="font-bold text-amber-900">{t("passenger.booking.localBookingUnavailable")}</p><button type="button" disabled className="mt-4 cursor-not-allowed rounded-xl bg-amber-200 px-6 py-3 font-black text-amber-800">{t("passenger.booking.localFareComingNext")}</button></div> : <div className="rounded-2xl border border-blue-200 bg-blue-50 p-5"><p className="font-bold text-blue-900">{t("passenger.booking.selectSeatsHelp")}</p><button type="button" onClick={() => navigate(`/passenger/trips/${tripId}/seats`)} className="mt-4 rounded-xl bg-[#08264a] px-6 py-3 font-black text-white">{t("passenger.booking.selectSeats")}</button></div>}
    </>}
  </div></PassengerLayout>;
}
