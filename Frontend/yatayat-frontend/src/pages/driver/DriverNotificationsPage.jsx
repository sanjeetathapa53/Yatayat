import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Bell, Building2, Bus, CheckCircle2, Loader2, Mail,
  MapPin, RefreshCw, Route, XCircle,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import DriverLayout from "../../components/layout/DriverLayout";
import ConfirmationModal from "../../components/common/ConfirmationModal";
import { apiFetch } from "../../utils/api";
import {
  fetchDriverNotifications,
  markAllDriverNotificationsRead,
  markDriverNotificationRead,
  notifyDriverNotificationsChanged,
} from "../../utils/driverNotifications";

const tabs = ["ALL", "UNREAD", "INVITATIONS"];

export default function DriverNotificationsPage() {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState([]);
  const [invitations, setInvitations] = useState([]);
  const [association, setAssociation] = useState(null);
  const [activeTab, setActiveTab] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [confirmation, setConfirmation] = useState(null);
  const [processing, setProcessing] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [notificationData, invitationResponse, associationResponse] = await Promise.all([
        fetchDriverNotifications(false),
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
      setNotifications(notificationData);
      setInvitations(Array.isArray(invitationData) ? invitationData : []);
      setAssociation(associationResponse.status === 204 ? null : await associationResponse.json());
    } catch (loadError) {
      setError(loadError.message || "Unable to load notifications.");
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    Promise.resolve().then(load);
  }, [load]);

  const visibleNotifications = useMemo(
    () => activeTab === "UNREAD" ? notifications.filter((item) => !item.read) : notifications,
    [activeTab, notifications]
  );
  const unreadCount = notifications.filter((item) => !item.read).length;

  const openNotification = async (notification) => {
    try {
      if (!notification.read) {
        await markDriverNotificationRead(notification.id);
        setNotifications((current) => current.map((item) =>
          item.id === notification.id ? { ...item, read: true } : item));
      }
      const destination = notification.relatedEntityType === "LOCAL_SERVICE_RUN"
        ? "/driver/local-services"
        : notification.relatedEntityType === "SCHEDULED_TRIP"
          ? "/driver/trip"
          : null;
      if (destination) navigate(destination);
    } catch (notificationError) {
      setError(notificationError.message);
    }
  };

  const markAllRead = async () => {
    try {
      setProcessing(true);
      await markAllDriverNotificationsRead();
      setNotifications((current) => current.map((item) => ({ ...item, read: true })));
    } catch (markError) {
      setError(markError.message);
    } finally {
      setProcessing(false);
    }
  };

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
      notifyDriverNotificationsChanged();
      await load();
    } catch (responseError) {
      setError(responseError.message);
    } finally {
      setProcessing(false);
    }
  };

  return (
    <DriverLayout activePage="Notifications">
      <div className="space-y-6">
        <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div><h1 className="text-3xl font-black text-slate-900">Notifications</h1><p className="mt-1 text-sm text-slate-600">Assignment updates and operator invitations.</p></div>
          <div className="flex flex-wrap gap-2">
            {unreadCount > 0 && <button type="button" disabled={processing} onClick={markAllRead} className="rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-black text-slate-700">Mark all read</button>}
            <button type="button" onClick={load} className="flex items-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 font-black text-white"><RefreshCw size={17} /> Refresh</button>
          </div>
        </header>

        <div className="flex gap-2 overflow-x-auto rounded-2xl border border-slate-200 bg-white p-2">
          {tabs.map((tab) => <button key={tab} type="button" onClick={() => setActiveTab(tab)} className={`rounded-xl px-4 py-2 text-sm font-black ${activeTab === tab ? "bg-[#08264a] text-white" : "text-slate-600 hover:bg-slate-100"}`}>{tab === "ALL" ? "All" : tab === "UNREAD" ? `Unread (${unreadCount})` : `Invitations (${invitations.length})`}</button>)}
        </div>

        {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-4 font-bold text-red-700">{error}</div>}
        {association && activeTab === "INVITATIONS" && <section className="rounded-3xl border border-emerald-200 bg-emerald-50 p-6"><div className="flex items-center gap-3 text-emerald-700"><CheckCircle2 /><h2 className="text-xl font-black">Active Operator</h2></div><p className="mt-4 text-2xl font-black text-slate-900">{association.operatorName}</p></section>}

        {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 size={40} className="animate-spin" /></div> : activeTab === "INVITATIONS" ? (
          invitations.length === 0 ? <Empty icon={<Mail size={46} />} title="No pending operator invitations." /> : <section className="grid gap-4 lg:grid-cols-2">{invitations.map((invitation) => <InvitationCard key={invitation.associationId} invitation={invitation} processing={processing} onRespond={setConfirmation} />)}</section>
        ) : visibleNotifications.length === 0 ? (
          <Empty icon={<Bell size={46} />} title={activeTab === "UNREAD" ? "You are all caught up." : "No driver notifications yet."} />
        ) : (
          <section className="space-y-3">{visibleNotifications.map((notification) => <NotificationCard key={notification.id} notification={notification} onOpen={openNotification} />)}</section>
        )}
      </div>
      <ConfirmationModal open={Boolean(confirmation)} title={confirmation?.action === "accept" ? "Accept invitation?" : "Reject invitation?"} message={confirmation?.action === "accept" ? `You will become actively associated with ${confirmation?.invitation.operatorName}.` : `Reject the invitation from ${confirmation?.invitation.operatorName}?`} confirmLabel={confirmation?.action === "accept" ? "Accept" : "Reject"} destructive={confirmation?.action === "reject"} busy={processing} busyLabel="Saving..." onConfirm={respond} onClose={() => setConfirmation(null)} />
    </DriverLayout>
  );
}

