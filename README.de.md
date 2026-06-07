<p align="center">
  <img src="docs/assets/32bit.gif" alt="32bit" height="72" align="middle">
  &nbsp;&nbsp;&nbsp;
  <img src="docs/assets/author.gif" alt="Abdulkadir Kılıç" height="80" align="middle">
  &nbsp;&nbsp;&nbsp;
  <img src="docs/assets/brand.png" alt="32 Bit Finance" height="110" align="middle">
</p>

<h1 align="center">32 BİT FİNANCE PLATFORM</h1>

<p align="center"><sub>Dokumentation auf Deutsch — technische Leitfäden unter <a href="docs/deutsch/">docs/deutsch/</a></sub></p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-ED8B00?logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Hibernate-59666C?logo=hibernate&logoColor=white" alt="Hibernate">
  <img src="https://img.shields.io/badge/JPA-6DB33F?logo=spring&logoColor=white" alt="JPA">
  <img src="https://img.shields.io/badge/React-61DAFB?logo=react&logoColor=black" alt="React">
  <img src="https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white" alt="TypeScript">
  <img src="https://img.shields.io/badge/Vite-646CFF?logo=vite&logoColor=white" alt="Vite">
  <img src="https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white" alt="Docker">
</p>
<p align="center">
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/Kafka-231F20?logo=apachekafka&logoColor=white" alt="Kafka">
  <img src="https://img.shields.io/badge/Keycloak-4D4DFF?logo=keycloak&logoColor=white" alt="Keycloak">
  <img src="https://img.shields.io/badge/OpenTelemetry-000000?logo=opentelemetry&logoColor=white" alt="OpenTelemetry">
  <img src="https://img.shields.io/badge/Prometheus-E6522C?logo=prometheus&logoColor=white" alt="Prometheus">
  <img src="https://img.shields.io/badge/Grafana-F46800?logo=grafana&logoColor=white" alt="Grafana">
  <img src="https://img.shields.io/badge/OpenSearch-005EB8?logo=opensearch&logoColor=white" alt="OpenSearch">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="Lizenz: MIT">
</p>

<p align="center">
  <a href="README.tr.md">Türkçe</a> · <a href="README.md">English</a> · <strong>Deutsch</strong>
</p>

<p align="center">
  <strong>Auf die Türkei ausgerichtetes Finanzportal in drei Sprachen</strong> — Portfolio aufbauen, Märkte und Nachrichten auf einem Bildschirm verfolgen.
  Oberfläche und Inhalte unterstützen <strong>Türkisch</strong>, <strong>Englisch</strong> und <strong>Deutsch</strong>; beim Sprachwechsel passen sich Menüs, Karten und Meldungen an.
</p>

<p align="center">
  Live-Preise für Aktien, Fonds, Krypto, Devisen, Anleihen und Eurobonds; Transaktionshistorie, Ziele und externe Portfolioansicht.
  Wichtige Nachrichten <strong>markieren</strong>, Zeichnungen und Notizen in der technischen Analyse <strong>speichern</strong> und später wieder öffnen.
  Zins- und Festgeldprodukte, Bankkurse, RSI und weitere Indikatoren, Alarme, Watchlist und Finanzbildung — eine Plattform zur Verwaltung Ihrer Portfolios.
</p>

## Inhaltsverzeichnis

