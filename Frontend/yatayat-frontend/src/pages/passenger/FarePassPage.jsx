import { useCallback, useEffect, useMemo, useState } from "react";
import { CheckCircle, Clock, Loader2, QrCode, Route, ShieldCheck, Wallet } from "lucide-react";
import { QRCodeSVG } from "qrcode.react";
import { useLocation, useNavigate } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import { useAuth } from "../../hooks/useAuth";
import { useLanguage } from "../../hooks/useLanguage";
import { useNotifications } from "../../context/NotificationContext";
import { apiFetch } from "../../utils/api";
import { getLocalFarePasses, getLocalFareQuote, purchaseLocalFarePass } from "../../utils/localFarePasses";
import { localRouteRequest } from "../../utils/localRoutes";

export default function FarePassPage() {
  const { t } = useLanguage();
  const { user } = useAuth();
  const { refreshUnreadCount } = useNotifications();
  const userId = user?.id;
  const navigate = useNavigate();
  const location = useLocation();
  const [routes, setRoutes] = useState([]);
  const [routeId, setRouteId] = useState(String(location.state?.routeId || ""));
  const [boardingStopId, setBoardingStopId] = useState(String(location.state?.boardingStopId || ""));
  const [destinationStopId, setDestinationStopId] = useState(String(location.state?.destinationStopId || ""));
  const [quote, setQuote] = useState(null);
  const [passes, setPasses] = useState([]);
  const [balance, setBalance] = useState(0);
  const [walletPin, setWalletPin] = useState("");
  const [loading, setLoading] = useState(true);
  const [quoting, setQuoting] = useState(false);
  const [purchasing, setPurchasing] = useState(false);
  const [error, setError] = useState("");
  const [selectedPass, setSelectedPass] = useState(null);

  const selectedRoute = useMemo(
    () => routes.find((route) => String(route.routeId) === routeId),
    [routeId, routes],
  );
  const boarding = selectedRoute?.orderedStops?.find(
    (stop) => String(stop.busStopId) === boardingStopId,
  );
  const destinationOptions = (selectedRoute?.orderedStops || []).filter(
    (stop) => boarding && stop.stopOrder > boarding.stopOrder,
  );
  const activePass = passes.find((pass) => pass.status === "VALID");

  const load = useCallback(async () => {
    try {
      const [routeData, passData, balanceResponse] = await Promise.all([
        localRouteRequest("/api/passenger/local-routes"),
        getLocalFarePasses(),
        apiFetch(`/api/wallet/balance/${userId}`),
      ]);
      if (!balanceResponse.ok) throw new Error("Wallet balance could not be loaded.");
      setRoutes(routeData);
      setPasses(passData);
      setBalance(Number(await balanceResponse.text()) || 0);
      setSelectedPass((current) => current || passData.find((pass) => pass.status === "VALID") || null);
    } catch (loadError) {
      if (loadError.status === 401) navigate("/login", { replace: true });
      else setError(loadError.message || "Local fare passes could not be loaded.");
    } finally {
      setLoading(false);
    }
  }, [navigate, userId]);

  useEffect(() => {
    Promise.resolve().then(load);
  }, [load]);

  useEffect(() => {
    if (!routeId || !boardingStopId || !destinationStopId) return;
    Promise.resolve().then(() => {
      setQuote(null);
      setQuoting(true);
      return getLocalFareQuote({
        routeId: Number(routeId),
        boardingStopId: Number(boardingStopId),
        destinationStopId: Number(destinationStopId),
      })
        .then(setQuote)
        .catch((requestError) => setError(requestError.message))
        .finally(() => setQuoting(false));
    });
  }, [routeId, boardingStopId, destinationStopId]);

  const purchase = async (event) => {
    event.preventDefault();
    if (!quote || purchasing) return;
    setPurchasing(true);
    setError("");
    try {
      const pass = await purchaseLocalFarePass({
        routeId: Number(routeId),
        boardingStopId: Number(boardingStopId),
        destinationStopId: Number(destinationStopId),
        walletPin,
      });
      setWalletPin("");
      setSelectedPass(pass);
      await Promise.all([load(), refreshUnreadCount()]);
    } catch (requestError) {
      setError(requestError.message || "Local fare pass could not be purchased.");
    } finally {
      setPurchasing(false);
    }
  };

  return (
    <PassengerLayout activePage="Routes" title={t("passenger.farePass.title")} subtitle={t("passenger.farePass.subtitle")}>
      {loading ? <div className="flex min-h-72 items-center justify-center"><Loader2 className="animate-spin text-[#08264a]" size={42} /></div> : (
        <div className="grid grid-cols-1 gap-5 xl:grid-cols-12">
          <section className="space-y-5 xl:col-span-7">
            <div className="rounded-2xl bg-[#08264a] p-5 text-white shadow-sm">
              <div className="flex items-start justify-between gap-4">
                <div><p className="text-xs font-black uppercase tracking-widest text-slate-300">{t("passenger.farePass.walletBalance")}</p><h2 className="mt-2 text-4xl font-black">NPR {balance.toLocaleString(undefined, { minimumFractionDigits: 2 })}</h2><p className="mt-1 text-sm text-slate-300">{t("passenger.farePass.availableBalance")}</p></div>
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/15"><Wallet size={26} /></div>
              </div>
            </div>

            <form onSubmit={purchase} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="mb-4 flex items-center gap-3"><div className="rounded-xl bg-emerald-100 p-3 text-emerald-700"><Route size={21} /></div><div><h2 className="text-xl font-black">{t("passenger.farePass.buyTitle")}</h2><p className="text-sm text-slate-500">{t("passenger.farePass.buyDescription")}</p></div></div>
              <div className="grid gap-4">
                <Select label={t("passenger.farePass.localRoute")} value={routeId} onChange={(value) => { setRouteId(value); setBoardingStopId(""); setDestinationStopId(""); }} options={routes.map((route) => ({ value: route.routeId, label: `${route.routeCode} · ${route.routeName}` }))} />
                <div className="grid gap-4 sm:grid-cols-2">
                  <Select label={t("passenger.farePass.boardingStop")} value={boardingStopId} onChange={(value) => { setBoardingStopId(value); setDestinationStopId(""); }} options={(selectedRoute?.orderedStops || []).slice(0, -1).map((stop) => ({ value: stop.busStopId, label: stop.stopName }))} />
                  <Select label={t("passenger.farePass.destinationStop")} value={destinationStopId} onChange={setDestinationStopId} options={destinationOptions.map((stop) => ({ value: stop.busStopId, label: stop.stopName }))} />
                </div>
                {quoting && <p className="text-sm font-bold text-slate-500">Calculating fare…</p>}
                {quote && <div className="rounded-xl bg-slate-50 p-4"><p className="font-black">{quote.boardingStopName} → {quote.destinationStopName}</p><p className="mt-1 text-3xl font-black text-emerald-700">NPR {quote.fare}</p></div>}
                <label className="text-xs font-black uppercase tracking-wide text-slate-500">Wallet PIN<input type="password" inputMode="numeric" maxLength={4} value={walletPin} onChange={(event) => setWalletPin(event.target.value.replace(/\D/g, ""))} placeholder="4-digit PIN" className="mt-2 w-full rounded-xl border border-slate-300 px-4 py-3 text-base font-black tracking-[0.35em] outline-none focus:border-[#08264a]" /></label>
                {error && <p className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700">{error}</p>}
                <button type="submit" disabled={!quote || walletPin.length !== 4 || purchasing} className="flex items-center justify-center gap-2 rounded-xl bg-[#08264a] py-3 text-sm font-black text-white disabled:opacity-50">{purchasing ? <Loader2 className="animate-spin" size={18} /> : <QrCode size={18} />}{purchasing ? t("passenger.farePass.issuing") : t("passenger.farePass.payAndGenerate")}</button>
              </div>
            </form>

            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-xl font-black">{t("passenger.farePass.myLocalTickets")}</h2>
              {passes.length === 0 ? <p className="mt-4 rounded-xl bg-slate-50 p-4 text-sm font-bold text-slate-500">{t("passenger.farePass.noPasses")}</p> : <div className="mt-4 space-y-3">{passes.map((pass) => <button type="button" key={pass.passNumber} onClick={() => setSelectedPass(pass)} className="flex w-full items-center justify-between rounded-xl border border-slate-200 p-4 text-left"><span><span className="block font-black">{pass.boardingStopName} → {pass.destinationStopName}</span><span className="text-xs font-semibold text-slate-500">{pass.passNumber}</span></span><Status value={pass.status} /></button>)}</div>}
            </section>
          </section>

          <aside className="xl:col-span-5">
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm xl:sticky xl:top-6">
              <div className="flex justify-between"><div><h2 className="text-xl font-black">QR Fare Pass</h2><p className="text-sm text-slate-500">Show this secure QR to your driver.</p></div><ShieldCheck className="text-emerald-700" /></div>
              {selectedPass?.qrPayload ? <div className="mt-5 text-center"><div className="inline-block rounded-2xl bg-white p-3 shadow-lg"><QRCodeSVG value={selectedPass.qrPayload} size={220} level="M" includeMargin /></div><div className="mt-4 space-y-2 text-left"><Info label="Pass" value={selectedPass.passNumber} /><Info label="Route" value={`${selectedPass.boardingStopName} → ${selectedPass.destinationStopName}`} /><Info label="Fare" value={`NPR ${selectedPass.fare}`} /><Info label="Valid until" value={formatDate(selectedPass.validUntil)} /><Info label="Status" value={selectedPass.status} /></div>{selectedPass.status === "VALID" && <p className="mt-4 flex items-center justify-center gap-2 rounded-xl bg-emerald-50 p-3 text-sm font-black text-emerald-700"><CheckCircle size={18} />Active for one scan</p>}</div> : <div className="mt-5 flex h-56 flex-col items-center justify-center rounded-2xl border-2 border-dashed border-slate-300 bg-slate-50 text-center"><QrCode size={46} className="text-slate-400" /><p className="mt-3 text-sm font-bold text-slate-500">{activePass ? "Select a pass to display its QR." : "Purchase a pass to generate its secure QR."}</p></div>}
              <div className="mt-4 flex items-center gap-2 rounded-xl bg-slate-50 p-4 text-sm font-bold text-slate-600"><Clock size={17} />Valid for 24 hours or one successful scan.</div>
            </div>
          </aside>
        </div>
      )}
    </PassengerLayout>
  );
}

function Select({ label, value, onChange, options }) {
  return <label className="text-xs font-black uppercase tracking-wide text-slate-500">{label}<select required value={value} onChange={(event) => onChange(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-4 py-3 font-bold text-slate-900"><option value="">Select</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>;
}
function Info({ label, value }) { return <div className="flex justify-between gap-4 border-b border-slate-100 pb-2 text-sm"><span className="text-slate-500">{label}</span><span className="text-right font-black">{value}</span></div>; }
function Status({ value }) { const tone = value === "VALID" ? "bg-emerald-100 text-emerald-700" : value === "USED" ? "bg-blue-100 text-blue-700" : "bg-slate-100 text-slate-600"; return <span className={`rounded-full px-3 py-1 text-xs font-black ${tone}`}>{value}</span>; }
function formatDate(value) { const date = new Date(value); return Number.isNaN(date.getTime()) ? value : date.toLocaleString(); }
