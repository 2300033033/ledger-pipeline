
package ledger_pipeline.controller;

import ledger_pipeline.model.Block;
import ledger_pipeline.service.BlockchainService;
import ledger_pipeline.service.ChainValidationService;
import ledger_pipeline.service.ReorgDetectionService;
import ledger_pipeline.service.ReorgRecoveryService;
import ledger_pipeline.service.ReorgMonitorService;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import ledger_pipeline.repository.BlockRepository;

import ledger_pipeline.model.ReorgEvent;
import ledger_pipeline.repository.ReorgEventRepository;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {
    
private final BlockRepository blockRepository;

    private final ReorgMonitorService reorgMonitorService;
    private final BlockchainService blockchainService;
    private final ChainValidationService chainValidationService;
    private final ReorgDetectionService reorgDetectionService;
    private final ReorgRecoveryService reorgRecoveryService;
    
private final ReorgEventRepository eventRepository;


   
public BlockchainController(
        BlockchainService blockchainService,
        ChainValidationService chainValidationService,
        ReorgDetectionService reorgDetectionService,
        ReorgRecoveryService reorgRecoveryService,
        ReorgMonitorService reorgMonitorService,
        ReorgEventRepository eventRepository,BlockRepository blockRepository) {
this.blockRepository = blockRepository;
    this.blockchainService = blockchainService;
    this.chainValidationService = chainValidationService;
    this.reorgDetectionService = reorgDetectionService;
    this.reorgRecoveryService = reorgRecoveryService;
    this.reorgMonitorService = reorgMonitorService;
    this.eventRepository = eventRepository;
}

    @GetMapping("/latest-block")
    public BigInteger getLatestBlock() throws IOException {
        return blockchainService.getLatestBlockNumber();
    }

    @GetMapping("/fetch-latest")
    public Block fetchLatestBlock() throws IOException {
        return blockchainService.fetchAndSaveLatestBlock();
    }

    @GetMapping("/fetch-blocks")
    public List<Block> fetchBlocks() throws IOException {
        return blockchainService.fetchAndSaveLatestBlocks(10);
    }

    @GetMapping("/validate-chain")
    public String validateChain() throws IOException {

        List<Block> blocks =
                blockchainService.fetchAndSaveLatestBlocks(10);

        boolean valid = chainValidationService.validateChain(blocks);

        if (valid) {
            return "Chain validation successful. No mismatch detected.";
        } else {
            return "Chain validation failed. Block continuity or parent-hash mismatch.";
        }
    }

    @GetMapping("/detect-reorg/{blockNumber}")
    public String detectReorg(@PathVariable Long blockNumber)
            throws IOException {

        boolean reorgDetected =
                reorgDetectionService.detectReorg(blockNumber);

        if (reorgDetected) {
            return "Reorganisation detected at block " + blockNumber;
        }

        return "No reorganisation detected at block " + blockNumber;
    }
    
    @PostMapping("/recover-blocks")
    public List<Block> recoverBlocks(
            @RequestParam Long startBlock,
            @RequestParam Long endBlock) throws IOException {

        return reorgRecoveryService.recoverBlocks(startBlock, endBlock);
        
    }
    @PostMapping("/check-and-recover")
public String checkAndRecover() throws IOException {
    return reorgMonitorService.checkAndRecover();
}

@GetMapping("/reorg-events")
public List<ReorgEvent> getReorgEvents() {
    return eventRepository.findAll(
        org.springframework.data.domain.Sort.by(
            org.springframework.data.domain.Sort.Direction.DESC,
            "detectedAt"
        )
    );
}

@GetMapping("/blocks/count")
public long getStoredBlockCount() {
    return blockRepository.count();
}

@GetMapping("/blocks/recent")
public List<Block> getRecentBlocks() {
    return blockRepository.findAll(
            PageRequest.of(
                    0,
                    10,
                    org.springframework.data.domain.Sort.by(
                            org.springframework.data.domain.Sort.Direction.DESC,
                            "blockNumber"
                    )
            )
    ).getContent();
}


}
