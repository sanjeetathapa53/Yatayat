package com.yatayat.backend.config;

import com.yatayat.backend.entity.BusStop;
import com.yatayat.backend.entity.Route;
import com.yatayat.backend.entity.RouteStatus;
import com.yatayat.backend.entity.RouteStop;
import com.yatayat.backend.entity.TripType;
import com.yatayat.backend.repository.BusStopRepository;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.RouteStopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds reusable national out-of-valley master routes while preserving existing data.
 * Scheduled-trip fares remain operator-defined, so seeded route stops carry no fare.
 */
@Component
public class LongDistanceRouteInitializer implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(LongDistanceRouteInitializer.class);
    private static final List<RouteSeed> ROUTES = buildRoutes();

    private static List<RouteSeed> buildRoutes() {
        List<RouteSeed> routes = new ArrayList<>();
        addKathmanduRoutes(routes);
        addGandakiRoutes(routes);
        addLumbiniRoutes(routes);
        addEasternRoutes(routes);
        addMadheshRoutes(routes);
        addSudurpashchimRoutes(routes);
        addKarnaliRoutes(routes);
        return List.copyOf(routes);
    }

    private static void addKathmanduRoutes(List<RouteSeed> routes) {
        routes.add(route("KTM-PKR", 200, 420, "Kathmandu", "Naubise", "Mugling", "Damauli", "Pokhara"));
        routes.add(route("KTM-BRT", 380, 600, "Kathmandu", "Dhulikhel", "Khurkot", "Sindhuli", "Bardibas", "Lahan", "Itahari", "Biratnagar"));
        routes.add(route("KTM-JNK", 225, 420, "Kathmandu", "Dhulikhel", "Khurkot", "Sindhuli", "Bardibas", "Janakpur"));
        routes.add(route("KTM-BIR", 135, 300, "Kathmandu", "Naubise", "Daman", "Hetauda", "Pathlaiya", "Birgunj"));
        routes.add(route("KTM-HTD", 85, 240, "Kathmandu", "Thankot", "Naubise", "Daman", "Hetauda"));
        routes.add(route("KTM-BHR", 150, 300, "Kathmandu", "Naubise", "Mugling", "Narayanghat", "Bharatpur"));
        routes.add(route("KTM-BTL", 260, 480, "Kathmandu", "Naubise", "Mugling", "Narayanghat", "Kawasoti", "Bardghat", "Butwal"));
        routes.add(route("KTM-NPJ", 520, 720, "Kathmandu", "Mugling", "Narayanghat", "Butwal", "Lamahi", "Kohalpur", "Nepalgunj"));
        routes.add(route("KTM-DHN", 680, 900, "Kathmandu", "Narayanghat", "Butwal", "Lamahi", "Kohalpur", "Chisapani", "Attariya", "Dhangadhi"));
        routes.add(route("KTM-DHR", 370, 600, "Kathmandu", "Dhulikhel", "Sindhuli", "Bardibas", "Lahan", "Itahari", "Dharan"));
        routes.add(route("KTM-ILM", 520, 780, "Kathmandu", "Sindhuli", "Bardibas", "Lahan", "Itahari", "Birtamod", "Ilam"));
        routes.add(route("KTM-KAK", 490, 720, "Kathmandu", "Sindhuli", "Bardibas", "Lahan", "Itahari", "Birtamod", "Kakarbhitta"));
        routes.add(route("KTM-RJB", 300, 480, "Kathmandu", "Dhulikhel", "Sindhuli", "Bardibas", "Lahan", "Rajbiraj"));
        routes.add(route("KTM-SUR", 600, 840, "Kathmandu", "Narayanghat", "Butwal", "Lamahi", "Kohalpur", "Chhinchu", "Surkhet"));
        routes.add(route("KTM-TAN", 300, 540, "Kathmandu", "Mugling", "Narayanghat", "Butwal", "Tansen"));
    }

    private static void addGandakiRoutes(List<RouteSeed> routes) {
        routes.add(route("PKR-KTM", 200, 420, "Pokhara", "Damauli", "Mugling", "Naubise", "Kathmandu"));
        routes.add(route("PKR-BTL", 160, 300, "Pokhara", "Syangja", "Waling", "Tansen", "Butwal"));
        routes.add(route("PKR-BHR", 145, 270, "Pokhara", "Damauli", "Abukhaireni", "Mugling", "Bharatpur"));
        routes.add(route("PKR-NPJ", 380, 600, "Pokhara", "Syangja", "Tansen", "Butwal", "Lamahi", "Kohalpur", "Nepalgunj"));
        routes.add(route("PKR-BAG", 75, 180, "Pokhara", "Hemja", "Nayapul", "Kusma", "Baglung"));
        routes.add(route("PKR-BEN", 85, 210, "Pokhara", "Hemja", "Nayapul", "Kusma", "Beni"));
        routes.add(route("PKR-GOR", 110, 240, "Pokhara", "Damauli", "Abukhaireni", "Gorkha"));
        routes.add(route("PKR-DHR", 540, 780, "Pokhara", "Mugling", "Narayanghat", "Hetauda", "Bardibas", "Lahan", "Itahari", "Dharan"));
    }

    private static void addLumbiniRoutes(List<RouteSeed> routes) {
        routes.add(route("BTL-KTM", 260, 480, "Butwal", "Bardghat", "Kawasoti", "Narayanghat", "Mugling", "Naubise", "Kathmandu"));
        routes.add(route("BTL-PKR", 160, 300, "Butwal", "Tansen", "Waling", "Syangja", "Pokhara"));
        routes.add(route("BTL-BHR", 125, 240, "Butwal", "Bardghat", "Kawasoti", "Narayanghat", "Bharatpur"));
        routes.add(route("BTL-NPJ", 250, 390, "Butwal", "Kapilvastu", "Bhalubang", "Lamahi", "Kohalpur", "Nepalgunj"));
        routes.add(route("BTL-BHW", 25, 60, "Butwal", "Manigram", "Bhairahawa"));
        routes.add(route("BTL-TAN", 40, 90, "Butwal", "Siddhababa", "Tansen"));
    }

    private static void addEasternRoutes(List<RouteSeed> routes) {
        routes.add(route("BRT-KTM", 380, 600, "Biratnagar", "Itahari", "Lahan", "Bardibas", "Sindhuli", "Khurkot", "Dhulikhel", "Kathmandu"));
        routes.add(route("DHR-KTM", 370, 600, "Dharan", "Itahari", "Lahan", "Bardibas", "Sindhuli", "Dhulikhel", "Kathmandu"));
        routes.add(route("ITH-KTM", 355, 570, "Itahari", "Lahan", "Bardibas", "Sindhuli", "Khurkot", "Dhulikhel", "Kathmandu"));
        routes.add(route("DHR-PKR", 540, 780, "Dharan", "Itahari", "Lahan", "Bardibas", "Hetauda", "Narayanghat", "Mugling", "Pokhara"));
        routes.add(route("BRT-KAK", 115, 180, "Biratnagar", "Itahari", "Damak", "Birtamod", "Kakarbhitta"));
        routes.add(route("BRT-DHR", 55, 105, "Biratnagar", "Duhabi", "Itahari", "Dharan"));
        routes.add(route("KAK-KTM", 490, 720, "Kakarbhitta", "Birtamod", "Itahari", "Lahan", "Bardibas", "Sindhuli", "Kathmandu"));
        routes.add(route("ILM-BRT", 180, 300, "Ilam", "Birtamod", "Damak", "Itahari", "Biratnagar"));
    }

    private static void addMadheshRoutes(List<RouteSeed> routes) {
        routes.add(route("JNK-KTM", 225, 420, "Janakpur", "Bardibas", "Sindhuli", "Khurkot", "Dhulikhel", "Kathmandu"));
        routes.add(route("BRJ-KTM", 135, 300, "Birgunj", "Pathlaiya", "Hetauda", "Daman", "Naubise", "Kathmandu"));
        routes.add(route("JNK-PKR", 390, 600, "Janakpur", "Bardibas", "Hetauda", "Narayanghat", "Mugling", "Pokhara"));
        routes.add(route("BRJ-PKR", 280, 480, "Birgunj", "Pathlaiya", "Hetauda", "Narayanghat", "Mugling", "Pokhara"));
        routes.add(route("RJB-KTM", 300, 480, "Rajbiraj", "Lahan", "Bardibas", "Sindhuli", "Dhulikhel", "Kathmandu"));
        routes.add(route("LHN-KTM", 280, 450, "Lahan", "Bardibas", "Sindhuli", "Khurkot", "Dhulikhel", "Kathmandu"));
        routes.add(route("BRD-KTM", 195, 360, "Bardibas", "Sindhuli", "Khurkot", "Dhulikhel", "Kathmandu"));
        routes.add(route("BHR-JNK", 260, 450, "Bharatpur", "Narayanghat", "Hetauda", "Bardibas", "Dhalkebar", "Janakpur"));
    }

    private static void addSudurpashchimRoutes(List<RouteSeed> routes) {
        routes.add(route("DHG-KTM", 680, 900, "Dhangadhi", "Attariya", "Chisapani", "Kohalpur", "Lamahi", "Butwal", "Narayanghat", "Kathmandu"));
        routes.add(route("MNR-KTM", 700, 930, "Mahendranagar", "Attariya", "Chisapani", "Kohalpur", "Lamahi", "Butwal", "Narayanghat", "Kathmandu"));
        routes.add(route("DHG-NPJ", 190, 300, "Dhangadhi", "Attariya", "Chisapani", "Kohalpur", "Nepalgunj"));
        routes.add(route("MNR-DHG", 55, 90, "Mahendranagar", "Jhalari", "Attariya", "Dhangadhi"));
        routes.add(route("DDL-DHG", 140, 270, "Dadeldhura", "Sahukharka", "Budhar", "Attariya", "Dhangadhi"));
    }

    private static void addKarnaliRoutes(List<RouteSeed> routes) {
        routes.add(route("SUR-KTM", 600, 840, "Surkhet", "Chhinchu", "Kohalpur", "Lamahi", "Butwal", "Narayanghat", "Kathmandu"));
        routes.add(route("NPJ-SUR", 105, 180, "Nepalgunj", "Kohalpur", "Bheriganga", "Chhinchu", "Surkhet"));
        routes.add(route("SUR-PKR", 410, 660, "Surkhet", "Chhinchu", "Kohalpur", "Lamahi", "Butwal", "Tansen", "Pokhara"));
        routes.add(route("SUR-JML", 235, 600, "Surkhet", "Dailekh", "Rakam Karnali", "Manma", "Nagma", "Jumla"));
        routes.add(route("SUR-DLK", 70, 150, "Surkhet", "Gurans", "Dailekh"));
        routes.add(route("NPJ-JML", 340, 720, "Nepalgunj", "Kohalpur", "Surkhet", "Dailekh", "Manma", "Nagma", "Jumla"));
    }

    private final RouteRepository routeRepository;
    private final BusStopRepository busStopRepository;
    private final RouteStopRepository routeStopRepository;

    public LongDistanceRouteInitializer(RouteRepository routeRepository,
                                        BusStopRepository busStopRepository,
                                        RouteStopRepository routeStopRepository) {
        this.routeRepository = routeRepository;
        this.busStopRepository = busStopRepository;
        this.routeStopRepository = routeStopRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int created = 0;
        for (RouteSeed seed : ROUTES) {
            if (routeRepository.existsByCodeIgnoreCase(seed.code()) ||
                    routeRepository.existsByTripTypeAndOriginIgnoreCaseAndDestinationIgnoreCase(
                            TripType.OUT_OF_VALLEY, seed.origin(), seed.destination())) {
                continue;
            }
            Route savedRoute = routeRepository.saveAndFlush(toRoute(seed));
            routeStopRepository.saveAllAndFlush(toRouteStops(savedRoute, seed));
            created++;
        }
        LOGGER.info("Long-distance master-route initialization complete: created={}, catalogSize={}",
                created, ROUTES.size());
    }

    static int catalogSize() {
        return ROUTES.size();
    }

    private Route toRoute(RouteSeed seed) {
        Route route = new Route();
        route.setCode(seed.code());
        route.setName(seed.origin() + " to " + seed.destination());
        route.setOrigin(seed.origin());
        route.setDestination(seed.destination());
        route.setDistanceKm(seed.distanceKm());
        route.setEstimatedDurationMinutes(seed.durationMinutes());
        route.setTripType(TripType.OUT_OF_VALLEY);
        route.setStatus(RouteStatus.ACTIVE);
        return route;
    }

    private List<RouteStop> toRouteStops(Route route, RouteSeed seed) {
        List<RouteStop> routeStops = new ArrayList<>();
        int lastIndex = seed.stops().size() - 1;
        for (int index = 0; index < seed.stops().size(); index++) {
            RouteStop routeStop = new RouteStop();
            routeStop.setRoute(route);
            routeStop.setBusStop(findOrCreateStop(seed.stops().get(index)));
            routeStop.setStopOrder(index + 1);
            routeStop.setEstimatedMinutesFromStart(seed.durationMinutes() * index / lastIndex);
            routeStop.setCumulativeFare(BigDecimal.ZERO);
            routeStop.setActive(true);
            routeStops.add(routeStop);
        }
        return routeStops;
    }

    private BusStop findOrCreateStop(String name) {
        String normalizedName = name.trim().replaceAll("\\s+", " ").toUpperCase();
        return busStopRepository.findByNormalizedName(normalizedName)
                .orElseGet(() -> {
                    BusStop stop = new BusStop();
                    stop.setName(name);
                    stop.setActive(true);
                    return busStopRepository.saveAndFlush(stop);
                });
    }

    private static RouteSeed route(String code, int distanceKm, int durationMinutes, String... stops) {
        List<String> orderedStops = List.of(stops);
        return new RouteSeed(code, orderedStops.get(0), orderedStops.get(orderedStops.size() - 1),
                BigDecimal.valueOf(distanceKm), durationMinutes, orderedStops);
    }

    private record RouteSeed(String code, String origin, String destination,
                             BigDecimal distanceKm, int durationMinutes, List<String> stops) {}
}
