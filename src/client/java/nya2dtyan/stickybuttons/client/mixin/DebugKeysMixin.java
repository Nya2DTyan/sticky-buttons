package nya2dtyan.stickybuttons.client.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import nya2dtyan.stickybuttons.client.StickyButtonsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ловит именно F3+T.
 *
 * <p>Почему не хук на {@code Minecraft.reloadResourcePacks()}: этот же метод
 * вызывают меню настроек и смена пака ресурсов, а залипание должно включаться
 * только по отладочной комбинации. {@code handleDebugKeys} — то самое место,
 * где проверяется {@code keyDebugReloadResourcePacks} (в 26.2 и 26.3 совпадает).
 *
 * <p>Почему не появление {@code LoadingOverlay}: с модом Remove Reloading Screen
 * (rrls) оверлея не бывает вовсе, и детект по нему не срабатывает.
 *
 * <p>Состояние кнопок читается до вызова: F3+T перезагружает ресурсы и может
 * занять несколько секунд, за которые игрок успеет отпустить мышь.
 */
@Mixin(KeyboardHandler.class)
public abstract class DebugKeysMixin {

	@Inject(method = "handleDebugKeys", at = @At("HEAD"))
	private void stickybuttons$captureF3T(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}

		KeyMapping reload = client.options == null ? null : client.options.keyDebugReloadResourcePacks;
		if (reload == null || !reload.matches(event)) {
			return;
		}

		StickyButtonsClient.onF3T();
	}
}
