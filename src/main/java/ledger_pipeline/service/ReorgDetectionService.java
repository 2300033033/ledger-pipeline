package ledger_pipeline.service;

import ledger_pipeline.model.Block;
import ledger_pipeline.repository.BlockRepository;

import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.http.HttpService;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;

@Service
public class ReorgDetectionService {

    private final BlockRepository blockRepository;
    private final Web3j web3j;

    public ReorgDetectionService(
        BlockRepository blockRepository,
        @Value("${alchemy.rpc.url}") String rpcUrl) {

    this.blockRepository = blockRepository;

    this.web3j = Web3j.build(new HttpService(rpcUrl));
}

    public boolean detectReorg(Long blockNumber)
            throws IOException {

        Optional<Block> storedBlock =
                blockRepository.findById(blockNumber);

        if (storedBlock.isEmpty()) {
            throw new IllegalArgumentException(
                "Block not found in database: " + blockNumber
            );
        }

        EthBlock response = web3j.ethGetBlockByNumber(
                DefaultBlockParameter.valueOf(
                    BigInteger.valueOf(blockNumber)
                ),
                false
        ).send();

        if (response.hasError()) {
            throw new IOException(
                response.getError().getMessage()
            );
        }

        EthBlock.Block currentBlock = response.getBlock();

        if (currentBlock == null) {
            throw new IOException("Blockchain block not found");
        }

        String storedHash = storedBlock.get().getBlockHash();
        String currentHash = currentBlock.getHash();

        if (!storedHash.equalsIgnoreCase(currentHash)) {

            System.out.println(
                "Reorganisation detected at block " + blockNumber
            );

            return true;
        }

        System.out.println(
            "No reorganisation at block " + blockNumber
        );

        return false;
    }
}