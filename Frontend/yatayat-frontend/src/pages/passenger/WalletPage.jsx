import { useEffect, useRef, useState } from "react";
import {
  Wallet,
  PlusCircle,
  Smartphone,
  CheckCircle,
  Clock,
  ArrowDownLeft,
  ArrowUpRight,
  ShieldCheck,
  Download,
  Eye,
  EyeOff,
  LockKeyhole,
} from "lucide-react";
import PassengerLayout from "../../components/layout/PassengerLayout";
import WalletActivationModal from "../../components/passenger/WalletActivationModal";
import { useLanguage } from "../../hooks/useLanguage";
import { useWalletBalanceVisibility } from "../../hooks/useWalletBalanceVisibility";
import { apiFetch } from "../../utils/api";
import {
  initiateWalletTopUp,
  submitEsewaWalletTopUp,
} from "../../utils/walletTopUps";

export default function WalletPage() {
  const { t } = useLanguage();
  const user = JSON.parse(localStorage.getItem("yatayatUser"));

  const [selectedAmount, setSelectedAmount] = useState(500);
  const [customAmount, setCustomAmount] = useState("");
  const [selectedMethod, setSelectedMethod] = useState("KHALTI");

  const [balance, setBalance] = useState(0);
  const [transactions, setTransactions] = useState([]);
  const [pinStatus, setPinStatus] = useState("");
  const [walletError, setWalletError] = useState("");
  const [processing, setProcessing] = useState(false);
  const { showBalance, toggleBalance } = useWalletBalanceVisibility();

  const success = new URLSearchParams(window.location.search).get("topup") === "success";
  const [showPinModal, setShowPinModal] = useState(false);
  const topUpRef = useRef(null);
  const historyRef = useRef(null);

  const finalAmount = customAmount ? Number(customAmount) : selectedAmount;
  const paymentMethods = getPaymentMethods(t);

  const loadWallet = async () => {
    try {
      setWalletError("");
      const balanceRes = await apiFetch(`/api/wallet/balance/${user.id}`);
      if (!balanceRes.ok) throw new Error(walletErrorMessage(balanceRes.status, t));
      const balanceText = await balanceRes.text();
      setBalance(Number(balanceText) || 0);

      const historyRes = await apiFetch(`/api/wallet/history/${user.id}`);
      if (!historyRes.ok) throw new Error(walletErrorMessage(historyRes.status, t));
      const historyData = await historyRes.json().catch(() => []);
      setTransactions(Array.isArray(historyData) ? historyData : []);
    } catch (error) {
      setBalance(0);
      setTransactions([]);
      setWalletError(error.message || t("passenger.wallet.loadError"));
    }
  };

  const checkPinStatus = async () => {
    try {
      const res = await apiFetch(`/api/wallet/pin-status/${user.id}`);
      if (!res.ok) throw new Error(walletErrorMessage(res.status, t));
      setPinStatus(await res.text());
    } catch (error) {
      setPinStatus("");
      setWalletError(error.message || t("passenger.wallet.pinStatusLoadError"));
    }
  };

  const handleRecharge = async () => {
    if (pinStatus !== "PIN_SET") {
      setWalletError("Activate your wallet before topping up.");
      return;
    }

    if (!finalAmount || finalAmount < 100 || finalAmount > 50000 || processing) {
      setWalletError("Top-up amount must be between NPR 100.00 and NPR 50,000.00.");
      return;
    }

    await rechargeWallet();
  };

  useEffect(() => {
    Promise.resolve().then(() => {
      loadWallet();
      checkPinStatus();
    });
    // Wallet data should load once for the authenticated user on page entry.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const rechargeWallet = async () => {
    try {
      setProcessing(true);
      setWalletError("");
      const initiation = await initiateWalletTopUp(selectedMethod, finalAmount);
      if (selectedMethod === "ESEWA") {
        submitEsewaWalletTopUp(initiation);
        return;
      }
      if (!initiation.redirectUrl) throw new Error("Khalti did not return a checkout URL.");
      window.location.assign(initiation.redirectUrl);
    } catch (error) {
      if (error.status === 401) {
        window.location.assign("/login");
        return;
      }
      setWalletError(error.message || "Unable to start the wallet top-up.");
      setProcessing(false);
    }
  };

  const openPinModal = () => {
    setWalletError("");
    setShowPinModal(true);
  };

  const handleWalletActivated = async () => {
    setPinStatus("PIN_SET");
    await loadWallet();
  };

  return (
    <PassengerLayout activePage="Wallet" title={t("passenger.wallet.pageTitle")} subtitle={t("passenger.wallet.pageSubtitle")}>
      <div className="mb-5 flex justify-end">
        <button className="flex w-full items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-black hover:bg-slate-50 sm:w-auto">
          <Download size={17} />
          {t("passenger.wallet.exportStatement")}
        </button>
      </div>

      {success && (
        <div className="mb-5 flex items-center gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-bold text-emerald-700">
          <CheckCircle size={19} />
          {t("passenger.wallet.rechargeSuccess")}
        </div>
      )}

      {walletError && (
        <div className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700">
          {walletError}
        </div>
      )}

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-12">
        <section className="space-y-5 xl:col-span-7">
          <div className="rounded-3xl bg-[#08264a] p-6 text-white shadow-lg">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-black uppercase tracking-widest text-slate-300">
                  {t("passenger.wallet.availableBalance")}
                </p>

                <div className="mt-3 flex items-center gap-3">
                  <h2 className="text-4xl font-black sm:text-5xl">
                    {showBalance
                      ? `NPR ${balance.toLocaleString(undefined, {
                          minimumFractionDigits: 2,
                          maximumFractionDigits: 2,
                        })}`
                      : "NPR ••••••"}
                  </h2>
                  <button
                    type="button"
                    onClick={toggleBalance}
                    aria-label={showBalance ? "Hide wallet balance" : "Show wallet balance"}
                    className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white/10 text-slate-200 transition hover:bg-white/20 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
                  >
                    {showBalance ? <EyeOff size={20} /> : <Eye size={20} />}
                  </button>
                </div>

                <p className="mt-3 text-sm text-slate-300">
                  {t("passenger.wallet.walletStatus")}:{" "}
                  <span className={pinStatus === "PIN_SET" ? "font-black text-emerald-300" : "font-black text-yellow-300"}>
                    {pinStatus === "PIN_SET" ? t("common.active") : t("common.inactive")}
                  </span>
                </p>
              </div>

              <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white/15">
                <Wallet size={30} />
              </div>
            </div>

            {pinStatus !== "PIN_SET" && (
              <div className="mt-6 rounded-2xl bg-white/10 p-4">
                <p className="text-sm font-bold">
                  {t("passenger.wallet.activationPrompt")}
                </p>
              </div>
            )}

            <div className="mt-6 flex flex-wrap items-center gap-3 border-t border-white/15 pt-5">
              {pinStatus === "PIN_SET" ? (
                <button
                  type="button"
                  onClick={() => topUpRef.current?.scrollIntoView({ behavior: "smooth" })}
                  className="rounded-xl bg-white px-4 py-2.5 text-sm font-black text-[#08264a] transition hover:bg-slate-100"
                >
                  {t("passenger.wallet.rechargeWallet")}
                </button>
              ) : (
                <button
                  type="button"
                  onClick={openPinModal}
                  className="rounded-xl bg-white px-4 py-2.5 text-sm font-black text-[#08264a] transition hover:bg-slate-100"
                >
                  {t("passenger.wallet.activateWallet")}
                </button>
              )}
              <button
                type="button"
                onClick={() => historyRef.current?.scrollIntoView({ behavior: "smooth" })}
                className="rounded-xl border border-white/30 px-4 py-2.5 text-sm font-black text-white transition hover:bg-white/10"
              >
                {t("passenger.wallet.transactions")}
              </button>
              <span className={`rounded-full px-3 py-2 text-xs font-black ${
                pinStatus === "PIN_SET"
                  ? "bg-emerald-400/20 text-emerald-200"
                  : "bg-yellow-400/20 text-yellow-200"
              }`}>
                {pinStatus === "PIN_SET" ? t("common.active") : t("common.inactive")}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <WalletStat label={t("passenger.wallet.walletStatus")} value={pinStatus === "PIN_SET" ? t("common.active") : t("common.inactive")} />
            <WalletStat label={t("passenger.wallet.transactions")} value={(Array.isArray(transactions) ? transactions : []).length} />
            <WalletStat
              label={t("passenger.wallet.ticketPayments")}
              value={(Array.isArray(transactions) ? transactions : []).filter((t) => t.type === "TICKET_PAYMENT").length}
            />
          </div>

          <div ref={topUpRef} className="scroll-mt-24 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            {pinStatus !== "PIN_SET" ? (
              <div className="flex min-h-64 flex-col items-center justify-center text-center">
                <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
                  <LockKeyhole size={26} />
                </div>
                <h2 className="mt-4 text-xl font-black text-slate-900">
                  {t("passenger.wallet.activateWallet")}
                </h2>
                <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">
                  {t("passenger.wallet.activationPrompt")}
                </p>
                <button
                  type="button"
                  onClick={openPinModal}
                  className="mt-5 rounded-xl bg-[#08264a] px-6 py-3 text-sm font-black text-white transition hover:bg-[#0d3566]"
                >
                  {t("passenger.wallet.activateWallet")}
                </button>
              </div>
            ) : (
              <>
            <div className="mb-5 flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                <PlusCircle size={21} />
              </div>

              <div>
                <h2 className="text-xl font-black text-slate-900">
                  {t("passenger.wallet.rechargeWallet")}
                </h2>
                <p className="text-sm text-slate-500">
                  {t("passenger.wallet.rechargeSubtitle")}
                </p>
              </div>
            </div>

            <p className="mb-3 text-xs font-black uppercase tracking-widest text-slate-500">
              {t("passenger.wallet.selectAmount")}
            </p>

            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              {[100, 200, 500, 1000].map((amount) => (
                <button
                  key={amount}
                  onClick={() => {
                    setSelectedAmount(amount);
                    setCustomAmount("");
                  }}
                  className={`rounded-xl border px-4 py-3 text-sm font-black transition ${
                    selectedAmount === amount && !customAmount
                      ? "border-[#08264a] bg-[#08264a] text-white"
                      : "border-slate-300 bg-slate-50 hover:border-[#08264a]"
                  }`}
                >
                  NPR {amount}
                </button>
              ))}
            </div>

            <input
              type="number"
              placeholder={t("passenger.wallet.customAmountPlaceholder")}
              value={customAmount}
              onChange={(e) => setCustomAmount(e.target.value)}
              className="mt-4 w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm font-bold outline-none focus:border-[#08264a]"
            />

            <div className="mt-5 grid grid-cols-1 gap-3 md:grid-cols-2">
              {paymentMethods.map((method) => (
                <button
                  key={method.id}
                  onClick={() => setSelectedMethod(method.id)}
                  className={`flex items-center gap-3 rounded-xl border p-4 text-left transition hover:shadow-sm ${
                    selectedMethod === method.id
                      ? "border-[#08264a] bg-slate-100"
                      : "border-slate-200 bg-white"
                  }`}
                >
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                    {method.icon}
                  </div>

                  <div>
                    <h3 className="text-sm font-black text-slate-900">
                      {method.name}
                    </h3>
                    <p className="text-xs text-slate-500">{method.desc}</p>
                  </div>
                </button>
              ))}
            </div>

            <div className="mt-5 rounded-xl bg-slate-50 p-4">
              <div className="flex justify-between text-sm">
                <span className="text-slate-500">{t("passenger.wallet.rechargeAmount")}</span>
                <span className="font-black">NPR {finalAmount || 0}</span>
              </div>

              <div className="mt-3 flex justify-between border-t border-slate-200 pt-3">
                <span className="font-black">{t("passenger.wallet.totalPayable")}</span>
                <span className="font-black text-emerald-700">
                  NPR {finalAmount || 0}
                </span>
              </div>
            </div>

            <button
              onClick={handleRecharge}
              disabled={processing || pinStatus !== "PIN_SET"}
              className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] py-3 text-sm font-black text-white transition hover:bg-[#0d3566] disabled:cursor-not-allowed disabled:opacity-60"
            >
              <PlusCircle size={18} />
              {processing
                ? "Opening secure checkout…"
                : t("passenger.wallet.rechargeWallet")}
            </button>
              </>
            )}
          </div>
        </section>

        <aside className="space-y-5 xl:col-span-5">
          <div ref={historyRef} className="scroll-mt-24 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">
              {t("passenger.wallet.recentTransactions")}
            </h2>

            <div className="mt-5 space-y-3">
              {(Array.isArray(transactions) ? transactions : []).length === 0 ? (
                <div className="rounded-xl bg-slate-50 p-6 text-center">
                  <p className="font-black text-slate-900">{t("passenger.wallet.noTransactions")}</p>
                  <p className="mt-1 text-sm text-slate-500">
                    {t("passenger.wallet.noTransactionsDesc")}
                  </p>
                </div>
              ) : (
                (Array.isArray(transactions) ? transactions : []).map((item) => (
                  <Transaction key={item.id} item={item} t={t} />
                ))
              )}
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                <ShieldCheck size={21} />
              </div>
              <div>
                <h2 className="font-black text-slate-900">{t("passenger.wallet.secureWallet")}</h2>
                <p className="mt-1 text-sm leading-6 text-slate-500">
                  {t("passenger.wallet.secureWalletDesc")}
                </p>
              </div>
            </div>
          </div>
        </aside>
      </div>

      <WalletActivationModal
        open={showPinModal}
        userId={user.id}
        onClose={() => setShowPinModal(false)}
        onActivated={handleWalletActivated}
      />
    </PassengerLayout>
  );
}

