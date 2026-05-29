package com.openroof.openroof.mapper;

import com.openroof.openroof.common.embeddable.GeoLocation;
import com.openroof.openroof.dto.property.CreatePropertyRequest;
import com.openroof.openroof.dto.property.PropertySummaryResponse;
import com.openroof.openroof.dto.property.UpdatePropertyRequest;
import com.openroof.openroof.model.enums.ConstructionStatus;
import com.openroof.openroof.model.enums.ListingType;
import com.openroof.openroof.model.enums.PropertyCategory;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.Visibility;
import com.openroof.openroof.model.property.Highlight;
import com.openroof.openroof.model.property.Location;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.property.PropertyMedia;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@code PropertyMapper}.
 */
@DisplayName("PropertyMapper")
class PropertyMapperTest {

    private final AgentProfileRepository agentProfileRepository = mock(AgentProfileRepository.class);
    private final PropertyMapper mapper = new PropertyMapper(agentProfileRepository);

    // ─── toResponse ──────────────────────────────────────────────────────────

    /**
     * toResponse_withNullGeoLocation_returnsNullLatLng.
     */
    @Test
    @DisplayName("toResponse_withNullGeoLocation_returnsNullLatLng")
    void toResponse_withNullGeoLocation_returnsNullLatLng() {
        Property p = Property.builder()
                .title("Test Property").propertyType(PropertyType.HOUSE)
                .address("Test Address").price(BigDecimal.valueOf(200_000))
                .geoLocation(null).build();

        var resp = mapper.toResponse(p);

        assertThat(resp.lat()).isNull();
        assertThat(resp.lng()).isNull();
    }

    /**
     * toResponse_withGeoLocation_mapsLatLng.
     */
    @Test
    @DisplayName("toResponse_withGeoLocation_mapsLatLng")
    void toResponse_withGeoLocation_mapsLatLng() {
        Property p = Property.builder()
                .title("Test").propertyType(PropertyType.APARTMENT).address("Addr").price(BigDecimal.ONE)
                .geoLocation(GeoLocation.builder()
                        .lat(BigDecimal.valueOf(-34.60))
                        .lng(BigDecimal.valueOf(-58.38))
                        .build())
                .build();

        var resp = mapper.toResponse(p);

        assertThat(resp.lat()).isEqualByComparingTo("-34.6");
        assertThat(resp.lng()).isEqualByComparingTo("-58.38");
    }

    /**
     * toResponse_withNullConstruction_returnsNullConstructionFields.
     */
    @Test
    @DisplayName("toResponse_withNullConstruction_returnsNullConstructionFields")
    void toResponse_withNullConstruction_returnsNullConstructionFields() {
        Property p = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .build();

        var resp = mapper.toResponse(p);

        assertThat(resp.constructionYear()).isNull();
        assertThat(resp.constructionStatus()).isNull();
        assertThat(resp.structureMaterial()).isNull();
    }

    /**
     * toResponse_withNullUtilities_returnsNullUtilityFields.
     */
    @Test
    @DisplayName("toResponse_withNullUtilities_returnsNullUtilityFields")
    void toResponse_withNullUtilities_returnsNullUtilityFields() {
        Property p = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .build();

        var resp = mapper.toResponse(p);

        assertThat(resp.waterConnection()).isNull();
        assertThat(resp.sanitaryInstallation()).isNull();
        assertThat(resp.electricityInstallation()).isNull();
    }

    /**
     * toResponse_withActiveHighlight_isHighlightedTrue.
     */
    @Test
    @DisplayName("toResponse_withActiveHighlight_isHighlightedTrue")
    void toResponse_withActiveHighlight_isHighlightedTrue() {
        LocalDateTime now = LocalDateTime.now();
        Highlight active = Highlight.builder()
                .highlightedFrom(now.minusHours(2))
                .highlightedUntil(now.plusHours(2))
                .build();

        Property p = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .highlights(List.of(active)).build();

        var resp = mapper.toResponse(p);

        assertThat(resp.highlighted()).isTrue();
        assertThat(resp.highlightedUntil()).isEqualTo(active.getHighlightedUntil());
    }

