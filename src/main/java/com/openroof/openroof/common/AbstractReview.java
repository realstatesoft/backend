package com.openroof.openroof.common;

import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Superclase abstracta para reviews (property reviews, agent reviews).
 * Comparten: user + rating + comment.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