function getPaymentMethods(t) {
  return [
    { id: "ESEWA", name: "eSewa", desc: t("passenger.wallet.paymentMethodDesc"), icon: <Smartphone size={22} /> },
    { id: "KHALTI", name: "Khalti", desc: t("passenger.wallet.paymentMethodDesc"), icon: <Smartphone size={22} /> },
  ];
}

function walletErrorMessage(status, t) {
  if (status === 401) return t("passenger.wallet.sessionExpired");
  if (status === 403) return t("passenger.wallet.unauthorized");
  if (status === 404) return t("passenger.wallet.notFound");
  return t("passenger.wallet.loadError");
}

function WalletStat({ label, value }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <p className="text-xs font-black uppercase tracking-widest text-slate-500">
        {label}
      </p>
      <h3 className="mt-2 text-xl font-black text-slate-900">{value}</h3>
    </div>
  );
}

function Transaction({ item, t }) {
  const isRecharge = item.type === "TOPUP";

  return (
    <div className="flex items-center justify-between gap-3 rounded-xl bg-slate-50 p-4 hover:bg-slate-100">
      <div className="flex min-w-0 items-center gap-3">
        <div
          className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${
            isRecharge ? "bg-emerald-100 text-emerald-700" : "bg-red-100 text-red-700"
          }`}
        >
          {isRecharge ? <ArrowDownLeft size={20} /> : <ArrowUpRight size={20} />}
        </div>

        <div className="min-w-0">
          <p className="truncate text-sm font-black text-slate-900">
            {isRecharge ? t("passenger.wallet.topUpTransaction") : t("passenger.wallet.ticketPaymentTransaction")}
          </p>
          <p className="truncate text-xs text-slate-500">{item.paymentMethod}</p>
          <p className="text-[11px] text-slate-400">
            {new Date(item.transactionDate).toLocaleString()}
          </p>
        </div>
      </div>

      <div className="shrink-0 text-right">
        <p className={`text-sm font-black ${isRecharge ? "text-emerald-700" : "text-red-600"}`}>
          {isRecharge ? "+" : "-"} NPR {item.amount}
        </p>
        <p className="mt-1 flex items-center justify-end gap-1 text-[10px] font-bold text-slate-500">
          <Clock size={12} />
          {item.status}
        </p>
      </div>
    </div>
  );
}
