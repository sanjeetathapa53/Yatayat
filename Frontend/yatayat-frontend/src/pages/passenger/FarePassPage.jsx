import { useState } from "react";
import {
  Wallet,
  QrCode,
  Route,
  MapPin,
  ShieldCheck,
  RefreshCcw,
  CheckCircle,
  AlertCircle,
  Clock,
} from "lucide-react";
import PassengerLayout from "../../components/layout/PassengerLayout";

export default function FarePassPage() {
  const [qrGenerated, setQrGenerated] = useState(false);

  return (
    <PassengerLayout activePage="Routes">
      <header className="mb-6">
        <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
          Local QR Fare Pass
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Pay local bus fare from your wallet and generate a one-time QR pass.
        </p>
      </header>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-12">
        <section className="space-y-5 xl:col-span-7">
          <div className="rounded-2xl bg-[#08264a] p-5 text-white shadow-sm">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-black uppercase tracking-widest text-slate-300">
                  Wallet Balance
                </p>

                <h2 className="mt-2 text-4xl font-black">NPR 1,250</h2>

                <p className="mt-1 text-sm text-slate-300">
                  Available balance
                </p>
              </div>

              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-white/15">
                <Wallet size={26} />
              </div>
            </div>

            <div className="mt-5 flex flex-wrap gap-3">
              <span className="rounded-full bg-white/10 px-3 py-1 text-xs font-bold">
                Valid for 24 hours
              </span>
              <span className="rounded-full bg-emerald-600 px-3 py-1 text-xs font-bold">
                One-time use
              </span>
              <span className="rounded-full bg-white/10 px-3 py-1 text-xs font-bold">
                Offline ready
              </span>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-4 flex items-center gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                <Route size={21} />
              </div>

              <div>
                <h2 className="text-xl font-black text-slate-900">
                  Select Local Route
                </h2>
                <p className="text-sm text-slate-500">
                  This pass verifies local fare payment only. It does not reserve a seat.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <InputBox label="From" value="Ratnapark" />
              <InputBox label="To" value="Bhaktapur" />
            </div>

            <div className="mt-4 rounded-xl bg-slate-50 p-4">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-xs font-black uppercase tracking-widest text-slate-500">
                    Selected Route
                  </p>

                  <h3 className="mt-1 text-lg font-black">
                    Route #04 • Sajha Yatayat
                  </h3>

                  <p className="mt-1 flex items-center gap-2 text-sm text-slate-500">
                    <MapPin size={16} />
                    Ratnapark → Bhaktapur
                  </p>
                </div>

                <div className="text-left sm:text-right">
                  <p className="text-xs font-black uppercase tracking-widest text-slate-500">
                    Fare
                  </p>

                  <h3 className="mt-1 text-3xl font-black text-emerald-700">
                    NPR 45
                  </h3>
                </div>
              </div>
            </div>

            <button
              onClick={() => setQrGenerated(true)}
              className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] py-3 text-sm font-black text-white transition hover:bg-[#0d3566]"
            >
              <QrCode size={18} />
              Generate QR Fare Pass
            </button>
          </div>

          <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
            <div className="flex gap-2">
              <AlertCircle size={18} className="shrink-0" />
              <p>
                This QR pass is valid for 24 hours or until scanned once by the
                driver/conductor. Once scanned, it becomes USED and cannot be reused.
              </p>
            </div>
          </div>
        </section>

        <aside className="xl:col-span-5">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm xl:sticky xl:top-6">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 className="text-xl font-black text-slate-900">
                  QR Fare Pass
                </h2>
                <p className="mt-1 text-sm text-slate-500">
                  Show this to the driver/conductor for scanning.
                </p>
              </div>

              <ShieldCheck className="shrink-0 text-emerald-700" size={24} />
            </div>

            {!qrGenerated ? (
              <div className="mt-4 flex h-56 flex-col items-center justify-center rounded-2xl border-2 border-dashed border-slate-300 bg-slate-50 text-center">
                <QrCode size={46} className="text-slate-400" />
                <p className="mt-3 text-sm font-bold text-slate-500">
                  QR will appear after generation
                </p>
              </div>
            ) : (
              <div className="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
                <div className="mx-auto flex h-44 w-44 items-center justify-center rounded-2xl bg-white shadow-inner sm:h-48 sm:w-48">
                  <div className="grid grid-cols-5 gap-1">
                    {Array.from({ length: 45 }).map((_, index) => (
                      <span
                        key={index}
                        className={`h-4 w-4 sm:h-5 sm:w-5 ${
                          index % 3 === 0 || index % 7 === 0
                            ? "bg-[#08264a]"
                            : "bg-slate-200"
                        }`}
                      />
                    ))}
                  </div>
                </div>

                <div className="mt-4 space-y-2 text-sm">
                  <PassInfo label="Pass ID" value="LQR-2026-0012" />
                  <PassInfo label="Route" value="Ratnapark → Bhaktapur" />
                  <PassInfo label="Fare" value="NPR 45" />
                  <PassInfo label="Status" value="ACTIVE" />
                  <PassInfo label="Time Left" value="23h 59m" />
                  <PassInfo label="Usage" value="Unused" />
                </div>

                <div className="mt-4 flex items-center gap-2 rounded-xl bg-white p-3 text-sm font-bold text-emerald-700">
                  <CheckCircle size={18} />
                  Active • Offline QR Ready
                </div>
              </div>
            )}

            <div className="mt-4 rounded-xl bg-slate-50 p-4">
              <div className="flex items-center gap-2 text-sm font-black text-slate-700">
                <Clock size={17} />
                QR Validity
              </div>
              <p className="mt-2 text-sm leading-6 text-slate-500">
                Valid for 24 hours, but once scanned by the driver, it becomes
                used immediately.
              </p>
            </div>

            <button
              onClick={() => setQrGenerated(false)}
              className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl border border-slate-300 py-3 text-sm font-black hover:bg-slate-50"
            >
              <RefreshCcw size={17} />
              Reset Pass
            </button>
          </div>
        </aside>
      </div>
    </PassengerLayout>
  );
}

function InputBox({ label, value }) {
  return (
    <div>
      <label className="text-xs font-black uppercase tracking-widest text-slate-500">
        {label}
      </label>

      <input
        defaultValue={value}
        className="mt-2 w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm font-bold outline-none focus:border-[#08264a]"
      />
    </div>
  );
}

function PassInfo({ label, value }) {
  return (
    <div className="flex justify-between gap-4 border-b border-emerald-100 pb-1">
      <span className="text-slate-500">{label}</span>
      <span className="text-right font-black text-[#08264a]">{value}</span>
    </div>
  );
}