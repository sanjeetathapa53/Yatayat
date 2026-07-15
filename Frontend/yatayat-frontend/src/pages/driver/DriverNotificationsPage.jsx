import { useEffect, useState } from "react";
import { Building2, CheckCircle2, Loader2, Mail, RefreshCw, XCircle } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import DriverLayout from "../../components/layout/DriverLayout";
import { apiFetch } from "../../utils/api";

export default function DriverNotificationsPage() {
  const navigate = useNavigate();
  const [invitations, setInvitations] = useState([]);
  const [association, setAssociation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [confirmation, setConfirmation] = useState(null);
  const [processing, setProcessing] = useState(false);

  const load = async () => {
    try {
      setLoading(true);
      setError("");
      const [invitationResponse, associationResponse] = await Promise.all([
        apiFetch("/api/driver/operator-invitations"),
        apiFetch("/api/driver/operator-association"),
      ]);
      if (invitationResponse.status === 401 || associationResponse.status === 401) {
        navigate("/login", { replace: true });
        return;
      }
      if (invitationResponse.status === 403 || associationResponse.status === 403) {
        navigate("/driver/application-status", { replace: true });
        return;
      }
      const invitationData = await invitationResponse.json().catch(() => ({}));
      if (!invitationResponse.ok) throw new Error(invitationData.message || "Unable to load invitations.");
      setInvitations(Array.isArray(invitationData) ? invitationData : []);
      setAssociation(associationResponse.status === 204 ? null : await associationResponse.json());
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;
    Promise.all([
      apiFetch("/api/driver/operator-invitations"),
      apiFetch("/api/driver/operator-association"),
    ]).then(async ([invitationResponse, associationResponse]) => {
      if (invitationResponse.status === 401 || associationResponse.status === 401) {
        navigate("/login", { replace: true });
        return null;
      }
      if (invitationResponse.status === 403 || associationResponse.status === 403) {
        navigate("/driver/application-status", { replace: true });
        return null;
      }
      const invitationData = await invitationResponse.json().catch(() => ({}));
      if (!invitationResponse.ok) throw new Error(invitationData.message || "Unable to load invitations.");
      const activeData = associationResponse.status === 204 ? null : await associationResponse.json();
      return { invitationData, activeData };
    }).then((data) => {
      if (active && data) {
        setInvitations(data.invitationData);
        setAssociation(data.activeData);
      }
    }).catch((loadError) => { if (active) setError(loadError.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [navigate]);

  const respond = async () => {
    if (!confirmation) return;
    try {
      setProcessing(true);
      const response = await apiFetch(
        `/api/driver/operator-invitations/${confirmation.invitation.associationId}/${confirmation.action}`,
        { method: "POST" }
      );
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Unable to respond to invitation.");
      setInvitations((current) => current.filter((item) => item.associationId !== data.associationId));
      if (confirmation.action === "accept") setAssociation(data);
      toast.success(confirmation.action === "accept" ? "Operator invitation accepted." : "Operator invitation rejected.");
      setConfirmation(null);
    } catch (responseError) {
      setError(responseError.message);
    } finally {
      setProcessing(false);
    }
  };

  return <DriverLayout activePage="Notifications"><div className="space-y-7">
    <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center"><div><h1 className="text-3xl font-black text-slate-900">Operator Invitations</h1><p className="mt-1 text-sm text-slate-600">Review invitations from approved transport operators.</p></div><button type="button" onClick={load} className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 font-black text-white"><RefreshCw size={17} /> Refresh</button></header>
    {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}
    {association && <section className="rounded-3xl border border-emerald-200 bg-emerald-50 p-6"><div className="flex items-center gap-3 text-emerald-700"><CheckCircle2 /><h2 className="text-xl font-black">Active Operator</h2></div><p className="mt-4 text-2xl font-black text-slate-900">{association.operatorName}</p><p className="mt-1 text-sm text-slate-600">{association.operatorEmail} · {association.operatorPhone}</p></section>}
    {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 size={40} className="animate-spin" /></div> : invitations.length === 0 ? <div className="rounded-3xl border border-slate-200 bg-white p-14 text-center"><Mail size={46} className="mx-auto text-slate-300" /><h2 className="mt-4 text-xl font-black">No pending operator invitations.</h2></div> : <section className="grid gap-4 lg:grid-cols-2">{invitations.map((invitation) => <article key={invitation.associationId} className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"><div className="flex items-start gap-4"><div className="rounded-2xl bg-blue-50 p-3 text-[#08264a]"><Building2 /></div><div><h2 className="text-xl font-black text-slate-900">{invitation.operatorName}</h2><p className="mt-1 text-sm text-slate-500">{invitation.operatorEmail}</p><p className="mt-3 text-xs font-bold uppercase text-slate-400">Invited {new Date(invitation.invitedAt).toLocaleString()}</p></div></div><div className="mt-6 flex gap-3"><button type="button" disabled={processing} onClick={() => setConfirmation({ action: "accept", invitation })} className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-emerald-600 py-3 font-black text-white"><CheckCircle2 size={18} /> Accept</button><button type="button" disabled={processing} onClick={() => setConfirmation({ action: "reject", invitation })} className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-red-600 py-3 font-black text-white"><XCircle size={18} /> Reject</button></div></article>)}</section>}
  </div>
  {confirmation && <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4"><div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl"><h2 className="text-xl font-black">{confirmation.action === "accept" ? "Accept invitation?" : "Reject invitation?"}</h2><p className="mt-3 text-sm text-slate-600">{confirmation.action === "accept" ? `You will become actively associated with ${confirmation.invitation.operatorName}.` : `Reject the invitation from ${confirmation.invitation.operatorName}?`}</p><div className="mt-6 flex gap-3"><button type="button" disabled={processing} onClick={() => setConfirmation(null)} className="flex-1 rounded-xl border border-slate-300 py-3 font-black">Cancel</button><button type="button" disabled={processing} onClick={respond} className={`flex flex-1 items-center justify-center gap-2 rounded-xl py-3 font-black text-white ${confirmation.action === "accept" ? "bg-emerald-600" : "bg-red-600"}`}>{processing && <Loader2 size={17} className="animate-spin" />} Confirm</button></div></div></div>}
  </DriverLayout>;
}
