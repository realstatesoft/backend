package com.openroof.openroof.service;

import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    @DisplayName("getOverview() expone la acción rápida de reportes hacia /properties")
    void getOverview_reportsQuickActionPointsToFlags() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countActiveUsers(org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(userRepository.countCreatedBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(propertyRepository.count()).thenReturn(0L);
        when(propertyRepository.countByStatus(PropertyStatus.PENDING)).thenReturn(0L);
        when(propertyRepository.countByStatusIn(org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(propertyRepository.countCreatedBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(propertyRepository.findByDeletedAtIsNullAndTrashedAtIsNullAndStatusIn(List.of(PropertyStatus.PENDING, PropertyStatus.APPROVED, PropertyStatus.REJECTED)))
                .thenReturn(new ArrayList<>());

        var overview = adminDashboardService.getOverview();

        assertThat(overview.quickActions())
                .anySatisfy(action -> {
                    assertThat(action.title()).isEqualTo("Ver reportes");
                    assertThat(action.path()).isEqualTo("/properties");
                });
    }
}
