import { useEffect, useRef, useState } from "react";
import {
  Bus,
  CheckCircle,
  ChevronDown,
  Keyboard,
  Loader2,
  QrCode,
  RefreshCw,
  ShieldCheck,
  XCircle,
} from "lucide-react";
import { Html5Qrcode } from "html5-qrcode";
import { useNavigate } from "react-router-dom";
import DriverLayout from "../../components/layout/DriverLayout";
import { manifestIdFromReference, validateDriverTicket } from "../../utils/driverTickets";
import { validateDriverLocalFarePass } from "../../utils/localFarePasses";

const scannerRegionId = "driver-qr-reader";

export default function DriverScannerPage() {
  const navigate = useNavigate();
  const scannerRef = useRef(null);
  const validatingRef = useRef(false);
  const lastPayloadRef = useRef("");
  const [cameraState, setCameraState] = useState("idle");
  const [manualOpen, setManualOpen] = useState(false);
  const [manualPayload, setManualPayload] = useState("");
  const [manualError, setManualError] = useState("");
  const [result, setResult] = useState(null);
  const [recentScans, setRecentScans] = useState([]);

  const stopCamera = async () => {
    const scanner = scannerRef.current;
    if (!scanner) return;
    try {
      if (scanner.getState && scanner.getState() === 2) {
        await scanner.stop();
      }
      await scanner.clear();
    } catch {
      // Camera cleanup can fail if the stream already stopped.
    } finally {
      scannerRef.current = null;
      setCameraState("idle");
    }
  };

  useEffect(() => () => { stopCamera(); }, []);

  const startCamera = async () => {
    setResult(null);
    setCameraState("permission");
    lastPayloadRef.current = "";

    try {
      const scanner = new Html5Qrcode(scannerRegionId);
      scannerRef.current = scanner;
      await scanner.start(
        { facingMode: "environment" },
        { fps: 8, qrbox: { width: 260, height: 260 } },
        async (decodedText) => {
          if (validatingRef.current || decodedText === lastPayloadRef.current) return;
          lastPayloadRef.current = decodedText;
          await stopCamera();
          await validatePayload(decodedText, "camera");
        },
        () => {}
      );
      setCameraState("scanning");
    } catch (error) {
      console.error("Camera scanner error:", error);
      setCameraState("denied");
      setResult({
        result: "CAMERA_DENIED",
        message: "Camera could not be started. Use the manual QR payload fallback if needed.",
      });
    }
  };

  const validatePayload = async (qrPayload, source = "manual") => {
    if (!qrPayload?.trim() || validatingRef.current) return;

    if (source === "manual") {
      const validation = validateManualQrPayload(qrPayload);
      if (!validation.valid) {
        setManualError(validation.message);
        setResult({
          result: "INVALID_QR",
          message: validation.message,
        });
        return;
      }
      setManualError("");
    }

    validatingRef.current = true;
    setCameraState("validating");
    setResult(null);

    try {
      const parsed = JSON.parse(qrPayload.trim());
      const data = parsed.type === "LOCAL_FARE_PASS"
        ? await validateDriverLocalFarePass(qrPayload.trim())
        : await validateDriverTicket(qrPayload.trim());
      setResult(data);
      rememberScan(data, source);
    } catch (error) {
      const data = error.data || {};
      const failure = {
        result: data.result || (error.status === 401 ? "SESSION_EXPIRED" : "ERROR"),
        message: data.message || "Ticket validation failed. Please try again.",
      };
      setResult(failure);
      rememberScan(failure, source);
    } finally {
      validatingRef.current = false;
      setCameraState("idle");
    }
  };

  const rememberScan = (scanResult, source) => {
    setRecentScans((current) => [
      {
        result: scanResult.result,
        ticketNumber: scanResult.ticketNumber || scanResult.passNumber,
        passengerName: scanResult.passengerName,
        time: new Date().toLocaleTimeString(),
        source,
      },
      ...current.slice(0, 5),
    ]);
  };

  const reset = () => {
    setResult(null);
    setManualPayload("");
    setManualError("");
    lastPayloadRef.current = "";
  };

  const pastePayload = async () => {
    try {
      const text = await navigator.clipboard.readText();
      setManualPayload(text);
      const validation = validateManualQrPayload(text);
      setManualError(validation.valid ? "" : validation.message);
    } catch {
      setManualError("Clipboard access was blocked. Paste the full QR payload into the box manually.");
    }
  };

  const manifestId = manifestIdFromReference(result?.scheduledTripReference);

  return (
    <DriverLayout activePage="Scanner">
      <header className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900 sm:text-2xl">Boarding QR Scanner</h1>
          <p className="mt-1 text-sm text-slate-600">
            Scan passenger e-ticket QR codes. Final validation is performed securely by the backend.
          </p>
        </div>
        <span className="w-fit rounded-full bg-emerald-100 px-3 py-1.5 text-[11px] font-semibold uppercase tracking-wide text-emerald-700">
          Driver controlled
        </span>
      </header>

      <div className="grid items-start gap-4 lg:grid-cols-12">
        <section className="space-y-4 lg:col-span-7">
          <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
            <div className="bg-[#08264a] p-4 text-white">
              <div className="flex items-center gap-3">
                <div className="rounded-xl bg-white/10 p-2.5"><QrCode size={24} /></div>
                <div>
                  <h2 className="text-lg font-semibold">Camera Scanner</h2>
                  <p className="text-sm text-blue-100">Camera starts only when you press Start Scanning.</p>
                </div>
              </div>
            </div>

            <div className="p-4">
              <div className="min-h-56 overflow-hidden rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50 p-3 sm:min-h-64">
                <div id={scannerRegionId} className="mx-auto max-w-full overflow-hidden rounded-2xl" />
                {cameraState !== "scanning" && (
                  <div className="flex min-h-56 flex-col items-center justify-center text-center sm:min-h-64">
                    {cameraState === "permission" || cameraState === "validating" ? (
                      <Loader2 size={36} className="animate-spin text-[#08264a]" />
                    ) : (
                      <QrCode size={40} className="text-slate-300" />
                    )}
                    <h3 className="mt-3 text-base font-semibold text-slate-900">
                      {cameraState === "permission" ? "Requesting camera permission" : cameraState === "validating" ? "Validating ticket" : "Ready to scan"}
                    </h3>
                    <p className="mt-1.5 max-w-sm text-sm text-slate-500">
                      Place the passenger ticket QR inside the frame. The camera stops automatically after detection.
                    </p>
                  </div>
                )}
              </div>

              <div className="mt-4 flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  onClick={startCamera}
                  disabled={cameraState === "permission" || cameraState === "scanning" || cameraState === "validating"}
                  className="inline-flex h-11 w-fit items-center justify-center gap-2 rounded-xl bg-[#08264a] px-4 text-sm font-semibold text-white transition hover:bg-[#0d3566] disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {cameraState === "permission" ? <Loader2 size={18} className="animate-spin" /> : <QrCode size={18} />}
                  Start Scanning
                </button>
                <button
                  type="button"
                  onClick={stopCamera}
                  disabled={cameraState !== "scanning"}
                  className="inline-flex h-11 w-fit items-center justify-center rounded-xl border border-slate-300 px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Stop Camera
                </button>
              </div>
            </div>
          </div>

          {import.meta.env.DEV && <section className="rounded-3xl border border-amber-200 bg-amber-50/40 p-5 shadow-sm">
            <button
              type="button"
              onClick={() => setManualOpen((open) => !open)}
              className="flex w-full items-center justify-between gap-3 text-left"
            >
              <span className="flex items-center gap-3 font-black text-slate-900">
                <Keyboard size={20} />
                Development / Demo Manual QR Validation
              </span>
              <ChevronDown className={`transition ${manualOpen ? "rotate-180" : ""}`} />
            </button>

            {manualOpen && (
              <div className="mt-5">
                <p className="text-sm text-slate-500">
                  Webcam fallback for local testing. Paste the complete QR payload copied from the passenger ticket.
                  Ticket numbers, booking references, URLs, passenger IDs, and trip IDs are not accepted here.
                </p>
                <textarea
                  value={manualPayload}
                  onChange={(event) => {
                    setManualPayload(event.target.value);
                    if (manualError) setManualError("");
                  }}
                  rows={7}
                  className="mt-4 min-h-40 w-full resize-y rounded-2xl border border-slate-300 bg-slate-50 p-4 font-mono text-sm font-semibold text-slate-700 outline-none transition focus:border-[#08264a] focus:bg-white focus:ring-4 focus:ring-blue-100"
                  placeholder={`{
  "version": 1,
  "ticketNumber": "YT-TKT-20260718-53024F",
  "token": "..."
}`}
                />
                {manualError && <p className="mt-3 rounded-2xl border border-red-100 bg-red-50 p-3 text-sm font-bold text-red-700">{manualError}</p>}
                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  <button
                    type="button"
                    onClick={pastePayload}
                    disabled={cameraState === "validating"}
                    className="flex items-center justify-center gap-2 rounded-2xl border border-slate-300 bg-white px-5 py-3 text-sm font-black text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    <Keyboard size={18} />
                    Paste Full QR Payload
                  </button>
                <button
                  type="button"
                  onClick={() => validatePayload(manualPayload)}
                  disabled={!manualPayload.trim() || cameraState === "validating"}
                  className="flex items-center justify-center gap-2 rounded-2xl bg-[#08264a] px-5 py-3 text-sm font-black text-white transition hover:bg-[#0d3566] disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {cameraState === "validating" ? <Loader2 size={18} className="animate-spin" /> : <ShieldCheck size={18} />}
                  Validate Ticket
                </button>
                </div>
              </div>
            )}
          </section>}
        </section>

        <aside className="space-y-4 lg:col-span-5">
          <ResultCard result={result} onReset={reset} onManifest={() => manifestId && navigate(`/driver/trips/${manifestId}/manifest`)} hasManifest={Boolean(manifestId)} />
          <RecentScans scans={recentScans} />
        </aside>
      </div>
    </DriverLayout>
  );
}

