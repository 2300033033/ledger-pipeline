
package ledger_pipeline.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reorg_events")
public class ReorgEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long startBlock;
    private Long endBlock;
    private Integer affectedBlocks;

    private LocalDateTime detectedAt;
    private LocalDateTime recoveredAt;

    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStartBlock() {
        return startBlock;
    }

    public void setStartBlock(Long startBlock) {
        this.startBlock = startBlock;
    }

    public Long getEndBlock() {
        return endBlock;
    }

    public void setEndBlock(Long endBlock) {
        this.endBlock = endBlock;
    }

    public Integer getAffectedBlocks() {
        return affectedBlocks;
    }

    public void setAffectedBlocks(Integer affectedBlocks) {
        this.affectedBlocks = affectedBlocks;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public LocalDateTime getRecoveredAt() {
        return recoveredAt;
    }

    public void setRecoveredAt(LocalDateTime recoveredAt) {
        this.recoveredAt = recoveredAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
