package scripts.tests;

import java.util.Arrays;
import java.util.List;

import ru.ytkab0bp.slicebeam.utils.FilamentPaletteSyncPolicy;
import ru.ytkab0bp.slicebeam.utils.FilamentSlot;

public final class FilamentPaletteSyncPolicyTest {
    public static void main(String[] args) {
        List<FilamentSlot> configured = Arrays.asList(new FilamentSlot(0xFF111111, "PETG"));
        List<FilamentSlot> printer = Arrays.asList(
                new FilamentSlot(0xFF222222, "PETG"),
                new FilamentSlot(0xFF333333, "PLA"),
                new FilamentSlot(0xFF444444, "PLA"),
                new FilamentSlot(0xFF555555, "PLA"));
        List<FilamentSlot> synced = FilamentPaletteSyncPolicy.updateExisting(configured, printer);
        if (synced.size() != 1) throw new AssertionError("Sync restored removed palette slots");
        if (!"PETG".equals(synced.get(0).type) || synced.get(0).color != 0xFF222222)
            throw new AssertionError("Existing slot did not receive the printer's live material/color");

        List<FilamentSlot> withGap = Arrays.asList(
                new FilamentSlot(0xFF101010, "PETG"), new FilamentSlot(0xFF202020, "PLA"));
        List<FilamentSlot> gapResult = FilamentPaletteSyncPolicy.updateExisting(withGap,
                Arrays.asList(null, new FilamentSlot(0xFF303030, "PLA")));
        if (gapResult.size() != 2 || gapResult.get(0).color != 0xFF101010
                || gapResult.get(1).color != 0xFF303030)
            throw new AssertionError("Empty printer slot changed a configured palette slot");
        System.out.println("FilamentPaletteSyncPolicyTest passed: deleted slots stay deleted and empty gates preserve configured values.");
    }
}
