import { useEffect, useState } from "react";
import { ArrowLeft, Loader2 } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import OperatorLayout from "../../components/layout/OperatorLayout";
import { Status } from "./OperatorBusesPage";
import { apiFetch } from "../../utils/api";

export default function OperatorBusDetailsPage() {
  const { id } = useParams(); const navigate = useNavigate();
  const [bus, setBus] = useState(null); const [error, setError] = useState("");
  useEffect(() => { apiFetch(`/api/operator/buses/${id}`).then(async (response) => { if (response.status === 401) return navigate("/login", { replace: true }); if (response.status === 403) return navigate("/operator/application-status", { replace: true }); if (!response.ok) throw new Error("Bus not found or unavailable."); return response.json(); }).then((data) => data && setBus(data)).catch((loadError) => setError(loadError.message)); }, [id, navigate]);
  return <OperatorLayout><div className="mx-auto max-w-4xl space-y-5"><button type="button" onClick={() => navigate("/operator/buses")} className="flex items-center gap-2 text-sm font-semibold"><ArrowLeft size={17} /> Back to buses</button>{error ? <div className="rounded-2xl border border-red-200 bg-red-50 p-5 font-bold text-red-700">{error}</div> : !bus ? <div className="flex min-h-48 items-center justify-center"><Loader2 className="animate-spin" size={32} /></div> : <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><div className="flex justify-between gap-4"><div><h1 className="text-2xl font-semibold">{bus.busName}</h1><p className="mt-2 font-bold text-slate-500">{bus.busNumber}</p></div><Status status={bus.status} /></div>{bus.status === "REJECTED" && bus.rejectionReason && <div className="mt-4 rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">Reason: {bus.rejectionReason}</div>}<div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2">{[["Bus Type",bus.busType],["Seat Capacity",bus.seatCapacity],["Model",bus.model],["Manufacture Year",bus.manufactureYear],["Fuel Type",bus.fuelType],["Permit Number",bus.permitNumber],["Permit Expiry",bus.permitExpiryDate],["Insurance Expiry",bus.insuranceExpiryDate],["Approved Date",bus.approvedAt]].map(([label,value]) => <div key={label} className="rounded-2xl bg-slate-50 p-4"><p className="text-xs font-semibold uppercase text-slate-500">{label}</p><p className="mt-2 font-semibold">{value || "Not provided"}</p></div>)}</div></div>}</div></OperatorLayout>;
}
