package nya2dtyan.stickybuttons.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.network.chat.Component;
import nya2dtyan.stickybuttons.MouseState;
import nya2dtyan.stickybuttons.StickyLogic;

/**
 * Возвращает поведение старых версий: кнопки мыши, зажатые перед перезагрузкой
 * ресурсов по F3+T, остаются зажатыми и после неё — как автокликер.
 *
 * <p>Почему это приходится делать заново. В 26.2 и 26.3 {@code MouseHandler.onButton}
 * обновляет {@code isLeftPressed} только когда нет ни экрана, ни оверлея. На время
 * перезагрузки ресурсов игра ставит {@link LoadingOverlay}, события мыши до
 * состояния кнопок не доходят, а {@code MouseHandler.grabMouse()} при закрытии
 * оверлея вызывает {@code KeyMapping.setAll()} — состояние привязок
 * пересинхронизируется с физическим. Раньше этой пересинхронизации не было,
 * отпускание терялось, и функция продолжала выполняться сама.
 *
 * <p>Две вещи, без которых залипает только одна кнопка:
 * <ol>
 *   <li>Состояние берём из устройства ({@link MouseState}), а не из
 *       {@code MouseHandler}: игра обновляет свои флаги только при доставке
 *       события конкретной кнопки, поэтому картина «ЛКМ и ПКМ зажаты
 *       одновременно» через них неполная.</li>
 *   <li>Держим привязки через {@link StickyHolder} и миксин на
 *       {@code KeyMapping.setDown}: события мыши приходят вне тика, и
 *       выставленный в тике флаг игра успевает погасить до опроса.</li>
 * </ol>
 */
public final class StickyButtonsClient implements ClientModInitializer {

	private final StickyLogic logic = new StickyLogic();
	private int lastMask;
	private boolean announced;

	@Override
	public void onInitializeClient() {
		// START, а не END: состояние нужно выставить до того, как Minecraft.tick()
		// вызовет handleKeybinds и опросит привязки.
		ClientTickEvents.START_CLIENT_TICK.register(this::onTick);
	}

	private void onTick(Minecraft client) {
		if (client.player == null || client.level == null) {
			clearState(client);
			return;
		}

		boolean overlay = client.gui.overlay() instanceof LoadingOverlay;
		int buttons = physicalButtons(client);

		int mask = logic.tick(overlay, buttons);

		sync(client.options.keyAttack, StickyLogic.LEFT, mask);
		sync(client.options.keyUse, StickyLogic.RIGHT, mask);
		sync(client.options.keyPickItem, StickyLogic.MIDDLE, mask);

		if (mask != lastMask) {
			onStateChanged(client, lastMask, mask);
			lastMask = mask;
		}
	}

	/**
	 * Держим привязку нажатой и помечаем её в держателе, чтобы миксин не дал
	 * игре погасить её между тиками. Снятие отметки обязательно: иначе кнопка
	 * останется нажатой после того, как игрок её отпустил.
	 */
	private static void sync(KeyMapping mapping, int bit, int mask) {
		boolean held = (mask & bit) != 0;
		StickyHolder.force(mapping, held);
		if (held && !mapping.isDown()) {
			mapping.setDown(true);
		}
	}

	/** Маска физически зажатых кнопок; при недоступном API — состояние игры. */
	private static int physicalButtons(Minecraft client) {
		long handle = 0L;
		try {
			handle = client.getWindow().handle();
		} catch (Throwable ignored) {
			// окна нет (стенд) — просто пойдём запасным путём
		}

		int mask = MouseState.buttons(handle);
		if (mask != 0) {
			return mask;
		}

		// Запасной путь: сырое API недоступно. Флаги игры годятся, чтобы
		// зафиксировать зажатие, но комбинации могут определяться неполно.
		if (client.mouseHandler == null) {
			return 0;
		}
		if (client.mouseHandler.isLeftPressed()) {
			mask |= StickyLogic.LEFT;
		}
		if (client.mouseHandler.isRightPressed()) {
			mask |= StickyLogic.RIGHT;
		}
		if (client.mouseHandler.isMiddlePressed()) {
			mask |= StickyLogic.MIDDLE;
		}
		return mask;
	}

	private void clearState(Minecraft client) {
		if (lastMask == 0 && !logic.isArmed()) {
			return;
		}
		if (client != null) {
			// Снимаем и отметку, и сам флаг: иначе при выходе из мира привязка
			// останется нажатой до следующего KeyMapping.setAll().
			release(client.options.keyAttack);
			release(client.options.keyUse);
			release(client.options.keyPickItem);
		}
		logic.reset();
		lastMask = 0;
		announced = false;
	}

	private static void release(KeyMapping mapping) {
		StickyHolder.force(mapping, false);
		if (mapping.isDown()) {
			mapping.setDown(false);
		}
	}

	private void onStateChanged(Minecraft client, int before, int after) {
		if (after == 0 && before != 0) {
			client.gui.hud.setOverlayMessage(Component.translatable("stickybuttons.released"), false);
			announced = false;
		} else if (after != 0 && !announced) {
			client.gui.hud.setOverlayMessage(Component.translatable("stickybuttons.engaged"), false);
			announced = true;
		}
	}
}
