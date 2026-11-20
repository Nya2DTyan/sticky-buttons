import nya2dtyan.stickybuttons.StickyLogic;

/**
 * Стенд логики залипания: вызывается на СОБРАННОМ jar через classpath клиента,
 * поэтому проверяет тот байткод, который получит игрок.
 *
 * Сценарии повторяют то, что происходит в игре при F3+T:
 * кнопки зажаты → открылся оверлей загрузки → оверлей закрылся.
 *
 * Отдельно проверяются комбинации: ЛКМ+ПКМ, ПКМ+средняя, все три.
 * Именно комбинации не работали раньше — залипала только одна кнопка.
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

	/** «Сеанс»: нажали указанные кнопки, открыли и закрыли оверлей. */
	private static StickyLogic cycle(int pressed) {
		StickyLogic l = new StickyLogic();
		l.tick(false, pressed);
		l.tick(true, pressed);
		return l;
	}

	public static void main(String[] args) {
		System.out.println("=== 1. Левая кнопка: зажата → F3+T → отпущена ===");
		{
			StickyLogic l = new StickyLogic();
			ok(l.tick(false, StickyLogic.LEFT) == 0, "до оверлея залипания нет");
			ok(l.tick(true, StickyLogic.LEFT) == 0, "во время оверлея залипание не выставлено");
			ok(l.isArmed(), "нажатие запомнено");
			int m = l.tick(false, 0);
			ok((m & StickyLogic.LEFT) != 0, "после оверлея ЛКМ залипла");
			ok(l.isSticky(StickyLogic.LEFT), "состояние залипания выставлено");
			ok((l.tick(false, 0) & StickyLogic.LEFT) != 0, "держится следующий тик");
		}

		System.out.println("\n=== 2. ЛКМ и ПКМ вместе (основной сценарий жалобы) ===");
		{
			StickyLogic l = cycle(StickyLogic.LEFT | StickyLogic.RIGHT);
			int m = l.tick(false, 0);
			ok((m & StickyLogic.LEFT) != 0, "ЛКМ залипла");
			ok((m & StickyLogic.RIGHT) != 0, "ПКМ залипла");
			ok(l.stickyMask() == (StickyLogic.LEFT | StickyLogic.RIGHT), "обе кнопки в маске");
			int m2 = l.tick(false, 0);
			ok((m2 & StickyLogic.LEFT) != 0 && (m2 & StickyLogic.RIGHT) != 0, "обе держатся следующий тик");
		}

		System.out.println("\n=== 3. ПКМ + средняя ===");
		{
			StickyLogic l = cycle(StickyLogic.RIGHT | StickyLogic.MIDDLE);
			int m = l.tick(false, 0);
			ok((m & StickyLogic.RIGHT) != 0, "ПКМ залипла");
			ok((m & StickyLogic.MIDDLE) != 0, "средняя залипла");
			ok((m & StickyLogic.LEFT) == 0, "ЛКМ не тронута");
		}

		System.out.println("\n=== 4. Все три кнопки ===");
		{
			StickyLogic l = cycle(StickyLogic.LEFT | StickyLogic.RIGHT | StickyLogic.MIDDLE);
			int m = l.tick(false, 0);
			ok(m == (StickyLogic.LEFT | StickyLogic.RIGHT | StickyLogic.MIDDLE), "все три залипли");
		}

		System.out.println("\n=== 5. Снятие по одной кнопке ===");
		{
			StickyLogic l = cycle(StickyLogic.LEFT | StickyLogic.RIGHT);
			l.tick(false, 0);
			int m = l.tick(false, StickyLogic.LEFT);
			ok((m & StickyLogic.LEFT) == 0, "нажатие ЛКМ снимает только её");
			ok((m & StickyLogic.RIGHT) != 0, "ПКМ остаётся залипшей");
			int m2 = l.tick(false, 0);
			ok((m2 & StickyLogic.RIGHT) != 0 && (m2 & StickyLogic.LEFT) == 0, "ЛКМ не залипает снова");
			ok((l.tick(false, StickyLogic.RIGHT) & StickyLogic.RIGHT) == 0, "нажатие ПКМ снимает её");
			ok(l.stickyMask() == 0, "маска пуста");
		}

		System.out.println("\n=== 6. Кнопки отпущены ДО перезагрузки — залипания нет ===");
		{
			StickyLogic l = new StickyLogic();
			l.tick(false, StickyLogic.LEFT | StickyLogic.RIGHT);
			l.tick(false, 0);
			l.tick(true, 0);
			ok(!l.isArmed(), "нечего запоминать");
			ok(l.tick(false, 0) == 0, "залипания нет");
		}

		System.out.println("\n=== 7. Одна отпущена до, другая зажата ===");
		{
			StickyLogic l = new StickyLogic();
			l.tick(false, StickyLogic.LEFT | StickyLogic.RIGHT);
			l.tick(false, StickyLogic.RIGHT);
			l.tick(true, StickyLogic.RIGHT);
			int m = l.tick(false, 0);
			ok((m & StickyLogic.RIGHT) != 0, "залипла только ПКМ");
			ok((m & StickyLogic.LEFT) == 0, "ЛКМ не залипла");
		}

		System.out.println("\n=== 8. Оверлей без нажатых кнопок ===");
		{
			StickyLogic l = new StickyLogic();
			l.tick(false, 0);
			l.tick(true, 0);
			ok(l.tick(false, 0) == 0, "ничего не залипает");
		}

		System.out.println("\n=== 9. Два F3+T подряд ===");
		{
			StickyLogic l = cycle(StickyLogic.LEFT | StickyLogic.RIGHT);
			l.tick(false, 0);
			ok(l.stickyMask() == (StickyLogic.LEFT | StickyLogic.RIGHT), "залипли после первой перезагрузки");
			l.tick(true, 0);
			int m = l.tick(false, 0);
			ok(m == (StickyLogic.LEFT | StickyLogic.RIGHT), "после второй обе сохранились");
		}

		System.out.println("\n=== 10. Сброс (выход из мира) ===");
		{
			StickyLogic l = cycle(StickyLogic.LEFT | StickyLogic.RIGHT);
			l.tick(false, 0);
			ok(l.stickyMask() != 0, "залипание есть");
			l.reset();
			ok(l.stickyMask() == 0 && !l.isArmed(), "reset снимает всё");
			ok(l.tick(false, 0) == 0, "после reset залипания нет");
		}

		System.out.println("\n=== 11. release одной кнопки ===");
		{
			StickyLogic l = cycle(StickyLogic.LEFT | StickyLogic.RIGHT);
			l.tick(false, 0);
			l.release(StickyLogic.LEFT);
			ok(!l.isSticky(StickyLogic.LEFT), "ЛКМ снята");
			ok(l.isSticky(StickyLogic.RIGHT), "ПКМ осталась");
		}

		System.out.println("\n=== 12. Правая и средняя по отдельности ===");
		{
			StickyLogic r = cycle(StickyLogic.RIGHT);
			ok((r.tick(false, 0) & StickyLogic.RIGHT) != 0, "ПКМ залипает одна");
			StickyLogic md = cycle(StickyLogic.MIDDLE);
			ok((md.tick(false, 0) & StickyLogic.MIDDLE) != 0, "средняя залипает одна");
		}

		System.out.println(fail == 0 ? "\nВСЁ ЗЕЛЁНОЕ" : "\nПРОВАЛОВ: " + fail);
		if (fail != 0) {
			System.exit(1);
		}
	}
}
