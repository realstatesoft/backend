package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.agent.AgentAgendaResponse;
import com.openroof.openroof.dto.agent.CreateAgentAgendaRequest;
import com.openroof.openroof.dto.agent.UpdateAgentAgendaRequest;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.EventType;
import com.openroof.openroof.model.interaction.AgentAgenda;
import com.openroof.openroof.model.interaction.Visit;
import com.openroof.openroof.model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AgentAgendaMapper")
class AgentAgendaMapperTest {

    private final AgentAgendaMapper mapper = new AgentAgendaMapper();

    private static final LocalDateTime START = LocalDateTime.of(2026, 6, 1, 10, 0);
    private static final LocalDateTime END   = LocalDateTime.of(2026, 6, 1, 11, 0);

    // ─── helpers ─────────────────────────────────────────────────────────────

    private User mockUser(Long id) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        return u;
    }

    private AgentProfile mockAgent(Long id) {
        AgentProfile a = mock(AgentProfile.class);
        when(a.getId()).thenReturn(id);
        return a;
    }

    private Visit mockVisit(Long id, String buyerName) {
        Visit v = mock(Visit.class);
        when(v.getId()).thenReturn(id);
        User buyer = null;
        if (buyerName != null) {
            buyer = mock(User.class);
            when(buyer.getName()).thenReturn(buyerName);
        }
        when(v.getBuyer()).thenReturn(buyer);
        return v;
    }

    private AgentAgenda buildAgenda(Long id, User user, AgentProfile agent, Visit visit,
                                    EventType type, String title) {
        AgentAgenda a = AgentAgenda.builder()
                .user(user)
                .agent(agent)
                .visit(visit)
                .eventType(type)
                .title(title)
                .description("desc")
                .startsAt(START)
                .endsAt(END)
                .location("Sala 1")
                .notes("nota")
                .build();
        a.setId(id);
        return a;
    }

    // ─── toResponse — null guard ─────────────────────────────────────────────

    @Test
    @DisplayName("toResponse with null entity returns null")
    void toResponse_nullEntity_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    // ─── toResponse — scalar fields ──────────────────────────────────────────

    @Test
    @DisplayName("toResponse maps all scalar fields verbatim")
    void toResponse_fullEntity_mapsAllFields() {
        User user = mockUser(10L);
        AgentProfile agent = mockAgent(20L);
        Visit visit = mockVisit(5L, "Juan Comprador");
        AgentAgenda entity = buildAgenda(1L, user, agent, visit, EventType.VISIT, "Visita Casa");

        AgentAgendaResponse resp = mapper.toResponse(entity);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.userId()).isEqualTo(10L);
        assertThat(resp.agentId()).isEqualTo(20L);
        assertThat(resp.visitId()).isEqualTo(5L);
        assertThat(resp.eventType()).isEqualTo(EventType.VISIT);
        assertThat(resp.title()).isEqualTo("Visita Casa");
        assertThat(resp.description()).isEqualTo("desc");
        assertThat(resp.startsAt()).isEqualTo(START);
        assertThat(resp.endsAt()).isEqualTo(END);
        assertThat(resp.location()).isEqualTo("Sala 1");
        assertThat(resp.notes()).isEqualTo("nota");
        assertThat(resp.clientName()).isEqualTo("Juan Comprador");
    }

    @Test
    @DisplayName("toResponse with null user/agent/visit produces null foreign-key fields")
    void toResponse_nullForeignEntities_producesNullIds() {
        AgentAgenda entity = buildAgenda(2L, null, null, null, EventType.MEETING, "Reunión");

        AgentAgendaResponse resp = mapper.toResponse(entity);

        assertThat(resp.userId()).isNull();
        assertThat(resp.agentId()).isNull();
        assertThat(resp.visitId()).isNull();
        assertThat(resp.clientName()).isNull();
    }

    @Test
    @DisplayName("toResponse with visit that has no buyer produces null clientName")
    void toResponse_visitWithNoBuyer_clientNameIsNull() {
        Visit visitNoBuyer = mockVisit(7L, null);
        AgentAgenda entity = buildAgenda(3L, mockUser(1L), null, visitNoBuyer, EventType.BLOCKED, "Bloqueado");

        AgentAgendaResponse resp = mapper.toResponse(entity);

        assertThat(resp.visitId()).isEqualTo(7L);
        assertThat(resp.clientName()).isNull();
    }

    // ─── toEntity ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toEntity with null request returns null")
    void toEntity_nullRequest_returnsNull() {
        assertThat(mapper.toEntity(null, mockUser(1L), mockAgent(2L), null)).isNull();
    }

    @Test
    @DisplayName("toEntity maps all request fields into a new AgentAgenda")
    void toEntity_validRequest_mapsAllFields() {
        User user = mockUser(10L);
        AgentProfile agent = mockAgent(20L);
        CreateAgentAgendaRequest req = new CreateAgentAgendaRequest(
                EventType.MEETING, "Reunión mensual", "Repaso métricas",
                START, END, "Oficina central", "Traer laptop", null
        );

        AgentAgenda entity = mapper.toEntity(req, user, agent, null);

        assertThat(entity).isNotNull();
        assertThat(entity.getUser()).isSameAs(user);
        assertThat(entity.getAgent()).isSameAs(agent);
        assertThat(entity.getVisit()).isNull();
        assertThat(entity.getEventType()).isEqualTo(EventType.MEETING);
        assertThat(entity.getTitle()).isEqualTo("Reunión mensual");
        assertThat(entity.getDescription()).isEqualTo("Repaso métricas");
        assertThat(entity.getStartsAt()).isEqualTo(START);
        assertThat(entity.getEndsAt()).isEqualTo(END);
        assertThat(entity.getLocation()).isEqualTo("Oficina central");
        assertThat(entity.getNotes()).isEqualTo("Traer laptop");
    }

    @Test
    @DisplayName("toEntity accepts null agent — non-agent users have no AgentProfile")
    void toEntity_nullAgent_entityHasNullAgent() {
        CreateAgentAgendaRequest req = new CreateAgentAgendaRequest(
                EventType.OTHER, "Tarea personal", null, START, END, null, null, null
        );

        AgentAgenda entity = mapper.toEntity(req, mockUser(5L), null, null);

        assertThat(entity.getAgent()).isNull();
    }

    // ─── updateEntity ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateEntity with null request or null entity is a no-op")
    void updateEntity_nullInputs_noOp() {
        AgentAgenda entity = buildAgenda(1L, mockUser(1L), null, null, EventType.VISIT, "Original");
        mapper.updateEntity(entity, null, null); // should not throw
        assertThat(entity.getTitle()).isEqualTo("Original");

        mapper.updateEntity(null, new UpdateAgentAgendaRequest(null,null,null,null,null,null,null,null), null);
        // no exception expected
    }

    @Test
    @DisplayName("updateEntity applies only non-null fields from request")
    void updateEntity_partialRequest_onlyNonNullFieldsUpdated() {
        AgentAgenda entity = buildAgenda(1L, mockUser(1L), null, null, EventType.VISIT, "Título original");
        UpdateAgentAgendaRequest req = new UpdateAgentAgendaRequest(
                EventType.MEETING, "Título nuevo", null, null, null, null, null, null
        );

        mapper.updateEntity(entity, req, null);

        assertThat(entity.getEventType()).isEqualTo(EventType.MEETING);
        assertThat(entity.getTitle()).isEqualTo("Título nuevo");
        assertThat(entity.getDescription()).isEqualTo("desc"); // unchanged
        assertThat(entity.getStartsAt()).isEqualTo(START);     // unchanged
        assertThat(entity.getVisit()).isNull();
    }

    @Test
    @DisplayName("updateEntity always replaces visit — even with null (removes association)")
    void updateEntity_alwaysReplacesVisit() {
        Visit originalVisit = mockVisit(9L, null);
        AgentAgenda entity = buildAgenda(1L, mockUser(1L), null, originalVisit, EventType.VISIT, "Con visita");
        UpdateAgentAgendaRequest req = new UpdateAgentAgendaRequest(null,null,null,null,null,null,null, null);

        mapper.updateEntity(entity, req, null); // pass null as new visit

        assertThat(entity.getVisit()).isNull(); // association removed
    }

    @Test
    @DisplayName("updateEntity replaces visit when a new Visit is provided")
    void updateEntity_withNewVisit_replacesVisit() {
        AgentAgenda entity = buildAgenda(1L, mockUser(1L), null, null, EventType.MEETING, "Sin visita");
        Visit newVisit = mockVisit(42L, "Nuevo comprador");
        UpdateAgentAgendaRequest req = new UpdateAgentAgendaRequest(null,null,null,null,null,null,null,42L);

        mapper.updateEntity(entity, req, newVisit);

        assertThat(entity.getVisit()).isSameAs(newVisit);
    }
}
