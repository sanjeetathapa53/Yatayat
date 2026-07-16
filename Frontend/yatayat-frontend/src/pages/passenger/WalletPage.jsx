import { useEffect, useState } from "react";
import {
  Wallet,
  PlusCircle,
  Smartphone,
  Building2,
  CreditCard,
  CheckCircle,
  Clock,
  ArrowDownLeft,
  ArrowUpRight,
  ShieldCheck,
  Download,
} from "lucide-react";
import PassengerLayout from "../../components/layout/PassengerLayout";

const paymentMethods = [
  { id: "ESEWA", name: "eSewa", desc: "Digital wallet payment", icon: <Smartphone size={22} /> },
  { id: "KHALTI", name: "Khalti", desc: "Digital wallet payment", icon: <Smartphone size={22} /> },
  { id: "MOBILE_BANKING", name: "Mobile Banking", desc: "ConnectIPS / bank app", icon: <Building2 size={22} /> },
  { id: "CARD", name: "Debit / Credit Card", desc: "Visa / Mastercard", icon: <CreditCard size={22} /> },
];

export default function WalletPage() {
  const user = JSON.parse(localStorage.getItem("yatayatUser"));

  const [selectedAmount, setSelectedAmount] = useState(500);
  const [customAmount, setCustomAmount] = useState("");
  const [selectedMethod, setSelectedMethod] = useState("KHALTI");

  const [balance, setBalance] = useState(0);
  const [transactions, setTransactions] = useState([]);
  const [pinStatus, setPinStatus] = useState("");

  const [success, setSuccess] = useState(false);
  const [showPinModal, setShowPinModal] = useState(false);
  const [newPin, setNewPin] = useState("");
  const [confirmPin, setConfirmPin] = useState("");

  const finalAmount = customAmount ? Number(customAmount) : selectedAmount;

  useEffect(() => {
    loadWallet();
    checkPinStatus();
  }, []);

  const loadWallet = async () => {
    const balanceRes = await fetch(`http://localhost:8080/api/wallet/balance/${user.id}`, { credentials: "include" });
    const balanceText = await balanceRes.text();
    setBalance(Number(balanceText));

    const historyRes = await fetch(`http://localhost:8080/api/wallet/history/${user.id}`, { credentials: "include" });
    const historyData = await historyRes.json();
    setTransactions(historyData);
  };

  const checkPinStatus = async () => {
    const res = await fetch(`http://localhost:8080/api/wallet/pin-status/${user.id}`, { credentials: "include" });
    const text = await res.text();
    setPinStatus(text);
  };

  const handleRecharge = async () => {
    if (!finalAmount || finalAmount <= 0) return;

    if (pinStatus === "PIN_NOT_SET") {
      setShowPinModal(true);
      return;
    }

    await rechargeWallet();
  };

  const rechargeWallet = async () => {
    const response = await fetch("http://localhost:8080/api/wallet/topup", {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        userId: user.id,
        amount: finalAmount,
        paymentMethod: selectedMethod,
      }),
    });

    const text = await response.text();

    if (text === "Wallet topped up successfully") {
      setSuccess(true);
      await loadWallet();
      setCustomAmount("");
      setTimeout(() => setSuccess(false), 2500);
    }
  };

  const createWalletPin = async () => {
    if (newPin.length !== 4 || confirmPin.length !== 4) return;
    if (newPin !== confirmPin) return;

    const res = await fetch("http://localhost:8080/api/wallet/create-pin", {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        userId: user.id,
        walletPin: newPin,
      }),
    });

    const text = await res.text();

    if (text === "Wallet PIN created") {
      setShowPinModal(false);
      setPinStatus("PIN_SET");
      setNewPin("");
      setConfirmPin("");
      await rechargeWallet();
    } else {
      alert(text);
    }
  };

  return (
    <PassengerLayout activePage="Wallet">
      <div className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
            Yatayat Wallet
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Add balance, pay for tickets, and track your wallet activity.
          </p>
        </div>

        <button className="flex w-full items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-black hover:bg-slate-50 sm:w-auto">
          <Download size={17} />
          Export Statement
        </button>
      </div>

      {success && (
        <div className="mb-5 flex items-center gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-bold text-emerald-700">
          <CheckCircle size={19} />
          Wallet recharged successfully.
        </div>
      )}

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-12">
        <section className="space-y-5 xl:col-span-7">
          <div className="rounded-3xl bg-[#08264a] p-6 text-white shadow-lg">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-black uppercase tracking-widest text-slate-300">
                  Available Balance
                </p>

                <h2 className="mt-3 text-4xl font-black sm:text-5xl">
                  NPR{" "}
                  {balance.toLocaleString(undefined, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}
                </h2>

                <p className="mt-3 text-sm text-slate-300">
                  Wallet Status:{" "}
                  <span className={pinStatus === "PIN_SET" ? "font-black text-emerald-300" : "font-black text-yellow-300"}>
                    {pinStatus === "PIN_SET" ? "Active" : "Not Activated"}
                  </span>
                </p>
              </div>

              <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white/15">
                <Wallet size={30} />
              </div>
            </div>

            {pinStatus === "PIN_NOT_SET" && (
              <div className="mt-6 rounded-2xl bg-white/10 p-4">
                <p className="text-sm font-bold">
                  🔒 Create a 4-digit Wallet PIN to activate your wallet before your first recharge.
                </p>
              </div>
            )}
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <WalletStat label="Wallet Status" value={pinStatus === "PIN_SET" ? "Active" : "Inactive"} />
            <WalletStat label="Transactions" value={transactions.length} />
            <WalletStat
              label="Ticket Payments"
              value={transactions.filter((t) => t.type === "TICKET_PAYMENT").length}
            />
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-5 flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                <PlusCircle size={21} />
              </div>

              <div>
                <h2 className="text-xl font-black text-slate-900">
                  Recharge Wallet
                </h2>
                <p className="text-sm text-slate-500">
                  Add balance using mock eSewa/Khalti payment for now.
                </p>
              </div>
            </div>

            <p className="mb-3 text-xs font-black uppercase tracking-widest text-slate-500">
              Select Amount
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
              placeholder="Enter custom amount"
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
                <span className="text-slate-500">Recharge Amount</span>
                <span className="font-black">NPR {finalAmount || 0}</span>
              </div>

              <div className="mt-3 flex justify-between border-t border-slate-200 pt-3">
                <span className="font-black">Total Payable</span>
                <span className="font-black text-emerald-700">
                  NPR {finalAmount || 0}
                </span>
              </div>
            </div>

            <button
              onClick={handleRecharge}
              className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-[#08264a] py-3 text-sm font-black text-white transition hover:bg-[#0d3566]"
            >
              <PlusCircle size={18} />
              {pinStatus === "PIN_NOT_SET" ? "Activate Wallet & Recharge" : "Recharge Wallet"}
            </button>
          </div>
        </section>

        <aside className="space-y-5 xl:col-span-5">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="text-xl font-black text-slate-900">
              Recent Transactions
            </h2>

            <div className="mt-5 space-y-3">
              {transactions.length === 0 ? (
                <div className="rounded-xl bg-slate-50 p-6 text-center">
                  <p className="font-black text-slate-900">No transactions yet</p>
                  <p className="mt-1 text-sm text-slate-500">
                    Your recharge and ticket payments will appear here.
                  </p>
                </div>
              ) : (
                transactions.map((item) => (
                  <Transaction key={item.id} item={item} />
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
                <h2 className="font-black text-slate-900">Secure Wallet</h2>
                <p className="mt-1 text-sm leading-6 text-slate-500">
                  Your wallet PIN is encrypted using BCrypt and used to protect ticket payments.
                </p>
              </div>
            </div>
          </div>
        </aside>
      </div>

      {showPinModal && (
        <div className="fixed inset-0 z-999 flex items-center justify-center bg-black/50 px-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
            <div className="text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-[#08264a] text-white">
                🔐
              </div>

              <h2 className="mt-4 text-2xl font-black text-slate-900">
                Activate Your Wallet
              </h2>

              <p className="mt-2 text-sm leading-6 text-slate-500">
                Create a secure 4-digit PIN before adding money or paying for tickets.
              </p>
            </div>

            <PinInput label="Enter Wallet PIN" value={newPin} setValue={setNewPin} />
            <PinInput label="Confirm Wallet PIN" value={confirmPin} setValue={setConfirmPin} />

            {confirmPin.length === 4 && newPin !== confirmPin && (
              <p className="mt-3 text-center text-sm font-bold text-red-600">
                PINs do not match
              </p>
            )}

            <div className="mt-5 rounded-xl bg-slate-50 p-4 text-xs font-bold text-slate-500">
              ✓ PIN must be exactly 4 digits <br />
              ✓ Used for wallet payments and ticket booking
            </div>

            <div className="mt-6 flex gap-3">
              <button
                onClick={() => setShowPinModal(false)}
                className="flex-1 rounded-xl border border-slate-300 py-3 text-sm font-black text-slate-600 hover:bg-slate-50"
              >
                Cancel
              </button>

              <button
                onClick={createWalletPin}
                disabled={newPin.length !== 4 || confirmPin.length !== 4 || newPin !== confirmPin}
                className="flex-1 rounded-xl bg-[#08264a] py-3 text-sm font-black text-white hover:bg-[#0d3566] disabled:cursor-not-allowed disabled:bg-slate-300"
              >
                Activate Wallet
              </button>
            </div>
          </div>
        </div>
      )}
    </PassengerLayout>
  );
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

function Transaction({ item }) {
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
            {isRecharge ? "Wallet Top-up" : "Ticket Payment"}
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

function PinInput({ label, value, setValue }) {
  const handleChange = (index, digit) => {
    if (!/^\d?$/.test(digit)) return;

    const pinArray = value.padEnd(4, "").split("");
    pinArray[index] = digit;

    const newPin = pinArray.join("").trim();
    setValue(newPin);

    if (digit && index < 3) {
      document.getElementById(`${label}-${index + 1}`)?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === "Backspace" && !value[index] && index > 0) {
      document.getElementById(`${label}-${index - 1}`)?.focus();
    }
  };

  return (
    <div className="mt-6">
      <p className="mb-3 text-center text-xs font-black uppercase tracking-widest text-slate-500">
        {label}
      </p>

      <div className="flex justify-center gap-3">
        {[0, 1, 2, 3].map((index) => (
          <input
            key={index}
            id={`${label}-${index}`}
            type="password"
            inputMode="numeric"
            maxLength={1}
            value={value[index] || ""}
            onChange={(e) => handleChange(index, e.target.value)}
            onKeyDown={(e) => handleKeyDown(index, e)}
            className="h-14 w-14 rounded-2xl border border-slate-300 bg-slate-50 text-center text-2xl font-black outline-none transition focus:border-[#08264a] focus:bg-white"
          />
        ))}
      </div>
    </div>
  );
}
