#!/usr/bin/env bash
# Стенды проверки — на СОБРАННОМ моде, а не на исходниках.
#
# Зачем так: поведение «залипания» зависит от того, как игра опрашивает
# KeyMapping между тиками, и проверить это без запуска клиента можно только
# вызовом методов из готового jar через URLClassLoader на classpath клиента.
#
# Использование:
#   verify/run.sh            # цель по умолчанию — 26.3
#   verify/run.sh 26.2
#
# Перед запуском нужен собранный мод: ./gradlew build -Pmc=<версия>
set -euo pipefail

cd "$(dirname "$0")/.."

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
export PATH="${JAVA_HOME}/bin:${PATH}"

MC="${1:-26.3}"
LIBS="build/${MC}/libs"
JAR="$(ls "${LIBS}"/sticky-buttons-*.jar 2>/dev/null | grep -v sources | head -1 || true)"
if [[ -z "${JAR}" ]]; then
	echo "Нет собранного мода для цели ${MC}. Сначала: ./gradlew build -Pmc=${MC}" >&2
	exit 1
fi

CP="$(./gradlew -q printClientClasspath -Pmc="${MC}" --no-daemon | tail -1)"
if [[ -z "${CP}" ]]; then
	echo "Не удалось получить classpath для цели ${MC}" >&2
	exit 1
fi

OUT="build/${MC}/verify-classes"
mkdir -p "${OUT}"
javac -nowarn -encoding UTF-8 -cp "${CP}" -d "${OUT}" verify/*.java

# Стенды грузят собранный jar первыми в classpath: если класс есть и в jar,
# и в исходниках, победит jar — проверяется именно то, что получит игрок.
java -cp "${JAR}:${OUT}:${CP}" VerifyMixinTarget
java -cp "${JAR}:${OUT}:${CP}" VerifySticky