function NotificationCard({ notification, onOpen }) {
  const Icon = notification.relatedEntityType === "LOCAL_SERVICE_RUN" ? Bus : notification.relatedEntityType === "SCHEDULED_TRIP" ? Route : Building2;
  return <button type="button" onClick={() => onOpen(notification)} className={`flex w-full items-start gap-4 rounded-2xl border p-5 text-left shadow-sm transition hover:border-blue-300 ${notification.read ? "border-slate-200 bg-white" : "border-blue-200 bg-blue-50/70"}`}><span className="rounded-xl bg-white p-3 text-[#08264a] shadow-sm"><Icon size={21} /></span><span className="min-w-0 flex-1"><span className="flex items-start justify-between gap-3"><strong className="text-slate-900">{notification.title}</strong>{!notification.read && <span className="mt-1 h-2.5 w-2.5 shrink-0 rounded-full bg-blue-600" />}</span><span className="mt-1 block text-sm text-slate-600">{notification.message}</span><span className="mt-2 block text-xs font-bold text-slate-400">{new Date(notification.createdAt).toLocaleString()}</span></span><MapPin size={18} className="mt-1 shrink-0 text-slate-400" /></button>;
}

function InvitationCard({ invitation, processing, onRespond }) {
  return <article className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"><div className="flex items-start gap-4"><div className="rounded-2xl bg-blue-50 p-3 text-[#08264a]"><Building2 /></div><div><h2 className="text-xl font-black text-slate-900">{invitation.operatorName}</h2><p className="mt-3 text-xs font-bold uppercase text-slate-400">Invited {new Date(invitation.invitedAt).toLocaleString()}</p></div></div><div className="mt-6 flex gap-3"><button type="button" disabled={processing} onClick={() => onRespond({ action: "accept", invitation })} className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-emerald-600 py-3 font-black text-white"><CheckCircle2 size={18} /> Accept</button><button type="button" disabled={processing} onClick={() => onRespond({ action: "reject", invitation })} className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-red-600 py-3 font-black text-white"><XCircle size={18} /> Reject</button></div></article>;
}

function Empty({ icon, title }) {
  return <div className="rounded-3xl border border-slate-200 bg-white p-14 text-center text-slate-400">{icon}<h2 className="mt-4 text-xl font-black text-slate-800">{title}</h2></div>;
}
