import {
  Bus,
  MapPin,
  Navigation,
  Calendar,
  User,
  Search,
  Star,
  Wallet,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { useEffect, useState } from "react";

const buses = [
  {
    id: 1,
    name: "Sajha Yatayat",
    rating: "4.8",
    depart: "06:30 AM",
    arrive: "02:00 PM",
    from: "Gongabu Park, KTM",
    to: "Prithvi Chowk, PKR",
    duration: "7h 30m",
    type: "Super Deluxe AC",
    price: "1,850",
    seats: "Only 8 seats left!",
  },
  {
    id: 2,
    name: "Blue Sky Travels",
    rating: "4.5",
    depart: "07:45 AM",
    arrive: "04:00 PM",
    from: "Kalanki Bus Stop, KTM",
    to: "Lakeside, PKR",
    duration: "8h 15m",
    type: "Premium AC WiFi",
    price: "1,500",
    seats: "22 seats available",
  },
  {
    id: 3,
    name: "Yeti Intercity",
    rating: "New",
    depart: "09:00 AM",
    arrive: "04:45 PM",
    from: "Balaju, KTM",
    to: "Prithvi Chowk, PKR",
    duration: "7h 45m",
    type: "Luxury Sofa",
    price: "2,200",
    seats: "15 seats available",
  },
];

export default function BookTicketPage() {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem("yatayatUser"));

const [walletBalance, setWalletBalance] = useState(0);

useEffect(() => {
  const fetchBalance = async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/wallet/balance/${user.id}`
      );

      const balance = await response.text();
      setWalletBalance(Number(balance));
    } catch (error) {
      console.error(error);
    }
  };

  if (user?.id) {
    fetchBalance();
  }
}, []);

  return (
    <PassengerLayout activePage="Out-of-Valley">
      <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
            Out-of-Valley Booking
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Search buses, select a seat, pay, and receive your ticket by email.
          </p>
        </div>

        <button
  onClick={() => navigate("/wallet")}
  className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white shadow-sm hover:bg-[#0d3566] sm:w-auto"
>
  <Wallet size={18} />
  Wallet: NPR{" "}
  {walletBalance.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}
</button>
      </header>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5">
          <SearchBox
            icon={<MapPin size={19} />}
            label="Departure Point"
            value="Kathmandu"
          />
          <SearchBox
            icon={<Navigation size={19} />}
            label="Destination"
            value="Pokhara"
          />
          <SearchBox
            icon={<Calendar size={19} />}
            label="Travel Date"
            value="2026-07-25"
          />
          <SearchBox icon={<User size={19} />} label="Passengers" value="1" />

          <button className="flex items-center justify-center gap-2 rounded-xl bg-[#1d3f6e] px-5 py-4 text-sm font-black text-white transition hover:bg-[#08264a] xl:mt-6">
            <Search size={18} />
            Search
          </button>
        </div>
      </section>

      <section className="mt-7">
        <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-xl font-black text-slate-900">
            Found 12 Buses for Kathmandu to Pokhara
          </h2>

          <button className="text-left text-sm font-black text-slate-600 hover:text-[#08264a] sm:text-right">
            Sort by: Earliest First
          </button>
        </div>

        <div className="space-y-4">
          {buses.map((bus) => (
            <BusCard
              key={bus.id}
              bus={bus}
              onSelect={() =>
                navigate("/seat-selection", {
                  state: {
                    bus: {
                      ...bus,
                      routeName: `${bus.from} to ${bus.to}`,
                      busNumber: `YAT-${bus.id}00${bus.id}`,
                      fare: Number(bus.price.replace(",", "")),
                      travelDate: "2026-07-25",
                      departureTime: bus.depart,
                    },
                  },
                })
              }
            />
          ))}
        </div>

        <div className="mt-6 flex justify-center">
          <button className="rounded-full border border-slate-300 bg-white px-8 py-3 text-sm font-black hover:bg-slate-50">
            Load More Options
          </button>
        </div>
      </section>
    </PassengerLayout>
  );
}

function SearchBox({ icon, label, value }) {
  return (
    <div>
      <p className="mb-2 text-[11px] font-black uppercase tracking-widest text-slate-500">
        {label}
      </p>

      <div className="flex items-center gap-3 rounded-xl bg-slate-100 px-4 py-4">
        <span className="text-slate-500">{icon}</span>
        <span className="font-semibold text-slate-800">{value}</span>
      </div>
    </div>
  );
}

function BusCard({ bus, onSelect }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-1 hover:shadow-md">
      <div className="grid grid-cols-1 gap-5 xl:grid-cols-12 xl:items-center">
        <div className="flex items-center gap-4 xl:col-span-3">
          <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
            <Bus size={28} />
          </div>

          <div>
            <h3 className="font-black text-slate-900">{bus.name}</h3>
            <span className="mt-2 inline-flex items-center gap-1 rounded bg-emerald-100 px-2 py-1 text-xs font-black text-emerald-700">
              <Star size={12} className="fill-emerald-700" />
              {bus.rating}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 xl:col-span-6">
          <InfoBlock label="Departure" main={bus.depart} sub={bus.from} />
          <InfoBlock label="Duration" main={bus.duration} sub={bus.type} center />
          <InfoBlock label="Arrival" main={bus.arrive} sub={bus.to} />
        </div>

        <div className="border-t border-slate-200 pt-4 text-left xl:col-span-3 xl:border-l xl:border-t-0 xl:pl-6 xl:pt-0 xl:text-right">
          <p className="text-xs font-bold text-slate-500">Ticket Price</p>
          <h3 className="text-3xl font-black">NPR {bus.price}</h3>
          <p
            className={`text-xs font-bold ${
              bus.seats.includes("Only") ? "text-red-500" : "text-slate-500"
            }`}
          >
            {bus.seats}
          </p>

          <button
            onClick={onSelect}
            className="mt-4 w-full rounded-xl bg-[#08264a] px-7 py-3 text-sm font-bold text-white transition hover:bg-[#0d3566] sm:w-auto"
          >
            Select Bus
          </button>
        </div>
      </div>
    </div>
  );
}

function InfoBlock({ label, main, sub, center }) {
  return (
    <div className={center ? "sm:text-center" : ""}>
      <p className="text-[11px] font-black uppercase tracking-widest text-slate-500">
        {label}
      </p>
      <h3 className="mt-1 text-lg font-black sm:text-xl">{main}</h3>
      <p className="mt-1 text-sm text-slate-500">{sub}</p>
    </div>
  );
}