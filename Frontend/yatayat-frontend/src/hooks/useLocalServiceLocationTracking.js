import { useLocationTracking } from "./useDriverLocationTracking";
import { updateDriverLocalServiceLocation } from "../utils/localServices";

export function useLocalServiceLocationTracking(runId, isActive) {
  return useLocationTracking(runId, isActive, updateDriverLocalServiceLocation);
}
