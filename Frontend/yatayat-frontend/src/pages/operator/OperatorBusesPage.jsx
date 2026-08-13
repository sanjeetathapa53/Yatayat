import { useEffect, useState } from "react";
import { Bus, Check, Loader2, Plus, RefreshCw } from "lucide-react";
import { useNavigate } from "react-router-dom";
import OperatorLayout from "../../components/layout/OperatorLayout";
import { apiFetch } from "../../utils/api";

export default function OperatorBusesPage() {
  const navigate = useNavigate();
  const [buses, setBuses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadBuses = async () => {
    try {
      setLoading(true);
      setError("");
      const response = await apiFetch("/api/operator/buses");
      if (response.status === 401) return navigate("/login", { replace: true });
      if (response.status === 403 || response.status === 404) return navigate("/operator/application-status", { replace: true });
      if (!response.ok) throw new Error("Unable to load buses.");
      setBuses(await response.json());
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;

    apiFetch("/api/operator/buses").then(async (response) => {
      if (response.status === 401) {
        navigate("/login", { replace: true });
        return null;
      }
      if (response.status === 403 || response.status === 404) {
        navigate("/operator/application-status", { replace: true });
        return null;
      }
      if (!response.ok) throw new Error("Unable to load buses.");
      return response.json();
    }).then((data) => {
      if (active && data) setBuses(data);
    }).catch((loadError) => {
      if (active) setError(loadError.message);
    }).finally(() => {
      if (active) setLoading(false);
    });

    return () => { active = false; };
  }, [navigate]);

  return (
    <OperatorLayout>
      <div className="space-y-5">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div><h1 className="text-2xl font-semibold">My Buses</h1><p className="mt-1 text-sm text-slate-500">Vehicles registered by your organization.</p></div>
          <button type="button" onClick={() => navigate("/operator/buses/register")} className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-2.5 font-semibold text-white"><Plus size={18} /> Register Bus</button>
        </div>

        {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}
        {loading ? <div className="flex min-h-48 items-center justify-center"><Loader2 className="animate-spin" size={32} /></div> : buses.length === 0 ? (
          <div className="rounded-2xl border border-slate-200 bg-white px-6 py-16 text-center"><Bus className="mx-auto text-slate-300" size={48} /><h2 className="mt-5 text-xl font-semibold">No buses registered yet. Register your first bus to continue.</h2></div>
        ) : (
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            {buses.map((bus) => <button type="button" key={bus.id} onClick={() => navigate(`/operator/buses/${bus.id}`)} className="min-w-0 rounded-2xl border border-slate-200 bg-white p-4 text-left shadow-sm hover:border-[#08264a]"><div className="flex min-w-0 flex-wrap items-start justify-between gap-3"><div className="min-w-0"><h2 className="safe-wrap text-lg font-semibold text-slate-900">{bus.busName}</h2><p className="safe-wrap mt-0.5 text-sm font-medium text-slate-500">{bus.busNumber}</p></div><Status status={bus.status} /></div><div className="mt-4 grid grid-cols-2 gap-3 border-t border-slate-100 pt-3 text-sm text-slate-600"><p>Type: <b className="font-semibold text-slate-800">{bus.busType}</b></p><p>Seats: <b className="font-semibold text-slate-800">{bus.seatCapacity}</b></p></div>{bus.status === "REJECTED" && bus.rejectionReason && <p className="mt-3 rounded-xl bg-red-50 p-3 text-sm font-bold text-red-700">Reason: {bus.rejectionReason}</p>}</button>)}
          </div>
        )}
        {!loading && <button type="button" onClick={loadBuses} className="flex items-center gap-2 text-sm font-semibold"><RefreshCw size={16} /> Refresh</button>}
      </div>
    </OperatorLayout>
  );
}

export function Status({ status }) {
  const tone = status === "APPROVED" ? "bg-emerald-100 text-emerald-700" : status === "REJECTED" ? "bg-red-100 text-red-700" : status === "PENDING" ? "bg-amber-100 text-amber-700" : "bg-slate-100 text-slate-700";
  return <span className={`inline-flex h-6 shrink-0 items-center gap-1 rounded-full px-2.5 text-[11px] font-semibold leading-none ${tone}`}>{status === "APPROVED" && <Check aria-hidden="true" size={12} strokeWidth={2.5} />}{status}</span>;
}
