import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, Loader2, ShieldAlert } from "lucide-react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import PassengerLayout from "../../components/layout/PassengerLayout";
import {
  verifyEsewaWalletTopUp,
  verifyKhaltiWalletTopUp,
} from "../../utils/walletTopUps";

export default function WalletTopUpCallbackPage({ provider }) {
  const [search] = useSearchParams();
  const params = useParams();
  const navigate = useNavigate();
  const [state, setState] = useState({
    status: "VERIFYING",
    message: "Verifying your wallet top-up…",
  });

  const callback = useMemo(() => {
    if (provider === "KHALTI") {
      return {
        reference: (search.get("topUpReference") || "").trim(),
        pidx: (search.get("pidx") || "").trim(),
      };
    }
    const rawUuid = (params.transactionUuid || "").trim();
    const marker = "?data=";
    const markerIndex = rawUuid.indexOf(marker);
    return {
      reference: (params.topUpReference || "").trim(),
      transactionUuid: markerIndex >= 0
        ? rawUuid.slice(0, markerIndex).trim()
        : rawUuid,
      data: (
        search.get("data")
        || (markerIndex >= 0 ? rawUuid.slice(markerIndex + marker.length) : "")
      ).trim(),
    };
  }, [params.topUpReference, params.transactionUuid, provider, search]);

  useEffect(() => {
    let active = true;
    let redirectTimer;
    const malformed = !callback.reference || (
      provider === "KHALTI"
        ? !callback.pidx
        : (!callback.transactionUuid && !callback.data)
    );
    if (malformed) {
      Promise.resolve().then(() => {
        if (active) {
          setState({ status: "ERROR", message: "The payment callback is incomplete." });
        }
      });
      return () => { active = false; };
    }
    const request = provider === "KHALTI"
      ? verifyKhaltiWalletTopUp(callback.reference, callback.pidx)
      : verifyEsewaWalletTopUp(
        callback.reference,
        callback.transactionUuid,
        callback.data,
      );
    request.then((result) => {
      if (!active) return;
      if (result.credited && result.paymentStatus === "SUCCESS") {
        setState({
          status: "SUCCESS",
          message: "Your wallet was credited successfully. Returning to Wallet…",
        });
        redirectTimer = window.setTimeout(() => {
          if (active) navigate("/wallet?topup=success", { replace: true });
        }, 1500);
      } else {
        setState({
          status: result.paymentStatus || "ERROR",
          message: result.message || "The payment has not completed yet.",
        });
      }
    }).catch((error) => {
      if (!active) return;
      if (error.status === 401) {
        navigate("/login", { replace: true });
        return;
      }
      setState({
        status: "ERROR",
        message: error.message || "Unable to verify the wallet top-up.",
      });
    });
    return () => {
      active = false;
      if (redirectTimer) window.clearTimeout(redirectTimer);
    };
  }, [callback, navigate, provider]);

  return (
    <PassengerLayout activePage="Wallet">
      <div className="mx-auto flex min-h-[65vh] max-w-xl items-center justify-center">
        <section className="w-full rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-xl shadow-blue-950/10">
          {state.status === "VERIFYING" ? (
            <Loader2 className="mx-auto animate-spin text-[#08264a]" size={54} />
          ) : state.status === "SUCCESS" ? (
            <CheckCircle2 className="mx-auto text-emerald-600" size={58} />
          ) : (
            <ShieldAlert className="mx-auto text-amber-600" size={58} />
          )}
          <h1 className="mt-5 text-2xl font-black text-slate-950">
            {state.status === "VERIFYING"
              ? "Verifying top-up"
              : state.status === "SUCCESS"
                ? "Top-up successful"
                : "Top-up not completed"}
          </h1>
          <p className="mt-3 text-sm font-semibold leading-6 text-slate-600">
            {state.message}
          </p>
          {state.status !== "VERIFYING" && (
            <Link
              to="/wallet"
              className="mt-7 inline-flex rounded-xl bg-[#08264a] px-5 py-3 text-sm font-black text-white"
            >
              Return to Wallet
            </Link>
          )}
        </section>
      </div>
    </PassengerLayout>
  );
}
