package com.yatayat.backend.config;

import com.yatayat.backend.entity.Bus;
import com.yatayat.backend.entity.BusStatus;
import com.yatayat.backend.entity.OperatorType;
import com.yatayat.backend.entity.OperatorVerificationStatus;
import com.yatayat.backend.entity.TransportOperator;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.BusRepository;
import com.yatayat.backend.repository.TransportOperatorRepository;
import com.yatayat.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OperatorBusInitializerTests {
    @Autowired UserRepository userRepository;
    @Autowired TransportOperatorRepository operatorRepository;
    @Autowired BusRepository busRepository;

    private OperatorBusInitializer initializer;
    private TransportOperator approvedOperator;

    @BeforeEach
    void setUp() {
        initializer = new OperatorBusInitializer(operatorRepository, busRepository);
        approvedOperator = operator("Everest Yatayat", "everest@example.com",
                "DOT-OP-1001", OperatorVerificationStatus.APPROVED);
        operator("Waiting Yatayat", "waiting@example.com",
                "DOT-OP-1002", OperatorVerificationStatus.PENDING);
    }

    @Test
    void seedsRealisticApprovedBusesForExistingApprovedOperators() throws Exception {
        initializer.run();

        List<Bus> buses = busRepository.findByOperatorOrderByCreatedAtDesc(approvedOperator);
        assertThat(buses).hasSize(OperatorBusInitializer.busesPerOperator());
        assertThat(buses).allSatisfy(bus -> {
            assertThat(bus.getOperator()).isEqualTo(approvedOperator);
            assertThat(bus.getOperatorName()).isEqualTo(approvedOperator.getName());
            assertThat(bus.getStatus()).isEqualTo(BusStatus.APPROVED);
            assertThat(bus.getBusNumber()).startsWith("BA 2 KHA ");
            assertThat(bus.getPermitNumber()).startsWith("DOTM-SEED-");
            assertThat(bus.getSeatCapacity()).isBetween(1, 100);
            assertThat(bus.getApprovedAt()).isNotNull();
        });
        assertThat(busRepository.findAllByOrderByCreatedAtDesc())
                .hasSize(OperatorBusInitializer.busesPerOperator());
    }

    @Test
    void restartIsIdempotentAndPreservesExistingBuses() throws Exception {
        initializer.run();
        long countAfterFirstRun = busRepository.count();

        initializer.run();

        assertThat(busRepository.count()).isEqualTo(countAfterFirstRun);
        assertThat(busRepository.findByOperatorOrderByCreatedAtDesc(approvedOperator))
                .hasSize(OperatorBusInitializer.busesPerOperator());
    }

    private TransportOperator operator(String name, String email, String registration,
                                       OperatorVerificationStatus status) {
        User user = userRepository.saveAndFlush(
                new User(name, email, "9800000000", "encoded", "OPERATOR"));
        TransportOperator operator = new TransportOperator();
        operator.setUser(user);
        operator.setName(name);
        operator.setOperatorType(OperatorType.PRIVATE_COMPANY);
        operator.setRegistrationNumber(registration);
        operator.setPermitNumber("PERMIT-" + registration);
        operator.setContactPerson("Operations Manager");
        operator.setEmail(email);
        operator.setPhone("9800000000");
        operator.setAddress("Kathmandu, Nepal");
        operator.setVerificationStatus(status);
        return operatorRepository.saveAndFlush(operator);
    }
}
