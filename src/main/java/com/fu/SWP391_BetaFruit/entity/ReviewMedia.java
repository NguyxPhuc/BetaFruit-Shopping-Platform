package com.fu.SWP391_BetaFruit.entity;

import com.fu.SWP391_BetaFruit.enums.MediaType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ReviewMedia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MediaId")
    private Integer mediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReviewId", nullable = false)
    private Review review;

    @Column(name = "MediaUrl", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "MediaType", length = 20)
    private MediaType mediaType;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
