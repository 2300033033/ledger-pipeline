
import { useEffect, useState } from "react";
import "./App.css";

const API = "http://localhost:8080/api/blockchain";

function App() {
  const [latestBlock, setLatestBlock] = useState("-");
  const [storedBlockCount, setStoredBlockCount] = useState(0);
  const [blocks, setBlocks] = useState([]);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [monitorLoading, setMonitorLoading] = useState(false);
  const [monitorMessage, setMonitorMessage] = useState("");
  const [eventsError, setEventsError] = useState(false);
  const [blocksError, setBlocksError] = useState(false);
  const [selectedEventId, setSelectedEventId] = useState("");

  const getLatestBlock = async () => {
    try {
      const response = await fetch(`${API}/latest-block`);
      if (!response.ok) throw new Error("Failed to fetch latest block");
      setLatestBlock(await response.json());
    } catch (error) {
      console.error("Error fetching latest block:", error);
    }
  };

  const getStoredBlockCount = async () => {
    try {
      const response = await fetch(`${API}/blocks/count`);
      if (!response.ok) throw new Error("Failed to fetch block count");
      setStoredBlockCount(await response.json());
    } catch (error) {
      console.error("Error fetching block count:", error);
    }
  };

  const getRecentBlocks = async () => {
    try {
      const response = await fetch(`${API}/blocks/recent`);
      if (!response.ok) throw new Error("Failed to fetch recent blocks");

      const data = await response.json();
      setBlocks(data);
      setBlocksError(false);
    } catch (error) {
      console.error("Error fetching recent blocks:", error);
      setBlocksError(true);
    }
  };

  const getReorgEvents = async () => {
    try {
      const response = await fetch(`${API}/reorg-events`);
      if (!response.ok) throw new Error("Failed to fetch recovery events");

      const data = await response.json();
      setEvents(data);
      setEventsError(false);
    } catch (error) {
      console.error("Error fetching recovery events:", error);
      setEventsError(true);
    }
  };

  const refreshDashboard = async () => {
    await Promise.all([
      getLatestBlock(),
      getStoredBlockCount(),
      getRecentBlocks(),
      getReorgEvents(),
    ]);
  };

  const fetchLatestBlock = async () => {
    try {
      setLoading(true);

      const response = await fetch(`${API}/fetch-latest`);
      if (!response.ok) throw new Error("Failed to extract latest block");

      const data = await response.json();
      setLatestBlock(data.blockNumber);

      await Promise.all([
        getStoredBlockCount(),
        getRecentBlocks(),
      ]);
    } catch (error) {
      console.error("Error extracting block:", error);
    } finally {
      setLoading(false);
    }
  };

  const runMonitor = async () => {
    try {
      setMonitorLoading(true);
      setMonitorMessage("Checking stored blocks against Ethereum...");

      const response = await fetch(`${API}/check-and-recover`, {
        method: "POST",
      });

      if (!response.ok) {
        throw new Error(`Monitor request failed: ${response.status}`);
      }

      const result = await response.text();
      setMonitorMessage(result);

      await Promise.all([
        getReorgEvents(),
        getRecentBlocks(),
        getStoredBlockCount(),
      ]);
    } catch (error) {
      console.error("Monitor error:", error);
      setMonitorMessage(`Monitor failed: ${error.message}`);
    } finally {
      setMonitorLoading(false);
    }
  };

  const getDuration = (event) => {
    if (!event?.detectedAt || !event?.recoveredAt) {
      return null;
    }

    const start = new Date(event.detectedAt).getTime();
    const end = new Date(event.recoveredAt).getTime();
    const duration = end - start;

    return Number.isFinite(duration) && duration >= 0
      ? duration
      : null;
  };

  const formatDate = (value) => {
    if (!value) return "-";

    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? value
      : date.toLocaleString();
  };

  const shortHash = (hash) => {
    if (!hash) return "-";
    return hash.length > 18
      ? `${hash.slice(0, 12)}...${hash.slice(-6)}`
      : hash;
  };

  useEffect(() => {
    refreshDashboard();

    const interval = setInterval(refreshDashboard, 15000);
    return () => clearInterval(interval);
  }, []);

  const latestEvent = events[0];

  const recoveredEvents = events.filter(
    (event) => event.status === "RECOVERED"
  );

  const totalRecoveries = recoveredEvents.length;

  const maxAffectedBlocks = events.reduce(
    (max, event) => Math.max(max, event.affectedBlocks || 0),
    0
  );

  const recoveryTimes = recoveredEvents
    .map(getDuration)
    .filter((duration) => duration !== null);

  const averageRecoveryTime = recoveryTimes.length
    ? (
        recoveryTimes.reduce((sum, time) => sum + time, 0) /
        recoveryTimes.length
      ).toFixed(2)
    : null;

  const selectedEvent =
    events.find((event) => String(event.id) === selectedEventId) ||
    latestEvent ||
    null;

  const visualBlocks = selectedEvent
    ? [
        {
          number: selectedEvent.startBlock - 1,
          label: "Previous block",
          affected: false,
        },
        ...Array.from(
          {
            length: Math.min(
              Math.max(selectedEvent.affectedBlocks || 1, 1),
              5
            ),
          },
          (_, index) => ({
            number: selectedEvent.startBlock + index,
            label: "Affected block",
            affected: true,
          })
        ),
        {
          number: selectedEvent.endBlock + 1,
          label: "Next block",
          affected: false,
        },
      ]
    : [];

  const additionalBlocks = selectedEvent
    ? Math.max((selectedEvent.affectedBlocks || 1) - 5, 0)
    : 0;

  const visualizationCard = (block, afterRecovery) => {
    const isAffected = block.affected;

    const stateLabel = !isAffected
      ? "Context"
      : !afterRecovery
      ? "Mismatch detected"
      : selectedEvent?.status === "RECOVERED"
      ? "Recovered"
      : selectedEvent?.status || "Pending";

    const borderColor = !isAffected
      ? "#64748b"
      : !afterRecovery
      ? "#ef4444"
      : selectedEvent?.status === "RECOVERED"
      ? "#22c55e"
      : "#eab308";

    return (
      <div
        key={`${afterRecovery ? "after" : "before"}-${block.number}`}
        style={{
          minWidth: "145px",
          flex: "1 1 145px",
          padding: "16px",
          border: `2px solid ${borderColor}`,
          borderRadius: "12px",
          background: "rgba(100, 116, 139, 0.06)",
          textAlign: "center",
        }}
      >
        <div style={{ fontSize: "12px", opacity: 0.7 }}>
          {block.label}
        </div>

        <div style={{ fontWeight: 700, margin: "8px 0" }}>
          #{block.number}
        </div>

        <div
          style={{
            fontSize: "12px",
            fontWeight: 600,
            color: borderColor,
          }}
        >
          {stateLabel}
        </div>
      </div>
    );
  };

  return (
    <div className="app">
      <header>
        <div>
          <h1>Ledger Data Extraction Pipeline</h1>
          <p>Chain Reorganisation Monitoring & Analytics</p>
        </div>

        <div className="status">
          <span className="status-dot"></span>
          Pipeline Dashboard
        </div>
      </header>

      <main>
        {/* MAIN STATISTICS */}
        <section className="cards">
          <div className="card">
            <p>Latest Ethereum Block</p>
            <h2>{latestBlock}</h2>
          </div>

          <div className="card">
            <p>Blocks Extracted</p>
            <h2>{storedBlockCount}</h2>
          </div>

          <div className="card">
            <p>Recorded Mismatch Events</p>
            <h2>{eventsError ? "-" : events.length}</h2>
          </div>

          <div className="card">
            <p>Latest Recovery Status</p>
            <h2
              className={
                latestEvent?.status === "RECOVERED" ? "healthy" : ""
              }
            >
              {eventsError
                ? "Unavailable"
                : latestEvent?.status || "No events"}
            </h2>
          </div>
        </section>

        {/* RECOVERY ANALYTICS */}
        <section className="panel">
          <h2>Recovery Analytics</h2>

          <section className="cards">
            <div className="card">
              <p>Successful Recoveries</p>
              <h2>{eventsError ? "-" : totalRecoveries}</h2>
            </div>

            <div className="card">
              <p>Maximum Affected Blocks</p>
              <h2>{eventsError ? "-" : maxAffectedBlocks}</h2>
            </div>

            <div className="card">
              <p>Average Recovery Time</p>
              <h2>
                {eventsError || averageRecoveryTime === null
                  ? "-"
                  : `${averageRecoveryTime} ms`}
              </h2>
            </div>
          </section>
        </section>

        {/* BLOCKCHAIN EXTRACTION */}
        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>Blockchain Extraction</h2>
              <p>Retrieve and store the latest Ethereum block.</p>
            </div>

            <button onClick={fetchLatestBlock} disabled={loading}>
              {loading ? "Extracting..." : "Fetch Latest Block"}
            </button>
          </div>
        </section>

        {/* RECENT BLOCKS */}
        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>Recently Extracted Blocks</h2>
              <p>Latest 10 blocks stored in PostgreSQL.</p>
            </div>

            <button onClick={getRecentBlocks}>
              Refresh Blocks
            </button>
          </div>

          {blocksError ? (
            <div className="empty">
              Unable to load stored blocks.
            </div>
          ) : blocks.length === 0 ? (
            <div className="empty">
              No stored blocks available.
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
                        {shortHash(block.blockHash)}
                      </td>
                      <td title={block.previousHash}>
                        {shortHash(block.previousHash)}
                      </td>
                      <td>{block.transactionCount}</td>
                      <td>
                        <span className="canonical">Stored</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* REORGANISATION MONITOR */}
        <section className="panel monitor">
          <div>
            <h2>Chain Reorganisation Monitor</h2>
            <p>
              The backend periodically compares recently stored
              block hashes against the current Ethereum chain.
            </p>
            <p style={{ marginTop: "10px", fontSize: "13px" }}>
              A detected mismatch may be caused by a real chain
              change or by simulated database corruption.
            </p>
          </div>

          <div>
            <div className="monitor-status">
              {eventsError
                ? "Unable to load event history"
                : latestEvent
                ? `Latest event: ${latestEvent.status}`
                : "No recorded mismatch events"}
            </div>

            <button
              onClick={runMonitor}
              disabled={monitorLoading}
              style={{ marginTop: "12px" }}
            >
              {monitorLoading
                ? "Checking..."
                : "Check & Recover Now"}
            </button>
          </div>

          {monitorMessage && (
            <p style={{ marginTop: "12px" }}>
              {monitorMessage}
            </p>
          )}
        </section>

        {/* CHAIN REORGANISATION VISUALIZER */}
        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>Chain Reorganisation Visualizer</h2>
              <p>
                Explore recorded mismatch and recovery events.
              </p>
            </div>

            <button onClick={getReorgEvents}>
              Refresh Events
            </button>
          </div>

          <div
            style={{
              padding: "12px 16px",
              borderRadius: "10px",
              border: "1px solid #64748b",
              marginBottom: "20px",
              fontSize: "13px",
              lineHeight: 1.6,
            }}
          >
            <strong>Visualization type: Illustrative reconstruction.</strong>
            {" "}
            Block numbers, affected ranges, statuses, and recovery
            durations come from PostgreSQL. The before-and-after
            diagrams do not represent saved historical hashes.
            A real Ethereum reorganisation has not been independently
            verified by these test events.
          </div>

          {eventsError ? (
            <div className="empty">
              Unable to load recovery events.
            </div>
          ) : events.length === 0 ? (
            <div className="empty">
              No recovery events available to visualize.
            </div>
          ) : (
            <>
              <div style={{ marginBottom: "20px" }}>
                <label
                  htmlFor="recovery-event-selector"
                  style={{
                    display: "block",
                    marginBottom: "8px",
                    fontWeight: 600,
                  }}
                >
                  Select a recovery event
                </label>

                <select
                  id="recovery-event-selector"
                  value={selectedEvent?.id ?? ""}
                  onChange={(e) =>
                    setSelectedEventId(e.target.value)
                  }
                  style={{
                    padding: "12px",
                    borderRadius: "8px",
                    width: "100%",
                    maxWidth: "440px",
                    background: "#1e293b",
                    color: "#ffffff",
                    border: "1px solid #64748b",
                  }}
                >
                  {events.map((event) => (
                    <option key={event.id} value={event.id}>
                      Event #{event.id} — Blocks{" "}
                      {event.startBlock} to {event.endBlock}
                    </option>
                  ))}
                </select>
              </div>

              <section className="cards">
                <div className="card">
                  <p>Selected Event</p>
                  <h2>#{selectedEvent.id}</h2>
                </div>

                <div className="card">
                  <p>Affected Blocks</p>
                  <h2>{selectedEvent.affectedBlocks}</h2>
                </div>

                <div className="card">
                  <p>Recovery Duration</p>
                  <h2>
                    {getDuration(selectedEvent) === null
                      ? "-"
                      : `${getDuration(selectedEvent)} ms`}
                  </h2>
                </div>

                <div className="card">
                  <p>Event Status</p>
                  <h2
                    className={
                      selectedEvent.status === "RECOVERED"
                        ? "healthy"
                        : ""
                    }
                  >
                    {selectedEvent.status}
                  </h2>
                </div>
              </section>

              <div style={{ marginTop: "24px" }}>
                <h3>Before Recovery — Detected Mismatch</h3>
                <p style={{ margin: "8px 0 16px", opacity: 0.75 }}>
                  Illustrative representation of the affected
                  stored-block range.
                </p>

                <div
                  style={{
                    display: "flex",
                    flexWrap: "wrap",
                    gap: "12px",
                  }}
                >
                  {visualBlocks.map((block) =>
                    visualizationCard(block, false)
                  )}
                </div>

                {additionalBlocks > 0 && (
                  <p style={{ marginTop: "12px" }}>
                    + {additionalBlocks} additional affected blocks
                    not displayed individually.
                  </p>
                )}
              </div>

              <div
                style={{
                  textAlign: "center",
                  fontSize: "28px",
                  padding: "20px",
                  fontWeight: 700,
                }}
              >
                ↓
                <div style={{ fontSize: "13px", marginTop: "8px" }}>
                  Compare stored hashes → Recover → Verify
                </div>
              </div>

              <div>
                <h3>After Recovery — Recorded Outcome</h3>
                <p style={{ margin: "8px 0 16px", opacity: 0.75 }}>
                  Green indicates an event recorded as RECOVERED.
                  Other statuses do not indicate a successful repair.
                </p>

                <div
                  style={{
                    display: "flex",
                    flexWrap: "wrap",
                    gap: "12px",
                  }}
                >
                  {visualBlocks.map((block) =>
                    visualizationCard(block, true)
                  )}
                </div>
              </div>

              <div
                style={{
                  marginTop: "24px",
                  padding: "16px",
                  borderRadius: "10px",
                  background: "rgba(100, 116, 139, 0.08)",
                }}
              >
                <h3>Recovery Event Details</h3>

                <p>
                  <strong>Block range:</strong>{" "}
                  {selectedEvent.startBlock} – {selectedEvent.endBlock}
                </p>
                <p>
                  <strong>Detected at:</strong>{" "}
                  {formatDate(selectedEvent.detectedAt)}
                </p>
                <p>
                  <strong>Recovered at:</strong>{" "}
                  {formatDate(selectedEvent.recoveredAt)}
                </p>
                <p>
                  <strong>Status:</strong>{" "}
                  {selectedEvent.status}
                </p>
                <p>
                  <strong>Event classification:</strong>{" "}
                  Stored-block mismatch. Real chain reorganisation
                  not independently confirmed.
                </p>
              </div>
            </>
          )}
        </section>

        {/* RECOVERY HISTORY */}
        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>Recovery Event History</h2>
              <p>
                Recorded mismatch detection and recovery results.
              </p>
            </div>

            <button onClick={getReorgEvents}>
              Refresh Events
            </button>
          </div>

          {eventsError ? (
            <div className="empty">
              Could not load recovery events.
            </div>
          ) : events.length === 0 ? (
            <div className="empty">
              No recovery events recorded yet.
            </div>
          ) : (
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Event ID</th>
                    <th>Start Block</th>
                    <th>End Block</th>
                    <th>Affected Blocks</th>
                    <th>Recovery Time</th>
                    <th>Status</th>
                  </tr>
                </thead>

                <tbody>
                  {events.map((event) => (
                    <tr key={event.id}>
                      <td>{event.id}</td>
                      <td>{event.startBlock}</td>
                      <td>{event.endBlock}</td>
                      <td>{event.affectedBlocks}</td>
                      <td>
                        {getDuration(event) === null
                          ? "-"
                          : `${getDuration(event)} ms`}
                      </td>
                      <td>
                        <span
                          className={
                            event.status === "RECOVERED"
                              ? "canonical"
                              : ""
                          }
                        >
                          {event.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

export default App;
