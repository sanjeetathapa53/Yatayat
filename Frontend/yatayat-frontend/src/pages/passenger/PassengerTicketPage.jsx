import { useEffect, useState } from "react";
import { ArrowLeft, CalendarClock, Check, Copy, Download, Loader2, Mail, MapPin, Ticket, Wallet } from "lucide-react";
import { QRCodeSVG } from "qrcode.react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "react-toastify";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { downloadTicketPdf, getTicketByNumber, handleTicketSession, sendTicketEmail } from "../../utils/passengerTickets";
import { formatBookingDate, formatNpr } from "../../utils/passengerBookings";

export default function PassengerTicketPage() {
  const { ticketNumber } = useParams();
  const navigate = useNavigate();
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
          setError(loadError.message || "Unable to load your ticket.");
          toast.error(loadError.message || "Unable to load your ticket.");
        }
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [navigate, ticketNumber]);

  const download = async () => {
    if (!ticket || downloading) return;
    setDownloading(true);
    try {
      await downloadTicketPdf(ticket.ticketNumber);
      toast.success("Ticket downloaded.");
    } catch {
      toast.error("Unable to download the ticket. Please try again.");
    } finally {
      setDownloading(false);
    }
  };

  const email = async () => {
    if (!ticket || emailing) return;
    setEmailing(true);
    try {
      await sendTicketEmail(ticket.ticketNumber);
      toast.success("E-ticket sent again to your registered email.");
    } catch {
      toast.error("Unable to send the e-ticket. Please try again.");
    } finally {
      setEmailing(false);
    }
  };

  const copyQrPayload = async () => {
    if (!ticket?.qrPayload) return;
    try {
      await navigator.clipboard.writeText(ticket.qrPayload);
      setCopiedPayload(true);
      toast.success("QR payload copied for development testing.");
      window.setTimeout(() => setCopiedPayload(false), 1800);
    } catch {
      toast.error("Unable to copy QR payload. Please copy it manually from DevTools.");
    }
  };

  return <PassengerLayout activePage="My Bookings"><div className="mx-auto max-w-5xl space-y-6">
    <button onClick={() => navigate(-1)} className="flex items-center gap-2 text-sm font-black text-[#08264a]"><ArrowLeft size={17} /> Back</button>
    {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin text-[#08264a]" size={42} /></div> : error ? <div className="rounded-3xl border border-red-200 bg-red-50 p-8 text-center"><h1 className="text-2xl font-black text-red-800">Unable to load your ticket.</h1><p className="mt-2 font-semibold text-red-600">{error}</p><button onClick={() => navigate("/passenger/bookings")} className="mt-5 rounded-xl bg-[#08264a] px-6 py-3 font-black text-white">My Bookings</button></div> : ticket && <>
      <section className="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-2xl shadow-blue-950/10">
        <div className="bg-[#08264a] p-7 text-white">
          <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
            <div>
              <p className="text-sm font-black uppercase tracking-[0.25em] text-blue-200">Yatayat</p>
              <h1 className="mt-2 text-3xl font-black">Electronic Bus Ticket</h1>
              <p className="mt-2 text-blue-100">Present this QR code to the driver before boarding.</p>
            </div>
            <span className="rounded-full bg-emerald-400/20 px-4 py-2 text-xs font-black text-emerald-100">{ticket.ticketStatus}</span>
          </div>
        </div>
        <div className="grid gap-8 p-6 lg:grid-cols-[0.95fr_1.25fr]">
          <div className="rounded-[1.75rem] border border-slate-100 bg-slate-50 p-6 text-center">
            <div className="mx-auto inline-block rounded-3xl bg-white p-4 shadow-xl shadow-slate-950/10">
              <QRCodeSVG value={ticket.qrPayload || ""} size={230} level="M" includeMargin />
            </div>
            <p className="mt-5 text-xs font-black uppercase tracking-wide text-slate-500">Ticket Number</p>
            <p className="mt-1 break-all text-xl font-black text-[#08264a]">{ticket.ticketNumber}</p>
            <p className="mt-3 text-xs font-bold text-slate-500">Booking {ticket.bookingReference}</p>
          </div>
          <div className="space-y-5">
            <div>
              <p className="text-sm font-black uppercase tracking-wide text-slate-500">Route</p>
              <h2 className="mt-1 text-3xl font-black text-slate-950">{ticket.origin} → {ticket.destination}</h2>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <TicketInfo icon={<Ticket size={18} />} label="Passenger" value={ticket.passengerName} />
              <TicketInfo icon={<CalendarClock size={18} />} label="Departure" value={formatBookingDate(ticket.departureAt)} />
              <TicketInfo label="Operator" value={ticket.operatorName} />
              <TicketInfo label="Bus" value={`${ticket.busName || "Bus"} (${ticket.busNumber})`} />
              <TicketInfo label="Seats" value={ticket.seatNumbers?.join(", ")} />
              <TicketInfo icon={<Wallet size={18} />} label="Fare Paid" value={formatNpr(ticket.totalFare)} />
              <TicketInfo icon={<MapPin size={18} />} label="Boarding Point" value={ticket.boardingPoint} />
              <TicketInfo icon={<MapPin size={18} />} label="Drop-off Point" value={ticket.dropOffPoint} />
              <TicketInfo label="Payment Method" value={ticket.paymentMethod || "Wallet"} />
              <TicketInfo label="Issued At" value={formatBookingDate(ticket.issuedAt)} />
            </div>
          </div>
        </div>
      </section>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <ActionButton icon={<Download size={18} />} disabled={downloading || emailing} onClick={download}>{downloading ? "Downloading..." : "Download PDF"}</ActionButton>
        <ActionButton icon={<Mail size={18} />} disabled={downloading || emailing} onClick={email}>{emailing ? "Sending..." : "Resend E-ticket"}</ActionButton>
        {import.meta.env.DEV && <ActionButton icon={copiedPayload ? <Check size={18} /> : <Copy size={18} />} disabled={!ticket.qrPayload} onClick={copyQrPayload}>{copiedPayload ? "QR Payload Copied" : "Copy QR Payload (Development Only)"}</ActionButton>}
        <ActionButton onClick={() => navigate(`/passenger/bookings/${ticket.bookingReference}`)}>Back to Booking</ActionButton>
        <ActionButton onClick={() => navigate("/passenger/bookings")}>My Bookings</ActionButton>
      </div>
    </>}
  </div></PassengerLayout>;
}

function TicketInfo({ icon, label, value }) {
  return <div className="rounded-2xl bg-slate-50 p-4">
    <p className="flex items-center gap-2 text-xs font-black uppercase tracking-wide text-slate-500">{icon}{label}</p>
    <p className="mt-2 break-words font-black text-slate-900">{value || "Not available"}</p>
  </div>;
}

function ActionButton({ icon, children, ...props }) {
  return <button {...props} className="flex items-center justify-center gap-2 rounded-2xl bg-[#08264a] px-5 py-3 font-black text-white shadow-lg shadow-blue-950/10 transition hover:bg-blue-950 disabled:cursor-not-allowed disabled:opacity-60">{icon}{children}</button>;
}
