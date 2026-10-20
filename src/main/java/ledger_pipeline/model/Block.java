package ledger_pipeline.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "blocks")
public class Block {

    @Id
    private Long blockNumber;

    private String blockHash;
    private String previousHash;
    private Long timestamp;
    private Integer transactionCount;

    public Block() {
    }

    public Block(Long blockNumber, String blockHash, String previousHash,
                 Long timestamp, Integer transactionCount) {
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.previousHash = previousHash;
        this.timestamp = timestamp;
        this.transactionCount = transactionCount;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }
}