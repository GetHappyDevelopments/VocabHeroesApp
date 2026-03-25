# VocabHeroes Kiosk Browser (Android)

Native Android-App zur Anzeige genau einer festen URL in einer abgesicherten Vollbild-WebView.

## Zielbild
- Feste URL wird beim Start automatisch geladen.
- Keine Adresszeile, keine Tabs, keine freie Browsernavigation.
- Fullscreen/immersive Darstellung.
- Klare Fehleransicht mit Retry.
- Family-Link-kompatibel (normale App, kein Device-Owner-/LockTask-Zwang).

## Konfiguration
Die Start-URL und erlaubten Hosts sind in `app/build.gradle.kts` hinterlegt:
- `START_URL`
- `ALLOWED_HOSTS` (CSV)

Beispiel:
- `START_URL = "https://kiosk.example.com"`
- `ALLOWED_HOSTS = "kiosk.example.com,cdn.example.com"`

## Sicherheit
- Nur `https` ist erlaubt.
- Navigation ist auf erlaubte Hosts begrenzt.
- Externe URLs werden blockiert.
- Sichere WebView-Defaults:
  - kein File-/Content-Zugriff
  - Mixed Content verboten
  - Safe Browsing aktiviert (sofern verfügbar)

## Verhalten
- Beim Start: sofortiges Laden der Start-URL.
- Bei Ladefehlern: Fehlerseite mit verständlicher Meldung und `Erneut laden`.
- Zurück-Taste: nur in WebView-Historie; sonst normales App-Verhalten.
- Orientierung: Portrait (gemäß Manifest).

## Build / Start
1. Projekt in Android Studio öffnen.
2. Gradle-Sync ausführen.
3. `START_URL` und `ALLOWED_HOSTS` anpassen.
4. App auf Gerät/Emulator starten.

Hinweis: In diesem Workspace ist kein lokales JDK/Gradle verfügbar, daher konnte kein Build ausgeführt werden.

## Tests
- Unit-Test für URL-Policy:
  - erlaubte HTTPS-Hosts
  - Blockierung von HTTP/fremden Hosts/malformed URLs

Datei: `app/src/test/java/com/vocabheroes/kioskbrowser/UrlPolicyTest.kt`

## Abdeckung der Spezifikation (Kernpunkte)
- A-001 / FA-001: automatische Start-URL -> umgesetzt.
- A-002 / FA-002: keine freie Browsernutzung -> umgesetzt über Host-/Scheme-Policy.
- A-003: Fullscreen ohne Browserleisten -> umgesetzt (immersive mode).
- A-004: normale Installation/Deinstallation -> umgesetzt (normale App, kein Device Owner).
- A-005 / Fehlerbehandlung: klare Fehleransicht mit Retry -> umgesetzt.
- A-006: minimale Berechtigungen + sichere WebView-Konfiguration -> umgesetzt.
- A-007: Family-Link-Kontrolle bleibt wirksam -> umgesetzt durch regulären App-Betrieb.
- A-008 (Soll): Erweiterbarkeit für Admin/Whitelist -> vorbereitet über zentrale Policy/Config.
