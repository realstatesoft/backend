package com.openroof.openroof.model.notification;

import com.openroof.openroof.model.enums.NotificationChannel;
import com.openroof.openroof.model.enums.NotificationEventType;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NotificationPreferenceId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_type", nullable = false, length = 60)
    private NotificationEventType eventType;

    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;
}
