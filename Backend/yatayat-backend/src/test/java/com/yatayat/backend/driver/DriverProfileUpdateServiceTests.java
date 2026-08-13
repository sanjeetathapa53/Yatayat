package com.yatayat.backend.driver;

import com.yatayat.backend.dto.DriverProfileUpdateRequest;
import com.yatayat.backend.entity.DriverProfile;
import com.yatayat.backend.entity.DriverVerificationStatus;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.DriverDocumentRepository;
import com.yatayat.backend.repository.DriverProfileRepository;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.service.DriverApplicationService;
import com.yatayat.backend.service.DriverDocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverProfileUpdateServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DriverProfileRepository driverProfileRepository;
    @Mock
    private DriverDocumentRepository driverDocumentRepository;
    @Mock
    private DriverDocumentStorageService storageService;

    private DriverApplicationService service;
    private User driver;
    private DriverProfile profile;

    @BeforeEach
    void setUp() {
        service = new DriverApplicationService(
                userRepository,
                driverProfileRepository,
                driverDocumentRepository,
                storageService
        );
        driver = new User(
                "Old Name",
                "driver@example.com",
                "9800000000",
                "encoded",
                "DRIVER"
        );
        driver.setId(7L);
        profile = new DriverProfile(driver);
        profile.setId(11L);
        profile.setVerificationStatus(DriverVerificationStatus.APPROVED);
    }

    @Test
    void updatesOnlyTheAuthenticatedDriversEditableProfileFields() {
        when(driverProfileRepository.findByUser(driver))
                .thenReturn(Optional.of(profile));
        when(userRepository.findById(7L)).thenReturn(Optional.of(driver));

        DriverProfileUpdateRequest request = new DriverProfileUpdateRequest(
                "  Updated Driver  ",
                "  +977 9812345678  ",
                "  Kathmandu  ",
                "  Lalitpur  ",
                "  Emergency Person  ",
                "  9800000001  ",
                "  Bagmati  "
        );

        Map<String, Object> response =
                service.updateDriverProfile(driver, request);

        assertThat(driver.getFullName()).isEqualTo("Updated Driver");
        assertThat(driver.getPhone()).isEqualTo("+977 9812345678");
        assertThat(profile.getPermanentAddress()).isEqualTo("Kathmandu");
        assertThat(profile.getCurrentAddress()).isEqualTo("Lalitpur");
        assertThat(profile.getEmergencyContactName())
                .isEqualTo("Emergency Person");
        assertThat(profile.getEmergencyContactPhone())
                .isEqualTo("9800000001");
        assertThat(profile.getPreferredOperatingArea()).isEqualTo("Bagmati");
        assertThat(profile.getVerificationStatus())
                .isEqualTo(DriverVerificationStatus.APPROVED);
        assertThat(response.get("message"))
                .isEqualTo("Driver profile updated successfully");
        verify(userRepository).save(driver);
        verify(driverProfileRepository).save(profile);
    }

    @Test
    void rejectsUpdateWhenAuthenticatedDriverHasNoProfile() {
        when(driverProfileRepository.findByUser(driver))
                .thenReturn(Optional.empty());

        DriverProfileUpdateRequest request = new DriverProfileUpdateRequest(
                "Updated Driver",
                "9812345678",
                "Kathmandu",
                "Lalitpur",
                "Emergency Person",
                "9800000001",
                ""
        );

        assertThatThrownBy(() -> service.updateDriverProfile(driver, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Driver profile was not found");
    }
}