package ledger_pipeline.controller;

import ledger_pipeline.model.Block;
import ledger_pipeline.service.BlockchainService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.io.IOException;
import java.math.BigInteger;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {
@GetMapping("/fetch-blocks")
public List<Block> fetchBlocks() throws IOException {
    return blockchainService.fetchAndSaveLatestBlocks(10);
}
    private final BlockchainService blockchainService;

    public BlockchainController(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    @GetMapping("/latest-block")
    public BigInteger getLatestBlock() throws IOException {
        return blockchainService.getLatestBlockNumber();
    }

    @GetMapping("/fetch-latest")
    public Block fetchLatestBlock() throws IOException {
        return blockchainService.fetchAndSaveLatestBlock();
    }
}