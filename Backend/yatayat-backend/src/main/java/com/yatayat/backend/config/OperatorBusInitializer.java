package com.yatayat.backend.config;

import com.yatayat.backend.entity.Bus;
import com.yatayat.backend.entity.BusStatus;
import com.yatayat.backend.entity.OperatorVerificationStatus;
import com.yatayat.backend.entity.TransportOperator;
import com.yatayat.backend.repository.BusRepository;
import com.yatayat.backend.repository.TransportOperatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds a compact demo fleet for existing approved operators.
 * Existing operators and buses are preserved.
 */
@Component
public class OperatorBusInitializer implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperatorBusInitializer.class);
    private static final List<BusTemplate> TEMPLATES = List.of(
            new BusTemplate("Tourist Deluxe", "Tata LP 912", 2022, 40, "Diesel"),
            new BusTemplate("AC Coach", "Ashok Leyland Viking", 2023, 44, "Diesel")
    );

    private final TransportOperatorRepository operatorRepository;
    private final BusRepository busRepository;

    public OperatorBusInitializer(TransportOperatorRepository operatorRepository,
                                  BusRepository busRepository) {
        this.operatorRepository = operatorRepository;
        this.busRepository = busRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<TransportOperator> operators = operatorRepository
                .findByVerificationStatusOrderByCreatedAtDesc(
                        OperatorVerificationStatus.APPROVED);
        int created = 0;
        for (TransportOperator operator : operators) {
            for (int index = 0; index < TEMPLATES.size(); index++) {
                Bus bus = toBus(operator, TEMPLATES.get(index), index + 1);
                if (busRepository.existsByBusNumberIgnoreCase(bus.getBusNumber()) ||
                        busRepository.existsByPermitNumberIgnoreCase(bus.getPermitNumber())) {
                    continue;
                }
                busRepository.saveAndFlush(bus);
                created++;
            }
        }
        LOGGER.info("Operator bus initialization complete: created={}, approvedOperators={}, templatesPerOperator={}",
                created, operators.size(), TEMPLATES.size());
    }

    static int busesPerOperator() {
        return TEMPLATES.size();
    }

    private Bus toBus(TransportOperator operator, BusTemplate template, int sequence) {
        Bus bus = new Bus();
        bus.setBusNumber(String.format("BA 2 KHA %04d", operator.getId() * 10 + sequence));
        bus.setBusName(operator.getName() + " " + template.name());
        bus.setModel(template.model());
        bus.setManufactureYear(template.year());
        bus.setSeatCapacity(template.seats());
        bus.setBusType(template.name());
        bus.setFuelType(template.fuelType());
        String operatorKey = Integer.toUnsignedString(
                operator.getRegistrationNumber().toUpperCase().hashCode(), 16).toUpperCase();
        bus.setPermitNumber("DOTM-SEED-" + operatorKey + "-" + sequence);
        bus.setPermitExpiryDate(LocalDate.now().plusYears(2));
        bus.setInsuranceExpiryDate(LocalDate.now().plusYears(1));
        bus.setStatus(BusStatus.APPROVED);
        bus.setApprovedAt(LocalDateTime.now());
        bus.setOperator(operator);
        bus.setOperatorName(operator.getName());
        return bus;
    }

    private record BusTemplate(String name, String model, int year, int seats, String fuelType) {}
}