function ResultCard({ result, onReset, onManifest, hasManifest }) {
  if (!result) {
    return (
      <div className="rounded-3xl border border-slate-200 bg-white p-5 text-center shadow-sm">
        <QrCode size={32} className="mx-auto text-slate-300" />
        <h2 className="mt-3 text-lg font-semibold text-slate-900">Waiting for ticket</h2>
        <p className="mt-1 text-sm text-slate-500">Scan a QR code or paste the full QR payload.</p>
      </div>
    );
  }

  const success = result.result === "VALID";
  const alreadyUsed = result.result === "ALREADY_USED";
  const color = success ? "emerald" : alreadyUsed ? "amber" : "red";

  return (
    <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
      <div className={`${color === "emerald" ? "bg-emerald-600" : color === "amber" ? "bg-amber-500" : "bg-red-600"} p-4 text-white`}>
        <div className="flex items-center gap-3">
          {success ? <CheckCircle size={24} /> : <XCircle size={24} />}
          <div>
            <h2 className="text-lg font-semibold">
              {success ? "Boarding Confirmed" : alreadyUsed ? "Already Used" : readableResult(result.result)}
            </h2>
            <p className="mt-1 text-sm opacity-90">{result.message}</p>
          </div>
        </div>
      </div>

      <div className="space-y-2 p-4">
        {(result.ticketNumber || result.passNumber) && (
          <>
            <Info label="Passenger" value={result.passengerName} />
            <Info label="Route" value={result.passNumber ? `${result.boardingStopName || ""} → ${result.destinationStopName || ""}` : `${result.route?.origin || ""} → ${result.route?.destination || ""}`} />
            {result.ticketNumber && <Info label="Seats" value={(result.seatNumbers || []).join(", ")} />}
            <Info label={result.passNumber ? "Local Fare Pass" : "Ticket"} value={result.passNumber || result.ticketNumber} />
            {result.fare != null && <Info label="Fare" value={`NPR ${result.fare}`} />}
            <Info label="Boarded At" value={formatDateTime(result.boardedAt || result.usedAt)} />
          </>
        )}

        <div className="mt-2 flex flex-wrap items-center justify-center gap-2">
        <button
          type="button"
          onClick={onReset}
          className="inline-flex h-11 w-fit items-center justify-center gap-2 rounded-xl bg-[#08264a] px-4 text-sm font-semibold text-white transition hover:bg-[#0d3566]"
        >
          <RefreshCw size={18} />
          Scan Next Ticket
        </button>
        {hasManifest && (
          <button
            type="button"
            onClick={onManifest}
            className="inline-flex h-11 w-fit items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
          >
            <Bus size={18} />
            View Manifest
          </button>
        )}
        </div>
      </div>
    </div>
  );
}

