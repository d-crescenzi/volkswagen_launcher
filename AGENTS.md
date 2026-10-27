# AGENTS.md

Regole operative per lavorare su questo progetto. Rispettarle sempre.

## Contesto progetto

Launcher Android per auto (Kotlin, Compose, Gradle KTS). **Single-module**: esiste solo `:app`,
namespace `com.crescenzi.volkswagen_launcher`. Nessun ViewModel, nessuna navigazione a rotte,
nessun DI: è un launcher a schermata unica (un `HorizontalPager` di due pagine — media e griglia app)
montato da `MainActivity`. Si resta deliberatamente semplici: niente moduli, ViewModel o navigazione
finché non servono davvero.

`MainActivity` è una `ComponentActivity` single-activity che monta `AppTheme { LauncherScreen() }`,
nasconde la status bar e **blocca il font scale a `1f` in `attachBaseContext`** (il testo lo dimensiona
l'app, non il sistema). Orientamento landscape (launcher d'auto).

## Struttura dei package

```
com.crescenzi.volkswagen_launcher
├── MainActivity.kt                 entry point: window, font scale, monta LauncherScreen
├── core/                           helper di piattaforma, agnostici dalla UI
│   ├── SystemActions.kt            Context.safeOpen(intent), Context.hasNotificationAccess()
│   └── NetworkTime.kt              networkTimeOffset()
├── service/                        componenti Android fuori dalla UI
│   └── MediaControlService.kt      NotificationListenerService per le MediaSession
├── theme/                          tema Compose (unica fonte di stile)
│   ├── AppColor.kt                 palette (unico posto con gli esadecimali)
│   ├── AppDimens.kt                misure/costanti UI condivise
│   ├── AppType.kt                  tipografia (Google Sans Flex)
│   └── AppTheme.kt                 MaterialTheme del progetto
├── widgets/                        composable riusabili e agnostici
│   └── CircleButton.kt
└── presentation/                   schermate e loro componenti
    ├── LauncherScreen.kt           la schermata: top bar + pager + indicatore + fade laterali
    ├── LauncherComponents.kt       componenti della schermata (TopBar, PageIndicator)
    ├── apps/                       pagina griglia app
    │   ├── AppsPage.kt             AppsGrid
    │   ├── AppsComponents.kt       AppCell
    │   └── AppCatalog.kt           LaunchApp + elenco pacchetti + Context.loadMappedApps()
    ├── media/                      pagina player
    │   ├── MediaPage.kt            MediaPanel (accesso/idle/now-playing)
    │   └── MediaComponents.kt      MediaControls (titolo, progress, transport)
    └── volume/
        └── VolumeSlider.kt
```

## Dove va il codice

- **UI riusabile e agnostica** (widget che non dipendono da una schermata specifica) → `widgets/`.
- **Tema** (colori, misure, tipografia) → sempre `theme/`, mai valori di stile sparsi altrove.
- **Helper di piattaforma** (azioni su `Context`, rete, sistema) → `core/`. Niente UI qui dentro.
- **Componenti Android fuori dalla UI** (service, receiver) → `service/`.
- **Schermate e wiring Compose** → `presentation/`. Una schermata resta solo layout + wiring.
- **Componenti specifici di una schermata** (non riusabili altrove) non vanno in `widgets/`: si
  mettono in un file `<Nome>Components.kt` **affianco** alla schermata, nello stesso package
  (es. `presentation/apps/AppsComponents.kt` accanto a `AppsPage.kt`). Se un componente diventa
  riusabile da più schermate, lo si promuove in `widgets/`.
- **Modello/dati di una pagina** (es. l'elenco app e il loro caricamento) sta nel package della
  pagina (`presentation/apps/AppCatalog.kt`), non mischiato dentro il composable.

## Costanti e misure

- Le **misure UI condivise** (dimensioni di cerchi, icone, bottoni, celle della griglia) stanno in
  `theme/AppDimens.kt`: mai esadecimali di colore fuori da `AppColor`, mai `.dp` magici inline nel
  mezzo del codice quando il valore ha un senso riusabile.
- Un valore che serve **solo dentro un file** (un ratio, la larghezza di touch di uno slider, la
  durata di un'animazione, l'ampiezza di un fade) resta lì come `private const val` / `private val`
  in testa al file: non va promosso in `AppDimens`.

## Naming del Context

- Context di **UI** (`LocalContext.current`, Activity): si chiama `ctx`.
- Se mai servisse l'**application context** in un campo/costruttore (oggi non capita): `appCtx`.
- Gli helper di `core/` sono **extension su `Context`** (`Context.safeOpen(...)`,
  `Context.hasNotificationAccess()`): lì il context è il receiver e non ha nome.

## UI / Compose

- **Colori sempre da `AppColor`**, mai `Color(0x…)` nelle schermate: gli esadecimali vivono solo in
  `AppColor.kt`. Un colore nuovo si aggiunge lì. `AppColor` alimenta anche il `ColorScheme` in
  `AppTheme`; da `MaterialTheme.colorScheme` si prende quello che serve a Material (es. l'indicatore
  di pagina).
- **`modifier` è SEMPRE il primo parametro.** Ogni composable che accetta un `Modifier` lo dichiara
  in testa alla firma con default `Modifier`, prima di ogni altro parametro. La trailing lambda
  (`onClick`/`content`) resta invece **ultima**, così la sintassi a blocco continua a funzionare.
- **Tema in `theme/`**: `AppTheme` (`AppColor`/`AppDimens`/`AppType`). Le risorse a livello di
  finestra stanno in `res/values/themes.xml`; il resto dello stile è nel tema Compose.
- Niente XML di layout, niente ViewBinding, niente Activity extra: una sola `MainActivity`.

## Stringhe (risorse localizzate)

- **Mai stringhe inline** nel codice: ogni testo user-facing va in `res/values/strings.xml` e si
  referenzia (`stringResource(R.string.x)` / `ctx.getString(...)`).
- Oggi c'è **una sola locale** (il default in `res/values/`, in italiano). Se si aggiungono locale,
  ogni nuova stringa va messa in **tutti** i file `strings.xml`, mai solo nel default.
- **Mai stringhe morte.** Una stringa non più referenziata va rimossa subito da tutti i file
  `strings.xml`.

## Firma / build

- Chiavi di firma in `app/keystore.properties` (referenziato da `app/build.gradle.kts`), usato sia
  per debug che per release. Non reintrodurre credenziali hardcodate nel `build.gradle.kts`.

## Stile commenti

Commenti solo dove servono: corti, in inglese, mai eccessivi. Il default è **nessun commento**: il
codice si spiega da sé. Un commento si scrive solo per dire ciò che il codice **non può mostrare** —
un vincolo, un perché non ovvio. Preferire una riga al blocco. Non si commenta *cosa fa* la riga
sotto. **Non toccare mai i commenti già presenti** nei file.

## Git / operazioni delicate

- Commit e push **solo se richiesto esplicitamente**. Messaggi di commit semplici, in minuscolo,
  senza riferimenti a strumenti o assistenti.
- Nessuna operazione delicata se non esplicitamente richiesta.
