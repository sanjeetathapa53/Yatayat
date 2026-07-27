import { useCallback, useState } from "react";

const WALLET_BALANCE_VISIBILITY_KEY = "yatayat.wallet.balanceVisible";

export function useWalletBalanceVisibility() {
  const [showBalance, setShowBalance] = useState(
    () => sessionStorage.getItem(WALLET_BALANCE_VISIBILITY_KEY) === "true",
  );

  const toggleBalance = useCallback(() => {
    setShowBalance((visible) => {
      const next = !visible;
      sessionStorage.setItem(WALLET_BALANCE_VISIBILITY_KEY, String(next));
      return next;
    });
  }, []);

  return { showBalance, toggleBalance };
}
