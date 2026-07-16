import { useEffect, useState } from "react";
import { apiFetch } from "../utils/api";

export default function useWallet() {
  const user = JSON.parse(localStorage.getItem("yatayatUser"));

  const [balance, setBalance] = useState(0);
  const [transactions, setTransactions] = useState([]);

  const loadWallet = async () => {
    if (!user?.id) return;

    const balanceRes = await apiFetch(`/api/wallet/balance/${user.id}`);
    if (!balanceRes.ok) {
      setBalance(0);
      setTransactions([]);
      return;
    }
    const balanceText = await balanceRes.text();
    setBalance(Number(balanceText) || 0);

    const historyRes = await apiFetch(`/api/wallet/history/${user.id}`);
    if (!historyRes.ok) {
      setTransactions([]);
      return;
    }
    const historyData = await historyRes.json().catch(() => []);
    setTransactions(Array.isArray(historyData) ? historyData : []);
  };

  const topUpWallet = async (amount, paymentMethod) => {
    if (!user?.id || !amount || amount <= 0) return false;

    const res = await fetch("http://localhost:8080/api/wallet/topup", {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        userId: user.id,
        amount,
        paymentMethod,
      }),
    });

    const text = await res.text();

    if (text === "Wallet topped up successfully") {
      await loadWallet();
      return true;
    }

    return false;
  };

  useEffect(() => {
    loadWallet();
  }, []);

  return {
    balance,
    transactions,
    loadWallet,
    topUpWallet,
  };
}
