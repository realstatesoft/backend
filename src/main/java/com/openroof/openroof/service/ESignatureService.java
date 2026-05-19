package com.openroof.openroof.service;

import com.openroof.openroof.dto.rental.SignLeaseRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.repository.LeaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ESignatureService {

    private static final long SIGNATURE_TOKEN_TTL_HOURS = 72L;

    private final LeaseRepository leaseRepository;
    private final NotificationService notificationService;
    private final LeaseService leaseService;

    @Transactional
    public Lease sendForSignature(Long leaseId) {
        Lease lease = leaseRepository.findByIdForUpdate(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", "id", leaseId));

        if (lease.getStatus() != LeaseStatus.DRAFT) {
            throw new BadRequestException("Lease can only be sent for signature from DRAFT status");
        }

        lease.setStatus(LeaseStatus.PENDING_SIGNATURE);
        lease.setSignatureTokenLandlord(UUID.randomUUID());
        lease.setSignatureTokenTenant(UUID.randomUUID());
        lease.setSignatureTokenExpiresAt(LocalDateTime.now().plusHours(SIGNATURE_TOKEN_TTL_HOURS));

        Lease saved = leaseRepository.save(lease);
        runAfterCommit(() -> notificationService.notifyLeaseSentForSignature(saved));
        return saved;
    }

    @Transactional
    public Lease sign(Long leaseId, String rawToken, SignLeaseRequest request, String ip, String userAgent) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Signature token is required");
        }

        Lease lease = leaseRepository.findByIdForUpdate(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease", "id", leaseId));

        if (lease.getStatus() != LeaseStatus.PENDING_SIGNATURE) {
            throw new BadRequestException("Lease must be PENDING_SIGNATURE to be signed");
        }

        LocalDateTime now = LocalDateTime.now();
        if (lease.getSignatureTokenExpiresAt() == null || now.isAfter(lease.getSignatureTokenExpiresAt())) {
            throw new BadRequestException("Signature token has expired");
        }

        UUID token = parseToken(rawToken);
        NotificationService.SignerSide signerSide = resolveSignerSide(lease, token);

        if (signerSide == NotificationService.SignerSide.LANDLORD) {
            if (lease.getSignedByLandlordAt() != null) {
                throw new BadRequestException("Lease already signed by landlord");
            }
            lease.setSignedByLandlordAt(now);
            lease.setSignatureTokenLandlord(null); // token de un solo uso
        } else {
            if (lease.getSignedByTenantAt() != null) {
                throw new BadRequestException("Lease already signed by tenant");
            }
            lease.setSignedByTenantAt(now);
            lease.setSignatureTokenTenant(null); // token de un solo uso
        }

        appendAuditEvent(lease, signerSide, ip, userAgent, now, rawToken, request);
        Lease saved = leaseRepository.save(lease);

        if (saved.isSigned()) {
            leaseService.activateLease(saved.getId());
            Lease finalLease = leaseRepository.findById(saved.getId()).orElse(saved);
            runAfterCommit(() -> notificationService.notifyLeaseSigned(finalLease, signerSide));
            return finalLease;
        }

        runAfterCommit(() -> notificationService.notifyLeaseSigned(saved, signerSide));
        return saved;
    }

    private UUID parseToken(String rawToken) {
        try {
            return UUID.fromString(rawToken.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid signature token");
        }
    }

    private NotificationService.SignerSide resolveSignerSide(Lease lease, UUID token) {
        if (token.equals(lease.getSignatureTokenLandlord())) {
            return NotificationService.SignerSide.LANDLORD;
        }
        if (token.equals(lease.getSignatureTokenTenant())) {
            return NotificationService.SignerSide.TENANT;
        }
        throw new BadRequestException("Invalid signature token");
    }

    @SuppressWarnings("unchecked")
    private void appendAuditEvent(
            Lease lease,
            NotificationService.SignerSide signerSide,
            String ip,
            String userAgent,
            LocalDateTime timestamp,
            String token,
            SignLeaseRequest request
    ) {
        Map<String, Object> trail = lease.getSignatureAuditTrail() != null
                ? new HashMap<>(lease.getSignatureAuditTrail())
                : new HashMap<>();

        List<Map<String, Object>> events = new ArrayList<>();
        Object currentEvents = trail.get("events");
        if (currentEvents instanceof List<?> rawList) {
            for (Object event : rawList) {
                if (event instanceof Map<?, ?> mapEvent) {
                    events.add(new HashMap<>((Map<String, Object>) mapEvent));
                }
            }
        }

        Map<String, Object> newEvent = new LinkedHashMap<>();
        newEvent.put("signer", signerSide.name());
        newEvent.put("ip", ip);
        newEvent.put("userAgent", userAgent);
        newEvent.put("timestamp", timestamp.toString());
        newEvent.put("tokenHash", sha256(token));
        if (request != null && request.signatureData() != null && !request.signatureData().isBlank()) {
            newEvent.put("signatureData", request.signatureData());
        }

        events.add(newEvent);
        trail.put("events", events);
        lease.setSignatureAuditTrail(trail);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            log.error("SHA-256 not available", ex);
            throw new IllegalStateException("SHA-256 not available");
        }
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeRun(action);
                }
            });
        } else {
            safeRun(action);
        }
    }

    private void safeRun(Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            log.error("Post-commit notification callback failed; transaction already committed", t);
        }
    }
}
