# NationHearts

Paper-Plugin für Minecraft 1.21.11 mit einem separaten **3-Leben-System**. Die normalen roten Minecraft-Herzen bleiben unverändert.

## Gewünschtes System

- 💙 **3 blaue Leben** beim ersten Join
- ❤️ **10 normale Minecraft-Herzen** bleiben unverändert
- ⚔️ **Spieler-Kill** → Opfer verliert 1 blaues Leben
- 🩶 verlorene Leben werden grau angezeigt
- ☠️ bei **0 Leben** → „Du hast keine Leben mehr!“ + Kick
- 🚫 Spieler mit 0 Leben können nicht mehr joinen
- 🔧 `/nation hearts set <Spieler> <1|2|3>`
- 💾 Speicherung in `plugins/NationHearts/data.yml`
- 🔓 Setzt ein Admin einen Spieler wieder auf 1–3 Leben, kann er wieder joinen
- 🎨 verwendet exakt die Herz-Glyphen aus dem Nations-Resourcepack:
  - `U+E01B` → `assets/risiko/textures/font/active_heart.png`
  - `U+E01C` → `assets/risiko/textures/font/inactive_heart.png`
- 🤖 GitHub Actions baut automatisch die Plugin-JAR.

## Anzeige

Die blauen/grauen Herzen werden **nicht** als zusätzliche Minecraft-Gesundheit umgesetzt. Dadurch bleiben die 10 roten Vanilla-Herzen exakt normal.

Das Plugin sendet die beiden vorhandenen Glyphen als Actionbar-HUD. Im mitgelieferten Resourcepack ist die Herz-Glyph bereits mit dem passenden Font-Ascent (`-16`) eingerichtet, damit sie in der unteren HUD-Zone ausgerichtet wird.

**Wichtig:** Das Nations-Resourcepack muss auf dem Client aktiv sein. Ohne das Resourcepack sieht man nur die Unicode-Glyphen, nicht die blauen/grauen Herzen.

> Eine serverseitige Paper-API kann die Position des Vanilla-Herz-HUDs nicht direkt verändern. Für eine echte Änderung der Vanilla-GUI-Datei wäre ein Client-/HUD-Mod nötig. Mit dem vorhandenen Resourcepack ist die Actionbar-Glyph-Technik die passende server-only Lösung und erhält die roten Herzen unverändert.

## Commands

`/nation hearts set <Spieler> <1|2|3>`

Permission: `nationhearts.admin` (standardmäßig OP).

## Speicherung

Die Daten liegen in:

`plugins/NationHearts/data.yml`

Beispiel:

```yaml
players:
  00000000-0000-0000-0000-000000000000: 2
```

## Bauen

Java 21 + Gradle verwenden:

```bash
gradle build
```

Die JAR liegt danach in:

`build/libs/NationHearts-1.0.0.jar`

## GitHub Actions

`.github/workflows/build.yml` baut das Projekt bei Pushes und Pull Requests automatisch. Die fertige JAR wird als GitHub Actions Artifact hochgeladen.

## Resourcepack

Der Ordner `resourcepack/` enthält eine Kopie des mitgelieferten Resourcepacks als Referenz. Für deinen Server kannst du denselben Resourcepack-Ordner als ZIP an deine Spieler verteilen.
