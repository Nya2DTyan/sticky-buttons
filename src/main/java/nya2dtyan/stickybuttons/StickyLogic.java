package nya2dtyan.stickybuttons;

/**
 * Логика «залипания» кнопок мыши, отделённая от Minecraft.
 *
 * <p>Зачем отдельный класс: проверять поведение без запуска игры. Стенды из
 * {@code verify/} вызывают {@link #tick} на СОБРАННОМ jar и убеждаются, что
 * зажатие восстанавливается ровно в тех случаях, где это ожидается.
 *
 * <p>Что воспроизводится. В старых версиях при удержании кнопки мыши и
 * перезагрузке ресурсов по F3+T событие отпускания терялось: экран загрузки
 * перехватывал ввод, {@code KeyMapping} оставался в состоянии «нажато», и
 * функция продолжала выполняться сама — эффект автокликера. Сейчас состояние
 * кнопок пересинхронизируется с реальным, и эффект пропал.
 *
 * <p>Кнопки обрабатываются независимо: ЛКМ, ПКМ и средняя. Каждая может быть
 * зажата одновременно с другими — например, копать и ставить блоки сразу.
 */
public final class StickyLogic {

	/** Бит «зажать левую кнопку» в маске, возвращаемой {@link #tick}. */
	public static final int LEFT = 1;
	/** Бит «зажать правую кнопку». */
	public static final int RIGHT = 1 << 1;
	/** Бит «зажать среднюю кнопку». */
	public static final int MIDDLE = 1 << 2;

	private static final int ALL = LEFT | RIGHT | MIDDLE;

	private boolean prevOverlay;

	/** Что было зажато в момент открытия оверлея. */
	private int armed;

	/** Что удерживаем сейчас. */
	private int sticky;

	/** Состояние кнопок на прошлом тике — чтобы ловить момент нажатия. */
	private int prevButtons;

	/**
	 * Один тик клиента.
	 *
	 * @param overlayOpen открыт ли оверлей загрузки ресурсов
	 * @param buttons маска физически зажатых кнопок ({@link #LEFT} / {@link #RIGHT} / {@link #MIDDLE})
	 * @return маска кнопок, которые нужно удерживать нажатыми
	 */
	public int tick(boolean overlayOpen, int buttons) {
		buttons &= ALL;

		// 1. Оверлей только что открылся — запоминаем, что было зажато.
		if (overlayOpen && !prevOverlay) {
			armed = buttons;
		}

		// 2. Оверлей закрылся — переносим зажатие в «залипшее» состояние.
		//    Именно здесь в старых версиях терялось отпускание; теперь оно
		//    намеренно игнорируется, но только для тех кнопок, что были нажаты.
		if (!overlayOpen && prevOverlay) {
			sticky |= armed;
			armed = 0;
		}

		// 3. Игрок снова нажал кнопку — залипание снимается, клик идёт как обычно.
		if (!overlayOpen) {
			int freshlyPressed = buttons & ~prevButtons;
			sticky &= ~freshlyPressed;
		}

		prevOverlay = overlayOpen;
		prevButtons = buttons;
		return sticky;
	}

	public boolean isSticky(int bit) {
		return (sticky & bit) != 0;
	}

	public boolean isArmed() {
		return armed != 0;
	}

	/** Текущая маска удерживаемых кнопок. */
	public int stickyMask() {
		return sticky;
	}

	/** Снять всё залипание (выход из мира, открытие экрана). */
	public void reset() {
		prevOverlay = false;
		prevButtons = 0;
		armed = 0;
		sticky = 0;
	}

	/** Снять залипание с одной кнопки. */
	public void release(int bit) {
		sticky &= ~bit;
		armed &= ~bit;
	}
}
