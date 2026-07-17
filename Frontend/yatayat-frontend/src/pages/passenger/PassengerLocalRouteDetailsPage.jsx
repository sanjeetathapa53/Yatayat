import { useEffect, useState } from "react";
import { ArrowLeft, Loader2, MapPin } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { localRouteRequest } from "../../utils/localRoutes";

export default function PassengerLocalRouteDetailsPage() {
  const { routeId } = useParams();
  const navigate = useNavigate();
  const [route, setRoute] = useState(null);
  const [error, setError] = useState("");
  useEffect(() => { localRouteRequest(`/api/passenger/local-routes/${routeId}`).then(setRoute).catch((requestError) => { if (requestError.status === 401) navigate("/login", { replace: true }); else setError(requestError.message); }); }, [navigate, routeId]);
  return <PassengerLayout activePage="Find Local Route"><div className="mx-auto max-w-4xl space-y-6"><button onClick={() => navigate("/passenger/local-routes")} className="flex items-center gap-2 text-sm font-black"><ArrowLeft size={17} /> Back to local route search</button>{error && <div className="rounded-2xl border border-red-200 bg-red-50 p-5 font-bold text-red-700">{error}</div>}{!route && !error ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin" size={40} /></div> : route && <><header className="rounded-3xl bg-[#08264a] p-7 text-white"><span className="rounded-full bg-emerald-400/20 px-3 py-1 text-xs font-black text-emerald-100">LOCAL</span><h1 className="mt-4 text-3xl font-black">{route.routeName}</h1><p className="mt-1 text-blue-100">{route.routeCode}</p></header><section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"><h2 className="text-xl font-black">Registered route direction</h2><div className="mt-5 flex items-center gap-3 rounded-2xl bg-slate-50 p-5 font-bold"><MapPin size={20} />{route.stopSummary.join(" → ")}</div><div className="mt-5 grid gap-4 sm:grid-cols-3"><Info label="Distance" value={`${route.distanceKm} km`} /><Info label="Estimated duration" value={`${route.estimatedDurationMinutes} minutes`} /><Info label="Fare" value={route.fareInformation} /></div><button disabled className="mt-6 w-full cursor-not-allowed rounded-xl bg-slate-200 py-3 font-black text-slate-500">Live tracking — Coming Soon</button></section></>}</div></PassengerLayout>;
}
function Info({ label, value }) { return <div className="rounded-2xl bg-slate-50 p-4"><p className="text-xs font-black uppercase text-slate-500">{label}</p><p className="mt-2 font-bold">{value}</p></div>; }
