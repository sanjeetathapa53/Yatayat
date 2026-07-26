import {
  Map,
  QrCode,
  PlusCircle,
  RotateCcw,
  Star,
  Bus,
  BadgeCheck,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { useLanguage } from "../../context/LanguageContext";

export default function PassengerDashboard() {
  const navigate = useNavigate();
  const { t } = useLanguage();
  const user = JSON.parse(localStorage.getItem("yatayatUser"));

  const [walletBalance, setWalletBalance] = useState(0);
  const [walletStatus, setWalletStatus] = useState("");

  useEffect(() => {
    const fetchWalletSummary = async () => {
      try {
        const response = await fetch(
          `http://localhost:8080/api/wallet/balance/${user.id}`,
          { credentials: "include" }
        );

        const balance = await response.text();
        setWalletBalance(Number(balance));

        const statusResponse = await fetch(
          `http://localhost:8080/api/wallet/pin-status/${user.id}`,
          { credentials: "include" }
        );

        if (statusResponse.ok) {
          setWalletStatus(await statusResponse.text());
        }
      } catch (error) {
        console.error("Failed to fetch wallet summary:", error);
      }
    };

    if (user?.id) {
      fetchWalletSummary();
    }
  }, []);



  return (
    <PassengerLayout
      activePage="Dashboard"
      title={`Namaste, ${user?.fullName?.split(" ")[0] || "Passenger"} 👋`}
      subtitle="View your wallet, routes, bookings, passes, and live buses."
    >
      <div className="grid min-w-0 grid-cols-1 items-start gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(300px,330px)]">
        <section className="min-w-0 space-y-5">
          <WalletCard
            balance={walletBalance}
            status={walletStatus}
            onWallet={() => navigate("/wallet")}
            t={t}
          />

          <section>
            <div className="mb-3">
              <p className="text-xs font-black uppercase tracking-widest text-emerald-700">{t("passenger.dashboard.localTransport")}</p>
              <h3 className="text-xl font-black text-slate-900 sm:text-2xl">{t("passenger.dashboard.travelInsideValley")}</h3>
            </div>
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
            <ActionCard
              icon={<Map size={26} />}
              title={t("passenger.layout.findLocalRoute")}
              desc={t("passenger.dashboard.originToDestination")}
              onClick={() => navigate("/passenger/local-routes")}
            />

            <ActionCard
              icon={<Map size={26} />}
              title={t("passenger.dashboard.trackLocalBus")}
              desc={t("passenger.dashboard.comingSoon")}
              disabled
            />

            <ActionCard
              icon={<QrCode size={26} />}
              title={t("passenger.dashboard.localFare")}
              desc={t("passenger.dashboard.localQrFare")}
              onClick={() => navigate("/fare-pass")}
            />

            <ActionCard
              icon={<BadgeCheck size={26} />}
              title={t("passenger.dashboard.myLocalTickets")}
              desc={t("passenger.dashboard.farePasses")}
              onClick={() => navigate("/fare-pass")}
            />
            </div>
          </section>

          <section>
            <div className="mb-3">
              <p className="text-xs font-black uppercase tracking-widest text-violet-700">{t("passenger.dashboard.outOfValleyTransport")}</p>
              <h3 className="text-xl font-black text-slate-900 sm:text-2xl">{t("passenger.dashboard.intercityTravel")}</h3>
            </div>
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
            <ActionCard
              icon={<Bus size={26} />}
              title={t("passenger.dashboard.bookOutOfValleyBus")}
              desc={t("passenger.dashboard.scheduledIntercityTrips")}
              onClick={() => navigate("/passenger/out-of-valley")}
            />
            <ActionCard icon={<BadgeCheck size={26} />} title={t("passenger.layout.myBookings")} desc={t("passenger.dashboard.viewCancelBookings")} onClick={() => navigate("/passenger/bookings")} />
            <ActionCard icon={<QrCode size={26} />} title={t("passenger.dashboard.travelTickets")} desc={t("passenger.dashboard.bookingConfirmations")} onClick={() => navigate("/passenger/bookings")} />
            </div>
          </section>

          <div className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                <QrCode size={26} />
              </div>

              <div>
                <p className="text-xs font-black uppercase tracking-widest text-emerald-700">
                  {t("passenger.dashboard.activeFarePass")}
                </p>
                <h3 className="text-lg font-black text-slate-900 sm:text-xl">
                  Route 14: Koteshwor → Kalanki
                </h3>
                <p className="mt-1 text-sm text-slate-500">
                  {t("passenger.dashboard.farePassValidity")}
                </p>
              </div>
            </div>

            <button
              onClick={() => navigate("/fare-pass")}
              className="flex items-center justify-center gap-2 rounded-lg bg-[#08264a] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#0d3566]"
            >
              <QrCode size={18} />
              {t("passenger.dashboard.showQr")}
            </button>
          </div>

          <section>
            <div className="mb-3 flex items-center justify-between">
              <h3 className="text-xl font-black text-slate-900 sm:text-2xl">
                {t("passenger.dashboard.outOfValleyBookings")}
              </h3>

              <button
                onClick={() => navigate("/passenger/bookings")}
                className="text-xs font-black uppercase tracking-wider text-emerald-700 hover:underline"
              >
                View all →
              </button>
            </div>

            <button
              onClick={() => navigate("/passenger/bookings")}
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
              {t("passenger.dashboard.favoriteRoutes")}
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

        <aside className="min-w-0">
          <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="mb-4 text-xl font-black text-slate-900 sm:text-2xl">
              {t("passenger.dashboard.recentMapSearch")}
            </h3>

            <button
              onClick={() => navigate("/track-bus")}
              className="relative flex h-56 w-full items-center justify-center overflow-hidden rounded-xl bg-[#1f2f46] text-slate-300 transition hover:scale-[1.01] sm:h-64"
            >
              {t("passenger.dashboard.mapPreview")}
              <p className="absolute bottom-4 left-4 text-sm text-white">
                📍 Current Location: Koteshwor
              </p>
            </button>

            <div className="mt-5 space-y-4">
              <InfoRow
                dot="bg-emerald-600"
                left={t("passenger.dashboard.nextBusArriving", { minutes: 4 })}
                right="Route 14"
              />
              <InfoRow
                dot="bg-slate-500"
                left={t("passenger.dashboard.expectedFareTo", { destination: "Kalanki" })}
                right="NPR 25"
              />
            </div>

            <button
              onClick={() => navigate("/passenger/local-routes")}
              className="mt-6 w-full rounded-xl border-2 border-[#08264a] py-3 text-sm font-black uppercase transition hover:bg-[#08264a] hover:text-white"
            >
              {t("passenger.layout.findLocalRoute")}
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

function WalletCard({ balance, status, onWallet, t }) {
  const isActive = status === "PIN_SET";
  const statusLabel =
    status === "PIN_SET"
      ? t("common.active")
      : status === "PIN_NOT_SET"
        ? t("common.inactive")
        : t("common.loading");

  return (
    <div className="relative h-auto min-h-[220px] w-full min-w-0 overflow-hidden rounded-2xl bg-[#08264a] p-5 text-white shadow-lg shadow-slate-900/10 sm:p-6">
      <div className="absolute inset-0 bg-linear-to-br from-[#061f3f] via-[#0d3d6d] to-[#106f82]" />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -right-12 -top-16 h-48 w-48 rounded-full bg-cyan-300/20 blur-2xl"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -bottom-20 right-28 h-44 w-44 rounded-full bg-emerald-300/20 blur-2xl"
      />

      <div className="relative z-10 grid min-h-[172px] gap-4 lg:grid-cols-[minmax(0,1.15fr)_minmax(230px,0.85fr)] lg:items-center">
        <div className="min-w-0">
          <p className="text-xs font-black uppercase tracking-[0.22em] text-blue-100">
            {t("passenger.dashboard.walletTitle")}
          </p>

          <div className="mt-4">
            <p className="text-sm font-bold text-blue-100">
              {t("passenger.dashboard.availableBalance")}
            </p>
            <h3 className="mt-1.5 break-words text-3xl font-black leading-tight sm:text-4xl xl:whitespace-nowrap xl:text-[46px]">
              NPR{" "}
              {balance.toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </h3>
          </div>

          <div className="mt-4 flex flex-col gap-2.5 sm:flex-row sm:flex-wrap sm:items-center">
            <button
              type="button"
              onClick={onWallet}
              className="flex min-h-10 items-center justify-center gap-2 rounded-xl bg-emerald-400 px-4 py-2.5 text-sm font-black text-[#08264a] transition hover:bg-emerald-300 focus:outline-none focus:ring-2 focus:ring-emerald-200 focus:ring-offset-2 focus:ring-offset-[#08264a]"
            >
              <PlusCircle size={17} />
              {t("common.topUp")}
            </button>

            <span
              className={`inline-flex min-h-10 items-center justify-center gap-2 rounded-full px-3.5 text-xs font-black ${
                isActive
                  ? "bg-emerald-400/20 text-emerald-100"
                  : "bg-white/15 text-slate-100"
              }`}
            >
              <span
                className={`h-2 w-2 rounded-full ${
                  isActive ? "bg-emerald-300" : "bg-amber-300"
                }`}
              />
              {statusLabel}
            </span>

            <button
              type="button"
              onClick={onWallet}
              className="flex min-h-10 items-center justify-center gap-2 rounded-xl bg-white/15 px-4 py-2.5 text-sm font-black text-white transition hover:bg-white/25 focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-[#08264a]"
            >
              <RotateCcw size={18} />
              {t("common.history")}
            </button>
          </div>
        </div>

        <div
          aria-hidden="true"
          className="pointer-events-none relative hidden h-[136px] min-w-0 overflow-hidden lg:block"
        >
          <div className="absolute right-2 top-5 h-[86px] w-[148px] rotate-3 rounded-3xl bg-white/15 shadow-2xl shadow-slate-950/20" />
          <div className="absolute right-8 top-9 h-20 w-[148px] -rotate-6 rounded-3xl bg-linear-to-br from-white/25 to-white/10 shadow-2xl shadow-slate-950/20" />
          <div className="absolute right-[120px] top-[52px] h-7 w-10 rounded-xl bg-emerald-300/90 shadow-lg shadow-emerald-950/20" />
          <div className="absolute right-[52px] top-[82px] h-1.5 w-[84px] rounded-full bg-white/35" />
          <div className="absolute right-[52px] top-[106px] h-1.5 w-16 rounded-full bg-white/25" />
          <div className="absolute bottom-4 right-8 flex gap-2">
            <span className="h-7 w-7 rounded-full bg-white/24" />
            <span className="h-7 w-7 rounded-full bg-emerald-300/70" />
          </div>
          <div className="absolute inset-y-0 right-[88px] w-px rotate-12 bg-white/10" />
        </div>
      </div>
    </div>
  );
}

function ActionCard({ icon, title, desc, onClick, disabled = false }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className="flex min-h-37.5 flex-col items-center justify-center rounded-xl border border-slate-200 bg-white p-5 text-center shadow-sm transition enabled:hover:-translate-y-1 enabled:hover:shadow-lg disabled:cursor-not-allowed disabled:opacity-65"
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
