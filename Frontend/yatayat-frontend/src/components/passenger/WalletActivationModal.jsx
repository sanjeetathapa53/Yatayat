import { useEffect, useRef, useState } from "react";
import { Eye, EyeOff, LockKeyhole, X } from "lucide-react";
import { toast } from "react-toastify";
import { useLanguage } from "../../hooks/useLanguage";
import { apiFetch } from "../../utils/api";

export default function WalletActivationModal({
  open,
  userId,
  onClose,
  onActivated,
}) {
  const { t } = useLanguage();
  const [newPin, setNewPin] = useState("");
  const [confirmPin, setConfirmPin] = useState("");
  const [showPinDigits, setShowPinDigits] = useState(false);
  const [activating, setActivating] = useState(false);

  const clearPinFields = () => {
    setNewPin("");
    setConfirmPin("");
    setShowPinDigits(false);
  };

  const closeModal = () => {
    if (activating) return;
    clearPinFields();
    onClose();
  };

  useEffect(() => {
    if (!open) return undefined;
    const handleEscape = (event) => {
      if (event.key === "Escape" && !activating) {
        setNewPin("");
        setConfirmPin("");
        setShowPinDigits(false);
        onClose();
      }
    };
    window.addEventListener("keydown", handleEscape);
    return () => window.removeEventListener("keydown", handleEscape);
  }, [activating, onClose, open]);

  if (!open) return null;

  const activateWallet = async () => {
    if (activating) return;
    if (!/^\d{4}$/.test(newPin) || !/^\d{4}$/.test(confirmPin)) {
      toast.error("Wallet PIN must contain exactly four digits.");
      return;
    }
    if (newPin !== confirmPin) {
      toast.error(t("passenger.wallet.pinMismatch"));
      return;
    }

    try {
      setActivating(true);
      const response = await apiFetch("/api/wallet/create-pin", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId, walletPin: newPin }),
      });
      const message = await response.text();
      if (!response.ok || message !== "Wallet PIN created") {
        throw new Error(message || "Unable to activate wallet.");
      }

      clearPinFields();
      await onActivated();
      onClose();
      toast.success("Wallet activated successfully.", {
        toastId: "wallet-activation-success",
      });
    } catch (error) {
      toast.error(error.message || "Unable to activate wallet.", {
        toastId: "wallet-activation-error",
      });
    } finally {
      setActivating(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-999 flex items-center justify-center overflow-y-auto bg-black/55 px-4 py-6 backdrop-blur-sm"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) closeModal();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="wallet-activation-title"
        className="relative w-full max-w-md rounded-3xl bg-white p-5 shadow-2xl sm:p-7"
      >
        <button
          type="button"
          onClick={closeModal}
          disabled={activating}
          aria-label="Close wallet activation"
          className="absolute right-4 top-4 flex h-9 w-9 items-center justify-center rounded-xl text-slate-500 transition hover:bg-slate-100 hover:text-slate-900 disabled:opacity-50"
        >
          <X size={20} />
        </button>

        <div className="text-center">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-[#08264a] text-white">
            <LockKeyhole size={30} />
          </div>
          <h2 id="wallet-activation-title" className="mt-4 text-2xl font-black text-slate-900">
            Activate Your Wallet
          </h2>
          <p className="mt-2 text-sm leading-6 text-slate-500">
            {t("passenger.wallet.activateWalletDesc")}
          </p>
        </div>

        <PinInput
          label={t("passenger.wallet.enterPin")}
          inputId="wallet-pin"
          value={newPin}
          setValue={setNewPin}
          reveal={showPinDigits}
        />
        <PinInput
          label={t("passenger.wallet.confirmPin")}
          inputId="confirm-wallet-pin"
          value={confirmPin}
          setValue={setConfirmPin}
          reveal={showPinDigits}
        />

        <button
          type="button"
          onClick={() => setShowPinDigits((visible) => !visible)}
          aria-label={showPinDigits ? "Hide wallet PIN" : "Show wallet PIN"}
          className="mx-auto mt-4 flex items-center gap-2 rounded-lg px-3 py-2 text-xs font-black text-slate-600 transition hover:bg-slate-100"
        >
          {showPinDigits ? <EyeOff size={16} /> : <Eye size={16} />}
          {showPinDigits ? "Hide PIN" : "Show PIN"}
        </button>

        {confirmPin.length === 4 && newPin !== confirmPin && (
          <p className="mt-3 text-center text-sm font-bold text-red-600">
            {t("passenger.wallet.pinMismatch")}
          </p>
        )}

        <div className="mt-5 rounded-xl bg-slate-50 p-4 text-xs font-bold text-slate-500">
          {t("passenger.wallet.pinRules")} <br />
          {t("passenger.wallet.pinUsage")}
        </div>

        <div className="mt-6 flex gap-3">
          <button
            type="button"
            onClick={closeModal}
            disabled={activating}
            className="flex-1 rounded-xl border border-slate-300 py-3 text-sm font-black text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {t("common.cancel")}
          </button>
          <button
            type="button"
            onClick={activateWallet}
            disabled={
              activating
              || !/^\d{4}$/.test(newPin)
              || !/^\d{4}$/.test(confirmPin)
              || newPin !== confirmPin
            }
            className="flex-1 rounded-xl bg-[#08264a] py-3 text-sm font-black text-white hover:bg-[#0d3566] disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {activating ? "Activating..." : t("passenger.wallet.activateWallet")}
          </button>
        </div>
      </div>
    </div>
  );
}

function PinInput({ label, inputId, value, setValue, reveal }) {
  const inputRefs = useRef([]);

  const handleChange = (index, digit) => {
    if (!/^\d?$/.test(digit)) return;
    const pinArray = Array.from({ length: 4 }, (_, position) => value[position] || "");
    pinArray[index] = digit;
    setValue(pinArray.join(""));
    if (digit && index < 3) inputRefs.current[index + 1]?.focus();
  };

  const handleKeyDown = (index, event) => {
    if (event.key === "ArrowLeft" && index > 0) {
      event.preventDefault();
      inputRefs.current[index - 1]?.focus();
    } else if (event.key === "ArrowRight" && index < 3) {
      event.preventDefault();
      inputRefs.current[index + 1]?.focus();
    } else if (event.key === "Backspace") {
      event.preventDefault();
      if (value[index]) {
        setValue(value.slice(0, index));
      } else if (index > 0) {
        setValue(value.slice(0, index - 1));
        inputRefs.current[index - 1]?.focus();
      }
    }
  };

  const handlePaste = (event) => {
    event.preventDefault();
    const pastedPin = event.clipboardData.getData("text").trim();
    if (!/^\d{4}$/.test(pastedPin)) return;
    setValue(pastedPin);
    inputRefs.current[3]?.focus();
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
            ref={(element) => {
              inputRefs.current[index] = element;
            }}
            id={`${inputId}-${index}`}
            type={reveal ? "text" : "password"}
            inputMode="numeric"
            autoComplete="new-password"
            maxLength={1}
            value={value[index] || ""}
            onChange={(event) => handleChange(index, event.target.value)}
            onKeyDown={(event) => handleKeyDown(index, event)}
            onPaste={handlePaste}
            aria-label={`${label} digit ${index + 1}`}
            className="h-12 w-12 rounded-xl border border-slate-300 bg-slate-50 text-center text-xl font-black outline-none transition focus:border-[#08264a] focus:bg-white focus:ring-2 focus:ring-[#08264a]/20 sm:h-14 sm:w-14 sm:rounded-2xl sm:text-2xl"
          />
        ))}
      </div>
    </div>
  );
}
