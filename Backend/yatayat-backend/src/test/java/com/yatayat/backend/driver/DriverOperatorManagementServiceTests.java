package com.yatayat.backend.driver;

import com.yatayat.backend.dto.DriverInvitationRequest;
import com.yatayat.backend.dto.DriverOperatorAssociationResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.DriverOperatorAssociationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverOperatorManagementServiceTests {

    @Mock private UserRepository userRepository;
    @Mock private DriverProfileRepository driverRepository;
    @Mock private TransportOperatorRepository operatorRepository;
    @Mock private DriverOperatorAssociationRepository associationRepository;
    @Mock private BusRepository busRepository;
    private DriverOperatorAssociationService service;
    private User operatorUser;
    private TransportOperator operator;
    private DriverProfile driver;

    @BeforeEach
    void setUp() {
        service = new DriverOperatorAssociationService(
                userRepository, driverRepository, operatorRepository, associationRepository, busRepository);
        operatorUser = new User("Operator", "operator@example.com", "9800000000", "encoded", "OPERATOR");
        operator = new TransportOperator();
        operator.setId(1L);
        operator.setUser(operatorUser);
        operator.setVerificationStatus(OperatorVerificationStatus.APPROVED);
        User driverUser = new User("Driver One", "driver@example.com", "9800000001", "encoded", "DRIVER");
        driver = new DriverProfile(driverUser);
        driver.setId(2L);
        driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        when(userRepository.findByEmailIgnoreCase("operator@example.com")).thenReturn(Optional.of(operatorUser));
        when(operatorRepository.findByUser(operatorUser)).thenReturn(Optional.of(operator));
    }

    @Test
    void activeDriverResponseIncludesOnlyThisOperatorsAssignedBuses() {
        DriverOperatorAssociation association = association(DriverOperatorAssociationStatus.ACTIVE);
        Bus bus = bus();
        when(associationRepository.findByOperatorOrderByInvitedAtDesc(operator)).thenReturn(List.of(association));
        when(busRepository.findByOperatorAndAssignedDriver(operator, driver)).thenReturn(List.of(bus));

        DriverOperatorAssociationResponse response = service.getOperatorDrivers("operator@example.com").get(0);

        assertEquals(1, response.assignedBuses().size());
        assertEquals("BA 1 PA 1234", response.assignedBuses().get(0).busNumber());
    }

    @Test
    void removeEndsAssociationAndUnassignsOperatorBuses() {
        DriverOperatorAssociation association = association(DriverOperatorAssociationStatus.ACTIVE);
        Bus bus = bus();
        bus.setAssignedDriver(driver);
        when(associationRepository.findLockedByIdAndOperator(5L, operator)).thenReturn(Optional.of(association));
        when(busRepository.findByOperatorAndAssignedDriver(operator, driver))
                .thenReturn(List.of(bus), List.of());
        when(associationRepository.saveAndFlush(association)).thenReturn(association);

        DriverOperatorAssociationResponse response = service.remove("operator@example.com", 5L);

        assertEquals("REMOVED", response.associationStatus());
        assertNull(bus.getAssignedDriver());
        verify(busRepository).saveAll(List.of(bus));
    }

    @Test
    void invitingDriverWithActiveAssociationIsPrevented() {
        when(driverRepository.findLockedById(2L)).thenReturn(Optional.of(driver));
        when(associationRepository.findByDriverAndStatus(driver, DriverOperatorAssociationStatus.ACTIVE))
                .thenReturn(Optional.of(association(DriverOperatorAssociationStatus.ACTIVE)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.invite("operator@example.com", new DriverInvitationRequest(2L)));

        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void duplicatePendingInvitationIsPrevented() {
        DriverOperatorAssociation pending = association(DriverOperatorAssociationStatus.PENDING);
        when(driverRepository.findLockedById(2L)).thenReturn(Optional.of(driver));
        when(associationRepository.findByDriverAndStatus(driver, DriverOperatorAssociationStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(associationRepository.findByDriverAndOperator(driver, operator)).thenReturn(Optional.of(pending));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.invite("operator@example.com", new DriverInvitationRequest(2L)));

        assertEquals(409, error.getStatusCode().value());
    }

    private DriverOperatorAssociation association(DriverOperatorAssociationStatus status) {
        DriverOperatorAssociation association = new DriverOperatorAssociation();
        association.setDriver(driver);
        association.setOperator(operator);
        association.setStatus(status);
        try {
            var field = DriverOperatorAssociation.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(association, 5L);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        return association;
    }

    private Bus bus() {
        Bus bus = new Bus();
        bus.setId(8L);
        bus.setBusNumber("BA 1 PA 1234");
        bus.setBusName("Green Line");
        bus.setOperator(operator);
        return bus;
    }
}
