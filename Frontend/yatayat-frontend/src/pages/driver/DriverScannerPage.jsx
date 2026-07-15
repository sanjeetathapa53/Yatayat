import { useEffect, useState } from "react";
import {
  QrCode,
  Ticket,
  Keyboard,
  CheckCircle,
  XCircle,
  User,
  ShieldCheck,
  Search,
  AlertTriangle,
  Bus,
  Loader2,
} from "lucide-react";
import DriverLayout from "../../components/layout/DriverLayout";
import { Html5QrcodeScanner } from "html5-qrcode";

export default function DriverScannerPage() {
  const [activeTab, setActiveTab] = useState("ticket");
  const [manualCode, setManualCode] = useState("");
  const [scanResult, setScanResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [recentScans, setRecentScans] = useState([]);

  const validateQr = async (qrCode) => {
    if (!qrCode?.trim()) return;

    setLoading(true);

    try {
      const res = await fetch("http://localhost:8080/api/bookings/validate-qr", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ qrCode }),
      });

      const data = await res.json();
      setScanResult(data);

      setRecentScans((prev) => [
        {
          qrCode,
          status: data.status,
          message: data.message,
          booking: data.booking,
          time: new Date().toLocaleTimeString(),
        },
        ...prev.slice(0, 4),
      ]);
    } catch (error) {
      console.error(error);
      setScanResult({
        success: false,
        status: "ERROR",
        message: "Could not verify ticket.",
      });
    } finally {
      setLoading(false);
    }
  };

  const confirmBoarding = async () => {
    if (!scanResult?.booking?.qrCode) return;

    setLoading(true);

    try {
      const res = await fetch("http://localhost:8080/api/bookings/mark-used", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ qrCode: scanResult.booking.qrCode }),
      });

      const data = await res.json();
      setScanResult(data);
    } catch (error) {
      console.error(error);
      setScanResult({
        success: false,
        status: "ERROR",
        message: "Could not confirm boarding.",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <DriverLayout activePage="Scanner">
      <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
            Ticket Scanner
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Scan passenger QR tickets and confirm boarding.
          </p>
        </div>

        <span className="rounded-full bg-emerald-100 px-4 py-2 text-xs font-black uppercase tracking-widest text-emerald-700">
          ● Online
        </span>
      </header>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-12">
        <section className="space-y-5 xl:col-span-7">
          <div className="rounded-2xl border border-slate-200 bg-white p-2 shadow-sm">
            <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
              <TabButton
                active={activeTab === "ticket"}
                icon={<QrCode size={18} />}
                label="Scan QR"
                onClick={() => setActiveTab("ticket")}
              />
              <TabButton
                active={activeTab === "manual"}
                icon={<Keyboard size={18} />}
                label="Manual Entry"
                onClick={() => setActiveTab("manual")}
              />
            </div>
          </div>

          {activeTab === "ticket" ? (
            <ScannerCard onScan={validateQr} />
          ) : (
            <ManualEntry
              manualCode={manualCode}
              setManualCode={setManualCode}
              onVerify={() => validateQr(manualCode)}
              loading={loading}
            />
          )}

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">
              Scanner Instructions
            </h2>

            <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
              <Instruction title="Scan QR" desc="Scan the ticket QR from phone, PDF, or email." />
              <Instruction title="Check Status" desc="Valid tickets can board. Cancelled or used tickets are rejected." />
              <Instruction title="Confirm Boarding" desc="After validation, mark the ticket as used." />
            </div>
          </div>
        </section>

        <aside className="space-y-5 xl:col-span-5">
          <VerificationCard
            result={scanResult}
            loading={loading}
            onConfirmBoarding={confirmBoarding}
            onReset={() => setScanResult(null)}
          />

          <RecentScans recentScans={recentScans} />

          <CapacityCard usedCount={recentScans.filter((s) => s.status === "USED").length} />
        </aside>
      </div>
    </DriverLayout>
  );
}

function ScannerCard({ onScan }) {
  useEffect(() => {
    const scanner = new Html5QrcodeScanner(
      "driver-qr-reader",
      {
        fps: 10,
        qrbox: { width: 260, height: 260 },
      },
      false
    );

    scanner.render(
      (decodedText) => {
        onScan(decodedText);
        scanner.clear().catch(() => {});
      },
      () => {}
    );

    return () => {
      scanner.clear().catch(() => {});
    };
  }, []);

  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <h2 className="mb-4 text-xl font-black text-slate-900">
        Camera QR Scanner
      </h2>

      <div
        id="driver-qr-reader"
        className="overflow-hidden rounded-2xl border border-slate-300"
      />

      <p className="mt-4 text-center text-sm font-semibold text-slate-500">
        Allow camera permission and place the QR inside the scanner.
      </p>
    </div>
  );
}

function ManualEntry({ manualCode, setManualCode, onVerify, loading }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <h2 className="text-xl font-black text-slate-900">
        Manual QR Verification
      </h2>

      <p className="mt-1 text-sm text-slate-500">
        Paste the QR code value manually if camera scanning is unavailable.
      </p>

      <div className="mt-5 flex items-center gap-3 rounded-xl border border-slate-300 bg-slate-50 px-4 py-3">
        <Search size={18} className="text-slate-500" />
        <input
          value={manualCode}
          onChange={(e) => setManualCode(e.target.value)}
          placeholder="Example: YATAYAT-xxxx-xxxx"
          className="w-full bg-transparent text-sm font-bold outline-none"
        />
      </div>

      <button
        onClick={onVerify}
        disabled={loading || !manualCode.trim()}
        className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] py-3 text-sm font-black text-white hover:bg-[#0d3566] disabled:opacity-60"
      >
        {loading ? <Loader2 size={18} className="animate-spin" /> : <ShieldCheck size={18} />}
        Verify Ticket
      </button>
    </div>
  );
}

