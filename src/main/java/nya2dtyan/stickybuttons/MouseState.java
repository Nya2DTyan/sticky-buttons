package nya2dtyan.stickybuttons;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Физическое состояние кнопок мыши, независимое от версии игры.
 *
 * <p>Зачем рефлексия. В 26.2 ввод идёт через GLFW, в 26.3 — уже через SDL.
 * Прямой импорт {@code org.lwjgl.sdl.SDLMouse} не компилируется под 26.2, а
 * импорт GLFW не нужен под 26.3. Один общий код получается только через поиск
 * подходящего API во время работы: сначала SDL, затем GLFW.
 *
 * <p>Почему не {@code MouseHandler.isLeftPressed()}: игра обновляет эти флаги
 * лишь при доставке события конкретной кнопки, и при закрытом оверлее картина
 * «ЛКМ и ПКМ зажаты одновременно» через них неполная. Нужно сырое состояние
 * устройства.
 */
public final class MouseState {

	/** Биты маски — те же, что у {@link StickyLogic}. */
	private static final int GLFW_MOUSE_BUTTON_1 = 0;
	private static final int GLFW_MOUSE_BUTTON_2 = 1;
	private static final int GLFW_MOUSE_BUTTON_3 = 2;

	private static final int SDL_BUTTON_LMASK = 1;
	private static final int SDL_BUTTON_MMASK = 2;
	private static final int SDL_BUTTON_RMASK = 4;

	private static Backend backend;

	private MouseState() {
	}

	@FunctionalInterface
	private interface Backend {
		int buttons() throws Exception;
	}

	/**
	 * @param windowHandle идентификатор окна (нужен GLFW; для SDL игнорируется)
	 * @return маска физически зажатых кнопок, {@code 0} если API недоступен
	 */
	public static int buttons(long windowHandle) {
		Backend b = backend();
		if (b == null) {
			return 0;
		}
		try {
			return b.buttons();
		} catch (Throwable t) {
			return 0;
		}
	}

	private static synchronized Backend backend() {
		if (backend != null) {
			return backend == NONE ? null : backend;
		}
		backend = findSdl();
		if (backend == null) {
			backend = findGlfw();
		}
		if (backend == null) {
			backend = NONE;
			return null;
		}
		return backend;
	}

	/** Заглушка «API не найден», чтобы не искать заново каждый тик. */
	private static final Backend NONE = () -> 0;

	private static Backend findSdl() {
		try {
			Class<?> sdlMouse = Class.forName("org.lwjgl.sdl.SDLMouse");
			Method get = sdlMouse.getMethod("SDL_GetMouseState", java.nio.FloatBuffer.class, java.nio.FloatBuffer.class);
			if (!Modifier.isStatic(get.getModifiers())) {
				return null;
			}
			return () -> {
				java.nio.FloatBuffer x = java.nio.FloatBuffer.allocate(1);
				java.nio.FloatBuffer y = java.nio.FloatBuffer.allocate(1);
				int state = (int) get.invoke(null, x, y);
				int mask = 0;
				if ((state & SDL_BUTTON_LMASK) != 0) {
					mask |= StickyLogic.LEFT;
				}
				if ((state & SDL_BUTTON_RMASK) != 0) {
					mask |= StickyLogic.RIGHT;
				}
				if ((state & SDL_BUTTON_MMASK) != 0) {
					mask |= StickyLogic.MIDDLE;
				}
				return mask;
			};
		} catch (Throwable t) {
			return null;
		}
	}

	private static Backend findGlfw() {
		try {
			Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
			Method getButton = glfw.getMethod("glfwGetMouseButton", long.class, int.class);
			Method getWindow = Class.forName("com.mojang.blaze3d.platform.Window")
					.getMethod("handle");
			Method getInstance = Class.forName("net.minecraft.client.Minecraft")
					.getMethod("getInstance");
			Method getWindowField = Class.forName("net.minecraft.client.Minecraft")
					.getMethod("getWindow");

			return () -> {
				try {
					Object minecraft = getInstance.invoke(null);
					Object window = getWindowField.invoke(minecraft);
					long handle = (long) getWindow.invoke(window);

					int mask = 0;
					if ((int) getButton.invoke(null, handle, GLFW_MOUSE_BUTTON_1) == 1) {
						mask |= StickyLogic.LEFT;
					}
					if ((int) getButton.invoke(null, handle, GLFW_MOUSE_BUTTON_2) == 1) {
						mask |= StickyLogic.RIGHT;
					}
					if ((int) getButton.invoke(null, handle, GLFW_MOUSE_BUTTON_3) == 1) {
						mask |= StickyLogic.MIDDLE;
					}
					return mask;
				} catch (Throwable t) {
					return 0;
				}
			};
		} catch (Throwable t) {
			return null;
		}
	}

	/** Для стендов: сбросить закэшированный backend. */
	public static synchronized void resetForTests() {
		backend = null;
	}
}
