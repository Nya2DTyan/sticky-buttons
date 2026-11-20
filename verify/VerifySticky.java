import nya2dtyan.stickybuttons.StickyLogic;

/**
 * Стенд логики залипания: вызывается на СОБРАННОМ jar через classpath клиента,
 * поэтому проверяет тот байткод, который получит игрок.
 *
 * Порядок событий воспроизводит игру: нажатие F3+T ({@link StickyLogic#trigger})
 * приходит раньше, чем мышь отпущена, затем идут тики.
 *
 * Отдельно проверяются комбинации: ЛКМ+ПКМ, ПКМ+средняя, все три — именно они
 * раньше залипали только частично.
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

	/** F3+T нажат при зажатых кнопках {@code pressed}; после — мышь отпущена. */
	private static StickyLogic pressF3T(int pressed) {
		StickyLogic l = new StickyLogic();
		l.tick(pressed);          // такт до нажатия
		l.trigger(pressed);       // F3+T
		return l;
	}

	public static void main(String[] args) {
		System.out.println("=== 1. Левая кнопка: зажата → F3+T → отпущена ===");
		{
			StickyLogic l = pressF3T(StickyLogic.LEFT);
			ok(l.isSticky(StickyLogic.LEFT), "сразу после F3+T ЛКМ зажата");
			int m = l.tick(0);
			ok((m & StickyLogic.LEFT) != 0, "после отпускания мыши ЛКМ осталась зажатой");
			ok((l.tick(0) & StickyLogic.LEFT) != 0, "держится следующий тик");
		}

		System.out.println("\n=== 2. ЛКМ и ПКМ вместе (основной сценарий жалобы) ===");
		{
			StickyLogic l = pressF3T(StickyLogic.LEFT | StickyLogic.RIGHT);
			int m = l.tick(0);
			ok((m & StickyLogic.LEFT) != 0, "ЛКМ зажата");
			ok((m & StickyLogic.RIGHT) != 0, "ПКМ зажата");
			ok(l.stickyMask() == (StickyLogic.LEFT | StickyLogic.RIGHT), "обе в маске");
			int m2 = l.tick(0);
			ok((m2 & StickyLogic.LEFT) != 0 && (m2 & StickyLogic.RIGHT) != 0, "обе держатся следующий тик");
		}

		System.out.println("\n=== 3. ПКМ + средняя ===");
		{
			StickyLogic l = pressF3T(StickyLogic.RIGHT | StickyLogic.MIDDLE);
			int m = l.tick(0);
			ok((m & StickyLogic.RIGHT) != 0, "ПКМ зажата");
			ok((m & StickyLogic.MIDDLE) != 0, "средняя зажата");
			ok((m & StickyLogic.LEFT) == 0, "ЛКМ не тронута");
		}

		System.out.println("\n=== 4. Все три кнопки ===");
		{
			StickyLogic l = pressF3T(StickyLogic.LEFT | StickyLogic.RIGHT | StickyLogic.MIDDLE);
			int m = l.tick(0);
			ok(m == (StickyLogic.LEFT | StickyLogic.RIGHT | StickyLogic.MIDDLE), "все три зажаты");
		}

		System.out.println("\n=== 5. Снятие по одной кнопке ===");
		{
			StickyLogic l = pressF3T(StickyLogic.LEFT | StickyLogic.RIGHT);
			l.tick(0);
			int m = l.tick(StickyLogic.LEFT);
			ok((m & StickyLogic.LEFT) == 0, "нажатие ЛКМ снимает только её");
			ok((m & StickyLogic.RIGHT) != 0, "ПКМ остаётся зажатой");
			int m2 = l.tick(0);
			ok((m2 & StickyLogic.RIGHT) != 0 && (m2 & StickyLogic.LEFT) == 0, "ЛКМ не зажимается снова");
			ok((l.tick(StickyLogic.RIGHT) & StickyLogic.RIGHT) == 0, "нажатие ПКМ снимает её");
			ok(l.stickyMask() == 0, "маска пуста");
		}

		System.out.println("\n=== 6. F3+T без нажатых кнопок — ничего не зажимается ===");
		{
			StickyLogic l = new StickyLogic();
			l.tick(0);
			l.trigger(0);
			ok(l.tick(0) == 0, "залипания нет");
		}

		System.out.println("\n=== 7. Одна отпущена до нажатия, другая зажата ===");
		{
			StickyLogic l = new StickyLogic();
			l.tick(StickyLogic.LEFT | StickyLogic.RIGHT);
			l.tick(StickyLogic.RIGHT);          // ЛКМ отпустили
			l.trigger(StickyLogic.RIGHT);       // F3+T
			int m = l.tick(0);
			ok((m & StickyLogic.RIGHT) != 0, "зажата только ПКМ");
			ok((m & StickyLogic.LEFT) == 0, "ЛКМ не зажата");
		}

		System.out.println("\n=== 8. Перезагрузка из меню НЕ зажимает (только F3+T) ===");
		{
			StickyLogic l = new StickyLogic();
			l.tick(StickyLogic.LEFT | StickyLogic.RIGHT);
			// trigger не вызывается — имитируем обычную перезагрузку ресурсов
			int m = l.tick(StickyLogic.LEFT | StickyLogic.RIGHT);
			ok(m == 0, "без F3+T залипания нет");
			int m2 = l.tick(0);
			ok(m2 == 0, "и после отпускания мыши тоже нет");
		}

		System.out.println("\n=== 9. Два F3+T подряд ===");
		{
			StickyLogic l = pressF3T(StickyLogic.LEFT | StickyLogic.RIGHT);
			ok(l.stickyMask() == (StickyLogic.LEFT | StickyLogic.RIGHT), "зажаты после первого");
			l.tick(0);
			l.trigger(0);   // второй раз, мышь уже отпущена
			int m = l.tick(0);
			ok(m == (StickyLogic.LEFT | StickyLogic.RIGHT), "после второго обе сохранились");
		}

		System.out.println("\n=== 10. Сброс (выход из мира) ===");
		{
			StickyLogic l = pressF3T(StickyLogic.LEFT | StickyLogic.RIGHT);
			l.tick(0);
			ok(l.stickyMask() != 0, "залипание есть");
			l.reset();
			ok(l.stickyMask() == 0, "reset снимает всё");
			ok(l.tick(0) == 0, "после reset залипания нет");
		}

		System.out.println("\n=== 11. release одной кнопки ===");
		{
			StickyLogic l = pressF3T(StickyLogic.LEFT | StickyLogic.RIGHT);
			l.tick(0);
			l.release(StickyLogic.LEFT);
			ok(!l.isSticky(StickyLogic.LEFT), "ЛКМ снята");
			ok(l.isSticky(StickyLogic.RIGHT), "ПКМ осталась");
		}

		System.out.println("\n=== 12. Правая и средняя по отдельности ===");
		{
			StickyLogic r = pressF3T(StickyLogic.RIGHT);
			ok((r.tick(0) & StickyLogic.RIGHT) != 0, "ПКМ зажимается одна");
			StickyLogic md = pressF3T(StickyLogic.MIDDLE);
			ok((md.tick(0) & StickyLogic.MIDDLE) != 0, "средняя зажимается одна");
		}

		System.out.println("\n=== 13. Залипание не снимается само по себе ===");
		{
			StickyLogic l = pressF3T(StickyLogic.LEFT);
			l.tick(0);
			boolean allHeld = true;
			for (int i = 0; i < 100; i++) {
				if ((l.tick(0) & StickyLogic.LEFT) == 0) {
					allHeld = false;
					break;
				}
			}
			ok(allHeld, "держится 100 тиков подряд");
		}

		System.out.println(fail == 0 ? "\nВСЁ ЗЕЛЁНОЕ" : "\nПРОВАЛОВ: " + fail);
		if (fail != 0) {
			System.exit(1);
		}
	}
}