- [Bildschirme](#bildschirme)
- [Finanzfunktionen](#finanzfunktionen)
- [Projektstruktur](#projektstruktur)
- [Plattform-Überblick](#plattform-überblick)
- [Datenkonfiguration](#datenkonfiguration)
- [Instrumente hinzufügen](#instrumente-hinzufügen)
- [Schnellstart (Docker)](#schnellstart-docker--empfohlen)
- [Ausführliche Einrichtung](#ausführliche-einrichtung)
- [Lokale Entwicklung (Kurz)](#lokale-entwicklung-kurz)
- [Dokumentation](#dokumentation)
- [Lizenz](#lizenz)

## Bildschirme

<table align="center" border="0" cellpadding="8" cellspacing="0" width="100%">
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Startseite</strong><br>
      <img src="docs/assets/screens/anasayfa.gif" alt="Startseite" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Märkte</strong><br>
      <img src="docs/assets/screens/piyasalar.gif" alt="Märkte" width="100%">
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Mein Portfolio</strong><br>
      <img src="docs/assets/screens/portfoyum.gif" alt="Mein Portfolio" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Zinsen / Festgeld</strong><br>
      <img src="docs/assets/screens/faizvadeli.gif" alt="Zinsen / Festgeld" width="100%">
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Analyse</strong><br>
      <img src="docs/assets/screens/analiz.gif" alt="Analyse" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Nachrichten</strong><br>
      <img src="docs/assets/screens/haberler.gif" alt="Nachrichten" width="100%">
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Bankkurse</strong><br>
      <img src="docs/assets/screens/bankakurlari.gif" alt="Bankkurse" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Finanzbildung</strong><br>
      <img src="docs/assets/screens/finansalokuryazarlik.gif" alt="Finanzbildung" width="100%">
    </td>
  </tr>
  <tr>
    <td align="center" valign="top" width="50%">
      <strong>Infokarten</strong><br>
      <img src="docs/assets/screens/bilgikartlari.gif" alt="Infokarten" width="100%">
    </td>
    <td align="center" valign="top" width="50%">
      <strong>Profil</strong><br>
      <img src="docs/assets/screens/profil.gif" alt="Profil" width="100%">
    </td>
  </tr>
</table>

## Finanzfunktionen

<p align="center"><sub>Konkrete Portal-Funktionen — Seiten-Tour oben; hier einzelne Features</sub></p>

<style>
  .readme-stack img { max-width: 100%; height: auto; }
  @media (max-width: 720px) {
    .readme-stack table table > tbody > tr:not(:has(td[colspan])) { display: block; }
    .readme-stack table table > tbody > tr:not(:has(td[colspan])) > td {
      display: block;
      width: 100% !important;
      max-width: 100%;
      box-sizing: border-box;
    }
    .readme-stack table table > tbody > tr:not(:has(td[colspan])) > td[width] {
      width: 100% !important;
      text-align: center;
      padding-bottom: 10px;
    }
    .readme-stack table table table tr { display: block; }
    .readme-stack table table table td {
      display: block;
      width: 100% !important;
      padding: 2px 0;
    }
  }
</style>

<div class="readme-stack">

<table align="center" border="0" cellpadding="0" cellspacing="0" width="100%">
  <tr>
    <td>
      <table border="0" cellpadding="14" cellspacing="0" width="100%">
        <tr>
          <td width="190" align="center" valign="top">
            <img src="docs/assets/features/f1-markets.webp" alt="Portfolio-Simulation — Asset-Auswahl und Renditeanalyse" width="172" loading="lazy">
            <br><sub>Portfolio-Simulation</sub>
          </td>
          <td valign="top">
            <h4>① Portfolio-Simulation</h4>
            <p>Wählen Sie ein beliebiges Asset aus der Markttabelle und fügen Sie es dem Simulator hinzu. Mit Ihrer Zusammenstellung sehen Sie <strong>Portfolio-Rendite vom Startdatum bis heute</strong>, <strong>Einzel-Asset-Renditen</strong> und wie sich die <strong>Gesamtrendite</strong> ändert, wenn Sie die Gewichtung anpassen.</p>
            <p><strong>Währung:</strong> TRY- vs. USD-Basis vergleichen — wie Wechselkurse dieselbe Allokation beeinflussen. Startdatum, Gewichtung (gleich verteilen / auf 100 % normalisieren) sowie Parität + FX-Komponenten auf einem Bildschirm.</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <img src="docs/assets/features/f2-treasury-bond-simulator.webp" alt="TR-Staatsanleihen-Renditechart und Anleihen-Simulator" width="172" loading="lazy">
            <br><sub>Zinsen / Festgeld</sub>
          </td>
          <td valign="top">
            <h4>② TR-Staatsanleihen — Renditechart &amp; Simulator</h4>
            <p>Verfolgen Sie die <strong>1J / 2J / 3J</strong> türkische Renditekurve und die <strong>historische Renditeserie</strong> der gewählten Laufzeit — mit TCMB-EVDS-Sekundärmarktdaten. Laufzeitwechsel aktualisiert Kurve und Zeitreihe.</p>
            <p><strong>Anleihen-Simulator:</strong> heutige Anlagesumme und Jahresrendite eingeben — geschätzter Clean Price, Fälligkeitserlös und Gesamtrendite sofort berechnet. Näherung an Rendite bis Fälligkeit unter Diskontierungsannahme; per Klick zur Leitzins-Grafik.</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <img src="docs/assets/features/f3-eurobond-simulator.webp" alt="TR-USD-Eurobond Kupon- und Renditecharts mit Cashflow-Simulator" width="172" loading="lazy">
            <br><sub>Zinsen / Festgeld · Eurobond</sub>
          </td>
          <td valign="top">
            <h4>③ TR-USD-Eurobond — Kupon- &amp; Rendite-Simulator</h4>
            <p>Eurobond-Tabelle je ISIN mit <strong>Clean Price</strong>, <strong>Kupon</strong> und <strong>Rendite bis Fälligkeit (YTM)</strong>; getrennte <strong>Preis-</strong> und <strong>Rendite-Historie</strong> für die gewählte Anleihe (1J / 5J / gesamt).</p>
            <p><strong>Cashflow:</strong> Nominal (USD) eingeben — jährlicher und halbjährlicher Kupon, geschätzte Anschaffungskosten und Nennwert bei Fälligkeit werden berechnet.</p>
            <p><strong>Historisches Kaufszenario:</strong> Kaufdatum und Clean Price im Chart markieren — Schlusskurs und Datum werden übernommen. Geschätzter <strong>Kupon-Cash</strong>, Verkaufserlös, Netto-P&amp;L (USD) und Rendite % auf den Kauf; optional Exit auf den letzten Chart-Tag fixieren.</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <img src="docs/assets/features/f4-inflation-simulator.webp" alt="VPI-Inflationschart und Kaufkraft-Simulator" width="172" loading="lazy">
            <br><sub>Zinsen / Festgeld · VPI</sub>
          </td>
          <td valign="top">
            <h4>④ VPI-Inflation — Kaufkraft-Simulator</h4>
            <p>Türkischer Gesamt-VPI (TCMB, 2003=100) als <strong>Jahres-%</strong>, <strong>Monats-%</strong> oder <strong>Indexstand</strong>; Startdatum im Chart markierbar.</p>
            <p><strong>Realer Wertverlust:</strong> nominale TRY-Summe und Zeitraum — zusammengesetzte Inflation (I<sub>Ende</sub>/I<sub>Start</sub>), <strong>Kaufkraftverlust</strong> (TRY und %), heutiger realer Gegenwert und <strong>annualisierte Inflation</strong>.</p>
            <p><strong>Erhaltungsschwelle:</strong> <strong>nominale Summe</strong> am Periodenende, um die Ausgangskaufkraft zu halten — Mindestrendite in realen Begriffen (Hinweis; ohne Steuern und persönlichen Warenkorb).</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <img src="docs/assets/features/f5-chart-drawing.webp" alt="Analyse-Seite — technische Chart-Zeichnungen" width="172" loading="lazy">
            <br><sub>Analyse · Chart-Zeichnungen</sub>
          </td>
          <td valign="top">
            <h4>⑤ Technische Analyse — Chart-Zeichnungen &amp; gespeicherte Setups</h4>
            <p>Am Kerzen-/Linienchart des gewählten Instruments mit <strong>eigener Farbe pro Werkzeug</strong>: <strong>Trendlinie</strong>, <strong>Strahl (Ray)</strong>, <strong>horizontale Unterstützung/Widerstand</strong>, <strong>vertikale Zeitmarke</strong>, <strong>Konsolidierungsbox (Rechteck)</strong>, <strong>Fibonacci-Retracement</strong> und <strong>Ankerpunkt</strong>. <strong>Preisspannen-Messung</strong> für Balkenanzahl und Rendite zwischen zwei Punkten.</p>
            <p><strong>Overlays:</strong> MA20 / MA50, RSI, Volumen und <strong>Vergleichs-Overlay</strong> mit bis zu drei Symbolen auf einer Zeitachse.</p>
            <p><strong>Speichern &amp; lernen:</strong> Zeichnungssatz benennen und speichern; <strong>frühere Zeichnungen</strong> je Asset öffnen und Support/Widerstand sowie Szenarien nachvollziehen. Technische Kompetenz aufbauen und Trades an der eigenen Annotationshistorie ausrichten (Anmeldung erforderlich).</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <img src="docs/assets/features/f6-my-analysis.webp" alt="Portfolio — Meine Analyse: gespeicherte Zeichnungen mit Live-Preis-Overlay" width="172" loading="lazy">
            <br><sub>Portfolio · Meine Analyse</sub>
          </td>
          <td valign="top">
            <h4>⑥ Meine Analyse — Gespeicherte Zeichnungen &amp; Live-Nachverfolgung</h4>
            <p>Unter <strong>Meine Analyse</strong> im Portfolio-Menü findest du alle auf der Analyse-Seite gespeicherten Zeichnungssätze. Jeder Eintrag fasst Symbol, Datum, <strong>Anzahl der Zeichnungen</strong>, Werkzeug-Badges und <strong>Preisspanne</strong> zusammen.</p>
            <p>Karte aufklappen: Zeichnungen werden mit <strong>aktuellen Kerzendaten</strong> neu geladen — Fibonacci-Niveaus, Support/Widerstand-Boxen und Trendlinien im Verhältnis zum <strong>heutigen Kurs</strong> prüfen und das Szenario live verfolgen. Bearbeitung über <strong>In Analyse-Seite öffnen</strong> fortsetzen (Anmeldung erforderlich).</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <img src="docs/assets/features/f7-chart-news.webp" alt="Analyse-Seite — Nachrichtenmarker im Chart und Kurswirkung am Folgetag" width="172" loading="lazy">
            <br><sub>Analyse · Chart-Nachrichten</sub>
          </td>
          <td valign="top">
            <h4>⑦ Analyse — Nachrichten im Chart &amp; Wirkungsanalyse</h4>
            <p>Mit der <strong>Nachrichten</strong>-Ebene auf der Analyse-Seite Schlagzeilen zum gewählten Asset auf der Preis-Zeitachse markieren. Marker anklicken für Titel, Kurzfassung, Quelle und Zuordnung (Asset / Kategorie / <strong>Favorit</strong>).</p>
            <p><strong>Veränderung am Folgetag:</strong> die <strong>% Kursbewegung</strong> vom Schluss des Nachrichtentags bis zur nächsten Session lesen — kurzfristige Marktreaktion messen. <strong>Stern-Filter</strong> zeigt nur favorisierte Meldungen im Chart für isolierte Wirkungsanalyse.</p>
            <p><strong>Favoriten:</strong> mit Stern markierte Artikel erscheinen auch auf der <strong>Nachrichten</strong>-Seite (Favoritenfilter) und unter <strong>Meine Nachrichten</strong> im Portfolio (Anmeldung erforderlich).</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <img src="docs/assets/features/f8-info-cards-literacy.webp" alt="Finanzielle Grundbildung — Infokarten und Begriffslexikon" width="172" loading="lazy">
            <br><sub>Infokarten · Grundbildung</sub>
          </td>
          <td valign="top">
            <h4>⑧ Infokarten &amp; finanzielle Grundbildung</h4>
            <p>Über die <strong>? (Hinweis-Modus)</strong>-Schaltfläche in der Kopfzeile Infokarten öffnen, die Admins an Portalelemente gebunden haben: <strong>Button oder Begriff anklicken</strong> für Kurzdefinition, Deutungshinweis und Link zum vollständigen Eintrag.</p>
            <p><strong>Finanzlexikon:</strong> Begriffe, Chart-Typen, Makroindikatoren und Analysewerkzeuge im filterbaren Katalog — Suche nach Schwierigkeit, Inhaltstyp und Portalseite.</p>
            <p><strong>Admin &amp; KI:</strong> Admins erstellen Inhalte per Elementauswahl auf der Seite oder Lexikon-Karten; <strong>KI-Feldvervollständigung</strong> und <strong>TR / EN / DE Übersetzung</strong> beschleunigen die Pflege — Komfort für Admins, verständliche Bildung für Nutzer.</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <img src="docs/assets/features/f9-admin-add-asset.webp" alt="Admin — neues Asset hinzufügen mit Marktsegment-Auswahl" width="172" loading="lazy">
            <br><sub>Admin · Asset hinzufügen</sub>
          </td>
          <td valign="top">
            <h4>⑨ Admin — Dynamisches Asset-Onboarding &amp; Daten-Trigger</h4>
            <p>Über <strong>Neues Asset hinzufügen</strong> im Admin-Panel ein neu gelistetes oder anderes Instrument per <strong>Krypto / BIST / NASDAQ</strong>-Segment dynamisch anlegen; <strong>Typ und Börse</strong> werden aus dem Preset automatisch gesetzt.</p>
            <p>Nach dem Speichern Weiterleitung zum <strong>Datenabruf-Register</strong>: Zeilen aktivieren/deaktivieren, <strong>historische Daten abrufen</strong> oder <strong>Live-Daten abrufen</strong> auslösen — Abdeckung (30 / 365 Tage) und letzter Fehlerstatus in einer Tabelle.</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <img src="docs/assets/features/f10-admin-user-management.webp" alt="Admin — Benutzerverzeichnis: Nachricht, Sperren und Löschen" width="172" loading="lazy">
            <br><sub>Admin · Benutzerverwaltung</sub>
          </td>
          <td valign="top">
            <h4>⑩ Admin — Benutzerverwaltung &amp; Kontomaßnahmen</h4>
            <p>Im Verzeichnis <strong>Gesamtbenutzer</strong> pro Nutzer eine <strong>Nachricht</strong> senden (Portal-Posteingang + E-Mail), Konto <strong>sperren</strong> oder <strong>endgültig löschen</strong>. Sperre mit optionalem Grund; beim Löschen E-Mail für Neuregistrierung sperrbar.</p>
            <p><strong>Sofortige Wirkung:</strong> gesperrte oder gelöschte Konten werden beim Surfen per API erkannt; Sitzung wird beendet und Weiterleitung zur Anmeldung mit <strong>Hinweisbanner</strong>. Sperre aufhebbar; Löschung unwiderruflich.</p>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>

</div>

## Projektstruktur

Die **32 Bit Finance Platform** ist ein Finanzportal mit Microservice-Architektur. Der Nutzer sendet Anfragen aus der **React**-Oberfläche an eine einzige Adresse (**api-gateway**); das Gateway authentifiziert über **Keycloak** und leitet an den jeweiligen Service weiter. Portal-Geschäftsregeln wie Portfolio, Alarme, Registrierung und MFA sind in **finance-api** gebündelt; Marktpreise, Nachrichten, technische Analyse und Benachrichtigungen sind in eigenen Services spezialisiert. Services rufen sich bei Bedarf per **HTTP** auf; Preisaktualisierungen, Alarme und Logs laufen asynchron über **Kafka**. Persistente Daten liegen in **PostgreSQL**; jedes Service-Schema wird separat mit **Flyway** verwaltet.

<h3 align="center">
  <a href="docs/deutsch/architecture.md">PROJEKTARCHITEKTUR</a>
</h3>

<p align="center">
  <a href="docs/deutsch/architecture.md">
    <img src="docs/diagrams/genelmimari.png" alt="Architekturdiagramm" width="900">
  </a>
</p>

<table align="center" border="0" cellpadding="20" cellspacing="0">
  <tr>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/api-gateway.md">api-gateway</a></h4>
      <p>Die <strong>einzige Eingangstür</strong> für Browser- und Mobile-Clients zum Backend. Validiert bei jeder <code>/api/v1/...</code>-Anfrage das JWT über Keycloak und übergibt die Nutzeridentität per sicheren Headern an Backend-Services. Leitet je nach Pfad an <code>finance-api</code>, <code>market-data-service</code>, <code>news-service</code> oder <code>analytics-service</code> weiter; wendet Rate-Limiting mit Redis und Circuit Breaker mit Resilience4j an. Bündelt die OpenAPI-Dokumentation aller Services in einer Swagger-Oberfläche. Detaillierte Routenliste: <a href="docs/deutsch/api.md">docs/deutsch/api.md</a>.</p>
    </td>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/finance-api.md">finance-api</a></h4>
      <p>Das <strong>Herz</strong> des Portals und die Backend-for-Frontend-(BFF)-Schicht. Nutzerspezifische Regeln für Portfolio, Transaktionshistorie, Ziele, Preisalarme, Watchlist, gespeicherte Chart-Zeichnungen und Profil leben hier. Registrierung, E-Mail-Verifizierung, TOTP-basierte MFA und vertrauenswürdige Geräte sowie Admin-KPIs, Infokarten und Nachrichten-Favoriten sind in diesem Service gebündelt. Holt Marktdaten per HTTP von <code>market-data-service</code>; konsumiert Live-Preise und FX-Snapshots aus Kafka. Schreibt kritische Domain-Ereignisse per transactional outbox nach Kafka; Daten in PostgreSQL mit Flyway.</p>
    </td>
  </tr>
  <tr>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/market-data-service.md">market-data-service</a></h4>
      <p>Die <strong>Marktdaten-Engine</strong> der Plattform. Katalogisiert BIST-, Nasdaq-, Krypto-, Fonds-, FX-, Anleihen- und Eurobond-Instrumente; aktualisiert Live- und Historienpreise per Scheduler. Bezieht Daten aus Quellen wie TCMB EVDS, Finnhub, Yahoo, CoinGecko und Stooq; liefert Leitzins, Bankkurse und Basisinstrumentdaten. Kann beim Erststart historische Preise backfillen; nutzt hybriden JSON-Cache für häufig abgerufene Endpunkte. Veröffentlicht Preisänderungen auf Kafka-Topics wie <code>market.price.updated</code> und speist Analytik sowie Portal.</p>
    </td>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/analytics-service.md">analytics-service</a></h4>
      <p>Der Spezialdienst, der Live-Preisströme in <strong>technische Analyse</strong> umwandelt. Konsumiert Preisereignisse von <code>market-data-service</code> über Kafka; berechnet RSI und ähnliche Indikatoren, erzeugt Insights und Policy-Metriken. Speichert Ergebnisse im eigenen PostgreSQL-Schema und stellt sie per HTTP-API über das Gateway bereit. Schwere Indikatorberechnungen bleiben vom Portal-BFF getrennt; Analyse-Seite und zugehörige Karten beziehen aktuelle Metriken von hier.</p>
    </td>
  </tr>
  <tr>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/news-service.md">news-service</a></h4>
      <p>Der Service, der Finanznachrichten <strong>sammelt und bereitstellt</strong>. Durchsucht definierte RSS-Quellen per geplanten Jobs, schreibt Artikel in die Datenbank und speist die Listen-/Filter-API des Portals. Bietet bei Bedarf Übersetzung über MyMemory; erreichbar über <code>/api/v1/news/**</code> am Gateway. Der Nachrichtenstrom skaliert unabhängig vom Portal; in Docker mit restriktivem Dateisystem und eigener DB-Passwort-Konfiguration.</p>
    </td>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/notification-service.md">notification-service</a></h4>
      <p>Das Zentrum für <strong>E-Mail-Benachrichtigungen</strong> an Nutzer. Hört auf Kafka-Ereignisse wie ausgelöste Alarme, verdächtige Anmeldungen, Watchlist-Änderungen und Analyse-Insights. Erzeugt je nach Typ templated E-Mails per SMTP und verlagert die Last aus der Live-UI. <code>finance-api</code> erzeugt Geschäftsregeln, dieser Service liefert — der Benachrichtigungskanal kann wechseln, ohne Domain-Code zu verkomplizieren.</p>
    </td>
  </tr>
  <tr>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/log-consumer-service.md">log-consumer-service</a></h4>
      <p>Der <strong>zentrale Log-Archivar</strong> aller Backend-Services. Anwendungen schreiben per Log4j2 im JSON-Format auf das Kafka-Topic <code>app.logs</code>; dieser Service konsumiert und indexiert in OpenSearch. Betrieb kann über Grafana, OpenSearch Dashboards oder Suche Fehler und Traces filtern. App-Pods müssen Logs nicht selbst speichern; die Pipeline wird plattformweit an einer Stelle verwaltet.</p>
    </td>
    <td valign="top" width="48%">
      <h4><a href="docs/deutsch/services/frontend-web.md">frontend-web</a></h4>
      <p>Die <strong>React-19</strong>-Single-Page-App (Vite + TypeScript), die der Nutzer sieht. Landing, Märkte, Analyse, Nachrichten, Bankkurse und Finanzbildung sind öffentlich; Portfolio, Zinsen/Festgeld, Dashboard und externe Portfolioansicht erfordern Anmeldung. Keycloak für Login und Registrierung; UI-Sprachen TR, EN und DE. Alle API-Aufrufe über <code>api-gateway</code>; separater <code>/admin</code>-Bereich für Admin und Infokarten.</p>
    </td>
  </tr>
</table>

> **Hinweis — Marktdaten und Scheduler**  
> Nach dem Start des Stacks kombiniert `market-data-service` historischen Preis-Backfill mit Live-Updates im Hintergrund. Das Vervollständigen von Katalog und Historien — abhängig von Instrumentenzahl und Provider-Latenz — kann **etwa 30 Minuten** dauern; das Portal füllt sich schrittweise, fehlende Charts oder leere Listen in den ersten Minuten sind normal.  
> Demo-Scheduler-Intervalle (z. B. Aktien ~1 Min., FX/Anleihen ~5 Min.) und Backfill-Schritte sind auf **kostenlose Kontingente** externer Quellen (TCMB EVDS, Yahoo, CoinGecko, Finnhub usw.) abgestimmt; API-Schlüssel stehen in `Docker/.env` (nicht im Repo). Pausen zwischen Anfragen vermeiden Rate-Limit-Verstöße. Bei kostenpflichtigen Plänen oder höherem Kontingent können `scheduler.*.delay-ms`, Backfill-`sleep-ms` und Cron-Ausdrücke in [docs/deutsch/configuration.md](docs/deutsch/configuration.md) verkürzt werden.  
> Fortschritt: `docker compose logs -f market-data-service`

## Plattform-Überblick

### Kafka-Ereignisse

| Topic | Produzent (Beisp.) | Konsument (Beisp.) |
|-------|---------------------|---------------------|
| `market.price.updated` | market-data-service | analytics-service, finance-api |
| `market.fx.snapshot.updated` | market-data-service | finance-api |
| `alarm-triggered` | finance-api (outbox) | notification-service |
| `watchlist.item.added` / `removed` | finance-api | notification-service |
| `login-security.alert` | finance-api | notification-service |
| `app.logs` | Alle Services | log-consumer-service |

### Infrastrukturkomponenten

Gemeinsame Komponenten via Docker Compose (`Docker/`):

| Komponente | Host-Port | Rolle |
|------------|-----------|-------|
| PostgreSQL | 5432 | Datenbanken `finance` + `keycloak` |
| Redis | 6379 | Gateway Rate Limit, finance-api Cache |
| Kafka | 9092 | Event Bus |
| Keycloak | 8085 | OIDC / Realm-Import |
| OpenSearch | 9200 | Log-Suche |
| OpenSearch Dashboards | 5601 | Log-UI |
| Jaeger | 16686 | Distributed Tracing |
| Prometheus | 9090 | Metrik-Scrape |
| Grafana | 3000 | Dashboards |

### Unterstützende Verzeichnisse

| Verzeichnis | Inhalt |
|-------------|--------|
| `Docker/` | `docker-compose`, Keycloak Realm, Observability Stack |
| `docs/` | Architektur, Services, API, Setup, Observability (`turkce/`, `english/`, `deutsch/`) |
| `photos/` | Profil-Avatare (lokal / Volume) |

## Datenkonfiguration

<p align="center"><sub>Wo Sie Live- und Historiendaten in der Docker-Demo aktivieren oder deaktivieren</sub></p>

<div class="readme-stack">

<table align="center" border="0" cellpadding="0" cellspacing="0" width="100%">
  <tr>
    <td>
      <table border="0" cellpadding="14" cellspacing="0" width="100%">
        <tr>
          <td width="190" align="center" valign="top">
            <a href="Docker/.env.example" title="Docker/.env.example öffnen">
              <img src="docs/assets/config/v1-docker-env.webp" alt="Docker .env.example — Marktdaten-Block" width="172" loading="lazy">
            </a>
            <br><sub><code>Docker/.env.example</code></sub>
          </td>
          <td valign="top">
            <h4>① Docker/.env — Marktdaten-Schalter</h4>
            <p>Im Demo-Stack steuern Sie <strong>Live-Ingest</strong> und <strong>Historien-Backfill</strong> aus einer Datei. Anlegen mit <code>cp .env.example .env</code>, dann diesen Block bearbeiten. Priorität: <code>docker-compose.yml</code> → <code>Docker/.env</code> → <code>application.yml</code>.</p>
            <table border="0" cellpadding="4" cellspacing="0">
              <tr>
                <td width="42%" valign="top"><code>MARKET_HISTORY_BACKFILL_*</code></td>
                <td valign="top">BIST-, NASDAQ- und Krypto-<strong>Historienkurse</strong>; Start beim Hochfahren</td>
              </tr>
              <tr>
                <td valign="top"><code>PROVIDERS_FINNHUB_ENABLED</code></td>
                <td valign="top">NASDAQ Live-Preise und Historie (Finnhub)</td>
              </tr>
              <tr>
                <td valign="top"><code>MARKET_FUND_*</code></td>
                <td valign="top">TEFAS-Fonds-NAV und Historien-Bootstrap</td>
              </tr>
              <tr>
                <td valign="top"><code>MARKET_BOND_*</code> · <code>MARKET_TRGOVUSD_*</code></td>
                <td valign="top">TCMB-Anleiherenditen und TR-USD-Eurobond-Charts</td>
              </tr>
              <tr>
                <td valign="top"><code>MARKET_VIOP_ENABLED</code></td>
                <td valign="top">VIOP-Derivate (Zinsen-/Festgeld-Karte)</td>
              </tr>
              <tr>
                <td valign="top"><code>MARKET_*_SYNC_ENABLED</code></td>
                <td valign="top">Leitzins, Repo, TL-Einlagen, VPI-Makro-Sync</td>
              </tr>
            </table>
            <p><strong>Nach Änderungen:</strong> <code>docker compose up -d --force-recreate market-data-service</code><br>
            <strong>Fortschritt:</strong> <code>docker compose logs -f market-data-service</code> · Details: <a href="docs/deutsch/configuration.md">docs/deutsch/configuration.md</a></p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <a href="market-data-service/src/main/resources/application.yml" title="application.yml öffnen">
              <img src="docs/assets/config/v2-scheduler-live.webp" alt="application.yml — scheduler.live Cron-Einstellungen" width="172" loading="lazy">
            </a>
            <br><sub><code>market-data-service/.../application.yml</code></sub>
          </td>
          <td valign="top">
            <h4>② application.yml — Live-Daten-Scheduler</h4>
            <p>Krypto-, BIST-, NASDAQ-, FX-, Fonds- und Anleihen-<strong>Live-Preise</strong> nutzen denselben Cron-Ausdruck. Standard: <strong>3× täglich</strong> — 09:00, 13:00, 17:00 (<code>Europe/Istanbul</code>).</p>
            <table border="0" cellpadding="4" cellspacing="0">
              <tr>
                <td width="42%" valign="top"><code>SCHEDULER_LIVE_CRON</code></td>
                <td valign="top">Live-Ingest-Zeitplan (Cron); z. B. <code>0 0 9,13,17 * * *</code></td>
              </tr>
              <tr>
                <td valign="top"><code>SCHEDULER_LIVE_ZONE</code></td>
                <td valign="top">Zeitzone; Standard <code>Europe/Istanbul</code></td>
              </tr>
              <tr>
                <td valign="top"><code>market.scheduler.enabled</code></td>
                <td valign="top">Krypto-Live-Ingest (Standard: an)</td>
              </tr>
              <tr>
                <td valign="top"><code>market.stock.scheduler.enabled</code></td>
                <td valign="top">BIST + NASDAQ Live-Ingest (Standard: an)</td>
              </tr>
              <tr>
                <td valign="top"><code>market.fx.scheduler-enabled</code></td>
                <td valign="top">FX-Kurse Live-Ingest (Standard: an)</td>
              </tr>
              <tr>
                <td valign="top"><code>scheduler.*.delay-ms</code></td>
                <td valign="top">Bootstrap-/Hilfsaufgaben-Intervalle (FX 5 Min., Aktie 1 Min., Anleihe 5 Min.)</td>
              </tr>
            </table>
            <p><strong>Hinweis:</strong> VIOP und Makroraten (Leitzins, VPI) haben eigene Crons — siehe <code>market.viop.cron</code> und <code>market.*.weekly-sync</code> im selben File.</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <a href="market-data-service/src/main/resources/application.yml#L150" title="application.yml — history.backfill-Block">
              <img src="docs/assets/config/v3-history-backfill.webp" alt="application.yml — market.history.backfill Einstellungen" width="172" loading="lazy">
            </a>
            <br><sub><code>market.history.backfill</code></sub>
          </td>
          <td valign="top">
            <h4>③ application.yml — Historiendaten (Backfill)</h4>
            <p>Zentraler Orchestrator für <strong>Historienkurse</strong> (BIST, NASDAQ, Krypto, FX). Lokal standardmäßig <strong>aus</strong>; in der Docker-Demo per <code>Docker/.env</code> <strong>an</strong>.</p>
            <table border="0" cellpadding="4" cellspacing="0">
              <tr>
                <td width="42%" valign="top"><code>MARKET_HISTORY_BACKFILL_ENABLED</code></td>
                <td valign="top">Historien-Ingest ein-/ausschalten</td>
              </tr>
              <tr>
                <td valign="top"><code>MARKET_HISTORY_BACKFILL_RUN_ON_STARTUP</code></td>
                <td valign="top">Sofort beim Stack-Start ausführen</td>
              </tr>
              <tr>
                <td valign="top"><code>years</code> · <code>chunk-days</code></td>
                <td valign="top">Rückblick (5 Jahre) und Chunk-Größe (90 Tage)</td>
              </tr>
              <tr>
                <td valign="top"><code>schedule-delay-ms</code></td>
                <td valign="top">Periodischer Lauf: alle 15 Minuten (900000 ms)</td>
              </tr>
              <tr>
                <td valign="top"><code>gate-live-until-history-ready</code></td>
                <td valign="top"><code>true</code> → Live-Preise warten auf fertige Historie</td>
              </tr>
              <tr>
                <td valign="top"><code>kafka.enabled</code></td>
                <td valign="top">Kafka während Backfill (Standard: aus, Schreiben in DB)</td>
              </tr>
            </table>
            <p><strong>Tipp:</strong> Für eine schnelle Demo reicht <code>Docker/.env</code>; Tiefe und Retry in diesem YAML-Block feinjustieren. Anleihen-, Fonds-NAV- und Eurobond-Historie haben eigene Bootstrap-Flags (<code>market.bond.history-bootstrap</code>, <code>market.fund.nav-history-bootstrap</code>).</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <a href="Docker/docker-compose.yml#L220" title="docker-compose.yml — market-data-service">
              <img src="docs/assets/config/v4-docker-compose.webp" alt="docker-compose.yml — market-data-service environment-Block" width="172" loading="lazy">
            </a>
            <br><sub><code>Docker/docker-compose.yml</code></sub>
          </td>
          <td valign="top">
            <h4>④ docker-compose.yml — Docker-Demo-Overrides</h4>
            <p>Umgebungsvariablen für den <code>market-data-service</code>-Container; sie <strong>vereinen</strong> <code>Docker/.env</code> mit Inline-Defaults. Viele in <code>application.yml</code> <strong>aus</strong> geschaltete Pipelines sind hier für die Demo <strong>an</strong>.</p>
            <table border="0" cellpadding="4" cellspacing="0">
              <tr>
                <td width="42%" valign="top"><code>SPRING_PROFILES_ACTIVE=docker</code></td>
                <td valign="top">Lädt <code>application-docker.yml</code> (Fonds-Scheduler usw.)</td>
              </tr>
              <tr>
                <td valign="top"><code>TCMB_API_KEY=${...:?}</code></td>
                <td valign="top">Pflicht — EVDS-Anleihen, Makro, FX-Daten</td>
              </tr>
              <tr>
                <td valign="top"><code>MARKET_HISTORY_BACKFILL_*:-true</code></td>
                <td valign="top">Historienkurse: lokal aus → in Docker an</td>
              </tr>
              <tr>
                <td valign="top"><code>MARKET_FUND_*:-true</code></td>
                <td valign="top">TEFAS-NAV-Scheduler + Historien-Bootstrap</td>
              </tr>
              <tr>
                <td valign="top"><code>MARKET_VIOP_CRON:-0 */2 * * * *</code></td>
                <td valign="top">VIOP: Demo alle 2 Min. (lokal werktags 19:40)</td>
              </tr>
              <tr>
                <td valign="top"><code>${VAR:-default}</code>-Syntax</td>
                <td valign="top">Rechter Default, wenn <code>.env</code> leer ist</td>
              </tr>
            </table>
            <p><strong>Priorität:</strong> Compose-Zeile → <code>Docker/.env</code> → <code>application.yml</code>. Nach Änderung: <code>docker compose up -d --force-recreate market-data-service</code></p>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>

</div>

## Instrumente hinzufügen

<p align="center"><sub>Verfolgte Instrumente sind in Java-Registry-Dateien definiert — werden beim Start in die DB synchronisiert</sub></p>

<table align="center" border="0" cellpadding="0" cellspacing="0" width="100%">
  <tr>
    <td>
      <table border="0" cellpadding="14" cellspacing="0" width="100%">
        <tr>
          <td width="190" align="center" valign="top">
            <a href="market-data-service/src/main/java/com/company/marketdataservice/catalog/registry/providers/CryptoRegistry.java" title="CryptoRegistry.java">
              <img src="docs/assets/assets-registry/a1-crypto-registry.webp" alt="CryptoRegistry.java — Krypto-Symbolliste" width="172" loading="lazy">
            </a>
            <br><sub><code>.../providers/CryptoRegistry.java</code></sub>
          </td>
          <td valign="top">
            <h4>① Krypto — CryptoRegistry</h4>
            <p>Fügt USDT-Paare (BTC, ETH, SOL, …) dem Plattform-Katalog hinzu. Live-Preise über <strong>Composite</strong>-Provider: Yahoo → CoinGecko → Binance.</p>
            <table border="0" cellpadding="4" cellspacing="0">
              <tr>
                <td width="38%" valign="top"><code>SYMBOLS</code></td>
                <td valign="top">Watchlist — z. B. <code>"BTCUSDT"</code> hinzufügen</td>
              </tr>
              <tr>
                <td valign="top"><code>COINGECKO_ID_BY_BASE</code></td>
                <td valign="top">Basis-Asset → CoinGecko-ID (z. B. <code>BTC → bitcoin</code>)</td>
              </tr>
              <tr>
                <td valign="top"><code>IngestProvider.COMPOSITE</code></td>
                <td valign="top">Provider-Zuordnung; DB-Sync automatisch</td>
              </tr>
            </table>
            <p><strong>Schritte:</strong> Symbol in <code>SYMBOLS</code> eintragen → ggf. <code>COINGECKO_ID_BY_BASE</code> anpassen → <code>docker compose up -d --build market-data-service</code></p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <a href="market-data-service/src/main/java/com/company/marketdataservice/catalog/registry/providers/BistRegistry.java" title="BistRegistry.java">
              <img src="docs/assets/assets-registry/a2-bist-registry.webp" alt="BistRegistry.java — BIST-Aktienliste" width="172" loading="lazy">
            </a>
            <br><sub><code>.../providers/BistRegistry.java</code></sub>
          </td>
          <td valign="top">
            <h4>② BIST — BistRegistry</h4>
            <p>Fügt Borsa-Istanbul-Aktien dem Katalog hinzu. Live- und Historienkurse über <strong>Yahoo Finance</strong>; Ticker-Format <code>SYMBOL.IS</code> (z. B. <code>GARAN.IS</code>).</p>
            <table border="0" cellpadding="4" cellspacing="0">
              <tr>
                <td width="38%" valign="top"><code>SYMBOLS</code></td>
                <td valign="top">BIST-Codes — z. B. <code>"GARAN"</code>, <code>"THYAO"</code></td>
              </tr>
              <tr>
                <td valign="top"><code>symbol + ".IS"</code></td>
                <td valign="top">Yahoo-Provider-Ticker wird automatisch erzeugt</td>
              </tr>
              <tr>
                <td valign="top"><code>IngestProvider.YAHOO</code></td>
                <td valign="top">Börse: <code>BIST</code>, Währung: <code>TRY</code></td>
              </tr>
            </table>
            <p><strong>Schritte:</strong> Code in <code>SYMBOLS</code> eintragen → <code>docker compose up -d --build market-data-service</code> → für Historie <code>MARKET_HISTORY_BACKFILL_ENABLED=true</code> sicherstellen</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <a href="market-data-service/src/main/java/com/company/marketdataservice/catalog/registry/providers/NasdaqRegistry.java" title="NasdaqRegistry.java">
              <img src="docs/assets/assets-registry/a3-nasdaq-registry.webp" alt="NasdaqRegistry.java — NASDAQ-Aktien und ETF-Liste" width="172" loading="lazy">
            </a>
            <br><sub><code>.../providers/NasdaqRegistry.java</code></sub>
          </td>
          <td valign="top">
            <h4>③ NASDAQ + ETF — NasdaqRegistry</h4>
            <p>Fügt US-Aktien und ETFs dem Katalog hinzu. Bei aktivem <strong>Finnhub</strong> kommen Live-/Historiendaten von dort; aus oder bei Fehler greift <strong>Yahoo</strong> als Fallback.</p>
            <table border="0" cellpadding="4" cellspacing="0">
              <tr>
                <td width="38%" valign="top"><code>STOCK_SYMBOLS</code></td>
                <td valign="top">Aktien — <code>AAPL</code>, <code>NVDA</code>, <code>MSFT</code> …</td>
              </tr>
              <tr>
                <td valign="top"><code>ETF_SYMBOLS</code></td>
                <td valign="top">ETFs — <code>SPY</code>, <code>QQQ</code>, <code>VOO</code>, <code>VTI</code>, <code>IVV</code></td>
              </tr>
              <tr>
                <td valign="top"><code>PROVIDERS_FINNHUB_ENABLED</code></td>
                <td valign="top"><code>Docker/.env</code> — Hauptschalter für NASDAQ-Ingest</td>
              </tr>
              <tr>
                <td valign="top"><code>FINNHUB_API_KEY</code></td>
                <td valign="top">Pflicht (Finnhub an); sonst bleiben Listen/Charts leer</td>
              </tr>
            </table>
            <p><strong>Schritte:</strong> Aktie in <code>STOCK_SYMBOLS</code> oder ETF in <code>ETF_SYMBOLS</code> → <code>FINNHUB_API_KEY</code> setzen → Rebuild</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <a href="market-data-service/src/main/java/com/company/marketdataservice/catalog/registry/providers/FundRegistry.java" title="FundRegistry.java">
              <img src="docs/assets/assets-registry/a4-fund-registry.webp" alt="FundRegistry.java — TEFAS-Fondscodes" width="172" loading="lazy">
            </a>
            <br><sub><code>.../providers/FundRegistry.java</code></sub>
          </td>
          <td valign="top">
            <h4>④ TEFAS-Fonds — FundRegistry</h4>
            <p>Fügt türkische Investmentfonds dem Katalog hinzu. NAV-Daten von der <strong>TEFAS-API</strong>; Plattform-Symbolformat <code>FUND_{code}</code> (z. B. <code>FUND_TI2</code>).</p>
            <table border="0" cellpadding="4" cellspacing="0">
              <tr>
                <td width="38%" valign="top"><code>TEFAS_CODES</code></td>
                <td valign="top">Fondscodes — <code>TI2</code>, <code>TP2</code>, <code>AFT</code> …</td>
              </tr>
              <tr>
                <td valign="top"><code>FUND_{code}</code></td>
                <td valign="top">Kanonisches Katalog-Symbol wird automatisch erzeugt</td>
              </tr>
              <tr>
                <td valign="top"><code>MARKET_FUND_SCHEDULER_ENABLED</code></td>
                <td valign="top">Live-NAV-Updates (<code>Docker/.env</code>)</td>
              </tr>
              <tr>
                <td valign="top"><code>MARKET_FUND_NAV_*</code></td>
                <td valign="top">Historischer NAV-Bootstrap und Lückenreparatur</td>
              </tr>
            </table>
            <p><strong>Schritte:</strong> Code in <code>TEFAS_CODES</code> → <code>MARKET_FUND_SCHEDULER_ENABLED=true</code> → Rebuild. API-URL: <code>application.yml</code> → <code>market.fund.tefas-fon-gnl-blg-url</code></p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <a href="market-data-service/src/main/java/com/company/marketdataservice/catalog/registry/providers/BondRegistry.java" title="BondRegistry.java">
              <img src="docs/assets/assets-registry/a5-bond-registry.webp" alt="BondRegistry.java — TCMB-Anleiherendite-Serien" width="172" loading="lazy">
            </a>
            <br><sub><code>.../providers/BondRegistry.java</code></sub>
          </td>
          <td valign="top">
            <h4>⑤ TCMB-Anleihe — BondRegistry</h4>
            <p>Fügt türkische Staatsanleihen-Renditen dem Katalog hinzu. Daten von <strong>TCMB EVDS</strong>; jede Zeile mappt Symbol → EVDS-Seriencode (z. B. <code>TRBOND1Y → TP.KTF10</code>).</p>
            <table border="0" cellpadding="4" cellspacing="0">
              <tr>
                <td width="38%" valign="top"><code>ROWS</code></td>
                <td valign="top"><code>TRBOND1Y</code>, <code>TRBOND2Y</code>, <code>TRBOND3Y</code> + EVDS-Codes</td>
              </tr>
              <tr>
                <td valign="top"><code>IngestProvider.TCMB_BOND</code></td>
                <td valign="top">Live-Rendite + Historien-Bootstrap</td>
              </tr>
              <tr>
                <td valign="top"><code>TCMB_API_KEY</code></td>
                <td valign="top"><code>Docker/.env</code> — EVDS-Zugang Pflicht</td>
              </tr>
              <tr>
                <td valign="top"><code>MARKET_BOND_*</code></td>
                <td valign="top">Historien-Bootstrap und tägliche Aktualisierung (<code>Docker/.env</code>)</td>
              </tr>
            </table>
            <p><strong>Schritte:</strong> <code>BondRow</code> für neue Laufzeit → <code>TCMB_API_KEY</code> → <code>MARKET_BOND_HISTORY_BOOTSTRAP_ENABLED=true</code> → Rebuild</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <a href="market-data-service/src/main/java/com/company/marketdataservice/catalog/registry/providers/FxRegistry.java" title="FxRegistry.java">
              <img src="docs/assets/assets-registry/a6-fx-registry.webp" alt="FxRegistry.java — FX- und Metallsymbole" width="172" loading="lazy">
            </a>
            <br><sub><code>.../providers/FxRegistry.java</code></sub>
          </td>
          <td valign="top">
            <h4>⑥ FX + Metalle — FxRegistry</h4>
            <p>Fügt TRY-Kreuze und Edelmetalle dem Katalog hinzu. Fiat: <strong>TCMB-XML</strong> + Fallback; Metalle (XAU, XAG …) über Stooq × USDTRY abgeleitet.</p>
            <table border="0" cellpadding="4" cellspacing="0">
              <tr>
                <td width="38%" valign="top"><code>fx(...)</code>-Zeilen</td>
                <td valign="top"><code>USDTRY</code>, <code>EURTRY</code> … <code>XAUTRY</code>, <code>XAGTRY</code></td>
              </tr>
              <tr>
                <td valign="top"><code>IngestProvider.TCMB</code></td>
                <td valign="top">Primärquelle für Fiat</td>
              </tr>
              <tr>
                <td valign="top"><code>market.fx.provider-order</code></td>
                <td valign="top"><code>application.yml</code> — TCMB, dann ExchangeRate API</td>
              </tr>
              <tr>
                <td valign="top"><code>market.fx.scheduler-enabled</code></td>
                <td valign="top">Live-FX-Updates (Standard: an)</td>
              </tr>
            </table>
            <p><strong>Schritte:</strong> <code>fx("SYMBOL", "Name", "BASE")</code> hinzufügen → bei Fiat <code>market.fx.provider-currencies</code> anpassen → Rebuild</p>
          </td>
        </tr>
        <tr><td colspan="2" height="20"></td></tr>
        <tr>
          <td width="190" align="center" valign="top">
            <a href="market-data-service/src/main/java/com/company/marketdataservice/catalog/registry/providers/EurobondRegistry.java" title="EurobondRegistry.java">
              <img src="docs/assets/assets-registry/a7-eurobond-registry.webp" alt="EurobondRegistry.java — TR-USD-Eurobond-Serien" width="172" loading="lazy">
            </a>
            <br><sub><code>.../providers/EurobondRegistry.java</code></sub>
          </td>
          <td valign="top">
            <h4>⑦ TR-USD-Eurobond — EurobondRegistry</h4>
            <p>Fügt türkische USD-Staatsanleihen-Benchmark-Renditen dem Katalog hinzu. Daten von <strong>Yahoo Finance</strong>-Chart-Tickern (z. B. <code>GTUSDTR5Y:GOV</code>).</p>
            <table border="0" cellpadding="4" cellspacing="0">
              <tr>
                <td width="38%" valign="top"><code>EurobondRow</code></td>
                <td valign="top"><code>TRGOVUSD1Y</code> … <code>TRGOVUSD15Y</code> + Yahoo-Symbol</td>
              </tr>
              <tr>
                <td valign="top"><code>IngestProvider.YAHOO</code></td>
                <td valign="top">MDS-Historien-Bootstrap + tägliche Aktualisierung</td>
              </tr>
              <tr>
                <td valign="top"><code>MARKET_TRGOVUSD_*</code></td>
                <td valign="top"><code>Docker/.env</code> — Historie und Refresh ein/aus</td>
              </tr>
              <tr>
                <td valign="top"><code>finance-api</code></td>
                <td valign="top">Separates Modul: <code>MARKET_TR_USD_EUROBOND_YAHOO_*</code> (ETF-Proxy-Charts)</td>
              </tr>
            </table>
            <p><strong>Schritte:</strong> <code>EurobondRow</code> für neue Laufzeit → Yahoo-Ticker prüfen → <code>MARKET_TRGOVUSD_HISTORY_BOOTSTRAP_ENABLED=true</code> → Rebuild</p>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>

## Schnellstart (Docker — empfohlen)

Im Repository **keine API-Schlüssel**. `cp .env.example .env` legt nur die Datei an; **Schlüssel und optional SMTP müssen Sie eintragen**. Schritt-für-Schritt: [docs/deutsch/getting-started.md](docs/deutsch/getting-started.md).

**Voraussetzungen:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Compose v2), ca. **8 GB RAM**.

| Schritt | Aktion |
|---------|--------|
| **1** | Repository klonen |
| **2** | `Docker/.env` anlegen (`cp .env.example .env`) |
| **3** | `Docker/.env` **ausfüllen:** Pflicht `TCMB_API_KEY`, `FINNHUB_API_KEY` |
| **4** | Optional: SMTP (Registrierung / Alarm-Mail), OpenAI (Admin-AI) |
| **5** | Aus `Docker`: `docker compose up -d --build` |
| **6** | Portal: http://localhost:5173 — Demo `admin1` / `123456` (Marktdaten 10–30 Min.) |

**1 · Klon**

```bash
git clone https://github.com/TaridaG/Toyota-32Bit-Finance-Platform.git
cd Toyota-32Bit-Finance-Platform/Docker
```

**2 · Umgebungsdatei**

```bash
cp .env.example .env
```

Windows (PowerShell): `Copy-Item .env.example .env`

> Die Kopie allein reicht nicht — in Schritt 3 die Datei **bearbeiten**.

**3 · Ausfüllen (Pflicht-API-Schlüssel)**

`Docker/.env` in einem Editor öffnen:

```env
TCMB_API_KEY=ihr-evds-schluessel
FINNHUB_API_KEY=ihr-finnhub-schluessel
```

- Schlüssel nur in `Docker/.env`; **diese Datei nicht committen**.
- `POSTGRES_PASSWORD` und `NEWS_DB_PASSWORD` in der Vorlage `123456` — für lokale Demo beibehalten.

**4 · Optional (SMTP / KI)**

| Variable | Zweck |
|----------|-------|
| `OPENAI_API_KEY` + `AI_ENABLED=true` | Admin-Infokarten-KI |
| `SPRING_MAIL_*` / `SMTP_*` | Registrierungs- und Alarm-E-Mail |
| `MARKET_EVDS_API_KEY` | Separater EVDS-Schlüssel (sonst `TCMB_API_KEY`; **keine leere `KEY=` Zeile**) |

E-Mail-Beispiel (Gmail **App-Passwort**, nicht das normale Login-Passwort):

```env
SMTP_USERNAME=you@gmail.com
SMTP_PASSWORD=16-stelliges-app-passwort
SPRING_MAIL_USERNAME=you@gmail.com
SPRING_MAIL_PASSWORD=16-stelliges-app-passwort
APP_REGISTRATION_VERIFICATION_FROM=you@gmail.com
NOTIFICATION_MAIL_FROM=you@gmail.com
```

Alle Variablen: [docs/deutsch/configuration.md](docs/deutsch/configuration.md).

> **Erstinstallation:** Nach Schritt 3 reicht in Schritt 5 `docker compose up`; `--force-recreate` ist nicht nötig.  
> **`.env` ändert sich bei laufendem Stack:** `docker compose up -d --force-recreate market-data-service` (TCMB/Finnhub), `finance-api` (Mail/KI), `notification-service` (SMTP).

**5 · Stack starten**

```bash
docker compose up -d --build
```

### Schnellzugriff

<p align="center"><sub>Bei laufendem Stack auf Logos klicken — Portal, API, Identität, Observability und Infrastruktur (TCP-Ports per Hover)</sub></p>

<p align="center">
  <a href="http://localhost:5173" title="Web-Oberfläche — http://localhost:5173"><img src="https://cdn.simpleicons.org/react/61DAFB" height="32" alt="Web-Oberfläche"></a>&nbsp;
  <a href="http://localhost:8080" title="API Gateway — http://localhost:8080"><img src="https://cdn.simpleicons.org/springboot/6DB33F" height="32" alt="API Gateway"></a>&nbsp;
  <a href="http://localhost:8080/swagger-ui.html" title="Swagger UI — http://localhost:8080/swagger-ui.html"><img src="https://cdn.simpleicons.org/swagger/85EA2D" height="32" alt="Swagger UI"></a>&nbsp;
  <a href="http://localhost:8085" title="Keycloak OIDC (Realm: finance) — http://localhost:8085"><img src="https://cdn.simpleicons.org/keycloak/4D4DFF" height="32" alt="Keycloak"></a>&nbsp;
  <a href="http://localhost:8085/admin" title="Keycloak Admin (admin / admin) — http://localhost:8085/admin"><img src="https://cdn.simpleicons.org/keycloak/FFFFFF" height="32" alt="Keycloak Admin"></a>
</p>
<p align="center">
  <a href="http://localhost:3000" title="Grafana (admin / admin) — http://localhost:3000"><img src="https://cdn.simpleicons.org/grafana/F46800" height="32" alt="Grafana"></a>&nbsp;
  <a href="http://localhost:9090" title="Prometheus — http://localhost:9090"><img src="https://cdn.simpleicons.org/prometheus/E6522C" height="32" alt="Prometheus"></a>&nbsp;
  <a href="http://localhost:16686" title="Jaeger — http://localhost:16686"><img src="https://cdn.simpleicons.org/opentelemetry/FFFFFF" height="32" alt="Jaeger"></a>&nbsp;
  <a href="http://localhost:5601" title="OpenSearch Dashboards — http://localhost:5601"><img src="https://cdn.simpleicons.org/opensearch/005EB8" height="32" alt="OpenSearch Dashboards"></a>&nbsp;
  <a href="https://localhost:9200" title="OpenSearch REST API (admin / 123456789) — https://localhost:9200"><img src="https://cdn.simpleicons.org/opensearch/FFFFFF" height="32" alt="OpenSearch API"></a>&nbsp;
  <span title="PostgreSQL — localhost:5432"><img src="https://cdn.simpleicons.org/postgresql/4169E1" height="32" alt="PostgreSQL"></span>&nbsp;
  <span title="Redis — localhost:6379"><img src="https://cdn.simpleicons.org/redis/DC382D" height="32" alt="Redis"></span>&nbsp;
  <span title="Kafka — localhost:9092"><img src="https://cdn.simpleicons.org/apachekafka/FFFFFF" height="32" alt="Kafka"></span>
</p>

### Demo-Benutzer

| Benutzer | Passwort | Rolle |
|----------|----------|-------|
| `user1` | `123456` | USER |
| `admin1` | `123456` | ADMIN |

Keycloak-Admin: http://localhost:8085/admin — `admin` / `admin`

Marktdaten-Verzögerung: Hinweis unter [Projektstruktur](#projektstruktur). Logs: `docker compose logs -f market-data-service`

## Lokale Entwicklung (Kurz)

1. Infrastruktur: `cd Docker && docker compose up -d postgres redis kafka keycloak`
2. Backend: `mvn -pl finance-api,market-data-service -am spring-boot:run` (Ports: [docs/deutsch/services.md](docs/deutsch/services.md))
3. Frontend: `cd frontend-web && npm install && npm run dev` (bei vollem Docker-Stack kein `.env.development` nötig)

Details: [docs/deutsch/getting-started.md](docs/deutsch/getting-started.md) (Pfad B / C).

## Dokumentation

Technische Leitfäden unter `docs/deutsch/`. Weitere Sprachen: [docs/README.md](docs/README.md) · [README.tr.md](README.tr.md) · [README.md](README.md)

| Dokument | Inhalt |
|----------|--------|
| [docs/deutsch/README.md](docs/deutsch/README.md) | Deutsch-Dokumentationsindex |
| [docs/deutsch/getting-started.md](docs/deutsch/getting-started.md) | Einrichtung (Docker, hybrid, Frontend) |
| [docs/deutsch/architecture.md](docs/deutsch/architecture.md) | Architektur und Datenfluss |
| [docs/deutsch/services.md](docs/deutsch/services.md) | Services, Ports |
| [docs/deutsch/api.md](docs/deutsch/api.md) | Gateway-Routen und OpenAPI |
| [docs/deutsch/configuration.md](docs/deutsch/configuration.md) | `Docker/.env` und Umgebungsvariablen |
| [docs/deutsch/development.md](docs/deutsch/development.md) | Entwicklung und Tests |
| [docs/deutsch/observability.md](docs/deutsch/observability.md) | Metriken, Traces, Logs |

## Haftungsausschluss

Dieses Repository ist ein **Demo- und Lernprojekt**. Es stellt keine Anlage-, Steuer- oder Rechtsberatung dar. Live-Marktdaten hängen von Drittanbieter-APIs und deren Nutzungsbedingungen ab. Der Einsatz in Produktion oder mit echtem Geld erfolgt auf eigenes Risiko.

## Lizenz

Dieses Projekt steht unter der [MIT-Lizenz](LICENSE).
