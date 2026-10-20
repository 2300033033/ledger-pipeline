import { useEffect, useState } from "react";
import "./App.css";

function App() {
  const [latestBlock, setLatestBlock] = useState("-");
  const [blocks, setBlocks] = useState([]);
  const [loading, setLoading] = useState(false);

  const getLatestBlock = async () => {
    try {
      const response = await fetch(
        "http://localhost:8080/api/blockchain/latest-block"
      );

      const data = await response.json();
      setLatestBlock(data);
    } catch (error) {
      console.error("Error fetching latest block:", error);
    }
  };

  const fetchLatestBlock = async () => {
    try {
      setLoading(true);

      const response = await fetch(
        "http://localhost:8080/api/blockchain/fetch-latest"
      );

      const data = await response.json();

      setLatestBlock(data.blockNumber);

      setBlocks((previousBlocks) => {
        const alreadyExists = previousBlocks.some(
          (block) => block.blockNumber === data.blockNumber
        );

        if (alreadyExists) {
          return previousBlocks;
        }

        return [data, ...previousBlocks];
      });
    } catch (error) {
      console.error("Error extracting block:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    getLatestBlock();
  }, []);

  return (
    <div className="app">
      <header>
        <div>
          <h1>Ledger Data Extraction Pipeline</h1>
          <p>Chain Reorganisation Monitoring & Analytics</p>
        </div>

        <div className="status">
          <span className="status-dot"></span>
          Pipeline Active
        </div>
      </header>

      <main>
        <section className="cards">
          <div className="card">
            <p>Latest Ethereum Block</p>
            <h2>{latestBlock}</h2>
          </div>

          <div className="card">
            <p>Blocks Extracted</p>
            <h2>{blocks.length}</h2>
          </div>

          <div className="card">
            <p>Reorganisations Detected</p>
            <h2>0</h2>
          </div>

          <div className="card">
            <p>Pipeline Status</p>
            <h2 className="healthy">Healthy</h2>
          </div>
        </section>

        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>Blockchain Extraction</h2>
              <p>Retrieve the latest canonical block from Ethereum.</p>
            </div>

            <button onClick={fetchLatestBlock} disabled={loading}>
              {loading ? "Extracting..." : "Fetch Latest Block"}
            </button>
          </div>
        </section>

        <section className="panel">
          <h2>Recently Extracted Blocks</h2>

          {blocks.length === 0 ? (
            <div className="empty">
              No blocks extracted during this session.
            </div>
          ) : (
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Block</th>
                    <th>Block Hash</th>
                    <th>Parent Hash</th>
                    <th>Transactions</th>
                    <th>Status</th>
                  </tr>
                </thead>

                <tbody>
                  {blocks.map((block) => (
                    <tr key={block.blockNumber}>
                      <td>{block.blockNumber}</td>

                      <td title={block.blockHash}>
                        {block.blockHash?.substring(0, 12)}...
                      </td>

                      <td title={block.previousHash}>
                        {block.previousHash?.substring(0, 12)}...
                      </td>

                      <td>{block.transactionCount}</td>

                      <td>
                        <span className="canonical">Canonical</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="panel monitor">
          <div>
            <h2>Chain Reorganisation Monitor</h2>
            <p>
              Monitors block relationships and parent hashes to identify
              changes in the canonical chain.
            </p>
          </div>

          <div className="monitor-status">
            <span className="status-dot"></span>
            No reorganisation detected
          </div>
        </section>
      </main>
    </div>
  );
}

export default App;