function RecentScans({ scans }) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
      <h2 className="text-base font-semibold text-slate-900">Recent Scan Results</h2>
      <div className="mt-3 space-y-2">
        {scans.length === 0 ? (
          <p className="text-sm text-slate-500">No scan attempts yet.</p>
        ) : scans.map((scan, index) => (
          <div key={`${scan.time}-${index}`} className="min-w-0 rounded-xl bg-slate-50 p-3">
            <div className="flex min-w-0 items-start justify-between gap-3">
              <p className="safe-wrap min-w-0 font-semibold text-slate-900">{scan.ticketNumber || readableResult(scan.result)}</p>
              <span className={`shrink-0 text-xs font-medium ${scan.result === "VALID" ? "text-emerald-700" : "text-red-600"}`}>{scan.result}</span>
            </div>
            <p className="mt-1 break-words text-xs text-slate-500">{scan.time} · {scan.source}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

function Info({ label, value }) {
  return (
    <div className="rounded-xl bg-slate-50 p-3">
      <p className="text-xs font-medium uppercase tracking-widest text-slate-500">{label}</p>
      <p className="mt-0.5 break-words font-semibold text-slate-900">{value || "Not available"}</p>
    </div>
  );
}

function readableResult(value) {
  return String(value || "ERROR").replaceAll("_", " ");
}

function formatDateTime(value) {
  if (!value) return "Not available";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function validateManualQrPayload(payload) {
  const trimmed = payload?.trim();
  if (!trimmed) {
    return { valid: false, message: "Paste the full QR payload before validating." };
  }
  if (/^https?:\/\//i.test(trimmed)) {
    return { valid: false, message: "A ticket page URL is not valid. Paste the QR payload JSON copied from the passenger ticket." };
  }
  if (/^(YT-TKT|YAT-|TRIP-)/i.test(trimmed)) {
    return { valid: false, message: "Only the full QR payload is accepted, not a ticket number, booking reference, or trip ID." };
  }
  try {
    const parsed = JSON.parse(trimmed);
    const keys = Object.keys(parsed).sort();
    const scheduledFields = ["ticketNumber", "token", "version"];
    const localFields = ["passNumber", "token", "type", "version"];
    const matchesFields = (expected) => keys.length === expected.length
      && expected.every((key, index) => keys[index] === key);
    const scheduled = matchesFields(scheduledFields) && parsed.ticketNumber;
    const local = matchesFields(localFields)
      && parsed.type === "LOCAL_FARE_PASS" && parsed.passNumber;
    if (parsed.version !== 1 || !parsed.token || (!scheduled && !local)) {
      return {
        valid: false,
        message: "Paste a complete scheduled-ticket or local fare-pass QR payload.",
      };
    }
    return { valid: true, message: "" };
  } catch {
    return { valid: false, message: "The manual QR value must be valid JSON copied exactly from the passenger ticket QR payload." };
  }
}
