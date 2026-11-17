# Contributing to WeekFlow

Спасибо за интерес к **WeekFlow**! Будем рады баг-репортам, идеям по улучшению UX, тестам и pull request'ам.

---

## Быстрый старт для разработчиков

### Требования
- **JDK 17** (`jvmToolchain(17)`)
- **Android Studio** (актуальная версия с поддержкой AGP `9.0.1` / Kotlin `2.1.0`)
- **Android SDK** (`compileSdk = 36`, `minSdk = 26`)

### Проверка локальной сборки и тестов
```bash
git clone https://github.com/arch-revan-dev/WeekFlow.git
cd WeekFlow

# Запуск unit-тестов
./gradlew testDebugUnitTest

# Сборка APK
./gradlew assembleDebug
```

---

## Как помочь проекту

1. **Сообщить об ошибке** — откройте [Bug Report](https://github.com/arch-revan-dev/WeekFlow/issues/new?template=bug_report.yml), укажите версию APK (`v4.6.0` и т.д.), модель устройства / версию Android и шаги воспроизведения.
2. **Предложить функцию** — создайте [Feature Request](https://github.com/arch-revan-dev/WeekFlow/issues/new?template=feature_request.yml).
3. **Прислать Pull Request**:
   - Сделайте fork репозитория и создайте ветку от `main` (`feat/...` или `fix/...`).
   - Держите изменения сфокусированными — один PR решает одну задачу.
   - Перед отправкой убедитесь, что `./gradlew testDebugUnitTest` проходит без ошибок.
   - Придерживайтесь offline-first философии: **никаких скрытых трекеров, рекламы и лишних сетевых разрешений**.
4. **Помочь с закрытым тестом Google Play** — новым индивидуальным аккаунтам Google Play требуется минимум 12 тестировщиков на 14 дней подряд. Когда начнётся набор в закрытое тестирование, приглашение появится в Issues / Releases!

---

## Лицензия
Отправляя вклад (Contribution), вы соглашаетесь, что ваш код распространяется на условиях [Apache License 2.0](LICENSE) (см. также [NOTICE](NOTICE)).
