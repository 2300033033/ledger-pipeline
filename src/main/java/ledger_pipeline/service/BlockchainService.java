package ledger_pipeline.service;

import ledger_pipeline.model.Block;
import ledger_pipeline.repository.BlockRepository;

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

    public BlockchainService(BlockRepository blockRepository) {

        this.blockRepository = blockRepository;

        String rpcUrl = "https://eth-mainnet.g.alchemy.com/v2/alch_tu09f28iQCWmhv0ZN0W8c";

        this.web3j = Web3j.build(new HttpService(rpcUrl));
    }

    public BigInteger getLatestBlockNumber() throws IOException {

        return web3j
                .ethBlockNumber()
                .send()
                .getBlockNumber();
    }

    public Block fetchAndSaveLatestBlock() throws IOException {

        BigInteger latestBlockNumber = getLatestBlockNumber();

        EthBlock response = web3j
                .ethGetBlockByNumber(
                        DefaultBlockParameter.valueOf(latestBlockNumber),
                        false
                )
                .send();

        EthBlock.Block ethereumBlock = response.getBlock();

        if (ethereumBlock == null) {
            throw new RuntimeException("Block could not be fetched");
        }

        Block block = new Block();

        block.setBlockNumber(
                ethereumBlock.getNumber().longValue()
        );

        block.setBlockHash(
                ethereumBlock.getHash()
        );

        block.setPreviousHash(
                ethereumBlock.getParentHash()
        );

        block.setTimestamp(
                ethereumBlock.getTimestamp().longValue()
        );

        block.setTransactionCount(
                ethereumBlock.getTransactions().size()
        );

        return blockRepository.save(block);
    }
    public List<Block> fetchAndSaveLatestBlocks(int count) throws IOException {

    BigInteger latestBlockNumber = getLatestBlockNumber();

    List<Block> savedBlocks = new ArrayList<>();

    for (int i = count - 1; i >= 0; i--) {

        BigInteger blockNumber =
                latestBlockNumber.subtract(BigInteger.valueOf(i));

        EthBlock response = web3j
                .ethGetBlockByNumber(
                        DefaultBlockParameter.valueOf(blockNumber),
                        false
                )
                .send();

        EthBlock.Block ethereumBlock = response.getBlock();

        if (ethereumBlock == null) {
            continue;
        }

        Block block = new Block();

        block.setBlockNumber(
                ethereumBlock.getNumber().longValue()
        );

        block.setBlockHash(
                ethereumBlock.getHash()
        );

        block.setPreviousHash(
                ethereumBlock.getParentHash()
        );

        block.setTimestamp(
                ethereumBlock.getTimestamp().longValue()
        );

        block.setTransactionCount(
                ethereumBlock.getTransactions().size()
        );

        Block savedBlock = blockRepository.save(block);

        savedBlocks.add(savedBlock);
    }

    return savedBlocks;
}
}