    /**
     * toResponse_withAllExpiredHighlights_isHighlightedFalse.
     */
    @Test
    @DisplayName("toResponse_withAllExpiredHighlights_isHighlightedFalse")
    void toResponse_withAllExpiredHighlights_isHighlightedFalse() {
        LocalDateTime now = LocalDateTime.now();
        Highlight expired = Highlight.builder()
                .highlightedFrom(now.minusDays(2))
                .highlightedUntil(now.minusDays(1))
                .build();

        Property p = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .highlights(List.of(expired)).build();

        var resp = mapper.toResponse(p);

        assertThat(resp.highlighted()).isFalse();
        assertThat(resp.highlightedUntil()).isNull();
    }

    /**
     * toResponse_withNullHighlights_isHighlightedFalse.
     */
    @Test
    @DisplayName("toResponse_withNullHighlights_isHighlightedFalse")
    void toResponse_withNullHighlights_isHighlightedFalse() {
        Property p = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .highlights(null).build();

        var resp = mapper.toResponse(p);

        assertThat(resp.highlighted()).isFalse();
    }

    /**
     * toResponse_withNullCollections_returnsEmptyLists.
     */
    @Test
    @DisplayName("toResponse_withNullCollections_returnsEmptyLists")
    void toResponse_withNullCollections_returnsEmptyLists() {
        Property p = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .rooms(null).media(null).exteriorFeatures(null).build();

        var resp = mapper.toResponse(p);

        assertThat(resp.rooms()).isEmpty();
        assertThat(resp.media()).isEmpty();
        assertThat(resp.exteriorFeatureIds()).isEmpty();
    }

    /**
     * toResponse_withOwnerAgentLocation_mapsIds.
     */
    @Test
    @DisplayName("toResponse_withOwnerAgentLocation_mapsIds")
    void toResponse_withOwnerAgentLocation_mapsIds() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        when(owner.getName()).thenReturn("Owner Name");

        AgentProfile agent = mock(AgentProfile.class);
        when(agent.getId()).thenReturn(2L);

        Location location = mock(Location.class);
        when(location.getId()).thenReturn(3L);
        when(location.getName()).thenReturn("Buenos Aires");

        Property p = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .owner(owner).agent(agent).location(location).build();

        var resp = mapper.toResponse(p);

