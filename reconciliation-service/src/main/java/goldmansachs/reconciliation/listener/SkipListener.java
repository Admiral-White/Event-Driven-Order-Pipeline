package goldmansachs.reconciliation.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.SkipListenerSupport;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class SkipListener extends SkipListenerSupport<Map<String, Object>, Map<String, Object>> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("An item was skipped on read operation", t);
    }

    @Override
    public void onSkipInWrite(Map<String, Object> item, Throwable t) {
        log.warn("An item was skipped on write operation. Item: {}", item, t);
    }

    @Override
    public void onSkipInProcess(Map<String, Object> item, Throwable t) {
        log.warn("An item was skipped on process operation. Settlement ID: {}", 
                item.get("settlement_id"), t);
    }
}
