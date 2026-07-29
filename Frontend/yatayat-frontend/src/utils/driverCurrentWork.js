export function selectCurrentDriverWork(scheduledTrip, localService) {
  if (scheduledTrip?.status === "IN_PROGRESS") {
    return { ...scheduledTrip, workType: "SCHEDULED_TRIP" };
  }
  if (localService?.status === "IN_SERVICE") {
    return { ...localService, workType: "LOCAL_SERVICE" };
  }
  if (scheduledTrip?.status === "BOARDING") {
    return { ...scheduledTrip, workType: "SCHEDULED_TRIP" };
  }
  if (!scheduledTrip) {
    return localService ? { ...localService, workType: "LOCAL_SERVICE" } : null;
  }
  if (!localService) {
    return { ...scheduledTrip, workType: "SCHEDULED_TRIP" };
  }

  const scheduledTime = new Date(scheduledTrip.departureAt).getTime();
  const localTime = new Date(
    `${localService.serviceDate}T${localService.plannedStartTime}`,
  ).getTime();

  return localTime < scheduledTime
    ? { ...localService, workType: "LOCAL_SERVICE" }
    : { ...scheduledTrip, workType: "SCHEDULED_TRIP" };
}
