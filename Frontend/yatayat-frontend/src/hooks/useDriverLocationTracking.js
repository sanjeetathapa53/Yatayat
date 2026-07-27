import { useCallback, useEffect, useRef, useState } from "react";
import { updateDriverTripLocation } from "../utils/driverTrips";

const UPDATE_INTERVAL_MS = 7000;

export const GPS_STATUS = Object.freeze({
  WAITING: "Waiting for GPS",
  ACTIVE: "GPS Active",
  PERMISSION_DENIED: "GPS Permission Denied",
  UNAVAILABLE: "GPS Unavailable",
  NETWORK_ERROR: "Network Error",
});

export function useLocationTracking(operationId, isActive, publishLocation) {
  const [status, setStatus] = useState(GPS_STATUS.WAITING);
  const [message, setMessage] = useState("");
  const latestLocationRef = useRef(null);
  const watchIdRef = useRef(null);
  const intervalIdRef = useRef(null);
  const requestControllerRef = useRef(null);
  const requestInFlightRef = useRef(false);
  const networkErrorRef = useRef(false);
  const mountedRef = useRef(true);

  const stopTracking = useCallback(() => {
    if (watchIdRef.current !== null && navigator.geolocation) {
      navigator.geolocation.clearWatch(watchIdRef.current);
      watchIdRef.current = null;
    }
    if (intervalIdRef.current !== null) {
      window.clearInterval(intervalIdRef.current);
      intervalIdRef.current = null;
    }
    latestLocationRef.current = null;
    requestControllerRef.current?.abort();
    requestControllerRef.current = null;
    requestInFlightRef.current = false;
    networkErrorRef.current = false;
  }, []);

  const sendLatestLocation = useCallback(async () => {
    const location = latestLocationRef.current;
    if (!operationId || !location || requestInFlightRef.current) return;

    requestInFlightRef.current = true;
    const requestController = new AbortController();
    requestControllerRef.current = requestController;
    try {
      await publishLocation(operationId, location, { signal: requestController.signal });
      networkErrorRef.current = false;
      if (mountedRef.current) {
        setStatus(GPS_STATUS.ACTIVE);
        setMessage("");
      }
    } catch (error) {
      if (error.name === "AbortError") return;
      networkErrorRef.current = true;
      if (mountedRef.current) {
        setStatus(GPS_STATUS.NETWORK_ERROR);
        setMessage(error.message || "Your location could not be sent. Tracking will retry automatically.");
      }
    } finally {
      if (requestControllerRef.current === requestController) {
        requestControllerRef.current = null;
        requestInFlightRef.current = false;
      }
    }
  }, [operationId, publishLocation]);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      stopTracking();
    };
  }, [stopTracking]);

  useEffect(() => {
    stopTracking();

    if (!isActive || !operationId) return undefined;

    if (!("geolocation" in navigator)) {
      Promise.resolve().then(() => {
        if (!mountedRef.current) return;
        setStatus(GPS_STATUS.UNAVAILABLE);
        setMessage("This browser or device does not support GPS location.");
      });
      return undefined;
    }

    Promise.resolve().then(() => {
      if (!mountedRef.current) return;
      setStatus(GPS_STATUS.WAITING);
      setMessage("Waiting for your device to provide a GPS position.");
    });

    watchIdRef.current = navigator.geolocation.watchPosition(
      (position) => {
        const isFirstPosition = latestLocationRef.current === null;
        const { latitude, longitude, accuracy, speed, heading } = position.coords;
        latestLocationRef.current = {
          latitude,
          longitude,
          accuracy: Number.isFinite(accuracy) ? accuracy : null,
          speed: Number.isFinite(speed) && speed >= 0 ? speed : null,
          heading: Number.isFinite(heading) && heading >= 0 ? heading : null,
        };

        if (!networkErrorRef.current) {
          setStatus(GPS_STATUS.ACTIVE);
          setMessage("");
        }
        if (isFirstPosition) void sendLatestLocation();
      },
      (error) => {
        if (error.code === error.PERMISSION_DENIED) {
          setStatus(GPS_STATUS.PERMISSION_DENIED);
          setMessage("Location permission was denied. Enable it in your browser settings to share this trip's location.");
          stopTracking();
          return;
        }
        setStatus(GPS_STATUS.UNAVAILABLE);
        setMessage("A GPS position is currently unavailable. The browser will keep trying.");
      },
      { enableHighAccuracy: true, maximumAge: 5000, timeout: 15000 },
    );

    intervalIdRef.current = window.setInterval(() => {
      void sendLatestLocation();
    }, UPDATE_INTERVAL_MS);

    const handleLogout = () => stopTracking();
    window.addEventListener("yatayat-auth-cleared", handleLogout);

    return () => {
      window.removeEventListener("yatayat-auth-cleared", handleLogout);
      stopTracking();
    };
  }, [isActive, operationId, sendLatestLocation, stopTracking]);

  return { status, message };
}

export function useDriverLocationTracking(tripId, isActive) {
  return useLocationTracking(tripId, isActive, updateDriverTripLocation);
}
