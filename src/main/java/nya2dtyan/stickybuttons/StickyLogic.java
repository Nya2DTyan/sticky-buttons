package nya2dtyan.stickybuttons;

/**
 * Логика «залипания» кнопок мыши, отделённая от Minecraft.
 *
 * <p>Зачем отдельный класс: проверять поведение без запуска игры. Стенды из
 * {@code verify/} вызывают {@link #tick} на собранном jar и убеждаются, что
 * зажатие восстанавливается ровно в тех случаях, где это ожидается.
 *
 * <p>Что воспроизводится. В старых версиях при удержании кнопки мыши и
 * перезагрузке ресурсов по F3+T событие отпускания терялось: экран загрузки
 * перехватывал ввод, {@code KeyMapping} оставался в состоянии «нажато», и
 * игрок продолжал копать или ставить блоки без участия рук — эффект
 * автокликера. Сейчас состояние кнопок пересинхронизируется с реальным, и
 * эффект пропал. Класс возвращает его осознанно, по явному желанию игрока.
 *
 * <p>Правила:
 * <ul>
 *   <li>кнопка была зажата в момент появления оверлея загрузки — запоминаем;</li>
 *   <li>оверлей закрылся — «зажимаем» соответствующую кнопку заново;</li>
 *   <li>залипание живёт, пока игрок не нажмёт эту кнопку снова: новое нажатие
 *       снимает его, и дальнейший клик работает как обычно.</li>
 * </ul>
 */
public final class StickyLogic {

	/** Бит «зажать левую кнопку» в маске, возвращаемой {@link #tick}. */
	public static final int LEFT = 1;
	/** Бит «зажать правую кнопку». */
	public static final int RIGHT = 1 << 1;
	/** Бит «зажать среднюю кнопку». */
	public static final int MIDDLE = 1 << 2;

	private boolean prevOverlay;
	private boolean prevLeft;
	private boolean prevRight;
	private boolean prevMiddle;

	private boolean armedLeft;
	private boolean armedRight;
	private boolean armedMiddle;

	private boolean stickyLeft;
	private boolean stickyRight;
	private boolean stickyMiddle;

	/**
	 * Один тик клиента.
	 *
	 * @param overlayOpen открыт ли оверлей загрузки ресурсов
	 * @param leftPressed нажата ли левая кнопка мыши по данным игры
	 * @param rightPressed нажата ли правая кнопка
	 * @param middlePressed нажата ли средняя кнопка
	 * @return маска кнопок, которые нужно удерживать нажатыми
	 */
	public int tick(boolean overlayOpen, boolean leftPressed, boolean rightPressed, boolean middlePressed) {
		// 1. Оверлей только что открылся — запоминаем, что было зажато.
		if (overlayOpen && !prevOverlay) {
			armedLeft = leftPressed;
			armedRight = rightPressed;
			armedMiddle = middlePressed;
		}

		// 2. Оверлей закрылся — переносим зажатие в «залипшее» состояние.
		if (!overlayOpen && prevOverlay) {
			stickyLeft |= armedLeft;
			stickyRight |= armedRight;
			stickyMiddle |= armedMiddle;
			armedLeft = false;
			armedRight = false;
			armedMiddle = false;
		}

		// 3. Игрок снова нажал кнопку — залипание снимается, клик идёт как обычно.
		if (!overlayOpen && !prevOverlay) {
			if (stickyLeft && leftPressed && !prevLeft) stickyLeft = false;
			if (stickyRight && rightPressed && !prevRight) stickyRight = false;
			if (stickyMiddle && middlePressed && !prevMiddle) stickyMiddle = false;
		}

		prevOverlay = overlayOpen;
		prevLeft = leftPressed;
		prevRight = rightPressed;
		prevMiddle = middlePressed;

		int mask = 0;
		if (stickyLeft) mask |= LEFT;
		if (stickyRight) mask |= RIGHT;
		if (stickyMiddle) mask |= MIDDLE;
		return mask;
	}

	public boolean isStickyLeft() {
		return stickyLeft;
	}

	public boolean isStickyRight() {
		return stickyRight;
	}

	public boolean isStickyMiddle() {
		return stickyMiddle;
	}

	public boolean isArmed() {
		return armedLeft || armedRight || armedMiddle;
	}

	/** Снять всё залипание (например, при выходе из мира). */
	public void reset() {
		prevOverlay = false;
		prevLeft = false;
		prevRight = false;
		prevMiddle = false;
		armedLeft = false;
		armedRight = false;
		armedMiddle = false;
		stickyLeft = false;
		stickyRight = false;
		stickyMiddle = false;
	}
}
