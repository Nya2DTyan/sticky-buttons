package nya2dtyan.stickybuttons.client;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.client.KeyMapping;

/**
 * Какие привязки сейчас удерживаются принудительно.
 *
 * <p>Зачем отдельный держатель: миксин на {@link KeyMapping#setDown(boolean)}
 * должен знать, чьё «отпускание» надо проигнорировать. Сравнение идёт по
 * идентичности объекта ({@link IdentityHashMap}), а не по equals — у KeyMapping
 * equals не переопределён, но полагаться на это не стоит.
 */
public final class StickyHolder {

	private static final Set<KeyMapping> FORCED =
			Collections.newSetFromMap(new IdentityHashMap<>());

	private StickyHolder() {
	}

	public static void force(KeyMapping mapping, boolean forced) {
		if (mapping == null) {
			return;
		}
		if (forced) {
			FORCED.add(mapping);
		} else {
			FORCED.remove(mapping);
		}
	}

	/** Нужно ли игнорировать попытку отпустить эту привязку. */
	public static boolean isForced(KeyMapping mapping) {
		return mapping != null && FORCED.contains(mapping);
	}

	public static void clear() {
		FORCED.clear();
	}
}
