import nya2dtyan.stickybuttons.StickyLogic;

/**
 * Стенд логики залипания: вызывается на СОБРАННОМ jar через URLClassLoader,
 * поэтому проверяет тот байткод, который получит игрок.
 *
 * Сценарии повторяют то, что происходит в игре при F3+T:
 * кнопка зажата → открылся оверлей загрузки → оверлей закрылся.
 */
public final class VerifySticky {
	private static int fail = 0;

	private static void ok(boolean cond, String msg) {
		if (cond) {
			System.out.println("  ✓ " + msg);
		} else {
			fail++;
			System.out.println("  ✗ " + msg);
		}
	}

	public static void main(String[] args) {
		System.out.println("=== 1. Левая кнопка: зажата → F3+T → отпущена ===");
		{
			StickyLogic l = new StickyLogic();
			// тик до перезагрузки: кнопка нажата
			ok(l.tick(false, true, false, false) == 0, "до оверлея залипания нет");
			// оверлей открылся, кнопка всё ещё физически нажата
			ok((l.tick(true, true, false, false) & StickyLogic.LEFT) == 0, "во время оверлея залипание не выставлено");
			ok(l.isArmed(), "нажатие запомнено");
			// оверлей закрылся, мышь отпущена — должен включиться автоклик
			int m = l.tick(false, false, false, false);
			ok((m & StickyLogic.LEFT) != 0, "после оверлея левая кнопка залипла");
			ok(l.isStickyLeft(), "состояние залипания выставлено");
			// держится на следующих тиках
			ok((l.tick(false, false, false, false) & StickyLogic.LEFT) != 0, "залипание держится следующий тик");
			ok((l.tick(false, false, false, false) & StickyLogic.LEFT) != 0, "и ещё один");
		}

		System.out.println("\n=== 2. Снятие залипания повторным нажатием ===");
		{
			StickyLogic l = new StickyLogic();
			l.tick(false, true, false, false);
			l.tick(true, true, false, false);
			l.tick(false, false, false, false);
			ok(l.isStickyLeft(), "залипание включено");
			// игрок нажимает кнопку снова — залипание снимается
			int m = l.tick(false, true, false, false);
			ok((m & StickyLogic.LEFT) == 0, "нажатие снимает залипание");
			ok(!l.isStickyLeft(), "состояние сброшено");
			// отпустил — обычное поведение, без залипания
			ok((l.tick(false, false, false, false) & StickyLogic.LEFT) == 0, "после снятия залипания нет");
		}

		System.out.println("\n=== 3. Кнопку отпустили ДО перезагрузки — залипания быть не должно ===");
		{
			StickyLogic l = new StickyLogic();
			l.tick(false, true, false, false);
			l.tick(false, false, false, false);   // отпустил
			l.tick(true, false, false, false);    // открылся оверлей, кнопка не нажата
			ok(!l.isArmed(), "нечего запоминать");
			int m = l.tick(false, false, false, false);
			ok(m == 0, "залипания нет");
		}

		System.out.println("\n=== 4. Правая кнопка и средняя ===");
		{
			StickyLogic l = new StickyLogic();
			l.tick(false, false, true, false);
			l.tick(true, false, true, false);
			ok((l.tick(false, false, false, false) & StickyLogic.RIGHT) != 0, "правая кнопка залипла");
			ok(l.isStickyRight(), "флаг правой выставлен");
			// снимаем нажатием правой
			ok((l.tick(false, false, true, false) & StickyLogic.RIGHT) == 0, "нажатие правой снимает залипание");
		}
		{
			StickyLogic l = new StickyLogic();
			l.tick(false, false, false, true);
			l.tick(true, false, false, true);
			ok((l.tick(false, false, false, false) & StickyLogic.MIDDLE) != 0, "средняя кнопка залипла");
			ok((l.tick(false, false, false, true) & StickyLogic.MIDDLE) == 0, "нажатие средней снимает залипание");
		}

		System.out.println("\n=== 5. Две кнопки одновременно ===");
		{
			StickyLogic l = new StickyLogic();
			l.tick(false, true, true, false);
			l.tick(true, true, true, false);
			int m = l.tick(false, false, false, false);
			ok((m & StickyLogic.LEFT) != 0 && (m & StickyLogic.RIGHT) != 0, "обе кнопки залипли");
			int m2 = l.tick(false, true, false, false);
			ok((m2 & StickyLogic.LEFT) == 0 && (m2 & StickyLogic.RIGHT) != 0, "снятие левой не трогает правую");
		}

		System.out.println("\n=== 6. Сброс (выход из мира) ===");
		{
			StickyLogic l = new StickyLogic();
			l.tick(false, true, false, false);
			l.tick(true, true, false, false);
			l.tick(false, false, false, false);
			ok(l.isStickyLeft(), "залипание есть");
			l.reset();
			ok(!l.isStickyLeft() && !l.isArmed(), "reset снимает всё");
			ok(l.tick(false, false, false, false) == 0, "после reset залипания нет");
		}

		System.out.println("\n=== 7. Оверлей без нажатой кнопки ===");
		{
			StickyLogic l = new StickyLogic();
			l.tick(false, false, false, false);
			l.tick(true, false, false, false);
			ok(l.tick(false, false, false, false) == 0, "ничего не залипает");
		}

		System.out.println("\n=== 8. Два F3+T подряд с зажатой кнопкой ===");
		{
			StickyLogic l = new StickyLogic();
			l.tick(false, true, false, false);
			l.tick(true, true, false, false);
			l.tick(false, false, false, false);
			ok(l.isStickyLeft(), "залипло после первой перезагрузки");
			// вторая перезагрузка: кнопка физически не нажата
			l.tick(true, false, false, false);
			int m = l.tick(false, false, false, false);
			ok((m & StickyLogic.LEFT) != 0, "после второй перезагрузки залипание сохранилось");
		}

		System.out.println(fail == 0 ? "\nВСЁ ЗЕЛЁНОЕ" : "\nПРОВАЛОВ: " + fail);
		if (fail != 0) {
			System.exit(1);
		}
	}
}
