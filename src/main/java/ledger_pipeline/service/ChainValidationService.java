package ledger_pipeline.service;

import ledger_pipeline.model.Block;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChainValidationService {

    public boolean validateChain(List<Block> blocks) {

        if (blocks == null || blocks.isEmpty()) {
            return false;
        }

        for (int i = 1; i < blocks.size(); i++) {

            Block previousBlock = blocks.get(i - 1);
            Block currentBlock = blocks.get(i);

            // Verify consecutive block numbers
            if (currentBlock.getBlockNumber() !=
                    previousBlock.getBlockNumber() + 1) {

                System.out.println("Missing or unordered block detected.");
                return false;
            }

            // Verify parent hash
            if (!currentBlock.getPreviousHash()
                    .equalsIgnoreCase(previousBlock.getBlockHash())) {

                System.out.println(
                    "Parent hash mismatch between blocks "
                    + previousBlock.getBlockNumber()
                    + " and "
                    + currentBlock.getBlockNumber()
                );

                return false;
            }
        }

        System.out.println("Chain validation successful.");
        return true;
    }
}