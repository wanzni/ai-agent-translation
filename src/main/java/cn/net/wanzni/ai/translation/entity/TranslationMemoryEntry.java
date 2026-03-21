package cn.net.wanzni.ai.translation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "translation_memory_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationMemoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "source_text", nullable = false, columnDefinition = "LONGTEXT")
    private String sourceText;

    @Lob
    @Column(name = "target_text", nullable = false, columnDefinition = "LONGTEXT")
    private String targetText;

    @Column(name = "source_language", nullable = false, length = 16)
    private String sourceLanguage;

    @Column(name = "target_language", nullable = false, length = 16)
    private String targetLanguage;

    @Column(name = "domain", length = 100)
    private String domain;

    @Column(name = "source_text_hash", length = 64)
    private String sourceTextHash;

    @Column(name = "quality_score")
    private Integer qualityScore;

    @Column(name = "approved", nullable = false)
    @Builder.Default
    private Boolean approved = false;

    @Column(name = "hit_count", nullable = false)
    @Builder.Default
    private Integer hitCount = 0;

    @Column(name = "created_from_task_id")
    private Long createdFromTaskId;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
