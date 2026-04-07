package com.openroof.openroof.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openroof.openroof.dto.agent.BusySlotResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.VisitRequestStatus;
import com.openroof.openroof.model.interaction.AgentAgenda;
import com.openroof.openroof.model.interaction.VisitRequest;
import com.openroof.openroof.repository.AgentAgendaRepository;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.VisitRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentAvailabilityService {

    private final AgentAgendaRepository agentAgendaRepository;
    private final VisitRequestRepository visitRequestRepository;
    private final AgentProfileRepository agentProfileRepository;

    /**
     * Returns the list of busy (occupied) time slots for a given agent on a given date.
     * Combines:
     *   1. Events from the agent's agenda (AgentAgenda with matching agent_id)
     *   2. Pending or Accepted visit requests assigned to the agent
     *
     * @param agentId  the agent profile ID
     * @param date     the date to check availability for
     * @return list of occupied slots
     */
    @Transactional(readOnly = true)
    public List<BusySlotResponse> getBusySlots(Long agentId, LocalDate date) {
        // Validate agent exists
        agentProfileRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", "id", agentId));

        LocalDateTime dayStart = date.atTime(LocalTime.of(7, 0));
        LocalDateTime dayEnd = date.atTime(LocalTime.of(19, 0));

        List<BusySlotResponse> busySlots = new ArrayList<>();

        // 1. Agenda events that overlap with the working day
        List<AgentAgenda> agendaEvents = agentAgendaRepository
                .findByAgentIdAndDateOverlap(agentId, dayStart, dayEnd);

        for (AgentAgenda event : agendaEvents) {
            // Asegurar que el slot no exceda los límites del día solicitado (07:00 - 19:00)
            LocalDateTime start = event.getStartsAt().isBefore(dayStart) ? dayStart : event.getStartsAt();
            LocalDateTime end = event.getEndsAt().isAfter(dayEnd) ? dayEnd : event.getEndsAt();

            busySlots.add(new BusySlotResponse(
                    start,
                    end,
                    "Ocupado (Agenda)" // Título genérico para proteger la privacidad
            ));
        }

        // 2. Visit requests in PENDING, ACCEPTED or COUNTER_PROPOSED status
        List<VisitRequestStatus> blockingStatuses = List.of(
                VisitRequestStatus.PENDING,
                VisitRequestStatus.ACCEPTED,
                VisitRequestStatus.COUNTER_PROPOSED
        );

        List<VisitRequest> visitRequests = visitRequestRepository
                .findBusyVisits(agentId, blockingStatuses, dayStart, dayEnd);

        for (VisitRequest vr : visitRequests) {
            LocalDateTime slotStart = null;
            String reason = "";

            // 1. If Pending or Accepted without counter-proposal, original time is active
            if (vr.getStatus() == VisitRequestStatus.PENDING || 
               (vr.getStatus() == VisitRequestStatus.ACCEPTED && vr.getCounterProposedAt() == null)) {
                slotStart = vr.getProposedAt();
                reason = vr.getStatus() == VisitRequestStatus.PENDING ? "Solicitud pendiente" : "Visita confirmada";
            }
            // 2. If Counter-proposed or Accepted with counter-proposal, new time is active
            else if (vr.getStatus() == VisitRequestStatus.COUNTER_PROPOSED || 
                    (vr.getStatus() == VisitRequestStatus.ACCEPTED && vr.getCounterProposedAt() != null)) {
                slotStart = vr.getCounterProposedAt();
                reason = vr.getStatus() == VisitRequestStatus.ACCEPTED ? "Visita confirmada (reprogramada)" : "Contra-propuesta enviada";
            }

            // Verify the slot is on the requested day and within working hours
            if (slotStart != null && slotStart.toLocalDate().equals(date)) {
                busySlots.add(new BusySlotResponse(slotStart, slotStart.plusHours(1), reason));
            }
        }



        return busySlots;
    }
}
