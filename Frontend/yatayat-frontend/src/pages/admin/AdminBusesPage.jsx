import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, Eye, Loader2, RefreshCw, Search, X, XCircle } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import AdminLayout from "../../components/layout/AdminLayout";
import { apiFetch } from "../../utils/api";

const filters = ["ALL", "PENDING", "APPROVED", "REJECTED", "SUSPENDED"];

export default function AdminBusesPage() {
  const navigate = useNavigate();
  const [buses, setBuses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ALL");
  const [selected, setSelected] = useState(null);
  const [action, setAction] = useState(null);
  const [reason, setReason] = useState("");
  const [saving, setSaving] = useState(false);

  const loadBuses = async () => {
    try {
      setLoading(true);
      setError("");
      const response = await apiFetch(`/api/admin/buses?status=${status}`);
      if (response.status === 401 || response.status === 403) {
        localStorage.removeItem("adminAuthenticated");
        navigate("/admin/login", { replace: true });
        return;
      }
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Unable to load buses.");
      setBuses(Array.isArray(data) ? data : []);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;
    apiFetch(`/api/admin/buses?status=${status}`).then(async (response) => {
      if (response.status === 401 || response.status === 403) {
        localStorage.removeItem("adminAuthenticated");
        navigate("/admin/login", { replace: true });
        return null;
      }
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || "Unable to load buses.");
      return data;
    }).then((data) => {
      if (active && data) setBuses(Array.isArray(data) ? data : []);
    }).catch((loadError) => {
      if (active) setError(loadError.message);
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, [navigate, status]);

  const visibleBuses = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return buses;
    return buses.filter((bus) => [bus.busNumber, bus.busName, bus.busType, bus.operatorName, bus.operatorEmail]
      .some((value) => String(value || "").toLowerCase().includes(query)));
  }, [buses, search]);

  const review = async () => {
    if (!selected || !action || (action === "reject" && !reason.trim())) return;
    try {
      setSaving(true);
      const response = await apiFetch(`/api/admin/buses/${selected.id}/${action}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: action === "reject" ? JSON.stringify({ reason: reason.trim() }) : undefined,
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(data.message || `Unable to ${action} bus.`);
      setBuses((current) => {
        if (status !== "ALL" && data.status !== status) {
          return current.filter((bus) => bus.id !== data.id);
        }
        return current.map((bus) => bus.id === data.id ? data : bus);
      });
      setSelected(data);
      setAction(null);
      setReason("");
      toast.success(action === "approve" ? "Bus approved." : "Bus rejected.");
    } catch (reviewError) {
      setError(reviewError.message);
    } finally {
      setSaving(false);
    }
  };

  return <AdminLayout title="Bus Approvals" subtitle="Review vehicle registrations submitted by transport operators.">
    <div className="space-y-5">
      <div className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm lg:flex-row lg:items-center">
        <div className="flex flex-1 items-center gap-3 rounded-xl border border-slate-300 px-4 py-2.5"><Search size={18} className="text-slate-400" /><input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search bus or operator" className="w-full outline-none" /></div>
        <select value={status} onChange={(event) => { setLoading(true); setStatus(event.target.value); }} className="rounded-xl border border-slate-300 bg-white px-4 py-2.5 font-bold">{filters.map((item) => <option key={item} value={item}>{item === "ALL" ? "All statuses" : item}</option>)}</select>
        <button type="button" onClick={loadBuses} className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-2.5 font-semibold text-white"><RefreshCw size={17} /> Refresh</button>
      </div>

      {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}
      {loading ? <div className="flex min-h-40 items-center justify-center"><Loader2 size={32} className="animate-spin" /></div> : visibleBuses.length === 0 ? <div className="rounded-2xl border border-slate-200 bg-white p-6 text-center font-semibold text-slate-500">No buses match this view.</div> :
        <><div className="grid gap-4 lg:hidden">{visibleBuses.map((bus) => <BusMobileCard key={bus.id} bus={bus} onView={() => setSelected(bus)} onApprove={() => { setSelected(bus); setAction("approve"); }} onReject={() => { setSelected(bus); setAction("reject"); setReason(""); }} />)}</div><div className="hidden overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm lg:block"><div className="overflow-x-auto"><table className="min-w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr>{["Bus", "Type / Seats", "Operator", "Submitted", "Status", "Actions"].map((heading) => <th key={heading} className="px-4 py-2.5 font-semibold">{heading}</th>)}</tr></thead><tbody className="divide-y divide-slate-100">{visibleBuses.map((bus) => <tr key={bus.id}><td className="px-4 py-2.5"><p className="font-semibold text-slate-900">{bus.busName}</p><p className="text-slate-500">{bus.busNumber}</p></td><td className="px-4 py-2.5">{bus.busType}<br /><span className="text-slate-500">{bus.seatCapacity} seats</span></td><td className="px-4 py-2.5"><p className="font-bold">{bus.operatorName}</p><p className="text-slate-500">{bus.operatorEmail}</p></td><td className="px-4 py-2.5 text-slate-500">{formatDate(bus.createdAt)}</td><td className="px-4 py-2.5"><StatusBadge status={bus.status} /></td><td className="px-4 py-2.5"><div className="flex gap-2"><button type="button" onClick={() => setSelected(bus)} className="rounded-lg border border-slate-300 p-2" title="View" aria-label={`View ${bus.busNumber}`}><Eye size={17} /></button>{bus.status === "PENDING" && <><button type="button" onClick={() => { setSelected(bus); setAction("approve"); }} className="rounded-lg bg-emerald-100 p-2 text-emerald-700" title="Approve" aria-label={`Approve ${bus.busNumber}`}><CheckCircle2 size={17} /></button><button type="button" onClick={() => { setSelected(bus); setAction("reject"); setReason(""); }} className="rounded-lg bg-red-100 p-2 text-red-700" title="Reject" aria-label={`Reject ${bus.busNumber}`}><XCircle size={17} /></button></>}</div></td></tr>)}</tbody></table></div></div></>}
    </div>

    {selected && !action && <Details bus={selected} onClose={() => setSelected(null)} onAction={setAction} />}
    {selected && action && <ReviewModal bus={selected} action={action} reason={reason} setReason={setReason} saving={saving} onCancel={() => { setAction(null); setReason(""); }} onConfirm={review} />}
  </AdminLayout>;
}

function Details({ bus, onClose, onAction }) {
  const rows = [["Bus number", bus.busNumber], ["Bus type", bus.busType], ["Seat capacity", bus.seatCapacity], ["Model", bus.model], ["Manufacture year", bus.manufactureYear], ["Fuel type", bus.fuelType], ["Permit number", bus.permitNumber], ["Permit expiry", bus.permitExpiryDate], ["Insurance expiry", bus.insuranceExpiryDate], ["Operator", bus.operatorName], ["Operator email", bus.operatorEmail], ["Operator phone", bus.operatorPhone], ["Operator registration", bus.operatorRegistrationNumber], ["Submitted", formatDate(bus.createdAt)], ["Approved", formatDate(bus.approvedAt)]];
  return <div className="responsive-modal-backdrop fixed inset-0 z-50 flex justify-center bg-black/50 sm:items-center"><div className="responsive-modal-panel w-full max-w-3xl rounded-2xl bg-white p-5 shadow-2xl sm:p-6"><div className="flex items-start justify-between gap-4"><div className="min-w-0"><h2 className="safe-wrap text-xl font-semibold sm:text-2xl">{bus.busName}</h2><div className="mt-2"><StatusBadge status={bus.status} /></div></div><button type="button" aria-label="Close bus details" onClick={onClose} className="rounded-lg p-2 hover:bg-slate-100"><X /></button></div>{bus.rejectionReason && <div className="mt-5 rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">Rejection reason: {bus.rejectionReason}</div>}<div className="mt-6 grid gap-3 sm:grid-cols-2">{rows.map(([label, value]) => <div key={label} className="rounded-xl bg-slate-50 p-4"><p className="text-xs font-semibold uppercase text-slate-500">{label}</p><p className="mt-2 break-words font-bold">{value || "Not provided"}</p></div>)}</div>{bus.status === "PENDING" && <div className="mobile-action-stack mt-6 flex gap-3"><button type="button" onClick={() => onAction("approve")} className="flex-1 rounded-xl bg-emerald-600 py-2.5 font-semibold text-white">Approve</button><button type="button" onClick={() => onAction("reject")} className="flex-1 rounded-xl bg-red-600 py-2.5 font-semibold text-white">Reject</button></div>}</div></div>;
}

function ReviewModal({ bus, action, reason, setReason, saving, onCancel, onConfirm }) {
  const rejecting = action === "reject";
  return <div className="responsive-modal-backdrop fixed inset-0 z-[60] flex justify-center bg-black/50 sm:items-center"><div className="responsive-modal-panel w-full max-w-md rounded-2xl bg-white p-5 shadow-2xl sm:p-6"><h2 className="text-xl font-semibold">{rejecting ? "Reject bus" : "Approve bus"}</h2><p className="safe-wrap mt-2 text-sm text-slate-600">{rejecting ? `Provide a reason for rejecting ${bus.busNumber}.` : `Confirm approval of ${bus.busNumber}.`}</p>{rejecting && <><textarea value={reason} onChange={(event) => setReason(event.target.value)} maxLength={1000} rows={4} placeholder="Rejection reason" className="mt-5 w-full rounded-xl border border-slate-300 p-3 outline-none focus:border-[#08264a]" /><p className="mt-1 text-right text-xs text-slate-400">{reason.length}/1000</p></>}<div className="mobile-action-stack mt-6 flex gap-3"><button type="button" disabled={saving} onClick={onCancel} className="flex-1 rounded-xl border border-slate-300 py-2.5 font-semibold">Cancel</button><button type="button" disabled={saving || (rejecting && !reason.trim())} onClick={onConfirm} className={`flex flex-1 items-center justify-center gap-2 rounded-xl py-2.5 font-semibold text-white disabled:opacity-50 ${rejecting ? "bg-red-600" : "bg-emerald-600"}`}>{saving && <Loader2 size={17} className="animate-spin" />}{rejecting ? "Reject" : "Approve"}</button></div></div></div>;
}

function BusMobileCard({ bus, onView, onApprove, onReject }) {
  return <article className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><h2 className="safe-wrap font-semibold text-slate-900">{bus.busName}</h2><p className="safe-wrap mt-1 text-sm font-bold text-slate-500">{bus.busNumber}</p></div><StatusBadge status={bus.status} /></div><div className="mt-4 grid gap-3 rounded-2xl bg-slate-50 p-4 text-sm"><InfoLine label="Type / seats" value={`${bus.busType || "Bus"} · ${bus.seatCapacity || 0} seats`} /><InfoLine label="Operator" value={bus.operatorName} /><InfoLine label="Email" value={bus.operatorEmail} /><InfoLine label="Submitted" value={formatDate(bus.createdAt)} /></div><div className="mt-4 grid gap-2 min-[360px]:grid-cols-3"><button type="button" onClick={onView} className="tap-target rounded-xl border border-slate-300 px-3 py-2 font-semibold text-slate-700">View</button>{bus.status === "PENDING" && <><button type="button" onClick={onApprove} className="tap-target rounded-xl bg-emerald-600 px-3 py-2 font-semibold text-white">Approve</button><button type="button" onClick={onReject} className="tap-target rounded-xl bg-red-600 px-3 py-2 font-semibold text-white">Reject</button></>}</div></article>;
}

function InfoLine({ label, value }) {
  return <div><p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className="safe-wrap mt-1 font-bold text-slate-800">{value || "Not available"}</p></div>;
}

function StatusBadge({ status }) {
  const tones = { PENDING: "bg-amber-100 text-amber-700", APPROVED: "bg-emerald-100 text-emerald-700", REJECTED: "bg-red-100 text-red-700", SUSPENDED: "bg-slate-200 text-slate-700" };
  return <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${tones[status] || "bg-blue-100 text-[#08264a]"}`}>{status}</span>;
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString() : "Not available";
}
