package nya2dtyan.stickybuttons.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.network.chat.Component;
import nya2dtyan.stickybuttons.StickyLogic;

/**
 * Возвращает поведение старых версий: кнопка мыши, зажатая перед перезагрузкой
 * ресурсов по F3+T, остаётся зажатой и после неё — как автокликер.
 *
 * <p>Почему это приходится делать заново. В 26.2 и 26.3 {@code MouseHandler.onButton}
 * обновляет {@code isLeftPressed} только когда нет ни экрана, ни оверлея. На время
 * перезагрузки ресурсов игра ставит {@link LoadingOverlay}, события мыши до
 * состояния кнопок не доходят, а {@code MouseHandler.grabMouse()} при закрытии
 * оверлея вызывает {@code KeyMapping.setAll()} — состояние кнопок
 * пересинхронизируется с физическим. Раньше этой пересинхронизации не было,
 * отпускание терялось, и функция продолжала выполняться сама.
 *
 * <p>Возвращаем эффект без миксинов: каждый тик сверяем состояние кнопок,
 * запоминаем зажатие на момент появления оверлея и после его закрытия
 * удерживаем соответствующую функцию нажатой, пока игрок не нажмёт кнопку снова.
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
			if (lastMask != 0) {
				logic.reset();
				lastMask = 0;
			}
			return;
		}

		boolean overlay = client.gui.overlay() instanceof LoadingOverlay;

		int mask = logic.tick(
				overlay,
				client.mouseHandler.isLeftPressed(),
				client.mouseHandler.isRightPressed(),
				client.mouseHandler.isMiddlePressed());

		// Держим функции нажатыми, пока залипание активно: KeyMapping.isDown()
		// опрашивается в handleKeybinds каждый тик.
		hold(client.options.keyAttack, (mask & StickyLogic.LEFT) != 0);
		hold(client.options.keyUse, (mask & StickyLogic.RIGHT) != 0);
		hold(client.options.keyPickItem, (mask & StickyLogic.MIDDLE) != 0);

		if (mask != lastMask) {
			onStateChanged(client, lastMask, mask);
			lastMask = mask;
		}
	}

	private static void hold(KeyMapping mapping, boolean down) {
		if (down && !mapping.isDown()) {
			mapping.setDown(true);
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
