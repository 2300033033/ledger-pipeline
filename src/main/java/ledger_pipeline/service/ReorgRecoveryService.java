
package ledger_pipeline.service;

import ledger_pipeline.model.Block;
import ledger_pipeline.repository.BlockRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReorgRecoveryService {

    private final BlockchainService blockchainService;
    private final BlockRepository blockRepository;

    public ReorgRecoveryService(
            BlockchainService blockchainService,
            BlockRepository blockRepository) {

        this.blockchainService = blockchainService;
        this.blockRepository = blockRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Block> recoverBlocks(Long startBlock, Long endBlock)
            throws IOException {

        if (startBlock == null || endBlock == null
                || startBlock < 0 || endBlock < startBlock
                || endBlock - startBlock > 99) {

            throw new IllegalArgumentException(
                    "Invalid recovery range (maximum 100 blocks)"
            );
        }

        List<Block> canonicalBlocks = new ArrayList<>();

        // Fetch correct blocks from Ethereum
        for (long number = startBlock; number <= endBlock; number++) {

            Block block = blockchainService.fetchBlock(
                    BigInteger.valueOf(number)
            );

            canonicalBlocks.add(block);
        }

        // Update existing blocks or insert missing blocks
        return blockRepository.saveAllAndFlush(canonicalBlocks);
    }
}
