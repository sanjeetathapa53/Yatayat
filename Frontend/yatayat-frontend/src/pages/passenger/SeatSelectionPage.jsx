import { useMemo, useState } from "react";
import {
  Bus,
  Armchair,
  ArrowLeft,
  CheckCircle,
} from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";

const defaultBus = {
  id: 1,
  name: "Sajha Yatayat",
  route: "Kathmandu → Pokhara",
  depart: "06:30 AM",
  arrive: "02:00 PM",
  duration: "7h 30m",
  type: "Super Deluxe AC",
  price: "1,850",
  seats: "Only 8 seats left!",
};

const occupiedSeats = ["A2", "B1", "C4", "D2", "E3", "F1", "G4", "H2"];

const seatRows = [
  ["A1", "A2", "", "A3", "A4"],
  ["B1", "B2", "", "B3", "B4"],
  ["C1", "C2", "", "C3", "C4"],
  ["D1", "D2", "", "D3", "D4"],
  ["E1", "E2", "", "E3", "E4"],
  ["F1", "F2", "", "F3", "F4"],
  ["G1", "G2", "", "G3", "G4"],
  ["H1", "H2", "", "H3", "H4"],
];

export default function SeatSelectionPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const bus = location.state?.bus || defaultBus;

  const [selectedSeat, setSelectedSeat] = useState("B2");

  const totalAmount = useMemo(() => {
    const price = Number(String(bus.price).replaceAll(",", ""));
    return price + 50;
  }, [bus.price]);

  const handleSeatClick = (seat) => {
    if (!seat || occupiedSeats.includes(seat)) return;
    setSelectedSeat(seat);
  };

  return (
    <PassengerLayout activePage="Out-of-Valley">
      <button
        onClick={() => navigate("/book-ticket")}
        className="mb-5 flex items-center gap-2 text-sm font-black text-slate-600 hover:text-[#08264a]"
      >
        <ArrowLeft size={17} />
        Back to buses
      </button>

      <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
            Select Your Seat
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Choose an available seat for your out-of-valley trip.
          </p>
        </div>

        <div className="rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white shadow-sm">
          {bus.name}
        </div>
      </header>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-12">
        <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6 xl:col-span-8">
          <div className="mb-6 rounded-2xl bg-slate-50 p-5">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <Info label="Bus" value={bus.name} />
              <Info label="Route" value={bus.route || "Kathmandu → Pokhara"} />
              <Info label="Departure" value={bus.depart || "06:30 AM"} />
              <Info label="Type" value={bus.type || "Deluxe"} />
            </div>
          </div>

          <div className="mx-auto max-w-lg rounded-3xl border border-slate-200 bg-[#eef3f8] p-4 sm:p-6">
            <div className="mb-6 flex items-center justify-between rounded-2xl bg-[#08264a] px-5 py-4 text-white">
              <div>
                <p className="text-xs font-black uppercase tracking-widest text-slate-300">
                  Front
                </p>
                <h3 className="text-lg font-black">Driver Area</h3>
              </div>

              <Bus size={28} />
            </div>

            <div className="space-y-3">
              {seatRows.map((row, rowIndex) => (
                <div key={rowIndex} className="grid grid-cols-5 gap-2 sm:gap-3">
                  {row.map((seat, index) =>
                    seat ? (
                      <button
                        key={seat}
                        onClick={() => handleSeatClick(seat)}
                        disabled={occupiedSeats.includes(seat)}
                        className={`flex h-11 items-center justify-center rounded-xl border text-xs font-black transition sm:h-12 sm:text-sm ${
                          occupiedSeats.includes(seat)
                            ? "cursor-not-allowed border-red-200 bg-red-100 text-red-500"
                            : selectedSeat === seat
                            ? "border-[#08264a] bg-[#08264a] text-white shadow-md"
                            : "border-slate-300 bg-white text-slate-700 hover:border-[#08264a] hover:bg-slate-50"
                        }`}
                      >
                        {seat}
                      </button>
                    ) : (
                      <div key={`gap-${rowIndex}-${index}`} />
                    )
                  )}
                </div>
              ))}
            </div>

            <div className="mt-6 grid grid-cols-1 gap-3 text-xs font-bold sm:grid-cols-3">
              <Legend color="bg-white border border-slate-300" label="Available" />
              <Legend color="bg-[#08264a]" label="Selected" />
              <Legend color="bg-red-100 border border-red-200" label="Occupied" />
            </div>
          </div>
        </section>

        <aside className="space-y-5 xl:col-span-4">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">
              Booking Summary
            </h2>

            <div className="mt-5 rounded-xl bg-emerald-50 p-4">
              <p className="text-xs font-black uppercase tracking-widest text-emerald-700">
                Selected Seat
              </p>
              <h3 className="mt-2 flex items-center gap-2 text-3xl font-black text-[#08264a]">
                <Armchair size={28} />
                {selectedSeat}
              </h3>
            </div>

            <div className="mt-5 space-y-3 text-sm">
              <SummaryRow label="Bus" value={bus.name} />
              <SummaryRow label="Route" value={bus.route || "Kathmandu → Pokhara"} />
              <SummaryRow label="Departure" value={bus.depart || "06:30 AM"} />
              <SummaryRow label="Seat" value={selectedSeat} />
              <SummaryRow label="Ticket Price" value={`NPR ${Number(String(bus.price).replaceAll(",", "")).toLocaleString()}`} />
              <SummaryRow label="Service Fee" value="NPR 50" />
            </div>

            <div className="mt-5 rounded-xl bg-slate-50 p-4">
              <div className="flex justify-between">
                <span className="font-black">Total Amount</span>
                <span className="text-xl font-black text-emerald-700">
                  NPR {totalAmount.toLocaleString()}
                </span>
              </div>
            </div>

            <button
              onClick={() =>
                navigate("/booking-summary", {
                  state: {
                    bookingData: {
                      routeName: bus.routeName || bus.route || "Kathmandu to Pokhara",
                      busNumber: bus.busNumber || `YAT-${bus.id}`,
                      seatNumber: selectedSeat,
                      travelDate: bus.travelDate || "2026-07-25",
                      departureTime: bus.departureTime || bus.depart || "06:30 AM",
                      fare: totalAmount,
                    },
                    bus,
                  },
                })
              }
              className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] py-3 text-sm font-black text-white transition hover:bg-[#0d3566]"
            >
              <CheckCircle size={18} />
              Continue
            </button>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-600 shadow-sm">
            <h3 className="font-black text-slate-900">Seat Rules</h3>
            <p className="mt-2 leading-6">
              Seats shown in red are already occupied. Once payment is completed,
              your selected seat will be locked and your ticket will be sent by
              email.
            </p>
          </div>
        </aside>
      </div>
    </PassengerLayout>
  );
}

function Info({ label, value }) {
  return (
    <div>
      <p className="text-xs font-black uppercase tracking-widest text-slate-500">
        {label}
      </p>
      <p className="mt-1 font-black text-slate-900">{value}</p>
    </div>
  );
}

function Legend({ color, label }) {
  return (
    <div className="flex items-center gap-2">
      <span className={`h-4 w-4 rounded ${color}`}></span>
      <span className="text-slate-500">{label}</span>
    </div>
  );
}

function SummaryRow({ label, value }) {
  return (
    <div className="flex justify-between gap-4 border-b border-slate-100 pb-2">
      <span className="text-slate-500">{label}</span>
      <span className="text-right font-black text-slate-900">{value}</span>
    </div>
  );
}