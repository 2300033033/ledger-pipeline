
package ledger_pipeline.service;

import ledger_pipeline.model.Block;
import ledger_pipeline.repository.BlockRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Service
public class BlockchainService {

    private final Web3j web3j;
    private final BlockRepository blockRepository;

    public BlockchainService(
            BlockRepository blockRepository,
            @Value("${alchemy.rpc.url}") String rpcUrl) {

        this.blockRepository = blockRepository;
        this.web3j = Web3j.build(new HttpService(rpcUrl));
    }

    public BigInteger getLatestBlockNumber() throws IOException {
        return web3j.ethBlockNumber()
                .send()
                .getBlockNumber();
    }

    // Fetch a block without saving it
    public Block fetchBlock(BigInteger blockNumber) throws IOException {

        EthBlock response = web3j
                .ethGetBlockByNumber(
                        DefaultBlockParameter.valueOf(blockNumber),
                        false
                )
                .send();

        if (response.hasError()) {
            throw new IOException(response.getError().getMessage());
        }

        EthBlock.Block ethereumBlock = response.getBlock();

        if (ethereumBlock == null) {
            throw new IOException(
                    "Block could not be fetched: " + blockNumber
            );
        }

        Block block = new Block();

        block.setBlockNumber(ethereumBlock.getNumber().longValue());
        block.setBlockHash(ethereumBlock.getHash());
        block.setPreviousHash(ethereumBlock.getParentHash());
        block.setTimestamp(ethereumBlock.getTimestamp().longValue());
        block.setTransactionCount(ethereumBlock.getTransactions().size());

        return block;
    }

    // Save a block to PostgreSQL
    public Block saveBlock(Block block) {
        return blockRepository.save(block);
    }

    // Fetch and save latest block
    public Block fetchAndSaveLatestBlock() throws IOException {

        BigInteger latestBlockNumber = getLatestBlockNumber();
        Block block = fetchBlock(latestBlockNumber);

        return saveBlock(block);
    }

    // Fetch and save multiple blocks
    public List<Block> fetchAndSaveLatestBlocks(int count)
            throws IOException {

        if (count < 1 || count > 100) {
            throw new IllegalArgumentException(
                    "Block count must be between 1 and 100"
            );
        }

        BigInteger latestBlockNumber = getLatestBlockNumber();
        List<Block> savedBlocks = new ArrayList<>();

        for (int i = count - 1; i >= 0; i--) {

            BigInteger blockNumber =
                    latestBlockNumber.subtract(BigInteger.valueOf(i));

            Block block = fetchBlock(blockNumber);
            Block savedBlock = saveBlock(block);

            savedBlocks.add(savedBlock);
        }

        return savedBlocks;
    }
}
