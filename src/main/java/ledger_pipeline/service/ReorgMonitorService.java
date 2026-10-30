
package ledger_pipeline.service;

import ledger_pipeline.model.Block;
import ledger_pipeline.model.ReorgEvent;
import ledger_pipeline.repository.BlockRepository;
import ledger_pipeline.repository.ReorgEventRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReorgMonitorService {

    private final BlockRepository blockRepository;
    private final BlockchainService blockchainService;
    private final ReorgRecoveryService recoveryService;
    private final ReorgEventRepository eventRepository;

    private final int monitoringWindow;

    public ReorgMonitorService(
            BlockRepository blockRepository,
            BlockchainService blockchainService,
            ReorgRecoveryService recoveryService,
            ReorgEventRepository eventRepository,
            @Value("${reorg.monitor.window:100}")
            int monitoringWindow) {

        this.blockRepository = blockRepository;
        this.blockchainService = blockchainService;
        this.recoveryService = recoveryService;
        this.eventRepository = eventRepository;

        this.monitoringWindow = Math.max(1, monitoringWindow);
    }

    public synchronized String checkAndRecover() throws IOException {

        List<Block> storedBlocks = new ArrayList<>(
                blockRepository.findAll(
                        PageRequest.of(
                                0,
                                monitoringWindow,
                                Sort.by(
                                        Sort.Direction.DESC,
                                        "blockNumber"
                                )
                        )
                ).getContent()
        );

        if (storedBlocks.isEmpty()) {
            return "No stored blocks available for monitoring.";
        }

        storedBlocks.sort(
                Comparator.comparing(Block::getBlockNumber)
        );

        List<Long> mismatchedBlocks = new ArrayList<>();

        for (Block stored : storedBlocks) {

            long blockNumber = stored.getBlockNumber();

            Block canonicalBlock = blockchainService.fetchBlock(
                    BigInteger.valueOf(blockNumber)
            );

            if (canonicalBlock == null
                    || canonicalBlock.getBlockHash() == null) {

                throw new IOException(
                        "Ethereum RPC returned an invalid block at "
                                + blockNumber
                );
            }

            String storedHash = stored.getBlockHash();
            String canonicalHash = canonicalBlock.getBlockHash();

            if (storedHash == null
                    || !storedHash.equalsIgnoreCase(canonicalHash)) {

                System.err.println(
                        "CHAIN MISMATCH DETECTED at block "
                                + blockNumber
                );

                mismatchedBlocks.add(blockNumber);
            }
        }

        if (mismatchedBlocks.isEmpty()) {
            return "No reorganisation detected.";
        }

        List<long[]> mismatchRanges = new ArrayList<>();

        long rangeStart = mismatchedBlocks.get(0);
        long rangeEnd = rangeStart;

        for (int i = 1; i < mismatchedBlocks.size(); i++) {

            long currentBlock = mismatchedBlocks.get(i);

            if (currentBlock == rangeEnd + 1) {

                rangeEnd = currentBlock;

            } else {

                mismatchRanges.add(
                        new long[]{rangeStart, rangeEnd}
                );

                rangeStart = currentBlock;
                rangeEnd = currentBlock;
            }
        }

        mismatchRanges.add(
                new long[]{rangeStart, rangeEnd}
        );

        int recoveredRanges = 0;
        int recoveredBlocks = 0;
        int failedRanges = 0;

        for (long[] range : mismatchRanges) {

            long startBlock = range[0];
            long endBlock = range[1];

            ReorgEvent event = new ReorgEvent();

            event.setStartBlock(startBlock);
            event.setEndBlock(endBlock);

            event.setAffectedBlocks(
                    Math.toIntExact(endBlock - startBlock + 1)
            );

            event.setDetectedAt(LocalDateTime.now());
            event.setStatus("DETECTED");

            event = eventRepository.save(event);

            try {

                System.out.println(
                        "Recovering blocks "
                                + startBlock
                                + " to "
                                + endBlock
                );

                recoveryService.recoverBlocks(
                        startBlock,
                        endBlock
                );

                // Verify each recovered block.
                for (long blockNumber = startBlock;
                     blockNumber <= endBlock;
                     blockNumber++) {

                    Block canonicalBlock =
                            blockchainService.fetchBlock(
                                    BigInteger.valueOf(blockNumber)
                            );

                    // Avoid lambda capture of changing loop variable.
                    Block recoveredBlock =
                            blockRepository.findById(blockNumber)
                                    .orElse(null);

                    if (recoveredBlock == null) {

                        throw new IllegalStateException(
                                "Recovered block missing: "
                                        + blockNumber
                        );
                    }

                    if (canonicalBlock == null
                            || canonicalBlock.getBlockHash() == null
                            || recoveredBlock.getBlockHash() == null
                            || !recoveredBlock.getBlockHash()
                                    .equalsIgnoreCase(
                                            canonicalBlock.getBlockHash()
                                    )) {

                        throw new IllegalStateException(
                                "Recovery verification failed at block "
                                        + blockNumber
                        );
                    }
                }

                event.setRecoveredAt(LocalDateTime.now());
                event.setStatus("RECOVERED");

                eventRepository.save(event);

                recoveredRanges++;
                recoveredBlocks += event.getAffectedBlocks();

                System.out.println(
                        "Recovery completed. Event ID: "
                                + event.getId()
                );

            } catch (Exception e) {

                event.setStatus("FAILED");
                eventRepository.save(event);

                failedRanges++;

                System.err.println(
                        "Recovery failed for blocks "
                                + startBlock
                                + " to "
                                + endBlock
                                + ": "
                                + e.getMessage()
                );
            }
        }

        return "Monitoring completed. Recovered ranges: "
                + recoveredRanges
                + ", recovered blocks: "
                + recoveredBlocks
                + ", failed ranges: "
                + failedRanges;
    }

    @Scheduled(
            fixedDelayString = "${reorg.monitor.delay-ms:60000}",
            initialDelayString = "${reorg.monitor.initial-delay-ms:30000}"
    )
    public void automaticReorgMonitoring() {

        try {

            String result = checkAndRecover();

            System.out.println(
                    "Automatic Reorg Monitor: "
                            + result
            );

        } catch (Exception e) {

            System.err.println(
                    "Automatic Reorg Monitor failed: "
                            + e.getMessage()
            );
        }
    }
}