        assertThat(resp.ownerId()).isEqualTo(1L);
        assertThat(resp.ownerName()).isEqualTo("Owner Name");
        assertThat(resp.agentId()).isEqualTo(2L);
        assertThat(resp.locationId()).isEqualTo(3L);
        assertThat(resp.locationName()).isEqualTo("Buenos Aires");
    }

    /**
     * toResponse_withOwnerHavingAvatar_mapsOwnerAvatarUrl.
     */
    @Test
    @DisplayName("toResponse_withOwnerHavingAvatar_mapsOwnerAvatarUrl")
    void toResponse_withOwnerHavingAvatar_mapsOwnerAvatarUrl() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        when(owner.getName()).thenReturn("Cesar Ayala");
        when(owner.getAvatarUrl()).thenReturn("https://example.com/avatars/cesar.jpg");

        when(agentProfileRepository.findByUser_Id(10L)).thenReturn(Optional.empty());

        Property p = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .owner(owner).build();

        var resp = mapper.toResponse(p);

        assertThat(resp.ownerAvatarUrl()).isEqualTo("https://example.com/avatars/cesar.jpg");
        assertThat(resp.ownerAgentProfileId()).isNull();
    }

    /**
     * toResponse_withOwnerNoAvatar_ownerAvatarUrlIsNull.
     */
    @Test
    @DisplayName("toResponse_withOwnerNoAvatar_ownerAvatarUrlIsNull")
    void toResponse_withOwnerNoAvatar_ownerAvatarUrlIsNull() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(11L);
        when(owner.getName()).thenReturn("No Avatar User");
        when(owner.getAvatarUrl()).thenReturn(null);

        when(agentProfileRepository.findByUser_Id(11L)).thenReturn(Optional.empty());

        Property p = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .owner(owner).build();

        var resp = mapper.toResponse(p);

        assertThat(resp.ownerAvatarUrl()).isNull();
        assertThat(resp.ownerAgentProfileId()).isNull();
    }

    /**
     * toResponse_withOwnerThatIsAgent_mapsOwnerAgentProfileId.
     */
    @Test
    @DisplayName("toResponse_withOwnerThatIsAgent_mapsOwnerAgentProfileId")
    void toResponse_withOwnerThatIsAgent_mapsOwnerAgentProfileId() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(20L);
        when(owner.getName()).thenReturn("Agent Owner");
        when(owner.getAvatarUrl()).thenReturn("https://example.com/avatars/agent.jpg");

        AgentProfile agentProfile = mock(AgentProfile.class);
        when(agentProfile.getId()).thenReturn(99L);
        when(agentProfileRepository.findByUser_Id(20L)).thenReturn(Optional.of(agentProfile));

        Property p = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .owner(owner).build();

        var resp = mapper.toResponse(p);

        assertThat(resp.ownerAvatarUrl()).isEqualTo("https://example.com/avatars/agent.jpg");
        assertThat(resp.ownerAgentProfileId()).isEqualTo(99L);
    }

    /**
     * toResponse_withNullOwner_ownerAvatarUrlAndAgentProfileIdAreNull.
     */
    @Test
    @DisplayName("toResponse_withNullOwner_ownerAvatarUrlAndAgentProfileIdAreNull")
    void toResponse_withNullOwner_ownerAvatarUrlAndAgentProfileIdAreNull() {
        Property p = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .owner(null).build();

        var resp = mapper.toResponse(p);

        assertThat(resp.ownerAvatarUrl()).isNull();
        assertThat(resp.ownerAgentProfileId()).isNull();
    }

    /**
     * toResponse_withMediaList_mapsMediaCorrectly.
     */
    @Test
    @DisplayName("toResponse_withMediaList_mapsMediaCorrectly")
    void toResponse_withMediaList_mapsMediaCorrectly() {
        PropertyMedia media = PropertyMedia.builder()
                .url("http://example.com/img.jpg")
                .thumbnailUrl("http://example.com/thumb.jpg")
                .isPrimary(true).orderIndex(0).title("Front view").build();

        Property p = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .media(List.of(media)).build();

        var resp = mapper.toResponse(p);

        assertThat(resp.media()).hasSize(1);
        assertThat(resp.media().get(0).url()).isEqualTo("http://example.com/img.jpg");
        assertThat(resp.media().get(0).isPrimary()).isTrue();
    }

    // ─── toSummaryResponse ───────────────────────────────────────────────────

    /**
     * toSummaryResponse_withPrimaryImage_returnsPrimaryImageUrl.
     */
    @Test
    @DisplayName("toSummaryResponse_withPrimaryImage_returnsPrimaryImageUrl")
    void toSummaryResponse_withPrimaryImage_returnsPrimaryImageUrl() {
        PropertyMedia secondary = PropertyMedia.builder()
                .url("http://example.com/secondary.jpg").isPrimary(false).build();
        PropertyMedia primary = PropertyMedia.builder()
                .url("http://example.com/primary.jpg").isPrimary(true).build();

        Property p = Property.builder()
                .title("Property Title").price(BigDecimal.valueOf(150_000))
                .propertyType(PropertyType.APARTMENT).category(PropertyCategory.SALE)
                .address("Main St 1").media(List.of(secondary, primary))
                .status(PropertyStatus.PUBLISHED).build();

        PropertySummaryResponse resp = mapper.toSummaryResponse(p);

        assertThat(resp.primaryImageUrl()).isEqualTo("http://example.com/primary.jpg");
    }

    /**
     * toSummaryResponse_withNoPrimaryImage_returnsNull.
     */
    @Test
    @DisplayName("toSummaryResponse_withNoPrimaryImage_returnsNull")
    void toSummaryResponse_withNoPrimaryImage_returnsNull() {
        PropertyMedia notPrimary = PropertyMedia.builder()
                .url("http://example.com/img.jpg").isPrimary(false).build();

        Property p = Property.builder()
                .title("T").price(BigDecimal.ONE).propertyType(PropertyType.HOUSE).address("A")
                .media(List.of(notPrimary)).build();

        PropertySummaryResponse resp = mapper.toSummaryResponse(p);

        assertThat(resp.primaryImageUrl()).isNull();
    }

    /**
     * toSummaryResponse_withRelevanceScore_setsScore.
     */
    @Test
    @DisplayName("toSummaryResponse_withRelevanceScore_setsScore")
    void toSummaryResponse_withRelevanceScore_setsScore() {
        Property p = Property.builder()
                .title("T").price(BigDecimal.ONE).propertyType(PropertyType.HOUSE).address("A")
                .build();

        PropertySummaryResponse resp = mapper.toSummaryResponse(p, 42);

        assertThat(resp.relevanceScore()).isEqualTo(42);
    }

    /**
     * toSummaryResponse_noScoreOverload_defaultsTo0.
     */
    @Test
    @DisplayName("toSummaryResponse_noScoreOverload_defaultsTo0")
    void toSummaryResponse_noScoreOverload_defaultsTo0() {
        Property p = Property.builder()
                .title("T").price(BigDecimal.ONE).propertyType(PropertyType.HOUSE).address("A")
                .build();

        PropertySummaryResponse resp = mapper.toSummaryResponse(p);

        assertThat(resp.relevanceScore()).isEqualTo(0);
    }

    /**
     * toSummaryResponse_withNullMedia_returnsNullPrimaryImage.
     */
    @Test
    @DisplayName("toSummaryResponse_withNullMedia_returnsNullPrimaryImage")
    void toSummaryResponse_withNullMedia_returnsNullPrimaryImage() {
        Property p = Property.builder()
                .title("T").price(BigDecimal.ONE).propertyType(PropertyType.HOUSE).address("A")
                .media(null).build();

        PropertySummaryResponse resp = mapper.toSummaryResponse(p);

        assertThat(resp.primaryImageUrl()).isNull();
    }

    // ─── toEntity ────────────────────────────────────────────────────────────
    // CreatePropertyRequest field order (37 params):
    // title, desc, propType, category, listingType,
    // rentAmount, rentCurrency, rentFrequency, rentBillingCycle,
    // address, lat, lng, locationId, price,
    // bedrooms, bathrooms, halfBathrooms, fullBathrooms, surfaceArea, builtArea, parkingSpaces, floorsCount,
    // ownerId, agentId,
    // constructionYear, constructionStatus, structureMaterial, wallsMaterial, floorMaterial, roofMaterial,
    // waterConnection, sanitaryInstallation, electricityInstallation,
    // availability, rooms, media, exteriorFeatureIds

    /**
     * toEntity_withRentListingType_setsRentFields.
     */
    @Test
    @DisplayName("toEntity_withRentListingType_setsRentFields")
    void toEntity_withRentListingType_setsRentFields() {
        CreatePropertyRequest req = new CreatePropertyRequest(
                "Apt for Rent", "Nice apt", PropertyType.APARTMENT, PropertyCategory.RENT,
                ListingType.RENT,
                BigDecimal.valueOf(1500), "USD", "MONTHLY", "MONTHLY",
                "Corrientes 500", null, null, null,
                BigDecimal.valueOf(1500),
                null, null, null, null, null, null, null, null,
                1L, null,
                null, null, null, null, null, null,
                null, null, null,
                null, null, null, null);

        Property entity = mapper.toEntity(req);

        assertThat(entity.getListingType()).isEqualTo(ListingType.RENT);
        assertThat(entity.getRentAmount()).isEqualByComparingTo("1500");
        assertThat(entity.getRentCurrency()).isEqualTo("USD");
        assertThat(entity.getRentFrequency()).isEqualTo("MONTHLY");
    }

    /**
     * toEntity_withSaleListingType_doesNotSetRentFields.
     */
    @Test
    @DisplayName("toEntity_withSaleListingType_doesNotSetRentFields")
    void toEntity_withSaleListingType_doesNotSetRentFields() {
        CreatePropertyRequest req = new CreatePropertyRequest(
                "House for Sale", null, PropertyType.HOUSE, PropertyCategory.SALE,
                ListingType.SALE,
                null, null, null, null,
                "Rivadavia 100", null, null, null,
                BigDecimal.valueOf(300_000),
                3, null, null, null, null, null, null, null,
                1L, null,
                null, null, null, null, null, null,
                null, null, null,
                null, null, null, null);

        Property entity = mapper.toEntity(req);

        assertThat(entity.getListingType()).isEqualTo(ListingType.SALE);
        assertThat(entity.getRentAmount()).isNull();
        assertThat(entity.getRentCurrency()).isNull();
    }

    /**
     * toEntity_withGeoLocation_setsGeoLocation.
     */
    @Test
    @DisplayName("toEntity_withGeoLocation_setsGeoLocation")
    void toEntity_withGeoLocation_setsGeoLocation() {
        CreatePropertyRequest req = new CreatePropertyRequest(
                "House", null, PropertyType.HOUSE, PropertyCategory.SALE,
                ListingType.SALE,
                null, null, null, null,
                "Av. Libertador 1",
                BigDecimal.valueOf(-34.6), BigDecimal.valueOf(-58.4), null,
                BigDecimal.valueOf(400_000),
                null, null, null, null, null, null, null, null,
                1L, null,
                null, null, null, null, null, null,
                null, null, null,
                null, null, null, null);

        Property entity = mapper.toEntity(req);

        assertThat(entity.getGeoLocation()).isNotNull();
        assertThat(entity.getGeoLocation().getLat()).isEqualByComparingTo("-34.6");
        assertThat(entity.getGeoLocation().getLng()).isEqualByComparingTo("-58.4");
    }

    /**
     * toEntity_withConstructionData_setsConstructionDetails.
     */
    @Test
    @DisplayName("toEntity_withConstructionData_setsConstructionDetails")
    void toEntity_withConstructionData_setsConstructionDetails() {
        CreatePropertyRequest req = new CreatePropertyRequest(
                "House", null, PropertyType.HOUSE, PropertyCategory.SALE,
                ListingType.SALE,
                null, null, null, null,
                "Addr", null, null, null,
                BigDecimal.valueOf(200_000),
                null, null, null, null, null, null, null, null,
                1L, null,
                2010, ConstructionStatus.USED, "Hormigon", null, "Ceramico", null,
                null, null, null,
                null, null, null, null);

        Property entity = mapper.toEntity(req);

        assertThat(entity.getConstruction()).isNotNull();
        assertThat(entity.getConstruction().getYear()).isEqualTo(2010);
        assertThat(entity.getConstruction().getStructureMaterial()).isEqualTo("Hormigon");
        assertThat(entity.getConstruction().getFloorMaterial()).isEqualTo("Ceramico");
    }

    /**
     * toEntity_withNoConstructionData_constructionIsNull.
     */
    @Test
    @DisplayName("toEntity_withNoConstructionData_constructionIsNull")
    void toEntity_withNoConstructionData_constructionIsNull() {
        CreatePropertyRequest req = new CreatePropertyRequest(
                "House", null, PropertyType.HOUSE, PropertyCategory.SALE,
                ListingType.SALE,
                null, null, null, null,
                "Addr", null, null, null,
                BigDecimal.valueOf(200_000),
                null, null, null, null, null, null, null, null,
                1L, null,
                null, null, null, null, null, null,
                null, null, null,
                null, null, null, null);

        Property entity = mapper.toEntity(req);

        assertThat(entity.getConstruction()).isNull();
    }

    // ─── updateEntity ────────────────────────────────────────────────────────
    // UpdatePropertyRequest field order (37 params):
    // title, desc, propType, category, listingType,
    // rentAmount, rentCurrency, rentFrequency, rentBillingCycle,
    // address, lat, lng, locationId, price,
    // bedrooms, bathrooms, halfBathrooms, fullBathrooms, surfaceArea, builtArea, parkingSpaces, floorsCount,
    // agentId,  ← NOTE: no ownerId
    // constructionYear, constructionStatus, structureMaterial, wallsMaterial, floorMaterial, roofMaterial,
    // waterConnection, sanitaryInstallation, electricityInstallation,
    // availability, visibility,  ← visibility is extra here
    // rooms, media, exteriorFeatureIds

    /**
     * updateEntity_switchFromRentToSale_clearsAllRentFields.
     */
    @Test
    @DisplayName("updateEntity_switchFromRentToSale_clearsAllRentFields")
    void updateEntity_switchFromRentToSale_clearsAllRentFields() {
        Property property = Property.builder()
                .title("Old Title").propertyType(PropertyType.APARTMENT).address("Old Addr")
                .price(BigDecimal.valueOf(100_000)).listingType(ListingType.RENT)
                .rentAmount(BigDecimal.valueOf(1000)).rentCurrency("USD")
                .rentFrequency("MONTHLY").rentBillingCycle("MONTHLY").build();

        UpdatePropertyRequest req = new UpdatePropertyRequest(
                null, null, null, null,
                ListingType.SALE,  // switch to SALE
                null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null,
                null, null, null, null, null, null,
                null, null, null,
                null, null, null, null, null);

        mapper.updateEntity(property, req);

        assertThat(property.getListingType()).isEqualTo(ListingType.SALE);
        assertThat(property.getRentAmount()).isNull();
        assertThat(property.getRentCurrency()).isNull();
        assertThat(property.getRentFrequency()).isNull();
        assertThat(property.getRentBillingCycle()).isNull();
    }

    /**
     * updateEntity_withBothLatLngNull_clearsGeoLocation.
     */
    @Test
    @DisplayName("updateEntity_withBothLatLngNull_clearsGeoLocation")
    void updateEntity_withBothLatLngNull_clearsGeoLocation() {
        Property property = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .geoLocation(GeoLocation.builder()
                        .lat(BigDecimal.valueOf(-34.6)).lng(BigDecimal.valueOf(-58.4)).build())
                .build();

        UpdatePropertyRequest req = new UpdatePropertyRequest(
                null, null, null, null, null,
                null, null, null, null,
                null,
                null, null,  // lat=null, lng=null → clears geoLocation
                null, null,
                null, null, null, null, null, null, null, null,
                null,
                null, null, null, null, null, null,
                null, null, null,
                null, null, null, null, null);

        mapper.updateEntity(property, req);

        assertThat(property.getGeoLocation()).isNull();
    }

    /**
     * updateEntity_withNewLatLng_updatesGeoLocation.
     */
    @Test
    @DisplayName("updateEntity_withNewLatLng_updatesGeoLocation")
    void updateEntity_withNewLatLng_updatesGeoLocation() {
        Property property = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .build();

        UpdatePropertyRequest req = new UpdatePropertyRequest(
                null, null, null, null, null,
                null, null, null, null,
                null,
                BigDecimal.valueOf(-34.7), BigDecimal.valueOf(-58.5),  // lat, lng
                null, null,
                null, null, null, null, null, null, null, null,
                null,
                null, null, null, null, null, null,
                null, null, null,
                null, null, null, null, null);

        mapper.updateEntity(property, req);

        assertThat(property.getGeoLocation()).isNotNull();
        assertThat(property.getGeoLocation().getLat()).isEqualByComparingTo("-34.7");
        assertThat(property.getGeoLocation().getLng()).isEqualByComparingTo("-58.5");
    }

    /**
     * updateEntity_withVisibilityPublic_updatesVisibility.
     */
    @Test
    @DisplayName("updateEntity_withVisibilityPublic_updatesVisibility")
    void updateEntity_withVisibilityPublic_updatesVisibility() {
        Property property = Property.builder()
                .title("T").propertyType(PropertyType.HOUSE).address("A").price(BigDecimal.ONE)
                .build();

        UpdatePropertyRequest req = new UpdatePropertyRequest(
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null,
                null, null, null, null, null, null,
                null, null, null,
                null, Visibility.PUBLIC,  // visibility
                null, null, null);

        mapper.updateEntity(property, req);

        assertThat(property.getVisibility()).isEqualTo(Visibility.PUBLIC);
    }
}
