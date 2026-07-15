package com.yatayat.backend.service;

import com.yatayat.backend.entity.DriverProfile;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.DriverProfileRepository;
import com.yatayat.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class DriverDashboardService {

    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;

    public DriverDashboardService(
            UserRepository userRepository,
            DriverProfileRepository driverProfileRepository
    ) {
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
    }

    public Map<String, Object> getDashboard(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Driver account not found"
                        )
                );

        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException(
                    "This account is not registered as a driver"
            );
        }

        DriverProfile profile = driverProfileRepository
                .findByUser(user)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Driver profile not found"
                        )
                );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        response.put("driver", buildDriverData(user, profile));
        response.put("assignment", buildAssignmentData());
        response.put("trip", buildTripData());
        response.put("stats", buildStatsData(profile));
        response.put("telemetry", buildTelemetryData());
        response.put("recentActivity", buildRecentActivity(profile));

        return response;
    }

    private Map<String, Object> buildDriverData(
            User user,
            DriverProfile profile
    ) {
        Map<String, Object> driver = new HashMap<>();

        driver.put("userId", user.getId());
        driver.put("applicationId", profile.getId());
        driver.put("fullName", user.getFullName());
        driver.put("email", user.getEmail());
        driver.put("phone", user.getPhone());
        driver.put("role", user.getRole());

        driver.put(
                "verificationStatus",
                profile.getVerificationStatus().name()
        );

        driver.put("licenseNumber", profile.getLicenseNumber());
        driver.put("licenseCategory", profile.getLicenseCategory());
        driver.put("licenseIssueDate", profile.getLicenseIssueDate());
        driver.put("licenseExpiryDate", profile.getLicenseExpiryDate());
        driver.put(
                "yearsOfExperience",
                profile.getYearsOfExperience()
        );
        driver.put(
                "preferredOperatingArea",
                profile.getPreferredOperatingArea()
        );
        driver.put("approvedAt", profile.getApprovedAt());

        return driver;
    }

    private Map<String, Object> buildAssignmentData() {
        Map<String, Object> assignment = new HashMap<>();

        assignment.put("assigned", false);
        assignment.put("busId", null);
        assignment.put("busNumber", null);
        assignment.put("busName", null);
        assignment.put("routeName", null);

        return assignment;
    }

    private Map<String, Object> buildTripData() {
        Map<String, Object> trip = new HashMap<>();

        trip.put("active", false);
        trip.put("tripId", null);
        trip.put("status", "NOT_STARTED");
        trip.put("origin", null);
        trip.put("destination", null);
        trip.put("nextStop", null);
        trip.put("estimatedArrival", null);

        return trip;
    }

    private Map<String, Object> buildStatsData(
            DriverProfile profile
    ) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("passengerCount", 0);
        stats.put("seatCapacity", 0);
        stats.put("todayRevenue", 0.0);
        stats.put("ticketsVerified", 0);
        stats.put("pendingBoarding", 0);
        stats.put("totalTrips", 0);
        stats.put("hoursDriven", 0);
        stats.put("averageRating", null);
        stats.put("fuelEfficiency", null);
        stats.put("averageDelay", null);

        boolean licenceValid =
                profile.getLicenseExpiryDate() != null &&
                        !profile.getLicenseExpiryDate()
                                .isBefore(LocalDate.now());

        stats.put("licenceValid", licenceValid);

        return stats;
    }

    private Map<String, Object> buildTelemetryData() {
        Map<String, Object> telemetry = new HashMap<>();

        telemetry.put("online", false);
        telemetry.put("locationSharing", false);
        telemetry.put("latitude", null);
        telemetry.put("longitude", null);
        telemetry.put("speedKmh", 0);
        telemetry.put("gpsSignal", "OFFLINE");
        telemetry.put("lastUpdated", null);

        return telemetry;
    }

    private Object buildRecentActivity(
            DriverProfile profile
    ) {
        return new Object[]{
                activity(
                        "Driver application approved",
                        "Your account is verified and ready for assignment.",
                        profile.getApprovedAt()
                ),
                activity(
                        "Driver profile created",
                        "Your professional information was submitted successfully.",
                        profile.getCreatedAt()
                ),
                activity(
                        "Waiting for bus assignment",
                        "No bus has been assigned to your account yet.",
                        null
                )
        };
    }

    private Map<String, Object> activity(
            String title,
            String description,
            Object time
    ) {
        Map<String, Object> activity = new HashMap<>();

        activity.put("title", title);
        activity.put("description", description);
        activity.put("time", time);

        return activity;
    }
}