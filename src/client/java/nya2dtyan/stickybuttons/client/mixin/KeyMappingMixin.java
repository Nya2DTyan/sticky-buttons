package nya2dtyan.stickybuttons.client.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import nya2dtyan.stickybuttons.client.StickyHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Не даёт игре отпустить «залипшую» кнопку.
 *
 * <p>Зачем миксин, а не просто повторная установка флага в тике: события мыши
 * SDL доставляются не в тике, а при опросе окна — отпускание может прийти уже
 * после {@code START_CLIENT_TICK} и до {@code handleKeybinds}. Тогда игра
 * выставляет {@code isDown = false} между моей установкой и опросом, и кнопка
 * «не залипает». Причём для двух кнопок порядок событий разный, поэтому одна
 * срабатывала, а вторая нет.
 *
 * <p>Перехватываем именно отпускание ({@code down == false}) у привязок, которые
 * мод удерживает. Нажатие пропускаем как обычно, чтобы игрок мог снять
 * залипание повторным кликом.
 *
 * <p>Исключение: если открыт экран (инвентарь, Esc), отпускание проходит
 * штатно — иначе игрок продолжал бы копать с открытым меню.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {

	@Inject(method = "setDown", at = @At("HEAD"), cancellable = true)
	private void stickybuttons$keepForced(boolean down, CallbackInfo ci) {
		if (down) {
			return;
		}

		KeyMapping self = (KeyMapping) (Object) this;
		if (!StickyHolder.isForced(self)) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}

		Screen screen = client.gui == null ? null : client.gui.screen();
		if (screen != null) {
			// Экран открыт — снимаем залипание и отдаём отпускание игре,
			// чтобы меню работало привычно.
			StickyHolder.force(self, false);
			return;
		}

		ci.cancel();
	}
}
