import {
    Bus,
    Calendar,
    Clock,
    CreditCard,
    MapPin,
    Ticket,
    User,
  } from "lucide-react";
  import { QRCodeCanvas } from "qrcode.react";
  
  export default function TicketPreview({ booking, passenger }) {
    const [from, to] = booking.routeName?.includes(" to ")
      ? booking.routeName.split(" to ")
      : booking.routeName?.includes("→")
      ? booking.routeName.split("→")
      : ["Kathmandu", "Pokhara"];
  
    return (
      <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-xl">
        <div className="bg-[#08264a] p-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-3xl font-black">YATAYAT</h2>
              <p className="text-sm text-slate-300">
                Smart Public Transport Nepal
              </p>
            </div>
  
            <div className="rounded-full bg-emerald-100 px-4 py-2 text-xs font-black text-emerald-700">
              {booking.bookingStatus || "CONFIRMED"}
            </div>
          </div>
        </div>
  
        <div className="grid grid-cols-1 gap-6 p-6 xl:grid-cols-12">
          <div className="space-y-5 xl:col-span-8">
            <Info icon={<User size={18} />} label="Passenger" value={passenger?.name || "Passenger"} />
  
            <div className="rounded-2xl bg-slate-50 p-5">
              <p className="text-xs font-black uppercase tracking-widest text-slate-500">
                Route
              </p>
  
              <div className="mt-4 grid grid-cols-3 items-center gap-4">
                <div>
                  <p className="text-sm text-slate-500">From</p>
                  <h3 className="text-2xl font-black text-slate-900">
                    {from.trim()}
                  </h3>
                </div>
  
                <div className="text-center">
                  <Bus className="mx-auto text-[#08264a]" size={26} />
                  <div className="mt-2 h-px bg-slate-300"></div>
                  <p className="mt-2 text-xs font-black text-[#08264a]">
                    Journey
                  </p>
                </div>
  
                <div className="text-right">
                  <p className="text-sm text-slate-500">To</p>
                  <h3 className="text-2xl font-black text-slate-900">
                    {to.trim()}
                  </h3>
                </div>
              </div>
            </div>
  
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Info icon={<Ticket size={18} />} label="Booking ID" value={`YT-${booking.id}`} />
              <Info icon={<Bus size={18} />} label="Bus Number" value={booking.busNumber} />
              <Info icon={<Calendar size={18} />} label="Travel Date" value={booking.travelDate} />
              <Info icon={<Clock size={18} />} label="Departure" value={booking.departureTime} />
              <Info icon={<Ticket size={18} />} label="Seat Number" value={booking.seatNumber} />
              <Info icon={<CreditCard size={18} />} label="Amount Paid" value={`NPR ${booking.fare}`} />
            </div>
          </div>
  
          <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 p-5 text-center xl:col-span-4">
            <QRCodeCanvas value={booking.qrCode} size={190} />
  
            <p className="mt-4 text-xs font-black uppercase tracking-widest text-slate-500">
              Scan while boarding
            </p>
  
            <p className="mt-2 break-all text-[11px] text-slate-400">
              {booking.qrCode}
            </p>
          </div>
        </div>
  
        <div className="border-t border-slate-200 bg-slate-50 px-6 py-4 text-center text-xs font-semibold text-slate-500">
          Please arrive at least 15 minutes before departure.
        </div>
      </div>
    );
  }
  
  function Info({ icon, label, value }) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white p-4">
        <div className="flex items-center gap-2 text-slate-500">
          {icon}
          <p className="text-[11px] font-black uppercase tracking-widest">
            {label}
          </p>
        </div>
  
        <p className="mt-2 font-black text-slate-900">{value || "N/A"}</p>
      </div>
    );
  }