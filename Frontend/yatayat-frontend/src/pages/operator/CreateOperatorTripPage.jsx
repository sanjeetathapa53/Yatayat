import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import OperatorLayout from "../../components/layout/OperatorLayout";
import OperatorTripForm from "../../components/operator/OperatorTripForm";
import { handleOperatorAccess, tripFormPayload, tripRequest } from "../../utils/operatorTrips";

const emptyForm = { routeId: "", busId: "", driverId: "", departureAt: "", estimatedArrivalAt: "", fare: "", boardingNotes: "" };

export default function CreateOperatorTripPage() {
  const navigate = useNavigate();
  const [eligibility, setEligibility] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    tripRequest("/api/operator/trips/eligibility").then((data) => { if (active) setEligibility(data); }).catch((loadError) => {
      if (active && !handleOperatorAccess(loadError, navigate)) setError(loadError.message);
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [navigate]);

  const submit = async (event) => {
    event.preventDefault(); setSubmitting(true); setError("");
    try {
      const trip = await tripRequest("/api/operator/trips", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(tripFormPayload(form)) });
      toast.success("Scheduled trip created successfully.");
      navigate(`/operator/trips/${trip.id}`, { replace: true });
    } catch (submitError) {
      if (!handleOperatorAccess(submitError, navigate)) setError(submitError.message);
    } finally { setSubmitting(false); }
  };

  return <OperatorLayout><div className="mx-auto max-w-5xl space-y-6"><div><h1 className="text-3xl font-black">Create Scheduled Trip</h1><p className="mt-1 text-sm text-slate-500">Assign an eligible route, bus, and driver.</p></div>{error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}{loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin" size={40} /></div> : eligibility && <><EligibilityNotice eligibility={eligibility} /><OperatorTripForm form={form} eligibility={eligibility} submitting={submitting} submitLabel="Create Trip" onChange={(key, value) => setForm((current) => ({ ...current, [key]: value }))} onSubmit={submit} onCancel={() => navigate("/operator/trips")} /></>}</div></OperatorLayout>;
}

function EligibilityNotice({ eligibility }) {
  const missing = [];
  if (!eligibility.routes.length) missing.push("active route");
  if (!eligibility.buses.length) missing.push("approved valid bus");
  if (!eligibility.drivers.length) missing.push("approved associated driver");
  if (!missing.length) return null;
  return <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm font-bold text-amber-800">You need at least one {missing.join(", one ")} before creating a trip.</div>;
}
