import java.lang.reflect.Method;
import net.minecraft.client.KeyMapping;

/**
 * Проверка, что миксин вообще может примениться в этой версии игры.
 *
 * <p>Зачем: {@code @Inject(method = "setDown")} привязывается к имени метода.
 * Если в новой версии игры метод переименуют или изменят дескриптор, миксин
 * не применится — и мод молча перестанет работать, без единой ошибки в логе,
 * потому что {@code defaultRequire: 1} сообщает об этом только при загрузке.
 * Стенд падает раньше и с понятным текстом.
 */
public final class VerifyMixinTarget {
	private static int fail = 0;

	private static void ok(boolean cond, String msg) {
		if (cond) {
			System.out.println("  ✓ " + msg);
		} else {
			fail++;
			System.out.println("  ✗ " + msg);
		}
	}

	public static void main(String[] args) throws Exception {
		System.out.println("=== Цель миксина в этой версии игры ===");

		// F3+T обрабатывается в handleDebugKeys(KeyEvent). Если сигнатуру
		// изменят, мод молча перестанет зажимать кнопки — проверяем заранее.
		try {
			Method debugKeys = net.minecraft.client.KeyboardHandler.class
					.getDeclaredMethod("handleDebugKeys", net.minecraft.client.input.KeyEvent.class);
			ok(true, "KeyboardHandler.handleDebugKeys(KeyEvent) существует");
			ok(boolean.class.equals(debugKeys.getReturnType()), "возвращает boolean");
		} catch (NoSuchMethodException e) {
			ok(false, "handleDebugKeys(KeyEvent) НЕ найден — триггер F3+T не сработает");
		}

		boolean hasF3T = false;
		for (java.lang.reflect.Field f : net.minecraft.client.Options.class.getFields()) {
			if (f.getName().equals("keyDebugReloadResourcePacks") && f.getType() == KeyMapping.class) {
				hasF3T = true;
				break;
			}
		}
		ok(hasF3T, "Options.keyDebugReloadResourcePacks существует");

		Method setDown;
		try {
			setDown = KeyMapping.class.getDeclaredMethod("setDown", boolean.class);
			ok(true, "KeyMapping.setDown(boolean) существует");
		} catch (NoSuchMethodException e) {
			ok(false, "KeyMapping.setDown(boolean) НЕ найден — миксин не применится");
			System.out.println("\nПРОВАЛОВ: " + fail);
			System.exit(1);
			return;
		}

		ok(void.class.equals(setDown.getReturnType()), "возвращает void (как ожидает инжектор)");
		ok(java.lang.reflect.Modifier.isPublic(setDown.getModifiers()), "метод публичный");
		ok(!setDown.isBridge() && !setDown.isSynthetic(), "метод не синтетический (инжектор его видит)");

		// Класс миксина должен существовать в том же jar, иначе конфиг ссылается
		// в пустоту. Аннотацию @Mixin проверить рефлексией нельзя: у неё
		// RetentionPolicy.CLASS, в рантайме она отсутствует. Поэтому читаем
		// сам конфиг миксинов и наличие аннотации в байткоде.
		try {
			Class<?> mixin = Class.forName("nya2dtyan.stickybuttons.client.mixin.KeyMappingMixin");
			ok(mixin != null, "класс KeyMappingMixin загружается из jar");
		} catch (ClassNotFoundException e) {
			ok(false, "класс миксина не найден в jar");
		}

		try (java.io.InputStream in = VerifyMixinTarget.class
				.getResourceAsStream("/stickybuttons.mixins.json")) {
			if (in == null) {
				ok(false, "stickybuttons.mixins.json отсутствует в jar");
			} else {
				String json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
				ok(json.contains("\"KeyMappingMixin\""), "конфиг перечисляет KeyMappingMixin");
				ok(json.contains("\"DebugKeysMixin\""), "конфиг перечисляет DebugKeysMixin");
				ok(json.contains("JAVA_21") || json.contains("JAVA_25"),
						"compatibilityLevel задан");
			}
		}

		// В байткоде аннотация остаётся (retention CLASS) — проверяем её наличие.
		try (java.io.InputStream in = VerifyMixinTarget.class
				.getResourceAsStream("/nya2dtyan/stickybuttons/client/mixin/KeyMappingMixin.class")) {
			if (in == null) {
				ok(false, "байткод миксина не найден в jar");
			} else {
				byte[] bytes = in.readAllBytes();
				String text = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
				ok(text.contains("Lorg/spongepowered/asm/mixin/Mixin;"),
						"в байткоде есть аннотация @Mixin");
				ok(text.contains("setDown"), "цель инжектора записана в байткоде");
			}
		}

		// Второй миксин — перехват самого F3+T.
		try {
			Class<?> mixin = Class.forName("nya2dtyan.stickybuttons.client.mixin.DebugKeysMixin");
			ok(mixin != null, "класс DebugKeysMixin загружается из jar");
		} catch (ClassNotFoundException e) {
			ok(false, "класс DebugKeysMixin не найден в jar");
		}

		try (java.io.InputStream in = VerifyMixinTarget.class
				.getResourceAsStream("/nya2dtyan/stickybuttons/client/mixin/DebugKeysMixin.class")) {
			if (in == null) {
				ok(false, "байткод DebugKeysMixin не найден в jar");
			} else {
				String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
				ok(text.contains("Lorg/spongepowered/asm/mixin/Mixin;"),
						"DebugKeysMixin помечен @Mixin");
				ok(text.contains("handleDebugKeys"), "цель инжектора handleDebugKeys в байткоде");
			}
		}

		// Имена привязок, которые мод удерживает, тоже должны существовать.
		for (String field : new String[] {"keyAttack", "keyUse", "keyPickItem"}) {
			boolean found = false;
			for (java.lang.reflect.Field f : net.minecraft.client.Options.class.getFields()) {
				if (f.getName().equals(field) && f.getType() == KeyMapping.class) {
					found = true;
					break;
				}
			}
			ok(found, "Options." + field + " существует и имеет тип KeyMapping");
		}

		System.out.println(fail == 0 ? "\nВСЁ ЗЕЛЁНОЕ" : "\nПРОВАЛОВ: " + fail);
		if (fail != 0) {
			System.exit(1);
		}
	}
}
