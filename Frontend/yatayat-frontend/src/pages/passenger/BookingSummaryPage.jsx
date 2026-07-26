import { useState } from "react";
import {
  User,
  Mail,
  Phone,
  Calendar,
  Clock,
  CreditCard,
  ArrowLeft,
  Gift,
  Edit3,
  Bus,
  Ticket,
} from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";

const defaultBus = {
  name: "Sajha Yatayat",
  route: "Kathmandu → Pokhara",
  depart: "06:30 AM",
  duration: "7h 30m",
  type: "Super Deluxe AC",
  price: "1,850",
};

export default function BookingSummaryPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const user = JSON.parse(localStorage.getItem("yatayatUser"));
  const bookingData = location.state?.bookingData;
  const bus = location.state?.bus || defaultBus;

  const selectedSeat = bookingData?.seatNumber || "B2";
  const totalAmount = bookingData?.fare || 0;

  const [passenger, setPassenger] = useState({
    name: user?.fullName || "",
    phone: user?.phone || "",
    email: user?.email || "",
  });

  const updatePassenger = (field, value) => {
    setPassenger((prev) => ({ ...prev, [field]: value }));
  };

  const handlePayment = () => {
    navigate("/payment", {
      state: {
        bookingData,
        passenger,
        bus,
      },
    });
  };

  return (
    <PassengerLayout activePage="Out-of-Valley" title="Booking Summary" subtitle="Review your passenger, trip, seat, and fare details before payment.">
      <button
        onClick={() => navigate("/seat-selection", { state: { bus } })}
        className="mb-5 flex items-center gap-2 text-sm font-black text-slate-600 hover:text-[#08264a]"
      >
        <ArrowLeft size={17} />
        Back to seat selection
      </button>

      <header className="mb-6 flex items-center justify-between">
        <div>
          <p className="text-xs font-black uppercase tracking-widest text-slate-500">
            Search › Selection › Summary
          </p>
        </div>
      </header>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-12">
        <section className="space-y-5 xl:col-span-7">
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="mb-5 flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                <User size={22} />
              </div>

              <div>
                <h2 className="text-xl font-black text-slate-900">
                  Passenger Details
                </h2>
                <p className="text-sm text-slate-500">
                  Ticket email will be sent to this email address.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <InputBox
                icon={<User size={18} />}
                label="Full Name"
                value={passenger.name}
                onChange={(value) => updatePassenger("name", value)}
              />

              <InputBox
                icon={<Phone size={18} />}
                label="Phone Number"
                value={passenger.phone}
                onChange={(value) => updatePassenger("phone", value)}
              />
            </div>

            <div className="mt-4">
              <InputBox
                icon={<Mail size={18} />}
                label="Email Address"
                value={passenger.email}
                onChange={(value) => updatePassenger("email", value)}
              />
            </div>
          </div>

          <div className="rounded-2xl bg-[#08264a] p-5 text-white shadow-sm">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/15">
                  <Gift size={23} />
                </div>

                <div>
                  <h3 className="font-black">Yatayat Rewards</h3>
                  <p className="text-sm text-slate-300">
                    You will earn points after this booking.
                  </p>
                </div>
              </div>

              <button className="rounded-lg border border-white/20 px-4 py-2 text-sm font-bold hover:bg-white/10">
                Apply Promo
              </button>
            </div>
          </div>
        </section>

        <aside className="space-y-5 xl:col-span-5">
          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="bg-[#08264a] p-6 text-white">
              <p className="text-xs font-black uppercase tracking-widest text-slate-300">
                {bus.type || "Super Deluxe AC"}
              </p>
              <h2 className="mt-2 text-2xl font-black">{bus.name}</h2>

              <div className="mt-4 rounded-full bg-blue-100 px-4 py-2 text-xs font-black text-[#08264a]">
                Seat: {selectedSeat}
              </div>
            </div>

            <div className="p-6">
              <SummaryRow
                icon={<Bus size={18} />}
                label="Route"
                value={bookingData?.routeName || bus.route || "Kathmandu to Pokhara"}
              />
              <SummaryRow
                icon={<Calendar size={18} />}
                label="Travel Date"
                value={bookingData?.travelDate || "2026-07-25"}
              />
              <SummaryRow
                icon={<Clock size={18} />}
                label="Departure Time"
                value={bookingData?.departureTime || bus.depart || "06:30 AM"}
              />
              <SummaryRow
                icon={<Ticket size={18} />}
                label="Seat Number"
                value={selectedSeat}
              />

              <div className="mt-6">
                <p className="mb-3 text-xs font-black uppercase tracking-widest text-slate-500">
                  Fare Breakdown
                </p>

                <FareRow
                  label="Total Payable"
                  value={`NPR ${totalAmount.toLocaleString()}`}
                />
              </div>
            </div>
          </div>

          <button
            onClick={handlePayment}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] py-4 text-sm font-black text-white transition hover:bg-[#0d3566]"
          >
            <CreditCard size={18} />
            Proceed to Payment
          </button>

          <button
            onClick={() => navigate("/seat-selection", { state: { bus } })}
            className="flex w-full items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white py-4 text-sm font-black text-[#08264a] transition hover:bg-slate-50"
          >
            <Edit3 size={17} />
            Edit Booking
          </button>
        </aside>
      </div>
    </PassengerLayout>
  );
}

function InputBox({ icon, label, value, onChange }) {
  return (
    <div>
      <label className="text-xs font-black uppercase tracking-widest text-slate-500">
        {label}
      </label>

      <div className="mt-2 flex items-center gap-3 rounded-xl border border-slate-300 bg-slate-50 px-4 py-3">
        <span className="text-slate-500">{icon}</span>
        <input
          value={value}
          onChange={(event) => onChange(event.target.value)}
          className="w-full bg-transparent text-sm font-bold outline-none"
        />
      </div>
    </div>
  );
}

function SummaryRow({ icon, label, value }) {
  return (
    <div className="flex items-center justify-between border-b border-slate-100 py-3">
      <div className="flex items-center gap-2 text-slate-500">
        {icon}
        <span className="text-sm font-semibold">{label}</span>
      </div>
      <span className="text-sm font-black text-slate-900">{value}</span>
    </div>
  );
}

function FareRow({ label, value }) {
  return (
    <div className="mb-3 flex justify-between text-sm">
      <span className="text-slate-500">{label}</span>
      <span className="font-black text-slate-900">{value}</span>
    </div>
  );
}