function VerificationCard({ result, loading, onConfirmBoarding, onReset }) {
  if (loading) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <Loader2 size={38} className="mx-auto animate-spin text-[#08264a]" />
        <h2 className="mt-4 text-xl font-black">Checking ticket...</h2>
      </div>
    );
  }

  if (!result) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <QrCode size={44} className="mx-auto text-slate-400" />
        <h2 className="mt-4 text-xl font-black text-slate-900">
          Waiting for Scan
        </h2>
        <p className="mt-2 text-sm text-slate-500">
          Scan or enter a QR code to verify ticket.
        </p>
      </div>
    );
  }

  const booking = result.booking;
  const valid = result.status === "VALID";
  const used = result.status === "USED";
  const cancelled = result.status === "CANCELLED";

  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div
        className={`px-5 py-4 text-white ${
          valid ? "bg-emerald-600" : used ? "bg-blue-600" : "bg-red-600"
        }`}
      >
        <div className="flex items-center gap-2">
          {valid ? <CheckCircle size={22} /> : <XCircle size={22} />}
          <h2 className="font-black">
            {valid
              ? "VALID TICKET"
              : used
              ? "ALREADY USED"
              : cancelled
              ? "CANCELLED TICKET"
              : "INVALID TICKET"}
          </h2>
        </div>

        <p className="mt-1 text-sm font-semibold opacity-90">{result.message}</p>
      </div>

      <div className="p-5">
        {booking ? (
          <>
            <div className="grid grid-cols-2 gap-3">
              <VerifyInfo label="Booking ID" value={`YT-${booking.id}`} />
              <VerifyInfo label="Seat" value={booking.seatNumber} />
              <VerifyInfo label="Bus" value={booking.busNumber} />
              <VerifyInfo label="Fare" value={`NPR ${booking.fare}`} />
              <VerifyInfo label="Date" value={booking.travelDate} />
              <VerifyInfo label="Departure" value={booking.departureTime} />
            </div>

            <div className="mt-4 rounded-xl bg-slate-50 p-4">
              <p className="text-xs font-black uppercase tracking-widest text-slate-500">
                Route
              </p>
              <p className="mt-2 font-black text-slate-900">{booking.routeName}</p>
            </div>

            {valid && (
              <button
                onClick={onConfirmBoarding}
                className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] py-3 text-sm font-black text-white hover:bg-[#0d3566]"
              >
                <CheckCircle size={18} />
                Confirm Boarding
              </button>
            )}
          </>
        ) : (
          <div className="rounded-2xl bg-red-50 p-5 text-center">
            <AlertTriangle className="mx-auto text-red-600" size={42} />
            <h3 className="mt-3 text-xl font-black text-red-700">
              Invalid QR
            </h3>
            <p className="mt-2 text-sm text-red-600">
              This QR does not match any ticket.
            </p>
          </div>
        )}

        <button
          onClick={onReset}
          className="mt-4 w-full rounded-xl border border-slate-300 py-3 text-sm font-black text-slate-700 hover:bg-slate-50"
        >
          Scan Another Ticket
        </button>
      </div>
    </div>
  );
}

function RecentScans({ recentScans }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <h2 className="text-xl font-black text-slate-900">Recent Scans</h2>

      <div className="mt-5 space-y-4">
        {recentScans.length === 0 ? (
          <p className="text-sm text-slate-500">No scans yet.</p>
        ) : (
          recentScans.map((scan, index) => (
            <div key={index} className="flex items-center justify-between rounded-xl bg-slate-50 p-4">
              <div>
                <p className="font-black text-slate-900">
                  {scan.booking ? `YT-${scan.booking.id}` : "Unknown Ticket"}
                </p>
                <p className="text-sm text-slate-500">
                  {scan.time} • {scan.booking?.seatNumber || "N/A"}
                </p>
              </div>

              <span
                className={`text-xs font-black ${
                  scan.status === "VALID" || scan.status === "USED"
                    ? "text-emerald-700"
                    : "text-red-600"
                }`}
              >
                {scan.status}
              </span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

function CapacityCard({ usedCount }) {
  return (
    <div className="overflow-hidden rounded-2xl bg-[#08264a] p-5 text-white shadow-sm">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-black uppercase tracking-widest text-slate-300">
            Boarded Today
          </p>
          <h3 className="mt-5 text-2xl font-black">
            {usedCount} <span className="text-base font-medium text-slate-300">tickets confirmed</span>
          </h3>
        </div>

        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-white/10">
          <Bus size={32} />
        </div>
      </div>
    </div>
  );
}

function TabButton({ active, icon, label, onClick }) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center justify-center gap-2 rounded-xl px-4 py-4 text-sm font-black transition ${
        active
          ? "bg-[#08264a] text-white"
          : "text-slate-500 hover:bg-slate-100 hover:text-[#08264a]"
      }`}
    >
      {icon}
      {label}
    </button>
  );
}

function VerifyInfo({ label, value }) {
  return (
    <div className="rounded-xl bg-slate-50 p-4">
      <p className="text-xs font-black uppercase tracking-widest text-slate-500">
        {label}
      </p>
      <p className="mt-2 font-black text-slate-900">{value || "N/A"}</p>
    </div>
  );
}

function Instruction({ title, desc }) {
  return (
    <div className="rounded-xl bg-slate-50 p-4">
      <h3 className="text-sm font-black text-slate-900">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-slate-500">{desc}</p>
    </div>
  );
}