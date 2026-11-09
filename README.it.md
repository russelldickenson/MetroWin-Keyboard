<h1 align="center">Phayz Keyboard</h1>

<p align="center">
  <b>La tastiera di Windows Phone su Android, con qualche aspetto e carattere in più.</b><br>
  Tasti Metro piatti, caratteri grandi e leggeri, scrittura a scorrimento, appunti, digitazione vocale.<br>
  Software libero. Niente pubblicità, analisi o tracker: ciò che scrivi resta sul tuo telefono.
</p>

<p align="center">
  <a href="LICENSE"><img alt="Licenza: GPL-3.0-or-later" src="https://img.shields.io/badge/licenza-GPL--3.0--or--later-1ba1e2"></a>
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-1ba1e2">
  <img alt="Layout: 17" src="https://img.shields.io/badge/layout-17-1ba1e2">
</p>

<p align="center"><a href="README.md">English</a> · <b>Italiano</b></p>

> **Phayz Keyboard è un fork.** Si basa su [MetroWin Keyboard](https://github.com/developerkenshi-coder/MetroWin-Keyboard) di developerkenshi-coder, del cui lavoro si serve e di cui mantiene il progetto grafico. Ha un proprio identificativo (`com.phayz.keyboard`), quindi si installa accanto all'originale senza sostituirlo. Non è affiliato né approvato dal progetto originale. Vedi [Cosa cambia](#cosa-cambia) per sapere esattamente cosa è stato tolto e aggiunto.

<p align="center"><img src="docs/screenshots/looks.png" width="92%" alt="I tre aspetti originali: Classic, Modern Glass e Modern Droid"></p>

<p align="center">
  <img src="docs/screenshots/home.png" width="22%" alt="La pagina iniziale con il riquadro della calibrazione">
  <img src="docs/screenshots/typing.png" width="22%" alt="La tastiera">
  <img src="docs/screenshots/calibration.png" width="22%" alt="Risultato della calibrazione">
  <img src="docs/screenshots/settings.png" width="22%" alt="Impostazioni">
</p>

<p align="center"><sub>Le schermate sono quelle del progetto originale: non mostrano ancora le novità di questo fork.</sub></p>

## Cosa cambia

Rispetto a MetroWin Keyboard originale (versione 26.1.2S):

**Aggiunto**
- **Carattere e spessore dei tasti** (*Impostazioni → Aspetto*): le lettere dei tasti possono usare il carattere di sistema, Open Sans, Roboto o Atkinson Hyperlegible, con spessore 200, 300, 400 o 500. Sono elencati solo gli spessori che si vedono diversi nel carattere scelto. I caratteri sono font variabili inclusi, con licenza SIL OFL.
- **Aspetto iPhone**: tastiera grigio chiaro (o scura), tasti bianchi arrotondati con una sottile ombra, tasti speciali più scuri, etichette "spazio" e "invio", separatori tra i suggerimenti e un fumetto bianco sopra il tasto premuto.
- **Aspetto Modern Slate**: tasti grigio scuro con simboli grigio chiaro su uno sfondo grigio chiaro (scuro nella modalità scura), ombre morbide e un tasto azione azzurro.
- **Versioni spaziate di Modern Droid e Modern Slate**: gli stessi aspetti con i tasti distanziati come nell'aspetto iPhone (circa 6 dp in orizzontale, 10 dp tra le file, alla stessa altezza della tastiera). Gli originali restano invariati.
- **Una nuova identità**: identificativo `com.phayz.keyboard`, per convivere con l'originale.

**Modificato**
- **L'app delle impostazioni è Material 3**, non più in stile Windows Phone: barra superiore, home a scorrimento con schede ed elenchi, tipografia, pulsanti e interruttori Material 3 e i colori dello sfondo (Material You) da Android 12. Gli aspetti della tastiera, compresi quelli Metro (Classic e Glass), restano com'erano.
- **Colore principale**: i 20 colori di Windows Phone non ci sono più; restano Material You (Modern Droid segue lo sfondo) e un solo colore a scelta. Classic mantiene il ciano di default.
- Il tasto mondo è **nascosto di default** (*Impostazioni → Dimensioni e disposizione → Mostra il tasto mondo* lo riporta).
- La firma delle release si legge da `keystore.properties`; [`keystore.properties.example`](keystore.properties.example) mostra i valori da compilare.

**Tolto**
- **Segui l'app**: l'opzione che tingeva la tastiera con il colore dell'app sotto, il suo servizio di accessibilità e le impostazioni relative.
- **La tua immagine**: la foto dietro i tasti, la sua attenuazione e il colore principale preso da essa, con le relative impostazioni in *Aspetto*.
- **GIF**: il pannello GIF, la ricerca, le fonti (GIPHY, Tenor, Klipy, Imgur, una cartella, un servizio tuo) e le relative impostazioni.
- **Traduzione**: il pannello di traduzione, i servizi (Google, Google Cloud, DeepL, LibreTranslate, MyMemory) e le relative impostazioni. Insieme alla rimozione degli aggiornamenti, l'app non chiede più alcun permesso `INTERNET`.
- **Integrazione con MetroWin Launcher**: il ricevitore di sincronizzazione del tema, l'importazione del file tema del launcher, la schermata *MetroWin Launcher* nelle impostazioni, la sua pagina del tutorial e il trattamento speciale della casella di ricerca del launcher.
- **Aggiornamenti**: il controllo degli aggiornamenti, la sua schermata nelle impostazioni, il lavoro periodico ogni 12 ore, la pagina del tutorial e i permessi che richiedeva (`INTERNET`, `REQUEST_INSTALL_PACKAGES`, `RECEIVE_BOOT_COMPLETED`). Le nuove versioni si installano da sé.
- **Altri progetti**: la schermata *Informazioni → Altri progetti*, che elencava solo le app dell'autore originale.

Tutto il resto è come nell'originale: il motore di scrittura, la correzione intelligente, la calibrazione, gli appunti, le emoji, la digitazione vocale, il backup, i 17 layout e i tre aspetti originali.

## Installazione

Per ora questo fork non ha release e l'app non ha un aggiornamento integrato. Compilalo dai sorgenti (sotto) e apri l'APK sul telefono (consenti al browser o al gestore file di installare app sconosciute).

1. Apri **Phayz Keyboard**. Una guida all'attivazione accende la tastiera e la seleziona, accorgendosi da sola di quando ogni passaggio è fatto.
2. Il tutorial è nell'app e dentro la tastiera (i tre puntini, poi **?**).

Android 8.0 (API 26) o successivo.

## Funzioni

**Scrittura**
- Scrittura a scorrimento, tocco prolungato per accenti, cifre e simboli (durata in ms, 200 di default), doppio spazio per il punto, maiuscole automatiche, scorrimento sulla barra spaziatrice per muovere il cursore.
- Anteprima del tasto che premi.
- Correzione che **Backspace annulla** e da cui impara (intelligente: pesa dove è caduto il dito, le parole intorno e ciò che hai annullato; propone lo spazio mancante), accenti, completamento, previsione della parola successiva e della frase, parole offensive bloccate (più la tua lista).
- Impara da te, **solo sul telefono**; mai nei campi password; esportabile e cancellabile.

**Strumenti**, a un tocco dalla barra: i tre puntini aprono la cassetta degli strumenti e il **tasto rapido** porta dritto agli appunti (o a qualsiasi strumento scelto), con un puntino quando hai copiato qualcosa.
- **Appunti** con limite a scelta, elementi fissati e **immagini**.
- **Emoji** ed **emoticon classiche** (`:-)`, kaomoji), con i toni della pelle.
- **Digitazione vocale**, anche trascrizione continua, volendo solo sul telefono.

**Calibrazione**: scrivi qualche frase e la tastiera impara dove cadono *le tue* dita (lo scarto, la curva del pollice, quanto si disperdono i tocchi), corregge i tocchi senza spostare i tasti, li ingrandisce o riduce gli spazi se sbagli ancora, e mette ogni correzione in funzione tra una frase e l'altra, così la vedi migliorare. Anche le lettere accentate fanno parte del test. Niente resta finché non applichi.

**Correzione intelligente**: un piccolo modello che gira sul telefono corregge i refusi da dove è caduto il dito, da quanto è probabile ogni scivolone sulla tastiera, da quanto è comune la parola, dalla parola precedente e dalle correzioni annullate. Due correzioni ugualmente probabili vengono mostrate, non applicate. Propone anche lo spazio mancante.

**Backup**: un solo file con tutto ciò che l'app conserva (impostazioni, parole apprese, dizionari, appunti); ripristinalo su un altro telefono o dopo aver reinstallato con un'altra firma.

**Lingue**: 17 layout inclusi (English, Italiano, Español, Français, Deutsch, Português, Nederlands, Polski, Türkçe, Русский, Українська, Ελληνικά, Čeština, Svenska, Română, العربية, עברית). Qualsiasi altro layout è un piccolo file di testo ([docs/LAYOUTS.md](docs/LAYOUTS.md)); si può importare qualsiasi dizionario (liste di parole, di frequenza, Hunspell `.dic`). Sono incluse solo piccole liste di partenza (en, it, es, fr, de, pt): importa un dizionario completo per le previsioni migliori.

**Aspetto**: *Classic* (Windows Phone piatto), *Modern Glass*, *Modern Droid* (colori Material You da Android 12), *iPhone*, *Modern Slate* e le versioni spaziate di Modern Droid e Modern Slate; scuro / chiaro / sistema, un colore tuo (Modern Droid segue lo sfondo), e nero puro. Lettere dei tasti nel carattere e nello spessore che scegli.

**Dimensioni**: anteprima dal vivo, altezza dei tasti (verticale e orizzontale), larghezza della tastiera con posizione sinistra / centro / destra, spazio tra i tasti, riga dei numeri e una calibrazione automatica che impara da qualche frase scritta dove cadono le tue dita.

**Feedback**: click in stile Windows Phone (sintetizzato, niente di Microsoft), suono di Android o un tuo file; vibrazione con intensità regolabile.

## Privacy e permessi

Nessuna analisi, pubblicità o tracker. L'elenco completo è in [PRIVACY.md](PRIVACY.md).

| Permesso | Perché |
| --- | --- |
| `RECORD_AUDIO` | Solo per la digitazione vocale; richiesto quando tocchi il microfono |
| `VIBRATE` | Vibrazione dei tasti |

Backup e trasferimento tra dispositivi sono disattivati per i dati dell'app.

## Compilare

JDK 17 o successivo e Android SDK (piattaforma 35).

```sh
git clone <l'indirizzo di questo repository>
cd <la cartella clonata>
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # test di motore, tastiera e tutorial sulla JVM
./build.sh                       # un APK firmato nella cartella principale (credenziali segnaposto; si cambiano con KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)
./gradlew assembleRelease        # serve keystore.properties (copia keystore.properties.example); -PsignWithDebugKey=true per una build di prova
```

## Contribuire

Segnalazioni e idee sono benvenute nel tracker delle issue. Vedi [CONTRIBUTING.md](CONTRIBUTING.md), con l'elenco da seguire per aggiungere una funzione (e la sua pagina del tutorial). Problemi di sicurezza: [SECURITY.md](SECURITY.md). Le modifiche alle parti condivise con l'originale possono valere la pena anche per il [progetto originale](https://github.com/developerkenshi-coder/MetroWin-Keyboard).

## Licenza e crediti

GNU General Public License v3.0 o successiva ([LICENSE](LICENSE)). MetroWin Keyboard originale è di developerkenshi-coder; questo fork ne mantiene la licenza e la storia. I caratteri sono Open Sans, Roboto e Atkinson Hyperlegible Next (SIL Open Font License 1.1); vedi [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Progetto indipendente, non affiliato né approvato da Microsoft, Apple o Google; Windows Phone è un marchio di Microsoft Corporation e l'aspetto iPhone imita solo l'aspetto di una tastiera, senza codice né caratteri di Apple.
