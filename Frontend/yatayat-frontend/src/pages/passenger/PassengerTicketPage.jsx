import { useEffect, useState } from "react";
import { ArrowLeft, CalendarClock, Check, Copy, Download, Loader2, Mail, MapPin, Ticket, Wallet } from "lucide-react";
import { QRCodeSVG } from "qrcode.react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { useLanguage } from "../../hooks/useLanguage";
import { downloadTicketPdf, getTicketByNumber, handleTicketSession, sendTicketEmail } from "../../utils/passengerTickets";
import { formatBookingDate, formatNpr } from "../../utils/passengerBookings";

export default function PassengerTicketPage() {
  const { ticketNumber } = useParams();
  const navigate = useNavigate();
  const { t } = useLanguage();
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [downloading, setDownloading] = useState(false);
  const [emailing, setEmailing] = useState(false);
  const [copiedPayload, setCopiedPayload] = useState(false);

  useEffect(() => {
    let active = true;
    getTicketByNumber(ticketNumber)
      .then((data) => { if (active) setTicket(data); })
      .catch((loadError) => {
        if (active && !handleTicketSession(loadError, navigate)) {
          setError(loadError.message || t("passenger.tickets.unableToLoadTicket"));
          toast.error(loadError.message || t("passenger.tickets.unableToLoadTicket"));
        }
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [navigate, t, ticketNumber]);

  const download = async () => {
    if (!ticket || downloading) return;
    setDownloading(true);
    try {
      await downloadTicketPdf(ticket.ticketNumber);
      toast.success(t("passenger.tickets.downloaded"));
    } catch {
      toast.error(t("passenger.tickets.downloadFailed"));
    } finally {
      setDownloading(false);
    }
  };

  const email = async () => {
    if (!ticket || emailing) return;
    setEmailing(true);
    try {
      await sendTicketEmail(ticket.ticketNumber);
      toast.success(t("passenger.tickets.emailSent"));
    } catch {
      toast.error(t("passenger.tickets.emailFailed"));
    } finally {
      setEmailing(false);
    }
  };

  const copyQrPayload = async () => {
    if (!ticket?.qrPayload) return;
    try {
      await navigator.clipboard.writeText(ticket.qrPayload);
      setCopiedPayload(true);
      toast.success(t("passenger.tickets.qrCopied"));
      window.setTimeout(() => setCopiedPayload(false), 1800);
    } catch {
      toast.error(t("passenger.tickets.qrCopyFailed"));
    }
  };

  return <PassengerLayout activePage="My Bookings" title={t("passenger.tickets.electronicBusTicket")} subtitle={t("passenger.tickets.presentQr")}><div className="mx-auto max-w-5xl space-y-6">
    <button type="button" onClick={() => navigate(-1)} className="flex items-center gap-2 text-sm font-black text-[#08264a]"><ArrowLeft size={17} /> {t("passenger.tickets.back")}</button>
    {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin text-[#08264a]" size={42} /></div> : error ? <div className="rounded-3xl border border-red-200 bg-red-50 p-8 text-center"><h2 className="text-2xl font-black text-red-800">{t("passenger.tickets.unableToLoadTicket")}</h2><p className="mt-2 font-semibold text-red-600">{error}</p><button type="button" onClick={() => navigate("/passenger/bookings")} className="mt-5 rounded-xl bg-[#08264a] px-6 py-3 font-black text-white">{t("passenger.tickets.myBookings")}</button></div> : ticket && <>
      <section className="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-2xl shadow-blue-950/10">
        <div className="bg-[#08264a] p-5 text-white sm:p-7">
          <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
            <div>
              <p className="text-sm font-black uppercase tracking-[0.25em] text-blue-200">Yatayat</p>
            </div>
            <span className="rounded-full bg-emerald-400/20 px-4 py-2 text-xs font-black text-emerald-100">{ticket.ticketStatus}</span>
          </div>
        </div>
        <div className="grid gap-6 p-4 sm:p-6 lg:grid-cols-[minmax(0,0.95fr)_minmax(0,1.25fr)] lg:gap-8">
          <div className="rounded-[1.75rem] border border-slate-100 bg-slate-50 p-4 text-center sm:p-6">
            <div className="qr-responsive mx-auto inline-block rounded-3xl bg-white p-3 shadow-xl shadow-slate-950/10 sm:p-4">
              <QRCodeSVG value={ticket.qrPayload || ""} size={230} level="M" includeMargin />
            </div>
            <p className="mt-5 text-xs font-black uppercase tracking-wide text-slate-500">{t("passenger.tickets.ticketNumber")}</p>
            <p className="mt-1 break-all text-xl font-black text-[#08264a]">{ticket.ticketNumber}</p>
            <p className="mt-3 text-xs font-bold text-slate-500">{t("passenger.tickets.bookingReferenceShort", { reference: ticket.bookingReference })}</p>
          </div>
          <div className="space-y-5">
            <div>
              <p className="text-sm font-black uppercase tracking-wide text-slate-500">{t("passenger.tickets.route")}</p>
              <h2 className="mt-1 text-3xl font-black text-slate-950">{ticket.origin} → {ticket.destination}</h2>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <TicketInfo icon={<Ticket size={18} />} label={t("passenger.tickets.passenger")} value={ticket.passengerName} t={t} />
              <TicketInfo icon={<CalendarClock size={18} />} label={t("passenger.tickets.departure")} value={formatBookingDate(ticket.departureAt)} t={t} />
              <TicketInfo label={t("passenger.tickets.operator")} value={ticket.operatorName} t={t} />
              <TicketInfo label={t("passenger.tickets.bus")} value={`${ticket.busName || t("passenger.tickets.genericBus")} (${ticket.busNumber})`} t={t} />
              <TicketInfo label={t("passenger.tickets.seats")} value={ticket.seatNumbers?.join(", ")} t={t} />
              <TicketInfo icon={<Wallet size={18} />} label={t("passenger.tickets.farePaid")} value={formatNpr(ticket.totalFare)} t={t} />
              <TicketInfo icon={<MapPin size={18} />} label={t("passenger.tickets.boardingPoint")} value={ticket.boardingPoint} t={t} />
              <TicketInfo icon={<MapPin size={18} />} label={t("passenger.tickets.dropOffPoint")} value={ticket.dropOffPoint} t={t} />
              <TicketInfo label={t("passenger.tickets.paymentMethod")} value={ticket.paymentMethod || t("passenger.tickets.wallet")} t={t} />
              <TicketInfo label={t("passenger.tickets.issuedAt")} value={formatBookingDate(ticket.issuedAt)} t={t} />
            </div>
          </div>
        </div>
      </section>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <ActionButton icon={<Download size={18} />} disabled={downloading || emailing} onClick={download}>{downloading ? t("passenger.tickets.downloading") : t("passenger.tickets.downloadPdf")}</ActionButton>
        <ActionButton icon={<Mail size={18} />} disabled={downloading || emailing} onClick={email}>{emailing ? t("passenger.tickets.sending") : t("passenger.tickets.resendEticket")}</ActionButton>
        {import.meta.env.DEV && <ActionButton icon={copiedPayload ? <Check size={18} /> : <Copy size={18} />} disabled={!ticket.qrPayload} onClick={copyQrPayload}>{copiedPayload ? t("passenger.tickets.qrPayloadCopied") : t("passenger.tickets.copyQrPayloadDev")}</ActionButton>}
        <ActionButton onClick={() => navigate(`/passenger/bookings/${ticket.bookingReference}`)}>{t("passenger.tickets.backToBooking")}</ActionButton>
        <ActionButton onClick={() => navigate("/passenger/bookings")}>{t("passenger.tickets.myBookings")}</ActionButton>
      </div>
    </>}
  </div></PassengerLayout>;
}

function TicketInfo({ icon, label, value, t }) {
  return <div className="rounded-2xl bg-slate-50 p-4">
    <p className="flex items-center gap-2 text-xs font-black uppercase tracking-wide text-slate-500">{icon}{label}</p>
    <p className="mt-2 break-words font-black text-slate-900">{value || t("passenger.tickets.notAvailable")}</p>
  </div>;
}

function ActionButton({ icon, children, ...props }) {
  return <button type="button" {...props} className="tap-target flex items-center justify-center gap-2 rounded-2xl bg-[#08264a] px-5 py-3 text-center font-black text-white shadow-lg shadow-blue-950/10 transition hover:bg-blue-950 disabled:cursor-not-allowed disabled:opacity-60">{icon}{children}</button>;
}
