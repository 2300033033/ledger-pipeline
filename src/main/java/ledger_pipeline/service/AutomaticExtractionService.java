
package ledger_pipeline.service;

import ledger_pipeline.model.Block;
import ledger_pipeline.repository.BlockRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Optional;

@Service
public class AutomaticExtractionService {

    private final BlockchainService blockchainService;
    private final BlockRepository blockRepository;
    private final ReorgRecoveryService recoveryService;

    private Long lastExtractedBlock = null;

    public AutomaticExtractionService(
            BlockchainService blockchainService,
            BlockRepository blockRepository,
            ReorgRecoveryService recoveryService) {

        this.blockchainService = blockchainService;
        this.blockRepository = blockRepository;
        this.recoveryService = recoveryService;
    }

    @Scheduled(fixedDelay = 15000)
    public void extractNewBlocks() {

        try {

            long latestBlock =
                    blockchainService.getLatestBlockNumber().longValue();

            // Find latest block already stored in PostgreSQL
            if (lastExtractedBlock == null) {

                lastExtractedBlock = blockRepository.findAll(
                        PageRequest.of(
                                0,
                                1,
                                Sort.by(
                                        Sort.Direction.DESC,
                                        "blockNumber"
                                )
                        )
                ).getContent()
                 .stream()
                 .findFirst()
                 .map(Block::getBlockNumber)
                 .orElse(latestBlock - 1);
            }

            int extractedCount = 0;

            while (lastExtractedBlock < latestBlock
                    && extractedCount < 10) {

                long nextBlock = lastExtractedBlock + 1;

                Block newBlock = blockchainService.fetchBlock(
                        BigInteger.valueOf(nextBlock)
                );

                Optional<Block> previousBlock =
                        blockRepository.findById(lastExtractedBlock);

                // Check parent-hash continuity
                if (previousBlock.isPresent()) {

                    String storedHash =
                            previousBlock.get().getBlockHash();

                    String parentHash =
                            newBlock.getPreviousHash();

                    if (!storedHash.equalsIgnoreCase(parentHash)) {

                        System.err.println(
                                "CHAIN MISMATCH DETECTED at block "
                                        + nextBlock
                        );

                        System.out.println(
                                "Starting automatic recovery..."
                        );

                        // Recover the previous stored block
                        recoveryService.recoverBlocks(
                                lastExtractedBlock,
                                lastExtractedBlock
                        );

                        // Reload the corrected block
                        Optional<Block> correctedBlock =
                                blockRepository.findById(
                                        lastExtractedBlock
                                );

                        if (correctedBlock.isEmpty()) {

                            System.err.println(
                                    "Recovery failed: previous block missing."
                            );

                            return;
                        }

                        String correctedHash =
                                correctedBlock.get().getBlockHash();

                        // Verify that recovery restored continuity
                        if (!correctedHash.equalsIgnoreCase(parentHash)) {

                            System.err.println(
                                    "Recovery did not restore chain continuity."
                            );

                            System.err.println(
                                    "Extraction stopped for safety."
                            );

                            return;
                        }

                        System.out.println(
                                "Recovery successful. Resuming extraction."
                        );
                    }
                }

                // Save canonical block
                blockchainService.saveBlock(newBlock);

                lastExtractedBlock = nextBlock;
                extractedCount++;

                System.out.println(
                        "Automatically extracted block: "
                                + nextBlock
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "Automatic extraction failed: "
                            + e.getMessage()
            );
        }
    }
}
