import {
  Map,
  QrCode,
  PlusCircle,
  RotateCcw,
  Star,
  Bell,
  UserCircle,
  Bus,
  BadgeCheck,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { getFirstName } from "../../utils/authUser";

export default function PassengerDashboard() {
  const navigate = useNavigate();
  const firstName = getFirstName();
  const user = JSON.parse(localStorage.getItem("yatayatUser"));

const [walletBalance, setWalletBalance] = useState(0);

useEffect(() => {
  const fetchBalance = async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/wallet/balance/${user.id}`,
        { credentials: "include" }
      );

      const balance = await response.text();
      setWalletBalance(Number(balance));
    } catch (error) {
      console.error("Failed to fetch wallet balance:", error);
    }
  };

  if (user?.id) {
    fetchBalance();
  }
}, []);



  return (
    <PassengerLayout activePage="Dashboard">
      <header className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <h2 className="text-2xl font-black tracking-tight sm:text-3xl">
  Namaste, {firstName} 👋
</h2>
        <div className="hidden items-center gap-4 lg:flex">
          <div className="flex rounded-full bg-slate-200 p-1 text-xs font-bold">
            <button className="rounded-full bg-[#08264a] px-3 py-1 text-white">
              EN
            </button>
            <button className="px-3 py-1 text-slate-600">नेपाली</button>
          </div>

          <button
            onClick={() => navigate("/notifications")}
            className="rounded-full p-2 transition hover:bg-white"
          >
            <Bell size={20} />
          </button>

          <button
            onClick={() => navigate("/profile")}
            className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700 transition hover:bg-emerald-200"
          >
            <UserCircle size={23} />
          </button>
        </div>
      </header>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-12">
        <section className="space-y-5 xl:col-span-8">
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
          <WalletCard
  balance={walletBalance}
  onWallet={() => navigate("/wallet")}
/>

            <ActionCard
              icon={<Map size={26} />}
              title="Track Bus"
              desc="Real-time GPS"
              onClick={() => navigate("/track-bus")}
            />

            <ActionCard
              icon={<QrCode size={26} />}
              title="Fare Pass"
              desc="Local QR Fare"
              onClick={() => navigate("/fare-pass")}
            />

            <ActionCard
              icon={<Bus size={26} />}
              title="Book Ticket"
              desc="Out-of-Valley"
              onClick={() => navigate("/book-ticket")}
            />

            <ActionCard
              icon={<BadgeCheck size={26} />}
              title="Quick Scan"
              desc="Show QR Pass"
              onClick={() => navigate("/fare-pass")}
            />
          </div>

          <div className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                <QrCode size={26} />
              </div>

              <div>
                <p className="text-xs font-black uppercase tracking-widest text-emerald-700">
                  Active Fare Pass
                </p>
                <h3 className="text-lg font-black text-slate-900 sm:text-xl">
                  Route 14: Koteshwor → Kalanki
                </h3>
                <p className="mt-1 text-sm text-slate-500">
                  Valid for 24 hours or until scanned once
                </p>
              </div>
            </div>

            <button
              onClick={() => navigate("/fare-pass")}
              className="flex items-center justify-center gap-2 rounded-lg bg-[#08264a] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#0d3566]"
            >
              <QrCode size={18} />
              Show QR
            </button>
          </div>

          <section>
            <div className="mb-3 flex items-center justify-between">
              <h3 className="text-xl font-black text-slate-900 sm:text-2xl">
                Out-of-Valley Bookings
              </h3>

              <button
                onClick={() => navigate("/my-bookings")}
                className="text-xs font-black uppercase tracking-wider text-emerald-700 hover:underline"
              >
                View all →
              </button>
            </div>

            <button
              onClick={() => navigate("/my-bookings")}
              className="flex w-full flex-col gap-4 rounded-xl border border-slate-200 bg-white p-4 text-left shadow-sm transition hover:-translate-y-1 hover:shadow-md sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="flex items-center gap-4">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-slate-100">
                  <Bus size={20} />
                </div>

                <div>
                  <h4 className="font-black text-slate-900">
                    Kathmandu to Pokhara
                  </h4>
                  <p className="text-sm text-slate-500">
                    25 Oct • 07:30 AM • Seat B4, B5
                  </p>
                </div>
              </div>

              <div className="text-left sm:text-right">
                <span className="rounded-full bg-emerald-100 px-3 py-1 text-[10px] font-black text-emerald-700">
                  CONFIRMED
                </span>
                <p className="mt-1 text-sm font-bold">NPR 1,200</p>
              </div>
            </button>
          </section>

          <section>
            <h3 className="mb-3 text-xl font-black text-slate-900 sm:text-2xl">
              Favorite Routes
            </h3>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <FavoriteRoute
                route="ROUTE 14"
                title="Koteshwor - Kalanki"
                time="Every 5-10 mins"
                onClick={() => navigate("/track-bus/14")}
              />

              <FavoriteRoute
                route="ROUTE 02"
                title="Ratnapark - Budhanilkantha"
                time="Every 15 mins"
                onClick={() => navigate("/track-bus/2")}
              />
            </div>
          </section>
        </section>

        <aside className="xl:col-span-4">
          <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="mb-4 text-xl font-black text-slate-900 sm:text-2xl">
              Recent Map Search
            </h3>

            <button
              onClick={() => navigate("/track-bus")}
              className="relative flex h-56 w-full items-center justify-center overflow-hidden rounded-xl bg-[#1f2f46] text-slate-300 transition hover:scale-[1.01] sm:h-64"
            >
              Map Preview
              <p className="absolute bottom-4 left-4 text-sm text-white">
                📍 Current Location: Koteshwor
              </p>
            </button>

            <div className="mt-5 space-y-4">
              <InfoRow
                dot="bg-emerald-600"
                left="Next bus arriving in 4 mins"
                right="Route 14"
              />
              <InfoRow
                dot="bg-slate-500"
                left="Expected fare to Kalanki"
                right="NPR 25"
              />
            </div>

            <button
              onClick={() => navigate("/routes")}
              className="mt-6 w-full rounded-xl border-2 border-[#08264a] py-3 text-sm font-black uppercase transition hover:bg-[#08264a] hover:text-white"
            >
              Plan New Journey
            </button>
          </div>
        </aside>
      </div>

      <footer className="mt-8 flex flex-col gap-3 border-t border-slate-200 pt-4 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between">
        <p>© 2026 Yatayat Passenger Portal</p>

        <div className="flex gap-5">
          <button onClick={() => navigate("/help")} className="hover:text-[#08264a]">
            Help
          </button>
          <button onClick={() => navigate("/privacy")} className="hover:text-[#08264a]">
            Privacy
          </button>
          <button onClick={() => navigate("/terms")} className="hover:text-[#08264a]">
            Terms
          </button>
        </div>
      </footer>
    </PassengerLayout>
  );
}

function WalletCard({ balance, onWallet }) {
  return (
    <div className="rounded-xl bg-linear-to-br from-[#1d3f6e] to-[#265e6e] p-6 text-white shadow-lg">
      <p className="text-xs font-black uppercase tracking-widest text-slate-300">
        Digital Wallet
      </p>

      <h3 className="mt-5 text-4xl font-black leading-tight">
  NPR <br />
  {balance.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}
</h3>

      <div className="mt-8 flex gap-3">
        <button
          onClick={onWallet}
          className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-emerald-400 py-3 text-xs font-black text-[#08264a] transition hover:bg-emerald-300"
        >
          <PlusCircle size={17} />
          Recharge
        </button>

        <button
          onClick={onWallet}
          className="rounded-lg bg-white/15 px-4 transition hover:bg-white/25"
        >
          <RotateCcw size={19} />
        </button>
      </div>
    </div>
  );
}

function ActionCard({ icon, title, desc, onClick }) {
  return (
    <button
      onClick={onClick}
      className="flex min-h-37.5 flex-col items-center justify-center rounded-xl border border-slate-200 bg-white p-5 text-center shadow-sm transition hover:-translate-y-1 hover:shadow-lg"
    >
      <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
        {icon}
      </div>

      <h3 className="font-black text-slate-900">{title}</h3>
      <p className="mt-1 text-sm text-slate-500">{desc}</p>
    </button>
  );
}

function FavoriteRoute({ route, title, time, onClick }) {
  return (
    <button
      onClick={onClick}
      className="rounded-xl border border-slate-200 bg-white p-5 text-left shadow-sm transition hover:-translate-y-1 hover:shadow-md"
    >
      <div className="flex justify-between">
        <span className="bg-slate-100 px-3 py-1 text-[10px] font-bold text-slate-500">
          {route}
        </span>
        <Star className="fill-emerald-700 text-emerald-700" size={19} />
      </div>

      <h4 className="mt-5 font-black text-slate-900">{title}</h4>
      <p className="text-sm text-slate-500">{time}</p>
    </button>
  );
}

function InfoRow({ dot, left, right }) {
  return (
    <div className="flex items-center justify-between text-sm">
      <div className="flex items-center gap-3">
        <span className={`h-2 w-2 rounded-full ${dot}`}></span>
        <span>{left}</span>
      </div>
      <span className="font-bold">{right}</span>
    </div>
  );
}
