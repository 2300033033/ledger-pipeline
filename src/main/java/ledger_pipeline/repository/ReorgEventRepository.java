
package ledger_pipeline.repository;

import ledger_pipeline.model.ReorgEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReorgEventRepository
        extends JpaRepository<ReorgEvent, Long> {
}
