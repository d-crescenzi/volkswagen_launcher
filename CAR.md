# CAR.md

Dati tecnici confermati sull'hardware su cui gira questo launcher. Tutto quello qui sotto è
verificato o dato per assodato, non ipotesi.

## Veicolo

- **VW Polo 6R**, piattaforma **PQ25**.

## Head unit

- **Modello**: XY Auto **F9212B** (stessa board rimarchiata anche come Roinvou, Gearelec, ecc.).
- **Build id**: `f9212b_00020_v001`.
- **SoC**: Allwinner **T3**, board **K2001M** — quad Cortex-A7 @1.3GHz.
- **RAM**: 2 GB.
- **ABI**: 32-bit **armeabi-v7a** (Cortex-A7) + arm64. Se si includono librerie native, mettere
  entrambe le ABI.
- **MCU firmware**: XY AUTO **J7.3** — gestisce ACC/accensione, radio, volume, comandi al volante.
- **Decoder CANbus**: Raise **CAN Pro 9.0.2**, profilo **VW PQ "Dasauto All"** (corretto per la Polo 6R).
- **OS**: Android **10** dichiarato (AOSP stock, versione spesso gonfiata dal vendor). **Non** è
  Android Automotive e **non** è Android Auto: è Android normale con un launcher del vendor sopra.
- **Schermo**: landscape fisso, ~**1024×600**, densità bassa (mdpi/hdpi).
- **Porte**: due USB-A host.
- **Stack audio del launcher vendor**: Media / GIS / BackCar / Ring / Call / AuxIn.
- **Rete**: MAC stabile `00:08:22:8a:2e:fc`. L'IP osservato è `192.168.1.115` ma via DHCP può
  cambiare — per ritrovare l'unità cercare il MAC, non l'IP.

## Sviluppo app (Android stock)

Sviluppo Android classico: niente `android.car`, niente `CarPropertyManager`, niente flavor
Automotive, niente SDK speciali. Paletti pratici di questo hardware:

- **minSdk basso** (24, volendo 21): la "Android 10" dichiarata è inaffidabile, così l'app si
  installa comunque anche se sotto girasse un Android più vecchio. `targetSdk` alto a piacere.
- **Niente Google Play Services garantiti**: queste unità cinesi spesso non hanno i GMS (o sono
  rotti). Evitare dipendenze hard da FusedLocationProvider (usare `LocationManager` di AOSP),
  Google Maps SDK (usare osmdroid / MapLibre), Firebase / push, Google Sign-In.
- **Orientamento**: progettare landscape fisso 1024×600 a densità bassa; testare con quelle misure,
  non in verticale.
- **Ciclo sonno/risveglio** (la vera particolarità): l'unità non si spegne mai del tutto — al
  togliere il quadro va in deep sleep, e al riaccendere non fa un boot vero. Conseguenze:
  - `BOOT_COMPLETED` non arriva quasi mai: per l'autostart all'accensione affidarsi all'evento
    **ACC ON del vendor** o allo **screen-on**, non al boot.
  - C'è un **task-killer aggressivo** che chiude le app in background al sonno. Per sopravvivere,
    aggiungere l'app alla whitelist nel menu di fabbrica ("App Settings").

## Installazione (il flusso assodato)

Per far girare un'app propria **non serve ADB**:

1. Build dell'APK (debug o release, uguale).
2. Copia su una chiavetta USB **FAT32** (`usb mode = host`, vedi sotto).
3. Sulla Polo: file manager → chiavetta → tocca l'APK → Installa.

Per aggiornare in place: stesso `applicationId`, `versionCode`/`versionName` più alti — si
sovrascrive senza disinstallare.

## Menu di fabbrica

- **Codice**: **8888** (confermato). Da provare in alternativa: 3368, 1617, 0000.
- Password "style/logo": 111333 oppure 222888.
- **USB mode**: Factory → *USB settings* → `usb mode` commuta la porta tra **host** e device.
  Tenerlo su **host** per l'uso normale (legge chiavette/dongle). La modalità *device* non serve
  (il tentativo ADB via cavo non ha mai funzionato su questa unità).
- **Non toccare** *Protocol settings* / *Protocol Parameter settings* / *Export the config*:
  configurano il CAN della Polo.

## ADB via Wi-Fi (opzionale, già predisposto)

Serve solo per comodità (logcat, deploy in un click, reverse del CAN), non per usare l'app. Le
porte USB non espongono ADB in rete; il metodo confermato funzionante **senza root** (SELinux
permissivo su queste T3) è via terminale installato sull'unità:

1. Installare un emulatore di terminale (es. Terminal Emulator for Android, `jackpal.androidterm`).
2. Nel terminale: `setprop persist.adb.tcp.port 5555` (variante: `service.adb.tcp.port 5555`).
   Verifica con `getprop persist.adb.tcp.port` → deve stampare `5555`.
3. Riavviare l'unità (al riavvio il demone ADB parte in ascolto sulla rete).
4. Dal Mac: `adb connect <ip>:5555`, poi accettare il popup sullo schermo della Polo.

## Dati dell'auto (CAN) — riferimento, non ancora integrato

La catena è: **CAN Polo (PQ25) → scatola Raise → UART seriale 38400 8N1 → MCU (J7.3) → servizio
vendor Android → launcher**. L'app **non** parla con la seriale: quella la gestisce l'MCU. Per
leggere i dati bisogna agganciarsi al servizio Android del vendor che li ridistribuisce (broadcast?
AIDL? content provider? — da determinare decompilando launcher e app CAN/MCU, serve ADB per
`adb pull`).

Segnali che il profilo VW PQ espone (non di più): porte, freno a mano, retromarcia, comandi al
volante, velocità, giri motore, voltaggio, temperatura esterna, carburante, km.

Riferimenti per il reverse del protocollo seriale Raise VW(PQ) (frame: header `0x2E`, function id,
length, data, checksum): `github.com/icarome/VwRaiseCanbox`, `github.com/smartgauges/canbox`.
Documentazione della piattaforma T3 K2001: `github.com/ejasmudar/K2001AndroidHUInfo` e il thread
4pda #840515.
