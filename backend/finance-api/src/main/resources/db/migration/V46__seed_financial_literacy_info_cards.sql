-- Finansal Okuryazarlık genel bilgi kartları (92 kart)
-- Güvenli upsert: hiçbir satır SİLİNMEZ.
-- ON CONFLICT (slug): içerik güncellenir; pages/target_terms birleştirilir;
-- target_element_ids / target_instrument_symbols yalnızca yeni değer varsa birleştirilir (boş [] mevcut bağları silmez).
-- Oluşturulma: node scripts/literacy-catalog/generate-migration.mjs

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Alan grafiği',
    'alan-grafigi',
    'ACTIVE',
    'CHART',
    'BEGINNER',
    'CHARTS',
    'Çizgi grafiğin altındaki alanın doldurulduğu, hacim veya birikim vurgulu grafiktir.',
    'Portföy değeri geçmişinde kümülatif etkiyi vurgular.',
    'Alan büyüklüğü toplam değer veya kümülatif performansı vurgular.',
    NULL,
    NULL,
    FALSE,
    '["Alan grafiği"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Çizgi grafik"]'::jsonb,
    '{"tr":{"title":"Alan grafiği","shortDescription":"Çizgi grafiğin altındaki alanın doldurulduğu, hacim veya birikim vurgulu grafiktir.","detailedDescription":"Portföy değeri geçmişinde kümülatif etkiyi vurgular.","howToInterpret":"Alan büyüklüğü toplam değer veya kümülatif performansı vurgular.","commonMistake":null,"example":null,"relatedTerms":["Çizgi grafik"]},"en":{"title":"Area chart","shortDescription":"A line chart with the area beneath filled, emphasizing volume or cumulative effect.","detailedDescription":"Highlights cumulative impact in portfolio value history.","howToInterpret":"The filled area emphasizes total value or cumulative performance.","commonMistake":null,"example":null,"relatedTerms":["Line chart"]},"de":{"title":"Flächendiagramm","shortDescription":"Ein Liniendiagramm mit gefüllter Fläche darunter, das Volumen oder kumulative Wirkung betont.","detailedDescription":"Hebt kumulative Effekte in der Portfolio-Wertentwicklung hervor.","howToInterpret":"Die Flächengröße betont Gesamtwert oder kumulative Performance.","commonMistake":null,"example":null,"relatedTerms":["Liniendiagramm"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Alım gücü',
    'alim-gucu',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'BASIC_FINANCE',
    'Belirli bir gelir veya birikimle satın alınabilen mal ve hizmet miktarıdır.',
    'Alım gücü enflasyon, kur ve gelir artışıyla değişir; yatırım başarısı için reel ölçüm kritiktir.',
    'Enflasyon yükseldikçe aynı nominal tutar daha az mal alır.',
    NULL,
    NULL,
    FALSE,
    '["Alım gücü","alim gucu"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["TÜFE Endeksi","Reel getiri","Enflasyon"]'::jsonb,
    '{"tr":{"title":"Alım gücü","shortDescription":"Belirli bir gelir veya birikimle satın alınabilen mal ve hizmet miktarıdır.","detailedDescription":"Alım gücü enflasyon, kur ve gelir artışıyla değişir; yatırım başarısı için reel ölçüm kritiktir.","howToInterpret":"Enflasyon yükseldikçe aynı nominal tutar daha az mal alır.","commonMistake":null,"example":null,"relatedTerms":["TÜFE Endeksi","Reel getiri","Enflasyon"]},"en":{"title":"Purchasing power","shortDescription":"The quantity of goods and services that a given income or savings can buy.","detailedDescription":"Purchasing power shifts with inflation, exchange rates, and income growth; real measurement is critical for judging investment success.","howToInterpret":"As inflation rises, the same nominal amount buys fewer goods.","commonMistake":null,"example":null,"relatedTerms":["CPI index","Real return","Inflation"]},"de":{"title":"Kaufkraft","shortDescription":"Die Menge an Gütern und Dienstleistungen, die mit einem bestimmten Einkommen oder Ersparnissen gekauft werden kann.","detailedDescription":"Kaufkraft verändert sich durch Inflation, Wechselkurse und Einkommenswachstum; reale Messung ist entscheidend für die Bewertung von Anlageerfolg.","howToInterpret":"Steigt die Inflation, kauft derselbe Nominalbetrag weniger Güter.","commonMistake":null,"example":null,"relatedTerms":["VPI-Index","Realrendite","Inflation"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Alternatif yatırım simülasyonu',
    'alternatif-yatirim',
    'ACTIVE',
    'ANALYSIS_TOOL',
    'INTERMEDIATE',
    'SIMULATION',
    'Aynı tutarın farklı varlık sınıflarında nasıl performans göstereceğini modelleyen araçtır.',
    'Eğitim ve senaryo analizi amaçlıdır.',
    'Geçmiş veriye dayalı simülasyon geleceği garanti etmez; varsayımları kontrol edin.',
    NULL,
    NULL,
    FALSE,
    '["Alternatif yatırım","simulasyon"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Getiri","Risk","Reel getiri"]'::jsonb,
    '{"tr":{"title":"Alternatif yatırım simülasyonu","shortDescription":"Aynı tutarın farklı varlık sınıflarında nasıl performans göstereceğini modelleyen araçtır.","detailedDescription":"Eğitim ve senaryo analizi amaçlıdır.","howToInterpret":"Geçmiş veriye dayalı simülasyon geleceği garanti etmez; varsayımları kontrol edin.","commonMistake":null,"example":null,"relatedTerms":["Getiri","Risk","Reel getiri"]},"en":{"title":"Alternative investment simulation","shortDescription":"A tool that models how the same amount might perform across different asset classes.","detailedDescription":"Intended for education and scenario analysis.","howToInterpret":"Simulations based on past data do not guarantee future results; review the assumptions.","commonMistake":null,"example":null,"relatedTerms":["Return","Risk","Real return"]},"de":{"title":"Alternative-Anlage-Simulation","shortDescription":"Ein Werkzeug, das modelliert, wie derselbe Betrag in verschiedenen Anlageklassen performen könnte.","detailedDescription":"Für Bildung und Szenarioanalyse gedacht.","howToInterpret":"Simulationen auf Basis historischer Daten garantieren keine Zukunft; prüfen Sie die Annahmen.","commonMistake":null,"example":null,"relatedTerms":["Rendite","Risiko","Realrendite"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Audit log',
    'audit-log',
    'ACTIVE',
    'SYSTEM_TERM',
    'ADVANCED',
    'SYSTEM_OBSERVABILITY',
    'Sistemde gerçekleşen işlemlerin denetim amaçlı kayıt altına alındığı günlüktür.',
    'Yönetim ve uyumluluk ekipleri için operasyonel iz kaydıdır.',
    'Kim, ne zaman, hangi işlemi yaptı sorularına yanıt verir.',
    NULL,
    NULL,
    TRUE,
    '["Audit log"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Trace","Correlation ID"]'::jsonb,
    '{"tr":{"title":"Audit log","shortDescription":"Sistemde gerçekleşen işlemlerin denetim amaçlı kayıt altına alındığı günlüktür.","detailedDescription":"Yönetim ve uyumluluk ekipleri için operasyonel iz kaydıdır.","howToInterpret":"Kim, ne zaman, hangi işlemi yaptı sorularına yanıt verir.","commonMistake":null,"example":null,"relatedTerms":["Trace","Correlation ID"]},"en":{"title":"Audit log","shortDescription":"A compliance-oriented log of actions performed in the system.","detailedDescription":"An operational trace record for administration and compliance teams.","howToInterpret":"Answers who did what and when.","commonMistake":null,"example":null,"relatedTerms":["Trace","Correlation ID"]},"de":{"title":"Audit-Log","shortDescription":"Ein compliance-orientiertes Protokoll der im System ausgeführten Aktionen.","detailedDescription":"Operativer Nachweis für Administration und Compliance-Teams.","howToInterpret":"Beantwortet, wer wann welche Aktion ausgeführt hat.","commonMistake":null,"example":null,"relatedTerms":["Trace","Correlation ID"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Banka kurları',
    'banka-kurlari',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'MARKET_DATA',
    'Bankaların müşteriye sunduğu alış-satış döviz kurları; spread banka politikasına göre değişir.',
    'Portal banka kurları ekranı farklı bankaları karşılaştırmaya yardımcı olur; efektif kur piyasa ortalamasından sapabilir.',
    'Büyük tutarlarda spread pazarlık konusu olabilir; TCMB referansı ile karşılaştırın.',
    NULL,
    NULL,
    FALSE,
    '["Banka kurları","bank rates"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Efektif kur","Spread","Döviz"]'::jsonb,
    '{"tr":{"title":"Banka kurları","shortDescription":"Bankaların müşteriye sunduğu alış-satış döviz kurları; spread banka politikasına göre değişir.","detailedDescription":"Portal banka kurları ekranı farklı bankaları karşılaştırmaya yardımcı olur; efektif kur piyasa ortalamasından sapabilir.","howToInterpret":"Büyük tutarlarda spread pazarlık konusu olabilir; TCMB referansı ile karşılaştırın.","commonMistake":null,"example":null,"relatedTerms":["Efektif kur","Spread","Döviz"]},"en":{"title":"Bank FX rates","shortDescription":"Bank bid/ask quotes for customers; spreads vary by bank policy.","detailedDescription":"The bank rates page helps compare institutions; may differ from interbank mid rates.","howToInterpret":"Large tickets may negotiate spreads; compare to official references.","commonMistake":null,"example":null,"relatedTerms":["Effective rate","Spread","Foreign exchange"]},"de":{"title":"Bank-Devisenkurse","shortDescription":"Bank-Geld-/Briefkurse für Kunden; Spreads bankabhängig.","detailedDescription":"Vergleichsseite hilft Institute zu vergleichen.","howToInterpret":"Große Beträge oft verhandelbar; mit Referenzkursen abgleichen.","commonMistake":null,"example":null,"relatedTerms":["Effektiver Kurs","Spread","Wechselkurs"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Bar grafik',
    'bar-grafik',
    'ACTIVE',
    'CHART',
    'BEGINNER',
    'CHARTS',
    'Kategoriler arası değerleri dikey veya yatay çubuklarla karşılaştırır.',
    'Performans kıyaslamalarında net okunur.',
    'Çubuk uzunluğu büyüklük farkını net gösterir; eksen ölçeğine dikkat edin.',
    NULL,
    NULL,
    FALSE,
    '["Bar grafik"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Performans karşılaştırması"]'::jsonb,
    '{"tr":{"title":"Bar grafik","shortDescription":"Kategoriler arası değerleri dikey veya yatay çubuklarla karşılaştırır.","detailedDescription":"Performans kıyaslamalarında net okunur.","howToInterpret":"Çubuk uzunluğu büyüklük farkını net gösterir; eksen ölçeğine dikkat edin.","commonMistake":null,"example":null,"relatedTerms":["Performans karşılaştırması"]},"en":{"title":"Bar chart","shortDescription":"Compares values across categories using vertical or horizontal bars.","detailedDescription":"Clear for performance comparisons.","howToInterpret":"Bar length shows magnitude differences clearly; check the axis scale.","commonMistake":null,"example":null,"relatedTerms":["Performance comparison"]},"de":{"title":"Balkendiagramm","shortDescription":"Vergleicht Werte zwischen Kategorien mit vertikalen oder horizontalen Balken.","detailedDescription":"Gut lesbar für Performancevergleiche.","howToInterpret":"Die Balkenlänge zeigt Größenunterschiede deutlich; achten Sie auf die Achsenskalierung.","commonMistake":null,"example":null,"relatedTerms":["Performancevergleich"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Beta',
    'beta',
    'ACTIVE',
    'TERM',
    'ADVANCED',
    'PORTFOLIO_ANALYSIS',
    'Hisse veya portföyün piyasa hareketlerine göre duyarlılık ölçüsüdür (referans: endeks).',
    'Beta ~1 piyasa ile benzer hareket; >1 daha oynak, <1 daha sakin. Geçmiş veriye dayalıdır.',
    'Beta kısa vadede değişir; düşük beta her zaman düşük risk demek değildir (ör. sektör riski).',
    NULL,
    NULL,
    FALSE,
    '["Beta","beta"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Risk","Volatilite","Hisse senedi"]'::jsonb,
    '{"tr":{"title":"Beta","shortDescription":"Hisse veya portföyün piyasa hareketlerine göre duyarlılık ölçüsüdür (referans: endeks).","detailedDescription":"Beta ~1 piyasa ile benzer hareket; >1 daha oynak, <1 daha sakin. Geçmiş veriye dayalıdır.","howToInterpret":"Beta kısa vadede değişir; düşük beta her zaman düşük risk demek değildir (ör. sektör riski).","commonMistake":null,"example":null,"relatedTerms":["Risk","Volatilite","Hisse senedi"]},"en":{"title":"Beta","shortDescription":"Sensitivity of a stock or portfolio to broad market moves (index benchmark).","detailedDescription":"Beta near 1 moves with the market; >1 more volatile, <1 less. Based on historical data.","howToInterpret":"Beta changes over time; low beta is not always low risk (sector risk remains).","commonMistake":null,"example":null,"relatedTerms":["Risk","Volatility","Stock"]},"de":{"title":"Beta","shortDescription":"Sensitivität einer Aktie oder eines Portfolios gegenüber dem Gesamtmarkt.","detailedDescription":"Beta ~1 bewegt sich wie der Markt; >1 volatiler, <1 ruhiger. Historisch geschätzt.","howToInterpret":"Beta ist zeitvariabel; niedriges Beta ≠ immer geringes Risiko.","commonMistake":null,"example":null,"relatedTerms":["Risiko","Volatilität","Aktie"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'BIST',
    'bist',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'MARKET_DATA',
    'Borsa İstanbul’da işlem gören pay piyasası ve endekslerinin genel adıdır.',
    'BIST endeksleri yerel hisse piyasasının genel yönünü özetler.',
    'BIST endeksleri piyasanın genel yönünü özetler; tek hisse performansından ayrı okunmalıdır.',
    NULL,
    NULL,
    FALSE,
    '["BIST","bist"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Hisse senedi","Isı haritası"]'::jsonb,
    '{"tr":{"title":"BIST","shortDescription":"Borsa İstanbul’da işlem gören pay piyasası ve endekslerinin genel adıdır.","detailedDescription":"BIST endeksleri yerel hisse piyasasının genel yönünü özetler.","howToInterpret":"BIST endeksleri piyasanın genel yönünü özetler; tek hisse performansından ayrı okunmalıdır.","commonMistake":null,"example":null,"relatedTerms":["Hisse senedi","Isı haritası"]},"en":{"title":"BIST","shortDescription":"The collective name for the equity market and indices traded on Borsa Istanbul.","detailedDescription":"BIST indices summarize the overall direction of the local equity market.","howToInterpret":"BIST indices reflect the broad market trend and should be read separately from individual stock performance.","commonMistake":null,"example":null,"relatedTerms":["Stock","Heat map"]},"de":{"title":"BIST","shortDescription":"Der Sammelbegriff für den Aktienmarkt und die Indizes an der Borsa Istanbul.","detailedDescription":"BIST-Indizes fassen die Gesamtrichtung des lokalen Aktienmarkts zusammen.","howToInterpret":"BIST-Indizes zeigen den breiten Markttrend und sollten getrennt von der Performance einzelner Aktien gelesen werden.","commonMistake":null,"example":null,"relatedTerms":["Aktie","Heatmap"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Bildirim tercihi',
    'bildirim-tercihi',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'NOTIFICATIONS',
    'Hangi kanallardan ve hangi konularda bildirim alınacağını belirleyen ayarlardır.',
    'Profil ayarlarından yönetilir.',
    'Gereksiz bildirimleri kapatmak odaklanmayı artırır; kritik uyarıları açık tutun.',
    NULL,
    NULL,
    FALSE,
    '["Bildirim tercihi"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["E-posta bildirimi","Uygulama içi bildirim"]'::jsonb,
    '{"tr":{"title":"Bildirim tercihi","shortDescription":"Hangi kanallardan ve hangi konularda bildirim alınacağını belirleyen ayarlardır.","detailedDescription":"Profil ayarlarından yönetilir.","howToInterpret":"Gereksiz bildirimleri kapatmak odaklanmayı artırır; kritik uyarıları açık tutun.","commonMistake":null,"example":null,"relatedTerms":["E-posta bildirimi","Uygulama içi bildirim"]},"en":{"title":"Notification preference","shortDescription":"Settings that define which channels and topics send notifications.","detailedDescription":"Managed from profile settings.","howToInterpret":"Turning off noise helps focus; keep critical alerts enabled.","commonMistake":null,"example":null,"relatedTerms":["Email notification","In-app notification"]},"de":{"title":"Benachrichtigungseinstellung","shortDescription":"Einstellungen, die festlegen, über welche Kanäle und zu welchen Themen Benachrichtigungen kommen.","detailedDescription":"Wird in den Profileinstellungen verwaltet.","howToInterpret":"Weniger Rauschen verbessert den Fokus; kritische Alarme sollten aktiv bleiben.","commonMistake":null,"example":null,"relatedTerms":["E-Mail-Benachrichtigung","In-App-Benachrichtigung"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Cari denge',
    'cari-denge',
    'ACTIVE',
    'MACRO_INDICATOR',
    'INTERMEDIATE',
    'TURKEY_ECONOMY',
    'Ülkenin dış ticaret ve gelir/gider akışlarının net sonucudur; cari açık veya fazla üretir.',
    'Türkiye için enerji ithalatı ve turizm gelirleri önemli bileşenlerdir; kur ve büyüme beklentilerini etkiler.',
    'Kalıcı cari açık kur baskısı veya rezerv ihtiyacı yaratabilir; tek çeyrek verisiyle trend çıkarmayın.',
    NULL,
    NULL,
    FALSE,
    '["Cari denge","cari açık"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Döviz","GSYİH","Enflasyon"]'::jsonb,
    '{"tr":{"title":"Cari denge","shortDescription":"Ülkenin dış ticaret ve gelir/gider akışlarının net sonucudur; cari açık veya fazla üretir.","detailedDescription":"Türkiye için enerji ithalatı ve turizm gelirleri önemli bileşenlerdir; kur ve büyüme beklentilerini etkiler.","howToInterpret":"Kalıcı cari açık kur baskısı veya rezerv ihtiyacı yaratabilir; tek çeyrek verisiyle trend çıkarmayın.","commonMistake":null,"example":null,"relatedTerms":["Döviz","GSYİH","Enflasyon"]},"en":{"title":"Current account balance","shortDescription":"Net of a country’s trade and income flows; surplus or deficit.","detailedDescription":"For Turkey, energy imports and tourism matter; influences FX and growth expectations.","howToInterpret":"Persistent deficits can pressure the currency; read trends, not one quarter.","commonMistake":null,"example":null,"relatedTerms":["Foreign exchange","GDP","Inflation"]},"de":{"title":"Leistungsbilanz","shortDescription":"Saldo aus Handel und Einkommen; Überschuss oder Defizit.","detailedDescription":"Energieimporte und Tourismus sind für die Türkei wichtig; beeinflusst FX und Wachstum.","howToInterpret":"Anhaltende Defizite können den Kurs belasten.","commonMistake":null,"example":null,"relatedTerms":["Wechselkurs","BIP","Inflation"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Correlation ID',
    'correlation-id',
    'ACTIVE',
    'SYSTEM_TERM',
    'ADVANCED',
    'SYSTEM_OBSERVABILITY',
    'Aynı kullanıcı isteğine ait log ve trace kayıtlarını birleştiren benzersiz kimliktir.',
    'Destek ve operasyon süreçlerinde referans numarasıdır.',
    'Destek ve operasyon ekipleri sorun gidermede bu kimliği paylaşır.',
    NULL,
    NULL,
    TRUE,
    '["Correlation ID"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Trace","Audit log"]'::jsonb,
    '{"tr":{"title":"Correlation ID","shortDescription":"Aynı kullanıcı isteğine ait log ve trace kayıtlarını birleştiren benzersiz kimliktir.","detailedDescription":"Destek ve operasyon süreçlerinde referans numarasıdır.","howToInterpret":"Destek ve operasyon ekipleri sorun gidermede bu kimliği paylaşır.","commonMistake":null,"example":null,"relatedTerms":["Trace","Audit log"]},"en":{"title":"Correlation ID","shortDescription":"A unique identifier that links log and trace records for the same user request.","detailedDescription":"Used as a reference number in support and operations workflows.","howToInterpret":"Support and operations teams share this ID when troubleshooting.","commonMistake":null,"example":null,"relatedTerms":["Trace","Audit log"]},"de":{"title":"Correlation ID","shortDescription":"Eine eindeutige Kennung, die Log- und Trace-Einträge derselben Benutzeranfrage verknüpft.","detailedDescription":"Referenznummer in Support- und Betriebsprozessen.","howToInterpret":"Support- und Betriebsteams teilen diese ID bei der Fehleranalyse.","commonMistake":null,"example":null,"relatedTerms":["Trace","Audit-Log"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Çeşitlendirme',
    'cesitlendirme',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'PORTFOLIO_ANALYSIS',
    'Riski farklı varlık sınıflarına yayarak tek kaynağa bağımlılığı azaltma stratejisidir.',
    'Hisse, tahvil, altın, döviz ve fon kombinasyonları farklı risk-getiri profilleri sunar; korelasyon zamanla değişir.',
    'Çeşitlendirme riski sıfırlamaz; aynı anda tüm varlıklar düşebilir (korelasyon krizi).',
    NULL,
    NULL,
    FALSE,
    '["Çeşitlendirme","diversification"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Portföy","Varlık dağılımı","Risk"]'::jsonb,
    '{"tr":{"title":"Çeşitlendirme","shortDescription":"Riski farklı varlık sınıflarına yayarak tek kaynağa bağımlılığı azaltma stratejisidir.","detailedDescription":"Hisse, tahvil, altın, döviz ve fon kombinasyonları farklı risk-getiri profilleri sunar; korelasyon zamanla değişir.","howToInterpret":"Çeşitlendirme riski sıfırlamaz; aynı anda tüm varlıklar düşebilir (korelasyon krizi).","commonMistake":null,"example":null,"relatedTerms":["Portföy","Varlık dağılımı","Risk"]},"en":{"title":"Diversification","shortDescription":"Spreading risk across asset classes to reduce reliance on a single bet.","detailedDescription":"Mixing equities, bonds, gold, FX, and funds offers different profiles; correlations shift in crises.","howToInterpret":"Diversification does not eliminate risk; correlations can rise in stress.","commonMistake":null,"example":null,"relatedTerms":["Portfolio","Asset allocation","Risk"]},"de":{"title":"Diversifikation","shortDescription":"Risiko auf mehrere Anlageklassen streuen, statt eine einzelne Wette.","detailedDescription":"Mix aus Aktien, Anleihen, Gold, FX und Fonds; Korrelationen ändern sich in Krisen.","howToInterpret":"Diversifikation eliminiert Risiko nicht.","commonMistake":null,"example":null,"relatedTerms":["Portfolio","Vermögensallokation","Risiko"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Çizgi grafik',
    'cizgi-grafik',
    'ACTIVE',
    'CHART',
    'BEGINNER',
    'CHARTS',
    'Zaman içindeki değer değişimini sürekli çizgiyle gösteren grafik türüdür.',
    'Fiyat ve performans tarihçesinde en yaygın görselleştirmedir.',
    'Trend yönü ve dönemsel değişim hızlıca okunur; tek noktaya odaklanmaktan kaçının.',
    NULL,
    NULL,
    FALSE,
    '["Çizgi grafik","grafik"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Alan grafiği","Sparkline"]'::jsonb,
    '{"tr":{"title":"Çizgi grafik","shortDescription":"Zaman içindeki değer değişimini sürekli çizgiyle gösteren grafik türüdür.","detailedDescription":"Fiyat ve performans tarihçesinde en yaygın görselleştirmedir.","howToInterpret":"Trend yönü ve dönemsel değişim hızlıca okunur; tek noktaya odaklanmaktan kaçının.","commonMistake":null,"example":null,"relatedTerms":["Alan grafiği","Sparkline"]},"en":{"title":"Line chart","shortDescription":"A chart type that plots value changes over time with a continuous line.","detailedDescription":"The most common visualization for price and performance history.","howToInterpret":"Trend direction and period change are easy to read; avoid focusing on a single point.","commonMistake":null,"example":null,"relatedTerms":["Area chart","Sparkline"]},"de":{"title":"Liniendiagramm","shortDescription":"Ein Diagrammtyp, der Wertveränderungen im Zeitverlauf mit einer durchgehenden Linie darstellt.","detailedDescription":"Die häufigste Visualisierung für Kurs- und Performanceverläufe.","howToInterpret":"Trendrichtung und periodische Veränderung lassen sich schnell ablesen; vermeiden Sie den Fokus auf einen einzelnen Punkt.","commonMistake":null,"example":null,"relatedTerms":["Flächendiagramm","Sparkline"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Destek ve direnç',
    'destek-ve-direnç',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'CHARTS',
    'Fiyatın historically tepki verdiği alt (destek) ve üst (direnç) bölgelerdir.',
    'Analiz çizim araçlarıyla işaretlenir; psikoloji ve likidite kümelenmeleriyle ilişkilidir, kesin kural değildir.',
    'Kırılım sonrası eski direnç destek olabilir; hacim ve kapanış teyidi önemlidir.',
    NULL,
    NULL,
    FALSE,
    '["Destek","direnç","support"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Referans çizgisi","Mum grafik","Teknik gösterge"]'::jsonb,
    '{"tr":{"title":"Destek ve direnç","shortDescription":"Fiyatın historically tepki verdiği alt (destek) ve üst (direnç) bölgelerdir.","detailedDescription":"Analiz çizim araçlarıyla işaretlenir; psikoloji ve likidite kümelenmeleriyle ilişkilidir, kesin kural değildir.","howToInterpret":"Kırılım sonrası eski direnç destek olabilir; hacim ve kapanış teyidi önemlidir.","commonMistake":null,"example":null,"relatedTerms":["Referans çizgisi","Mum grafik","Teknik gösterge"]},"en":{"title":"Support and resistance","shortDescription":"Price zones where the market has historically reacted to the downside (support) or upside (resistance).","detailedDescription":"Drawn with chart tools; reflects clustering of orders and sentiment, not a physical law.","howToInterpret":"After a breakout, old resistance may act as support; confirm with closes and volume.","commonMistake":null,"example":null,"relatedTerms":["Reference line","Candlestick chart","Technical indicator"]},"de":{"title":"Unterstützung und Widerstand","shortDescription":"Kurszonen mit historisch häufiger Reaktion nach unten (Unterstützung) oder oben (Widerstand).","detailedDescription":"Mit Charttools markiert; spiegelt Ordercluster und Sentiment, keine feste Regel.","howToInterpret":"Nach Ausbruch kann alter Widerstand zur Unterstützung werden.","commonMistake":null,"example":null,"relatedTerms":["Referenzlinie","Kerzenchart","Technischer Indikator"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Dönem getirisi',
    'donem-getirisi',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'MARKET_DATA',
    'Seçilen takvim aralığında (1 gün, 1 ay, 1 yıl) yüzde fiyat veya değer değişimidir.',
    'Portal tablolarındaki 1G, 1A, 3A, 6A ve 1Y kolonları aynı mantığın farklı pencereleridir; karşılaştırma için dönem uzunluğu eşit olmalıdır.',
    'Kısa vadeli güçlü getiri, uzun vadede sürdürülebilir olmayabilir. Farklı varlık sınıflarını aynı dönemle kıyaslayın.',
    '1 günlük performansı ile 1 yıllık hedefi aynı metrik gibi değerlendirmek.',
    NULL,
    FALSE,
    '["1G","1A","1Y","dönem getirisi"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Getiri","Volatilite","Trend skoru"]'::jsonb,
    '{"tr":{"title":"Dönem getirisi","shortDescription":"Seçilen takvim aralığında (1 gün, 1 ay, 1 yıl) yüzde fiyat veya değer değişimidir.","detailedDescription":"Portal tablolarındaki 1G, 1A, 3A, 6A ve 1Y kolonları aynı mantığın farklı pencereleridir; karşılaştırma için dönem uzunluğu eşit olmalıdır.","howToInterpret":"Kısa vadeli güçlü getiri, uzun vadede sürdürülebilir olmayabilir. Farklı varlık sınıflarını aynı dönemle kıyaslayın.","commonMistake":"1 günlük performansı ile 1 yıllık hedefi aynı metrik gibi değerlendirmek.","example":null,"relatedTerms":["Getiri","Volatilite","Trend skoru"]},"en":{"title":"Period return","shortDescription":"Percentage change in price or value over a chosen calendar window (1D, 1M, 1Y).","detailedDescription":"Table columns such as 1D, 1M, 3M, 6M, and 1Y are the same idea with different horizons; compare like with like.","howToInterpret":"Strong short-term returns may not persist; align the period with your investment horizon.","commonMistake":"Judging a one-day move with a one-year objective in mind.","example":null,"relatedTerms":["Return","Volatility","Trend score"]},"de":{"title":"Periodenrendite","shortDescription":"Prozentuale Wertänderung über ein gewähltes Zeitfenster (1T, 1M, 1J).","detailedDescription":"Spalten wie 1T, 1M, 3M, 6M und 1J sind dieselbe Logik mit unterschiedlicher Länge.","howToInterpret":"Kurzfristige Spitzen sind nicht automatisch langfristig haltbar.","commonMistake":"Eintägige Bewegung mit einem Jahresziel verwechseln.","example":null,"relatedTerms":["Rendite","Volatilität","Trend-Score"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Döviz',
    'doviz',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'MARKET_DATA',
    'Yabancı para birimlerinin TL veya başka para birimleri karşısındaki değeridir.',
    'Kur hareketleri ithalat maliyeti, enflasyon ve portföy çeşitlendirmesini etkiler.',
    'Kur hareketleri ithalat, enflasyon ve portföy çeşitlendirmesini etkileyebilir.',
    NULL,
    NULL,
    FALSE,
    '["Döviz","doviz","fx"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Efektif kur","Spread","TCMB kuru"]'::jsonb,
    '{"tr":{"title":"Döviz","shortDescription":"Yabancı para birimlerinin TL veya başka para birimleri karşısındaki değeridir.","detailedDescription":"Kur hareketleri ithalat maliyeti, enflasyon ve portföy çeşitlendirmesini etkiler.","howToInterpret":"Kur hareketleri ithalat, enflasyon ve portföy çeşitlendirmesini etkileyebilir.","commonMistake":null,"example":null,"relatedTerms":["Efektif kur","Spread","TCMB kuru"]},"en":{"title":"Foreign exchange","shortDescription":"The value of foreign currencies against the Turkish lira or other currencies.","detailedDescription":"Exchange-rate moves affect import costs, inflation, and portfolio diversification.","howToInterpret":"Currency moves can influence imports, inflation, and how diversified a portfolio is.","commonMistake":null,"example":null,"relatedTerms":["Effective rate","Spread","CBRT rate"]},"de":{"title":"Devisen","shortDescription":"Der Wert ausländischer Währungen gegenüber der Türkischen Lira oder anderen Währungen.","detailedDescription":"Wechselkursbewegungen beeinflussen Importkosten, Inflation und Portfoliodiversifikation.","howToInterpret":"Währungsbewegungen können Importe, Inflation und die Diversifikation eines Portfolios beeinflussen.","commonMistake":null,"example":null,"relatedTerms":["Effektiver Kurs","Spread","TCMB-Kurs"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'E-posta bildirimi',
    'eposta-bildirimi',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'NOTIFICATIONS',
    'Uyarıların e-posta kanalı üzerinden iletilmesidir.',
    'Uzun metinli özetler için uygundur.',
    'Gecikme olabilir; acil piyasa hareketleri için uygulama içi kanal tercih edilebilir.',
    NULL,
    NULL,
    FALSE,
    '["E-posta"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Uygulama içi bildirim","Bildirim tercihi"]'::jsonb,
    '{"tr":{"title":"E-posta bildirimi","shortDescription":"Uyarıların e-posta kanalı üzerinden iletilmesidir.","detailedDescription":"Uzun metinli özetler için uygundur.","howToInterpret":"Gecikme olabilir; acil piyasa hareketleri için uygulama içi kanal tercih edilebilir.","commonMistake":null,"example":null,"relatedTerms":["Uygulama içi bildirim","Bildirim tercihi"]},"en":{"title":"Email notification","shortDescription":"Delivery of alerts through the email channel.","detailedDescription":"Suitable for longer summary messages.","howToInterpret":"There may be delay; in-app channels may be better for urgent market moves.","commonMistake":null,"example":null,"relatedTerms":["In-app notification","Notification preference"]},"de":{"title":"E-Mail-Benachrichtigung","shortDescription":"Zustellung von Alarmen über den E-Mail-Kanal.","detailedDescription":"Geeignet für längere Zusammenfassungen.","howToInterpret":"Es kann Verzögerungen geben; für dringende Marktbewegungen eignet sich der In-App-Kanal besser.","commonMistake":null,"example":null,"relatedTerms":["In-App-Benachrichtigung","Benachrichtigungseinstellung"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Efektif kur',
    'efektif-kur',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'MARKET_DATA',
    'Nakit döviz alım-satımında uygulanan işlem kurlarıdır.',
    'Bankalar ve döviz bürolarında fiilen uygulanan alış-satış fiyatlarıdır.',
    'Efektif alış ve satış kurları arasındaki fark spread olarak yansır.',
    NULL,
    NULL,
    FALSE,
    '["Efektif kur","efektif"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Döviz","Spread"]'::jsonb,
    '{"tr":{"title":"Efektif kur","shortDescription":"Nakit döviz alım-satımında uygulanan işlem kurlarıdır.","detailedDescription":"Bankalar ve döviz bürolarında fiilen uygulanan alış-satış fiyatlarıdır.","howToInterpret":"Efektif alış ve satış kurları arasındaki fark spread olarak yansır.","commonMistake":null,"example":null,"relatedTerms":["Döviz","Spread"]},"en":{"title":"Effective rate","shortDescription":"The transaction rates applied when buying or selling cash foreign currency.","detailedDescription":"These are the actual bid and ask prices used by banks and exchange offices.","howToInterpret":"The gap between effective buy and sell rates appears as the spread.","commonMistake":null,"example":null,"relatedTerms":["Foreign exchange","Spread"]},"de":{"title":"Effektiver Kurs","shortDescription":"Die Transaktionskurse beim Kauf oder Verkauf von Bargeld-Devisen.","detailedDescription":"Die tatsächlich von Banken und Wechselstuben angewendeten Geld- und Briefkurse.","howToInterpret":"Die Spanne zwischen effektivem An- und Verkaufskurs erscheint als Spread.","commonMistake":null,"example":null,"relatedTerms":["Devisen","Spread"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Ekonomik Güven Endeksi',
    'ekonomik-guven',
    'ACTIVE',
    'MACRO_INDICATOR',
    'BEGINNER',
    'TURKEY_ECONOMY',
    'Tüketici ve üreticilerin genel ekonomik görünüme dair birleşik güven ölçüsüdür.',
    'Makro özet ekranlarda birlikte okunur.',
    'Düşüş eğilimi talep zayıflığına; yükseliş toparlanma beklentisine işaret edebilir.',
    NULL,
    NULL,
    FALSE,
    '["Ekonomik güven","guven"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Tüketici Güven Endeksi"]'::jsonb,
    '{"tr":{"title":"Ekonomik Güven Endeksi","shortDescription":"Tüketici ve üreticilerin genel ekonomik görünüme dair birleşik güven ölçüsüdür.","detailedDescription":"Makro özet ekranlarda birlikte okunur.","howToInterpret":"Düşüş eğilimi talep zayıflığına; yükseliş toparlanma beklentisine işaret edebilir.","commonMistake":null,"example":null,"relatedTerms":["Tüketici Güven Endeksi"]},"en":{"title":"Economic confidence index","shortDescription":"A combined measure of consumer and producer confidence in the overall economy.","detailedDescription":"Often read together on macro summary screens.","howToInterpret":"A downward trend may point to weaker demand; an upward trend may suggest recovery expectations.","commonMistake":null,"example":null,"relatedTerms":["Consumer confidence index"]},"de":{"title":"Wirtschaftsvertrauensindex","shortDescription":"Ein zusammengefasstes Maß für das Vertrauen von Verbrauchern und Produzenten in die Gesamtwirtschaft.","detailedDescription":"Wird auf Makro-Übersichtsseiten häufig gemeinsam gelesen.","howToInterpret":"Ein Abwärtstrend kann auf schwächere Nachfrage hinweisen; ein Aufwärtstrend auf Erholungserwartungen.","commonMistake":null,"example":null,"relatedTerms":["Verbrauchervertrauensindex"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Emtia vadeli işlem',
    'emtia-vadeli-islem',
    'ACTIVE',
    'ASSET',
    'INTERMEDIATE',
    'MARKET_DATA',
    'Altın, gümüş, bakır gibi emtiaların ileri tarihli kontratlarla işlem gördüğü piyasadır.',
    'Yurtdışı vadeli segmentinde GC=F (altın), SI=F (gümüş) gibi semboller küresel referans fiyatları izler; TRY gösterimi kur ile birlikte hesaplanır.',
    'Vadeli fiyat spot fiyattan farklı olabilir (carry/roll). Küresel risk ve dolar endeksi emtia fiyatlarını etkiler.',
    NULL,
    NULL,
    FALSE,
    '["Vadeli","GC=F","emtia","global futures"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Ons altın","Gram altın","VİOP","Volatilite"]'::jsonb,
    '{"tr":{"title":"Emtia vadeli işlem","shortDescription":"Altın, gümüş, bakır gibi emtiaların ileri tarihli kontratlarla işlem gördüğü piyasadır.","detailedDescription":"Yurtdışı vadeli segmentinde GC=F (altın), SI=F (gümüş) gibi semboller küresel referans fiyatları izler; TRY gösterimi kur ile birlikte hesaplanır.","howToInterpret":"Vadeli fiyat spot fiyattan farklı olabilir (carry/roll). Küresel risk ve dolar endeksi emtia fiyatlarını etkiler.","commonMistake":null,"example":null,"relatedTerms":["Ons altın","Gram altın","VİOP","Volatilite"]},"en":{"title":"Commodity futures","shortDescription":"Contracts to buy or sell commodities (gold, silver, copper) at a future date.","detailedDescription":"Symbols like GC=F and SI=F track global benchmarks; local TRY quotes also reflect FX moves.","howToInterpret":"Futures can diverge from spot; global risk appetite and the USD matter for commodities.","commonMistake":null,"example":null,"relatedTerms":["Gold ounce","Gram gold","Volatility"]},"de":{"title":"Rohstoff-Futures","shortDescription":"Kontrakte auf künftige Lieferung bzw. Abrechnung von Rohstoffen wie Gold, Silber, Kupfer.","detailedDescription":"Symbole wie GC=F und SI=F folgen globalen Referenzpreisen; TRY-Anzeigen spiegeln auch Wechselkurse.","howToInterpret":"Future-Preise können vom Spot abweichen; USD und Risikostimmung sind wichtig.","commonMistake":null,"example":null,"relatedTerms":["Goldunze","Gramm Gold","Volatilität"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Enflasyon',
    'enflasyon',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'BASIC_FINANCE',
    'Mal ve hizmet fiyatlarının genel düzeyde sürekli artışıdır.',
    'Enflasyon alım gücünü eritir; nominal getirileri reel getiriye çevirmek, yatırım karşılaştırmalarında zorunlu hale getirir.',
    'Yıllık TÜFE oranı tek başına yeterli değildir; trend, bileşenler (gıda, enerji) ve politika faiziyle birlikte okunmalıdır.',
    '“Fiyatlar biraz arttı” ile yapısal enflasyonu aynı kefeye koymak.',
    NULL,
    FALSE,
    '["Enflasyon","enflasyon","TÜFE"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["TÜFE Endeksi","Reel getiri","Alım gücü"]'::jsonb,
    '{"tr":{"title":"Enflasyon","shortDescription":"Mal ve hizmet fiyatlarının genel düzeyde sürekli artışıdır.","detailedDescription":"Enflasyon alım gücünü eritir; nominal getirileri reel getiriye çevirmek, yatırım karşılaştırmalarında zorunlu hale getirir.","howToInterpret":"Yıllık TÜFE oranı tek başına yeterli değildir; trend, bileşenler (gıda, enerji) ve politika faiziyle birlikte okunmalıdır.","commonMistake":"“Fiyatlar biraz arttı” ile yapısal enflasyonu aynı kefeye koymak.","example":null,"relatedTerms":["TÜFE Endeksi","Reel getiri","Alım gücü"]},"en":{"title":"Inflation","shortDescription":"A sustained rise in the general level of prices for goods and services.","detailedDescription":"Inflation erodes purchasing power; comparing investments requires real (inflation-adjusted) returns, not nominal figures alone.","howToInterpret":"Read annual CPI together with trend, components (food, energy), and policy rates—not as a single headline.","commonMistake":"Treating a one-off price spike the same as persistent inflation.","example":null,"relatedTerms":["CPI index","Real return","Purchasing power"]},"de":{"title":"Inflation","shortDescription":"Ein anhaltender Anstieg des allgemeinen Preisniveaus für Güter und Dienstleistungen.","detailedDescription":"Inflation mindert die Kaufkraft; für Vergleiche braucht man reale (inflationsbereinigte) Renditen, nicht nur nominale.","howToInterpret":"Jährliche Inflation immer im Kontext von Trend, Komponenten und Leitzins lesen.","commonMistake":"Einmalige Preissprünge mit dauerhafter Inflation gleichsetzen.","example":null,"relatedTerms":["VPI-Index","Reale Rendite","Kaufkraft"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Enflasyon karşılaştırması',
    'enflasyon-karsilastirma',
    'ACTIVE',
    'ANALYSIS_TOOL',
    'INTERMEDIATE',
    'SIMULATION',
    'Yatırım getirisini enflasyon veya TÜFE ile kıyaslayan analiz aracıdır.',
    'Reel performansı somutlaştırır.',
    'Getiri enflasyonun altındaysa alım gücü korunmamış demektir.',
    NULL,
    NULL,
    FALSE,
    '["Enflasyon karşılaştırma"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Reel getiri","TÜFE Yıllık Oran"]'::jsonb,
    '{"tr":{"title":"Enflasyon karşılaştırması","shortDescription":"Yatırım getirisini enflasyon veya TÜFE ile kıyaslayan analiz aracıdır.","detailedDescription":"Reel performansı somutlaştırır.","howToInterpret":"Getiri enflasyonun altındaysa alım gücü korunmamış demektir.","commonMistake":null,"example":null,"relatedTerms":["Reel getiri","TÜFE Yıllık Oran"]},"en":{"title":"Inflation comparison","shortDescription":"An analysis tool that compares investment return against inflation or CPI.","detailedDescription":"Makes real performance tangible.","howToInterpret":"If return is below inflation, purchasing power has not been preserved.","commonMistake":null,"example":null,"relatedTerms":["Real return","CPI annual rate"]},"de":{"title":"Inflationsvergleich","shortDescription":"Ein Analysewerkzeug, das Anlagerendite mit Inflation oder VPI vergleicht.","detailedDescription":"Macht reale Performance greifbar.","howToInterpret":"Liegt die Rendite unter der Inflation, wurde die Kaufkraft nicht erhalten.","commonMistake":null,"example":null,"relatedTerms":["Realrendite","VPI-Jahresrate"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Eurobond',
    'eurobond',
    'ACTIVE',
    'ASSET',
    'INTERMEDIATE',
    'MARKET_DATA',
    'Genellikle yabancı para cinsinden ihraç edilen uzun vadeli borçlanma senedidir.',
    'Türkiye’nin döviz cinsi borçlanma araçları portföy ve makro analizde izlenir.',
    'Eurobond getirisi faiz, kredi riski ve kur hareketlerinin birleşimidir.',
    NULL,
    NULL,
    FALSE,
    '["Eurobond","eurobond"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Tahvil","Faiz","ISIN"]'::jsonb,
    '{"tr":{"title":"Eurobond","shortDescription":"Genellikle yabancı para cinsinden ihraç edilen uzun vadeli borçlanma senedidir.","detailedDescription":"Türkiye’nin döviz cinsi borçlanma araçları portföy ve makro analizde izlenir.","howToInterpret":"Eurobond getirisi faiz, kredi riski ve kur hareketlerinin birleşimidir.","commonMistake":null,"example":null,"relatedTerms":["Tahvil","Faiz","ISIN"]},"en":{"title":"Eurobond","shortDescription":"A long-term debt security usually issued in a foreign currency.","detailedDescription":"Turkey’s foreign-currency debt instruments are tracked in portfolio and macro analysis.","howToInterpret":"Eurobond return reflects the combined effect of interest, credit risk, and currency moves.","commonMistake":null,"example":null,"relatedTerms":["Bond","Interest","ISIN"]},"de":{"title":"Eurobond","shortDescription":"Eine langfristige Schuldverschreibung, die meist in Fremdwährung begeben wird.","detailedDescription":"Die fremdwährungsdenominierten Schuldtitel der Türkei werden in Portfolio- und Makroanalysen verfolgt.","howToInterpret":"Die Eurobond-Rendite spiegelt Zinsen, Kreditrisiko und Währungsbewegungen zusammen wider.","commonMistake":null,"example":null,"relatedTerms":["Anleihe","Zins","ISIN"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'F/K oranı',
    'fk-orani',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'PORTFOLIO_ANALYSIS',
    'Fiyat/kazanç oranı; hisse fiyatının hisse başına kâra bölünmesidir.',
    'Sektör ortalamalarıyla kıyaslanır; yüksek F/K büyüme beklentisi, düşük F/K değerleme veya düşük büyüme algısı olabilir.',
    'Negatif kârda F/K anlamsızlaşır; tek yıllık kâr yerine normalize kâr kullanın.',
    NULL,
    NULL,
    FALSE,
    '["F/K","P/E","fiyat kazanç"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Hisse senedi","Piyasa değeri","Getiri"]'::jsonb,
    '{"tr":{"title":"F/K oranı","shortDescription":"Fiyat/kazanç oranı; hisse fiyatının hisse başına kâra bölünmesidir.","detailedDescription":"Sektör ortalamalarıyla kıyaslanır; yüksek F/K büyüme beklentisi, düşük F/K değerleme veya düşük büyüme algısı olabilir.","howToInterpret":"Negatif kârda F/K anlamsızlaşır; tek yıllık kâr yerine normalize kâr kullanın.","commonMistake":null,"example":null,"relatedTerms":["Hisse senedi","Piyasa değeri","Getiri"]},"en":{"title":"P/E ratio","shortDescription":"Price divided by earnings per share.","detailedDescription":"Compare within sectors; high P/E may mean growth expectations, low P/E value or low growth.","howToInterpret":"Meaningless with negative earnings; use normalized earnings, not one-offs.","commonMistake":null,"example":null,"relatedTerms":["Stock","Market cap","Return"]},"de":{"title":"KGV","shortDescription":"Kurs-Gewinn-Verhältnis: Aktienkurs geteilt durch Gewinn je Aktie.","detailedDescription":"Sektorspezifisch vergleichen; hohes KGV oft Wachstumserwartung.","howToInterpret":"Bei negativem Gewinn unbrauchbar; bereinigte Gewinne nutzen.","commonMistake":null,"example":null,"relatedTerms":["Aktie","Marktkapitalisierung","Rendite"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Faiz',
    'faiz',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'BASIC_FINANCE',
    'Paranın belirli bir süre için kullanımı karşılığında ödenen veya kazanılan orandır.',
    'Faiz oranı borçlanma maliyetini ve mevduat veya tahvil getirisini belirler; makro politika ve enflasyonla yakından ilişkilidir.',
    'Faiz oranı yükseldikçe borçlanma maliyeti artar; mevduat ve tahvil getirileri genelde yükselme eğilimindedir.',
    NULL,
    NULL,
    FALSE,
    '["Faiz","faiz"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Politika faizi","Mevduat faizi","Reel faiz"]'::jsonb,
    '{"tr":{"title":"Faiz","shortDescription":"Paranın belirli bir süre için kullanımı karşılığında ödenen veya kazanılan orandır.","detailedDescription":"Faiz oranı borçlanma maliyetini ve mevduat veya tahvil getirisini belirler; makro politika ve enflasyonla yakından ilişkilidir.","howToInterpret":"Faiz oranı yükseldikçe borçlanma maliyeti artar; mevduat ve tahvil getirileri genelde yükselme eğilimindedir.","commonMistake":null,"example":null,"relatedTerms":["Politika faizi","Mevduat faizi","Reel faiz"]},"en":{"title":"Interest","shortDescription":"The rate paid or earned for the use of money over a set period.","detailedDescription":"Interest rates determine borrowing costs and deposit or bond yields, and they are closely linked to macro policy and inflation.","howToInterpret":"As interest rates rise, borrowing becomes more expensive and deposit and bond yields usually move higher.","commonMistake":null,"example":null,"relatedTerms":["Policy rate","Deposit rate","Real interest rate"]},"de":{"title":"Zins","shortDescription":"Der Satz, der für die Nutzung von Geld über einen bestimmten Zeitraum gezahlt oder verdient wird.","detailedDescription":"Zinssätze bestimmen Kreditkosten sowie Einlagen- oder Anleiherenditen und hängen eng mit Geldpolitik und Inflation zusammen.","howToInterpret":"Steigende Zinsen erhöhen die Kreditkosten; Einlagen- und Anleiherenditen tendieren meist ebenfalls nach oben.","commonMistake":null,"example":null,"relatedTerms":["Leitzins","Einlagenzins","Realzins"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Fiyat uyarısı',
    'fiyat-uyarisi',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'NOTIFICATIONS',
    'Belirlenen fiyat koşulu gerçekleştiğinde kullanıcıya bildirim gönderen uyarıdır.',
    'Bilgilendirme amaçlıdır; yatırım tavsiyesi değildir.',
    'Uyarı tetiklenmesi yatırım tavsiyesi değildir; bilgilendirme amaçlıdır.',
    NULL,
    NULL,
    FALSE,
    '["Fiyat uyarısı"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Bildirim tercihi","Uygulama içi bildirim"]'::jsonb,
    '{"tr":{"title":"Fiyat uyarısı","shortDescription":"Belirlenen fiyat koşulu gerçekleştiğinde kullanıcıya bildirim gönderen uyarıdır.","detailedDescription":"Bilgilendirme amaçlıdır; yatırım tavsiyesi değildir.","howToInterpret":"Uyarı tetiklenmesi yatırım tavsiyesi değildir; bilgilendirme amaçlıdır.","commonMistake":null,"example":null,"relatedTerms":["Bildirim tercihi","Uygulama içi bildirim"]},"en":{"title":"Price alert","shortDescription":"A notification sent when a defined price condition is met.","detailedDescription":"For information only; not investment advice.","howToInterpret":"A triggered alert is not a recommendation; it is informational.","commonMistake":null,"example":null,"relatedTerms":["Notification preference","In-app notification"]},"de":{"title":"Kursalarm","shortDescription":"Eine Benachrichtigung, wenn eine definierte Kursbedingung erfüllt ist.","detailedDescription":"Nur zur Information; keine Anlageberatung.","howToInterpret":"Ein ausgelöster Alarm ist keine Empfehlung, sondern eine Information.","commonMistake":null,"example":null,"relatedTerms":["Benachrichtigungseinstellung","In-App-Benachrichtigung"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Fon',
    'fon',
    'ACTIVE',
    'ASSET',
    'BEGINNER',
    'MARKET_DATA',
    'Birden fazla varlığa yatırım yapan kolektif yatırım aracıdır.',
    'Profesyonel yönetim, çeşitlendirme ve likidite avantajı sunabilir; yönetim ücreti vardır.',
    'Fon performansı yönetim stratejisi, ücretler ve piyasa koşullarından etkilenir.',
    NULL,
    NULL,
    FALSE,
    '["Fon","fon"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Hisse senedi","Risk","TEFAS"]'::jsonb,
    '{"tr":{"title":"Fon","shortDescription":"Birden fazla varlığa yatırım yapan kolektif yatırım aracıdır.","detailedDescription":"Profesyonel yönetim, çeşitlendirme ve likidite avantajı sunabilir; yönetim ücreti vardır.","howToInterpret":"Fon performansı yönetim stratejisi, ücretler ve piyasa koşullarından etkilenir.","commonMistake":null,"example":null,"relatedTerms":["Hisse senedi","Risk","TEFAS"]},"en":{"title":"Fund","shortDescription":"A collective investment vehicle that invests in multiple assets.","detailedDescription":"Funds can offer professional management, diversification, and liquidity, but they charge management fees.","howToInterpret":"Fund performance depends on strategy, fees, and market conditions.","commonMistake":null,"example":null,"relatedTerms":["Stock","Risk","TEFAS"]},"de":{"title":"Fonds","shortDescription":"Ein kollektives Anlagevehikel, das in mehrere Vermögenswerte investiert.","detailedDescription":"Fonds können professionelles Management, Diversifikation und Liquidität bieten, erheben aber Verwaltungsgebühren.","howToInterpret":"Die Fondsperformance hängt von Strategie, Gebühren und Marktbedingungen ab.","commonMistake":null,"example":null,"relatedTerms":["Aktie","Risiko","TEFAS"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Geçmiş harcama simülasyonu',
    'gecmis-harcama',
    'ACTIVE',
    'ANALYSIS_TOOL',
    'BEGINNER',
    'SIMULATION',
    'Geçmişte yapılan harcamanın bugünkü TÜFE ile güncellenmiş karşılığını hesaplar.',
    'Alım gücü kaybını somutlaştırır.',
    'Sonuç alım gücü kaybını somutlaştırır; eğitim ve planlama amaçlıdır.',
    NULL,
    NULL,
    FALSE,
    '["Geçmiş harcama"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["TÜFE Endeksi","Alım gücü"]'::jsonb,
    '{"tr":{"title":"Geçmiş harcama simülasyonu","shortDescription":"Geçmişte yapılan harcamanın bugünkü TÜFE ile güncellenmiş karşılığını hesaplar.","detailedDescription":"Alım gücü kaybını somutlaştırır.","howToInterpret":"Sonuç alım gücü kaybını somutlaştırır; eğitim ve planlama amaçlıdır.","commonMistake":null,"example":null,"relatedTerms":["TÜFE Endeksi","Alım gücü"]},"en":{"title":"Past spending simulation","shortDescription":"Calculates what a past expense would be worth today using CPI.","detailedDescription":"Makes purchasing-power loss concrete.","howToInterpret":"The result illustrates loss of purchasing power and is meant for education and planning.","commonMistake":null,"example":null,"relatedTerms":["CPI index","Purchasing power"]},"de":{"title":"Vergangene-Ausgaben-Simulation","shortDescription":"Berechnet, welchem heutigen Gegenwert eine vergangene Ausgabe nach VPI entspricht.","detailedDescription":"Macht Kaufkraftverlust konkret.","howToInterpret":"Das Ergebnis verdeutlicht Kaufkraftverlust und dient Bildung und Planung.","commonMistake":null,"example":null,"relatedTerms":["VPI-Index","Kaufkraft"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Gerçekleşmemiş kar/zarar',
    'gerceklesmemis-kz',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'PORTFOLIO_ANALYSIS',
    'Açık pozisyonlarda piyasa fiyatına göre hesaplanan geçici kâr/zarardır.',
    'Satış yapılana kadar kesinleşmez.',
    'Piyasa fiyatı değiştikçe güncellenir; satış yapılana kadar kesinleşmez.',
    NULL,
    NULL,
    FALSE,
    '["Gerçekleşmemiş","kar","zarar"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Gerçekleşmiş kar/zarar","Güncel değer"]'::jsonb,
    '{"tr":{"title":"Gerçekleşmemiş kar/zarar","shortDescription":"Açık pozisyonlarda piyasa fiyatına göre hesaplanan geçici kâr/zarardır.","detailedDescription":"Satış yapılana kadar kesinleşmez.","howToInterpret":"Piyasa fiyatı değiştikçe güncellenir; satış yapılana kadar kesinleşmez.","commonMistake":null,"example":null,"relatedTerms":["Gerçekleşmiş kar/zarar","Güncel değer"]},"en":{"title":"Unrealized gain/loss","shortDescription":"Temporary profit or loss on open positions based on market prices.","detailedDescription":"It is not final until the position is sold.","howToInterpret":"It updates as prices move and remains provisional until a sale occurs.","commonMistake":null,"example":null,"relatedTerms":["Realized gain/loss","Current value"]},"de":{"title":"Unrealisierter Gewinn/Verlust","shortDescription":"Vorläufiger Gewinn oder Verlust offener Positionen auf Basis aktueller Marktpreise.","detailedDescription":"Er ist erst endgültig, wenn die Position verkauft wird.","howToInterpret":"Er aktualisiert sich mit Kursänderungen und bleibt bis zum Verkauf vorläufig.","commonMistake":null,"example":null,"relatedTerms":["Realisierter Gewinn/Verlust","Aktueller Wert"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Gerçekleşmiş kar/zarar',
    'gerceklesmis-kz',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'PORTFOLIO_ANALYSIS',
    'Satış veya kapanış ile kesinleşen kâr veya zarar tutarıdır.',
    'Nakit akışına yansıyan kesin sonuçtur.',
    'Gerçekleşmiş sonuç nakit akışına yansır; vergi ve maliyet etkileri ayrı değerlendirilmelidir.',
    NULL,
    NULL,
    FALSE,
    '["Gerçekleşmiş","kar","zarar"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Gerçekleşmemiş kar/zarar","P&L"]'::jsonb,
    '{"tr":{"title":"Gerçekleşmiş kar/zarar","shortDescription":"Satış veya kapanış ile kesinleşen kâr veya zarar tutarıdır.","detailedDescription":"Nakit akışına yansıyan kesin sonuçtur.","howToInterpret":"Gerçekleşmiş sonuç nakit akışına yansır; vergi ve maliyet etkileri ayrı değerlendirilmelidir.","commonMistake":null,"example":null,"relatedTerms":["Gerçekleşmemiş kar/zarar","P&L"]},"en":{"title":"Realized gain/loss","shortDescription":"Profit or loss that is finalized through a sale or position close.","detailedDescription":"The definite outcome that affects cash flow.","howToInterpret":"Realized results hit cash flow; tax and cost effects should be assessed separately.","commonMistake":null,"example":null,"relatedTerms":["Unrealized gain/loss","P&L"]},"de":{"title":"Realisierter Gewinn/Verlust","shortDescription":"Gewinn oder Verlust, der durch Verkauf oder Schließung einer Position final wird.","detailedDescription":"Das endgültige Ergebnis, das den Cashflow beeinflusst.","howToInterpret":"Realisierte Ergebnisse wirken auf den Cashflow; Steuer- und Kosteneffekte sollten separat betrachtet werden.","commonMistake":null,"example":null,"relatedTerms":["Unrealisierter Gewinn/Verlust","G&V"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Getiri',
    'getiri',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'BASIC_FINANCE',
    'Bir yatırımın belirli bir dönemde sağladığı kazanç oranı veya tutarıdır.',
    'Getiri; fiyat artışı, faiz, temettü veya kur farkı gibi kaynaklardan oluşabilir. Portalda performans karşılaştırmalarının temel ölçüsüdür.',
    'Yüzde getiri, başlangıç değerine göre değişimi gösterir. Dönem uzunluğu (günlük, aylık, yıllık) mutlaka birlikte okunmalıdır.',
    'Kısa vadede yüksek getiriyi uzun vadeli sürdürülebilir performans sanmak.',
    NULL,
    FALSE,
    '["Getiri","getiri","performans"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Nominal getiri","Reel getiri","Risk","Volatilite"]'::jsonb,
    '{"tr":{"title":"Getiri","shortDescription":"Bir yatırımın belirli bir dönemde sağladığı kazanç oranı veya tutarıdır.","detailedDescription":"Getiri; fiyat artışı, faiz, temettü veya kur farkı gibi kaynaklardan oluşabilir. Portalda performans karşılaştırmalarının temel ölçüsüdür.","howToInterpret":"Yüzde getiri, başlangıç değerine göre değişimi gösterir. Dönem uzunluğu (günlük, aylık, yıllık) mutlaka birlikte okunmalıdır.","commonMistake":"Kısa vadede yüksek getiriyi uzun vadeli sürdürülebilir performans sanmak.","example":null,"relatedTerms":["Nominal getiri","Reel getiri","Risk","Volatilite"]},"en":{"title":"Return","shortDescription":"The rate or amount of gain an investment produces over a given period.","detailedDescription":"Return can come from price appreciation, interest, dividends, or exchange-rate moves. It is the core metric for performance comparisons in the portal.","howToInterpret":"Percentage return shows change relative to the starting value. Always read it together with the period length (daily, monthly, annual).","commonMistake":"Treating a strong short-term return as evidence of sustainable long-term performance.","example":null,"relatedTerms":["Nominal return","Real return","Risk","Volatility"]},"de":{"title":"Rendite","shortDescription":"Die Rendite oder der Betrag, den eine Anlage in einem bestimmten Zeitraum erwirtschaftet.","detailedDescription":"Rendite kann aus Kursgewinnen, Zinsen, Dividenden oder Wechselkursbewegungen entstehen. Sie ist die zentrale Kennzahl für Performancevergleiche im Portal.","howToInterpret":"Die prozentuale Rendite zeigt die Veränderung gegenüber dem Anfangswert. Lesen Sie sie immer zusammen mit der Periodenlänge (täglich, monatlich, jährlich).","commonMistake":"Eine starke kurzfristige Rendite als nachhaltige Langfristperformance zu interpretieren.","example":null,"relatedTerms":["Nominalrendite","Realrendite","Risiko","Volatilität"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Gram altın',
    'gram-altin',
    'ACTIVE',
    'ASSET',
    'BEGINNER',
    'MARKET_DATA',
    'Türkiye piyasasında gram bazında fiyatlanan altın gösterimidir.',
    'Yerel yatırımcılar için yaygın altın referansıdır; ons ve kur ile birlikte hareket eder.',
    'Gram altın fiyatı ons altın ve kur hareketlerinden etkilenir.',
    NULL,
    NULL,
    FALSE,
    '["Gram altın","altin"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Ons altın","Döviz"]'::jsonb,
    '{"tr":{"title":"Gram altın","shortDescription":"Türkiye piyasasında gram bazında fiyatlanan altın gösterimidir.","detailedDescription":"Yerel yatırımcılar için yaygın altın referansıdır; ons ve kur ile birlikte hareket eder.","howToInterpret":"Gram altın fiyatı ons altın ve kur hareketlerinden etkilenir.","commonMistake":null,"example":null,"relatedTerms":["Ons altın","Döviz"]},"en":{"title":"Gram gold","shortDescription":"Gold priced per gram in the Turkish market.","detailedDescription":"A widely used local gold reference for investors; it moves with ounce gold prices and exchange rates.","howToInterpret":"Gram gold prices are influenced by ounce gold and currency moves.","commonMistake":null,"example":null,"relatedTerms":["Ounce gold","Foreign exchange"]},"de":{"title":"Gramm-Gold","shortDescription":"Gold, das am türkischen Markt pro Gramm notiert wird.","detailedDescription":"Eine weit verbreitete lokale Goldreferenz für Anleger; sie bewegt sich mit Unzen-Gold und Wechselkursen.","howToInterpret":"Gramm-Goldpreise werden von Unzen-Gold und Währungsbewegungen beeinflusst.","commonMistake":null,"example":null,"relatedTerms":["Unzen-Gold","Devisen"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'GSYİH',
    'gsyih',
    'ACTIVE',
    'MACRO_INDICATOR',
    'BEGINNER',
    'TURKEY_ECONOMY',
    'Gayri safi yurt içi hasıla; bir ülkede üretilen mal ve hizmetlerin toplam değeridir.',
    'Büyüme oranı iş döngüsü ve istihdamla bağlantılıdır; hisse ve tahvil piyasalarına duyarlılık yaratır.',
    'Reel GSYİH büyümesi nominalden anlamlıdır; per capita ile refah kıyası yapılır.',
    NULL,
    NULL,
    FALSE,
    '["GSYİH","GDP","büyüme"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Ekonomik Güven Endeksi","Enflasyon","Politika faizi"]'::jsonb,
    '{"tr":{"title":"GSYİH","shortDescription":"Gayri safi yurt içi hasıla; bir ülkede üretilen mal ve hizmetlerin toplam değeridir.","detailedDescription":"Büyüme oranı iş döngüsü ve istihdamla bağlantılıdır; hisse ve tahvil piyasalarına duyarlılık yaratır.","howToInterpret":"Reel GSYİH büyümesi nominalden anlamlıdır; per capita ile refah kıyası yapılır.","commonMistake":null,"example":null,"relatedTerms":["Ekonomik Güven Endeksi","Enflasyon","Politika faizi"]},"en":{"title":"GDP","shortDescription":"Gross Domestic Product—the total value of goods and services produced in a country.","detailedDescription":"Growth links to the business cycle, jobs, and market sentiment for equities and bonds.","howToInterpret":"Real GDP growth matters more than nominal; compare per capita for living standards.","commonMistake":null,"example":null,"relatedTerms":["Economic confidence","Inflation","Policy rate"]},"de":{"title":"BIP","shortDescription":"Bruttoinlandsprodukt—Gesamtwert aller in einem Land produzierten Güter und Dienstleistungen.","detailedDescription":"Wachstum hängt mit Konjunktur, Arbeitsmarkt und Marktstimmung zusammen.","howToInterpret":"Reales Wachstum ist entscheidend; pro Kopf für Wohlstand vergleichen.","commonMistake":null,"example":null,"relatedTerms":["Wirtschaftsvertrauen","Inflation","Leitzins"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Güncel değer',
    'guncel-deger',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'PORTFOLIO_ANALYSIS',
    'Pozisyonun güncel piyasa fiyatlarıyla hesaplanan toplam değeridir.',
    'Portföy özetinde en sık izlenen anlık metriklerden biridir.',
    'Güncel değer anlık piyasa fiyatına bağlıdır; gün içi dalgalanır.',
    NULL,
    NULL,
    FALSE,
    '["Güncel değer","deger"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Maliyet fiyatı","Nominal getiri"]'::jsonb,
    '{"tr":{"title":"Güncel değer","shortDescription":"Pozisyonun güncel piyasa fiyatlarıyla hesaplanan toplam değeridir.","detailedDescription":"Portföy özetinde en sık izlenen anlık metriklerden biridir.","howToInterpret":"Güncel değer anlık piyasa fiyatına bağlıdır; gün içi dalgalanır.","commonMistake":null,"example":null,"relatedTerms":["Maliyet fiyatı","Nominal getiri"]},"en":{"title":"Current value","shortDescription":"The total value of a position calculated at current market prices.","detailedDescription":"One of the most frequently watched live metrics in portfolio summaries.","howToInterpret":"Current value depends on live market prices and can move throughout the day.","commonMistake":null,"example":null,"relatedTerms":["Cost basis","Nominal return"]},"de":{"title":"Aktueller Wert","shortDescription":"Der Gesamtwert einer Position zu aktuellen Marktpreisen.","detailedDescription":"Eine der am häufigsten beobachteten Live-Kennzahlen in Portfolioübersichten.","howToInterpret":"Der aktuelle Wert hängt von Live-Marktpreisen ab und kann intraday schwanken.","commonMistake":null,"example":null,"relatedTerms":["Einstandspreis","Nominalrendite"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Hazine bonosu',
    'hazine-bonosu',
    'ACTIVE',
    'ASSET',
    'INTERMEDIATE',
    'MARKET_DATA',
    'Devletin kısa vadeli borçlanma aracıdır; genelde bir yıldan kısa vadelerde ihraç edilir.',
    'Düşük kredi riski algısı nedeniyle referans getiri eğrisinin kısa ucunu oluşturur; faiz ve likidite koşullarıyla fiyatlanır.',
    'Bono getirisi, politika faizi ve likidite beklentileriyle hareket eder; tahvil ile vade yapısını karıştırmayın.',
    NULL,
    NULL,
    FALSE,
    '["Hazine bonosu","bono"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Tahvil","Politika faizi","Tahvil getirisi"]'::jsonb,
    '{"tr":{"title":"Hazine bonosu","shortDescription":"Devletin kısa vadeli borçlanma aracıdır; genelde bir yıldan kısa vadelerde ihraç edilir.","detailedDescription":"Düşük kredi riski algısı nedeniyle referans getiri eğrisinin kısa ucunu oluşturur; faiz ve likidite koşullarıyla fiyatlanır.","howToInterpret":"Bono getirisi, politika faizi ve likidite beklentileriyle hareket eder; tahvil ile vade yapısını karıştırmayın.","commonMistake":null,"example":null,"relatedTerms":["Tahvil","Politika faizi","Tahvil getirisi"]},"en":{"title":"Treasury bill","shortDescription":"Short-term government debt, typically under one year.","detailedDescription":"Often viewed as low credit risk and anchors the short end of the yield curve.","howToInterpret":"Bill yields track policy rate and liquidity expectations; not the same as long bonds.","commonMistake":null,"example":null,"relatedTerms":["Bond","Policy rate","Bond yield"]},"de":{"title":"Schatzanweisung","shortDescription":"Kurzfristige Staatsverschuldung, typischerweise unter einem Jahr.","detailedDescription":"Gilt oft als geringes Kreditrisiko und prägt das kurze Ende der Zinsstruktur.","howToInterpret":"Renditen folgen Leitzins und Liquidität; nicht mit langen Anleihen verwechseln.","commonMistake":null,"example":null,"relatedTerms":["Anleihe","Leitzins","Anleiherendite"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Hisse senedi',
    'hisse-senedi',
    'ACTIVE',
    'ASSET',
    'BEGINNER',
    'MARKET_DATA',
    'Bir şirketin ortaklık payını temsil eden menkul kıymettir.',
    'Hisse sahipleri şirket kârına ve büyümesine bağlı olarak fiyat ve temettü üzerinden getiri elde edebilir.',
    'Hisse fiyatı şirket performansı, sektör ve genel piyasa beklentilerinden etkilenir.',
    NULL,
    NULL,
    FALSE,
    '["Hisse senedi","hisse","bist"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["BIST","P&L","Volatilite","Temettü"]'::jsonb,
    '{"tr":{"title":"Hisse senedi","shortDescription":"Bir şirketin ortaklık payını temsil eden menkul kıymettir.","detailedDescription":"Hisse sahipleri şirket kârına ve büyümesine bağlı olarak fiyat ve temettü üzerinden getiri elde edebilir.","howToInterpret":"Hisse fiyatı şirket performansı, sektör ve genel piyasa beklentilerinden etkilenir.","commonMistake":null,"example":null,"relatedTerms":["BIST","P&L","Volatilite","Temettü"]},"en":{"title":"Stock","shortDescription":"A security that represents an ownership share in a company.","detailedDescription":"Shareholders may earn returns through price appreciation and dividends tied to company profits and growth.","howToInterpret":"Stock prices reflect company performance, sector trends, and broader market expectations.","commonMistake":null,"example":null,"relatedTerms":["BIST","P&L","Volatility","Dividend"]},"de":{"title":"Aktie","shortDescription":"Ein Wertpapier, das einen Eigentumsanteil an einem Unternehmen repräsentiert.","detailedDescription":"Aktionäre können Rendite durch Kursgewinne und Dividenden erzielen, die an Unternehmensgewinne und Wachstum gekoppelt sind.","howToInterpret":"Aktienkurse spiegeln Unternehmensleistung, Branchentrends und allgemeine Markterwartungen wider.","commonMistake":null,"example":null,"relatedTerms":["BIST","G&V","Volatilität","Dividende"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Isı haritası',
    'isi-haritasi',
    'ACTIVE',
    'CHART',
    'INTERMEDIATE',
    'CHARTS',
    'Varlıkların performansını renk ve alan büyüklüğüyle gösteren görselleştirmedir.',
    'Piyasa özetinde çok sayıda enstrümanı bir bakışta karşılaştırır.',
    'Renk genelde getiri yönünü; kutu büyüklüğü ağırlık veya piyasa değerini temsil edebilir.',
    'Renk ile kutu büyüklüğünü aynı metrik sanmak.',
    NULL,
    FALSE,
    '["Isı haritası","heatmap"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["BIST","Hisse senedi","Kategori nabzı"]'::jsonb,
    '{"tr":{"title":"Isı haritası","shortDescription":"Varlıkların performansını renk ve alan büyüklüğüyle gösteren görselleştirmedir.","detailedDescription":"Piyasa özetinde çok sayıda enstrümanı bir bakışta karşılaştırır.","howToInterpret":"Renk genelde getiri yönünü; kutu büyüklüğü ağırlık veya piyasa değerini temsil edebilir.","commonMistake":"Renk ile kutu büyüklüğünü aynı metrik sanmak.","example":null,"relatedTerms":["BIST","Hisse senedi","Kategori nabzı"]},"en":{"title":"Heat map","shortDescription":"A visualization that shows asset performance using color and box size.","detailedDescription":"Compares many instruments at a glance in market summaries.","howToInterpret":"Color usually shows return direction; box size may represent weight or market cap.","commonMistake":"Assuming color and box size represent the same metric.","example":null,"relatedTerms":["BIST","Stock","Category pulse"]},"de":{"title":"Heatmap","shortDescription":"Eine Visualisierung, die Performance mit Farbe und Kastengröße darstellt.","detailedDescription":"Vergleicht viele Instrumente auf einen Blick in Marktübersichten.","howToInterpret":"Farbe zeigt meist die Renditerichtung; Kastengröße kann Gewicht oder Marktkapitalisierung bedeuten.","commonMistake":"Farbe und Kastengröße für dieselbe Kennzahl zu halten.","example":null,"relatedTerms":["BIST","Aktie","Kategorie-Puls"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'ISIN',
    'isin',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'MARKET_DATA',
    'Uluslararası menkul kıymet kimlik kodu; eurobond ve fonlarda enstrümanı benzersiz tanımlar.',
    'TR… ve US… önekleri ülke ve ihraççı hakkında ipucu verir; işlem ve takas için standarttır.',
    'Aynı ISIN farklı platformlarda aynı enstrümanı ifade eder; sembol yerine ISIN ile arama yapın.',
    NULL,
    NULL,
    FALSE,
    '["ISIN","isin"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Eurobond","Tahvil","Fon"]'::jsonb,
    '{"tr":{"title":"ISIN","shortDescription":"Uluslararası menkul kıymet kimlik kodu; eurobond ve fonlarda enstrümanı benzersiz tanımlar.","detailedDescription":"TR… ve US… önekleri ülke ve ihraççı hakkında ipucu verir; işlem ve takas için standarttır.","howToInterpret":"Aynı ISIN farklı platformlarda aynı enstrümanı ifade eder; sembol yerine ISIN ile arama yapın.","commonMistake":null,"example":null,"relatedTerms":["Eurobond","Tahvil","Fon"]},"en":{"title":"ISIN","shortDescription":"International Securities Identification Number for bonds, funds, and equities.","detailedDescription":"Standard identifier for settlement and lookup across platforms.","howToInterpret":"Same ISIN should mean the same instrument globally; search by ISIN when unsure.","commonMistake":null,"example":null,"relatedTerms":["Eurobond","Bond","Fund"]},"de":{"title":"ISIN","shortDescription":"Internationale Wertpapierkennnummer zur eindeutigen Identifikation.","detailedDescription":"Standard in Abwicklung und Suche über Plattformen hinweg.","howToInterpret":"Gleiche ISIN = gleiches Instrument; bei Unsicherheit ISIN nutzen.","commonMistake":null,"example":null,"relatedTerms":["Eurobond","Anleihe","Fonds"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'İşlem hacmi',
    'islem-hacmi',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'MARKET_DATA',
    'Belirli bir dönemde el değiştiren toplam miktar veya tutardır.',
    'Yüksek hacim fiyat hareketinin güvenilirliğini artırabilir; düşük hacimde fiyat sıçramaları yanıltıcı olabilir.',
    'Kırılım (breakout) anlarında hacim artışı teyit aranır; hacimsiz hareketlere temkinli yaklaşın.',
    NULL,
    NULL,
    FALSE,
    '["Hacim","volume"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Likidite","Mum grafik","Volatilite"]'::jsonb,
    '{"tr":{"title":"İşlem hacmi","shortDescription":"Belirli bir dönemde el değiştiren toplam miktar veya tutardır.","detailedDescription":"Yüksek hacim fiyat hareketinin güvenilirliğini artırabilir; düşük hacimde fiyat sıçramaları yanıltıcı olabilir.","howToInterpret":"Kırılım (breakout) anlarında hacim artışı teyit aranır; hacimsiz hareketlere temkinli yaklaşın.","commonMistake":null,"example":null,"relatedTerms":["Likidite","Mum grafik","Volatilite"]},"en":{"title":"Trading volume","shortDescription":"Total quantity or value traded over a period.","detailedDescription":"High volume can validate price moves; low volume spikes may be noisy or unreliable.","howToInterpret":"Look for volume confirmation on breakouts; be cautious on thin moves.","commonMistake":null,"example":null,"relatedTerms":["Liquidity","Candlestick chart","Volatility"]},"de":{"title":"Handelsvolumen","shortDescription":"Gesamtmenge oder -wert gehandelter Kontrakte/Aktien in einer Periode.","detailedDescription":"Hohes Volumen kann Kursbewegungen stützen; dünne Märkte sind volatiler.","howToInterpret":"Bei Ausbrüchen Volumenanstieg als Bestätigung nutzen.","commonMistake":null,"example":null,"relatedTerms":["Liquidität","Kerzenchart","Volatilität"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Kaldıraç',
    'kaldirac',
    'ACTIVE',
    'TERM',
    'ADVANCED',
    'PORTFOLIO_ANALYSIS',
    'Az sermaye ile büyük pozisyon taşımayı mümkün kılan borçlanma veya türev yapısıdır.',
    'VİOP ve marjlı işlemlerde kazanç ve kayıp oransal büyür; risk yönetimi zorunludur.',
    'Kaldıraç getiriyi büyütür ama likidasyon/ margin call riskini de artırır; deneyimsiz kullanıcılar için tehlikelidir.',
    'Kaldıraçlı ürünleri “hızlı zenginlik” aracı sanmak.',
    NULL,
    FALSE,
    '["Kaldıraç","leverage"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["VİOP","Risk","Volatilite"]'::jsonb,
    '{"tr":{"title":"Kaldıraç","shortDescription":"Az sermaye ile büyük pozisyon taşımayı mümkün kılan borçlanma veya türev yapısıdır.","detailedDescription":"VİOP ve marjlı işlemlerde kazanç ve kayıp oransal büyür; risk yönetimi zorunludur.","howToInterpret":"Kaldıraç getiriyi büyütür ama likidasyon/ margin call riskini de artırır; deneyimsiz kullanıcılar için tehlikelidir.","commonMistake":"Kaldıraçlı ürünleri “hızlı zenginlik” aracı sanmak.","example":null,"relatedTerms":["VİOP","Risk","Volatilite"]},"en":{"title":"Leverage","shortDescription":"Using debt or derivatives to control a larger position with less capital.","detailedDescription":"Amplifies gains and losses in futures and margin trading; requires strict risk control.","howToInterpret":"Leverage raises liquidation/margin risk; dangerous for inexperienced users.","commonMistake":"Seeing leveraged products as a quick wealth tool.","example":null,"relatedTerms":["Futures","Risk","Volatility"]},"de":{"title":"Hebel","shortDescription":"Fremdkapital oder Derivate, um mit wenig Eigenkapital große Positionen zu steuern.","detailedDescription":"Verstärkt Gewinne und Verluste; erfordert Risikomanagement.","howToInterpret":"Erhöht Margin- und Liquidationsrisiko.","commonMistake":"Hebelprodukte als schneller Reichtum missverstehen.","example":null,"relatedTerms":["Futures","Risiko","Volatilität"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Kategori nabzı',
    'kategori-nabzi',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'MARKET_DATA',
    'Piyasa segmentlerinin (kripto, BIST, döviz…) eşit ağırlıklı ortalama 1G hareketini gösterir.',
    'Piyasalar üst şeridindeki nabız, o anki genel risk iştahının kaba özetidir; tek enstrüman performansı değildir.',
    'Pozitif nabız çoğu segmentin yeşil olduğu anları özetler; kendi portföyünüzle aynı olmayabilir.',
    NULL,
    NULL,
    FALSE,
    '["Kategori nabzı","pulse","segment"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Dönem getirisi","Isı haritası","Volatilite"]'::jsonb,
    '{"tr":{"title":"Kategori nabzı","shortDescription":"Piyasa segmentlerinin (kripto, BIST, döviz…) eşit ağırlıklı ortalama 1G hareketini gösterir.","detailedDescription":"Piyasalar üst şeridindeki nabız, o anki genel risk iştahının kaba özetidir; tek enstrüman performansı değildir.","howToInterpret":"Pozitif nabız çoğu segmentin yeşil olduğu anları özetler; kendi portföyünüzle aynı olmayabilir.","commonMistake":null,"example":null,"relatedTerms":["Dönem getirisi","Isı haritası","Volatilite"]},"en":{"title":"Category pulse","shortDescription":"Equal-weight average 1D move per market segment (crypto, BIST, FX, etc.).","detailedDescription":"The strip on the Markets page is a coarse mood indicator, not your portfolio return.","howToInterpret":"Positive pulse means most segments are up today; may differ from your holdings.","commonMistake":null,"example":null,"relatedTerms":["Period return","Heat map","Volatility"]},"de":{"title":"Kategorie-Puls","shortDescription":"Gleichgewichteter durchschnittlicher 1T-Move je Marktsegment.","detailedDescription":"Grober Stimmungsindikator auf der Piyasalar-Seite, nicht Ihre Portfolio-Rendite.","howToInterpret":"Positiver Puls = heute meist grüne Segmente.","commonMistake":null,"example":null,"relatedTerms":["Periodenrendite","Heatmap","Volatilität"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Kripto varlık',
    'kripto-varlik',
    'ACTIVE',
    'ASSET',
    'INTERMEDIATE',
    'MARKET_DATA',
    'Blok zinciri tabanlı, dijital olarak saklanan ve işlem gören varlıklardır.',
    'Bitcoin, Ethereum gibi varlıklar yüksek volatilite ve düzenleyici belirsizlik taşıyabilir.',
    'Kripto varlıklar yüksek volatilite ve düzenleyici belirsizlik taşıyabilir.',
    NULL,
    NULL,
    FALSE,
    '["Kripto varlık","kripto"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Volatilite","Risk","Stablecoin"]'::jsonb,
    '{"tr":{"title":"Kripto varlık","shortDescription":"Blok zinciri tabanlı, dijital olarak saklanan ve işlem gören varlıklardır.","detailedDescription":"Bitcoin, Ethereum gibi varlıklar yüksek volatilite ve düzenleyici belirsizlik taşıyabilir.","howToInterpret":"Kripto varlıklar yüksek volatilite ve düzenleyici belirsizlik taşıyabilir.","commonMistake":null,"example":null,"relatedTerms":["Volatilite","Risk","Stablecoin"]},"en":{"title":"Crypto asset","shortDescription":"Digital assets stored and traded on blockchain networks.","detailedDescription":"Assets such as Bitcoin and Ethereum can carry high volatility and regulatory uncertainty.","howToInterpret":"Crypto assets may show high volatility and regulatory uncertainty.","commonMistake":null,"example":null,"relatedTerms":["Volatility","Risk","Stablecoin"]},"de":{"title":"Krypto-Asset","shortDescription":"Digitale Vermögenswerte, die auf Blockchain-Netzwerken gespeichert und gehandelt werden.","detailedDescription":"Vermögenswerte wie Bitcoin und Ethereum können hohe Volatilität und regulatorische Unsicherheit aufweisen.","howToInterpret":"Krypto-Assets können hohe Volatilität und regulatorische Unsicherheit mit sich bringen.","commonMistake":null,"example":null,"relatedTerms":["Volatilität","Risiko","Stablecoin"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Kupon',
    'kupon',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'BASIC_FINANCE',
    'Tahvil veya eurobond sahibine periyodik olarak ödenen faiz tutarıdır.',
    'Kupon oranı ve ödeme takvimi, sabit getirili enstrümanların nakit akışını belirler; fiyat ise piyasa faizleriyle değişir.',
    'Yüksek kupon nakit geliri sağlar; ancak fiyat düşüşü portföy değerini yine de azaltabilir.',
    NULL,
    NULL,
    FALSE,
    '["Kupon","coupon"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Tahvil","Tahvil getirisi","Faiz"]'::jsonb,
    '{"tr":{"title":"Kupon","shortDescription":"Tahvil veya eurobond sahibine periyodik olarak ödenen faiz tutarıdır.","detailedDescription":"Kupon oranı ve ödeme takvimi, sabit getirili enstrümanların nakit akışını belirler; fiyat ise piyasa faizleriyle değişir.","howToInterpret":"Yüksek kupon nakit geliri sağlar; ancak fiyat düşüşü portföy değerini yine de azaltabilir.","commonMistake":null,"example":null,"relatedTerms":["Tahvil","Tahvil getirisi","Faiz"]},"en":{"title":"Coupon","shortDescription":"Periodic interest paid to the holder of a bond or eurobond.","detailedDescription":"Coupon rate and schedule define cash flows; market price still moves with interest rates.","howToInterpret":"Coupons provide income; price losses can still reduce portfolio value.","commonMistake":null,"example":null,"relatedTerms":["Bond","Bond yield","Interest rate"]},"de":{"title":"Kupon","shortDescription":"Periodische Zinszahlung an Inhaber von Anleihen oder Eurobonds.","detailedDescription":"Kupon und Zahlungsplan definieren Cashflows; der Marktpreis folgt den Zinsen.","howToInterpret":"Kupon bringt Einnahmen; Kursverluste können dennoch den Wert drücken.","commonMistake":null,"example":null,"relatedTerms":["Anleihe","Anleiherendite","Zins"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Likidite',
    'likidite',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'BASIC_FINANCE',
    'Bir varlığın hızlı ve düşük maliyetle nakde çevrilebilme özelliğidir.',
    'Likidite yüksekse alım-satım farkı (spread) genelde daralır; düşük likiditede fiyat kayması ve işlem maliyeti artar.',
    'Likidite yüksekse alım-satım farkı (spread) genelde daralır; düşük likiditede fiyat kayması riski artar.',
    'Az işlem gören varlıklarda büyük emir vermek.',
    NULL,
    FALSE,
    '["Likidite","likidite"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Spread","Volatilite"]'::jsonb,
    '{"tr":{"title":"Likidite","shortDescription":"Bir varlığın hızlı ve düşük maliyetle nakde çevrilebilme özelliğidir.","detailedDescription":"Likidite yüksekse alım-satım farkı (spread) genelde daralır; düşük likiditede fiyat kayması ve işlem maliyeti artar.","howToInterpret":"Likidite yüksekse alım-satım farkı (spread) genelde daralır; düşük likiditede fiyat kayması riski artar.","commonMistake":"Az işlem gören varlıklarda büyük emir vermek.","example":null,"relatedTerms":["Spread","Volatilite"]},"en":{"title":"Liquidity","shortDescription":"How quickly and cheaply an asset can be converted into cash.","detailedDescription":"When liquidity is high, the bid-ask spread is usually tighter; when it is low, slippage and transaction costs tend to rise.","howToInterpret":"Higher liquidity generally means a narrower spread; lower liquidity increases the risk of price slippage.","commonMistake":"Placing large orders in thinly traded assets.","example":null,"relatedTerms":["Spread","Volatility"]},"de":{"title":"Liquidität","shortDescription":"Wie schnell und kostengünstig ein Vermögenswert in Bargeld umgewandelt werden kann.","detailedDescription":"Bei hoher Liquidität ist der Geld-Brief-Spread meist enger; bei geringer Liquidität steigen Slippage und Transaktionskosten.","howToInterpret":"Hohe Liquidität bedeutet in der Regel einen engeren Spread; geringe Liquidität erhöht das Slippage-Risiko.","commonMistake":"Große Orders in dünn gehandelten Vermögenswerten platzieren.","example":null,"relatedTerms":["Spread","Volatilität"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Makro durum özeti',
    'makro-durum-ozeti',
    'ACTIVE',
    'ANALYSIS_TOOL',
    'INTERMEDIATE',
    'TURKEY_ECONOMY',
    'Temel makro göstergeleri tek ekranda özetleyen karar destek görünümüdür.',
    'TÜFE, faiz ve güven endeksleri birlikte sunulur.',
    'Göstergeler birlikte okunmalı; tek bir veri noktasına aşırı anlam yüklemeyin.',
    NULL,
    NULL,
    FALSE,
    '["Makro durum"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["TÜFE Endeksi","Politika faizi","Ekonomik Güven Endeksi"]'::jsonb,
    '{"tr":{"title":"Makro durum özeti","shortDescription":"Temel makro göstergeleri tek ekranda özetleyen karar destek görünümüdür.","detailedDescription":"TÜFE, faiz ve güven endeksleri birlikte sunulur.","howToInterpret":"Göstergeler birlikte okunmalı; tek bir veri noktasına aşırı anlam yüklemeyin.","commonMistake":null,"example":null,"relatedTerms":["TÜFE Endeksi","Politika faizi","Ekonomik Güven Endeksi"]},"en":{"title":"Macro overview","shortDescription":"A decision-support view that summarizes key macro indicators on one screen.","detailedDescription":"Presents CPI, interest rates, and confidence indices together.","howToInterpret":"Read indicators together; do not over-interpret a single data point.","commonMistake":null,"example":null,"relatedTerms":["CPI index","Policy rate","Economic confidence index"]},"de":{"title":"Makro-Übersicht","shortDescription":"Eine Entscheidungsunterstützung, die zentrale Makroindikatoren auf einem Bildschirm zusammenfasst.","detailedDescription":"Zeigt VPI, Zinsen und Vertrauensindizes gemeinsam.","howToInterpret":"Indikatoren sollten gemeinsam gelesen werden; überinterpretieren Sie keinen einzelnen Datenpunkt.","commonMistake":null,"example":null,"relatedTerms":["VPI-Index","Leitzins","Wirtschaftsvertrauensindex"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Makro veri bildirimi',
    'makro-veri-bildirimi',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'NOTIFICATIONS',
    'Önemli makro veri açıklamalarında kullanıcıyı bilgilendiren bildirim türüdür.',
    'TÜFE, faiz kararı gibi olaylar için kullanılır.',
    'Veri açıklaması piyasa beklentisiyle karşılaştırılarak okunmalıdır.',
    NULL,
    NULL,
    FALSE,
    '["Makro veri"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["TÜFE Aylık Oran","Politika faizi"]'::jsonb,
    '{"tr":{"title":"Makro veri bildirimi","shortDescription":"Önemli makro veri açıklamalarında kullanıcıyı bilgilendiren bildirim türüdür.","detailedDescription":"TÜFE, faiz kararı gibi olaylar için kullanılır.","howToInterpret":"Veri açıklaması piyasa beklentisiyle karşılaştırılarak okunmalıdır.","commonMistake":null,"example":null,"relatedTerms":["TÜFE Aylık Oran","Politika faizi"]},"en":{"title":"Macro data notification","shortDescription":"A notification type that informs users about major macro data releases.","detailedDescription":"Used for events such as CPI releases and rate decisions.","howToInterpret":"Compare the release with market expectations when reading it.","commonMistake":null,"example":null,"relatedTerms":["CPI monthly rate","Policy rate"]},"de":{"title":"Makrodaten-Benachrichtigung","shortDescription":"Ein Benachrichtigungstyp über wichtige Makrodaten-Veröffentlichungen.","detailedDescription":"Wird für Ereignisse wie VPI-Veröffentlichungen und Zinsentscheide genutzt.","howToInterpret":"Die Veröffentlichung sollte mit Markterwartungen verglichen werden.","commonMistake":null,"example":null,"relatedTerms":["VPI-Monatsrate","Leitzins"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Maliyet fiyatı',
    'maliyet-fiyati',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'PORTFOLIO_ANALYSIS',
    'Varlığın portföye alınırken ödenen ortalama birim fiyatıdır.',
    'Kar/zarar hesabının referans noktasıdır.',
    'Güncel fiyat maliyetin üzerindeyse gerçekleşmemiş kâr oluşur.',
    NULL,
    NULL,
    FALSE,
    '["Maliyet fiyatı","maliyet"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Güncel değer","P&L"]'::jsonb,
    '{"tr":{"title":"Maliyet fiyatı","shortDescription":"Varlığın portföye alınırken ödenen ortalama birim fiyatıdır.","detailedDescription":"Kar/zarar hesabının referans noktasıdır.","howToInterpret":"Güncel fiyat maliyetin üzerindeyse gerçekleşmemiş kâr oluşur.","commonMistake":null,"example":null,"relatedTerms":["Güncel değer","P&L"]},"en":{"title":"Cost basis","shortDescription":"The average price paid when an asset was added to the portfolio.","detailedDescription":"The reference point for profit and loss calculations.","howToInterpret":"If the current price is above cost, an unrealized gain appears.","commonMistake":null,"example":null,"relatedTerms":["Current value","P&L"]},"de":{"title":"Einstandspreis","shortDescription":"Der durchschnittliche Preis, der beim Kauf eines Vermögenswerts gezahlt wurde.","detailedDescription":"Der Referenzpunkt für Gewinn- und Verlustberechnungen.","howToInterpret":"Liegt der aktuelle Kurs über dem Einstand, entsteht ein unrealisierter Gewinn.","commonMistake":null,"example":null,"relatedTerms":["Aktueller Wert","G&V"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Mevduat faizi',
    'mevduat-faizi',
    'ACTIVE',
    'MACRO_INDICATOR',
    'BEGINNER',
    'TURKEY_ECONOMY',
    'Bankalarda vadeli veya vadesiz mevduata ödenen faiz oranıdır.',
    'TL tasarrufların getirisi; enflasyonla karşılaştırıldığında reel getiri hesaplanır.',
    'Nominal mevduat getirisi enflasyonun altındaysa reel getiri negatif olabilir.',
    NULL,
    NULL,
    FALSE,
    '["Mevduat faizi","mevduat"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Politika faizi","Reel faiz"]'::jsonb,
    '{"tr":{"title":"Mevduat faizi","shortDescription":"Bankalarda vadeli veya vadesiz mevduata ödenen faiz oranıdır.","detailedDescription":"TL tasarrufların getirisi; enflasyonla karşılaştırıldığında reel getiri hesaplanır.","howToInterpret":"Nominal mevduat getirisi enflasyonun altındaysa reel getiri negatif olabilir.","commonMistake":null,"example":null,"relatedTerms":["Politika faizi","Reel faiz"]},"en":{"title":"Deposit rate","shortDescription":"The interest rate paid on time or demand deposits at banks.","detailedDescription":"The return on lira savings; compared with inflation to calculate real return.","howToInterpret":"If nominal deposit return is below inflation, real return may be negative.","commonMistake":null,"example":null,"relatedTerms":["Policy rate","Real interest rate"]},"de":{"title":"Einlagenzins","shortDescription":"Der Zinssatz auf Fest- oder Tagesgeld bei Banken.","detailedDescription":"Die Rendite auf Lira-Ersparnisse; im Vergleich mit Inflation wird die Realrendite berechnet.","howToInterpret":"Liegt die nominale Einlagenrendite unter der Inflation, kann die Realrendite negativ sein.","commonMistake":null,"example":null,"relatedTerms":["Leitzins","Realzins"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Mum grafik',
    'mum-grafik',
    'ACTIVE',
    'CHART',
    'INTERMEDIATE',
    'CHARTS',
    'Bir zaman aralığında açılış, kapanış, en yüksek ve en düşük fiyatı gösteren grafik türüdür.',
    'Teknik analizde standart gösterimdir.',
    'Tek mum tek başına karar için yeterli değildir; trend ve zaman dilimiyle birlikte okunmalıdır.',
    'Tek günlük mumdan uzun vadeli yön tahmini yapmak.',
    NULL,
    FALSE,
    '["Mum grafik","candlestick"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Volatilite","İşlem hacmi","RSI"]'::jsonb,
    '{"tr":{"title":"Mum grafik","shortDescription":"Bir zaman aralığında açılış, kapanış, en yüksek ve en düşük fiyatı gösteren grafik türüdür.","detailedDescription":"Teknik analizde standart gösterimdir.","howToInterpret":"Tek mum tek başına karar için yeterli değildir; trend ve zaman dilimiyle birlikte okunmalıdır.","commonMistake":"Tek günlük mumdan uzun vadeli yön tahmini yapmak.","example":null,"relatedTerms":["Volatilite","İşlem hacmi","RSI"]},"en":{"title":"Candlestick chart","shortDescription":"A chart showing open, close, high, and low prices for each time interval.","detailedDescription":"The standard display in technical analysis.","howToInterpret":"A single candle is not enough for a decision; read it with trend and timeframe context.","commonMistake":"Inferring long-term direction from one day’s candle.","example":null,"relatedTerms":["Volatility","Trading volume","RSI"]},"de":{"title":"Kerzenchart","shortDescription":"Ein Chart, der Eröffnungs-, Schluss-, Hoch- und Tiefkurse je Zeitintervall zeigt.","detailedDescription":"Die Standarddarstellung in der technischen Analyse.","howToInterpret":"Eine einzelne Kerze reicht für eine Entscheidung nicht aus; lesen Sie sie im Trend- und Zeitrahmen-Kontext.","commonMistake":"Aus einer Tageskerze eine langfristige Richtung abzuleiten.","example":null,"relatedTerms":["Volatilität","Handelsvolumen","RSI"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'NASDAQ',
    'nasdaq',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'MARKET_DATA',
    'ABD’de teknoloji ve büyüme hisselerinin yoğunlaştığı borsa endeks ve piyasasıdır.',
    'Portalda NASDAQ segmenti, Finnhub kaynaklı uluslararası hisse kotasyonlarını listeler; BIST’ten farklı işlem saatleri ve para birimi vardır.',
    'Teknoloji ağırlığı nedeniyle volatilite BIST’e göre farklı olabilir; kur riski TRY yatırımcısı için ayrı boyuttur.',
    NULL,
    NULL,
    FALSE,
    '["NASDAQ","nasdaq","ABD hisse"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Hisse senedi","Volatilite","Piyasa değeri"]'::jsonb,
    '{"tr":{"title":"NASDAQ","shortDescription":"ABD’de teknoloji ve büyüme hisselerinin yoğunlaştığı borsa endeks ve piyasasıdır.","detailedDescription":"Portalda NASDAQ segmenti, Finnhub kaynaklı uluslararası hisse kotasyonlarını listeler; BIST’ten farklı işlem saatleri ve para birimi vardır.","howToInterpret":"Teknoloji ağırlığı nedeniyle volatilite BIST’e göre farklı olabilir; kur riski TRY yatırımcısı için ayrı boyuttur.","commonMistake":null,"example":null,"relatedTerms":["Hisse senedi","Volatilite","Piyasa değeri"]},"en":{"title":"NASDAQ","shortDescription":"A U.S. market and index cluster known for technology and growth stocks.","detailedDescription":"In the portal, the NASDAQ segment lists international quotes; trading hours and currency differ from BIST.","howToInterpret":"Higher tech weight often means different volatility; FX matters for TRY-based investors.","commonMistake":null,"example":null,"relatedTerms":["Stock","Volatility","Market cap"]},"de":{"title":"NASDAQ","shortDescription":"US-Börse und Indexfamilie mit Schwerpunkt Technologie und Wachstumswerten.","detailedDescription":"Im Portal listet das NASDAQ-Segment internationale Kurse; Handelszeiten und Währung unterscheiden sich von BIST.","howToInterpret":"Tech-Gewichtung bringt oft andere Volatilität; FX-Risiko für TRY-Anleger beachten.","commonMistake":null,"example":null,"relatedTerms":["Aktie","Volatilität","Marktkapitalisierung"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Nominal değer',
    'nominal-deger',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'BASIC_FINANCE',
    'Enflasyon düzeltmesi yapılmamış, para birimi cinsinden ifade edilen tutardır.',
    'Nominal tutarlar karşılaştırmayı kolaylaştırır; ancak alım gücü değişimini yansıtmaz.',
    'Nominal artış, alım gücü artışı anlamına gelmeyebilir; reel değerle birlikte okunmalıdır.',
    NULL,
    NULL,
    FALSE,
    '["Nominal değer","nominal"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Reel değer","TÜFE Endeksi","Enflasyon"]'::jsonb,
    '{"tr":{"title":"Nominal değer","shortDescription":"Enflasyon düzeltmesi yapılmamış, para birimi cinsinden ifade edilen tutardır.","detailedDescription":"Nominal tutarlar karşılaştırmayı kolaylaştırır; ancak alım gücü değişimini yansıtmaz.","howToInterpret":"Nominal artış, alım gücü artışı anlamına gelmeyebilir; reel değerle birlikte okunmalıdır.","commonMistake":null,"example":null,"relatedTerms":["Reel değer","TÜFE Endeksi","Enflasyon"]},"en":{"title":"Nominal value","shortDescription":"An amount expressed in currency terms without adjusting for inflation.","detailedDescription":"Nominal figures make comparison straightforward, but they do not reflect changes in purchasing power.","howToInterpret":"A nominal increase does not necessarily mean greater purchasing power; read it alongside real value.","commonMistake":null,"example":null,"relatedTerms":["Real value","CPI index","Inflation"]},"de":{"title":"Nominalwert","shortDescription":"Ein Betrag in Geldeinheiten ohne Inflationsbereinigung.","detailedDescription":"Nominalbeträge erleichtern Vergleiche, spiegeln aber keine Kaufkraftveränderungen wider.","howToInterpret":"Ein nominaler Anstieg bedeutet nicht automatisch mehr Kaufkraft; lesen Sie ihn zusammen mit dem Realwert.","commonMistake":null,"example":null,"relatedTerms":["Realwert","VPI-Index","Inflation"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Nominal getiri',
    'nominal-getiri',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'PORTFOLIO_ANALYSIS',
    'Enflasyon düzeltmesi yapılmadan hesaplanan getiri oranıdır.',
    'Performans kartlarında yaygın gösterilir; reel getiri ile birlikte okunmalıdır.',
    'Pozitif nominal getiri, reel getiri negatif olabilir.',
    NULL,
    NULL,
    FALSE,
    '["Nominal getiri"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Reel getiri","TÜFE Endeksi"]'::jsonb,
    '{"tr":{"title":"Nominal getiri","shortDescription":"Enflasyon düzeltmesi yapılmadan hesaplanan getiri oranıdır.","detailedDescription":"Performans kartlarında yaygın gösterilir; reel getiri ile birlikte okunmalıdır.","howToInterpret":"Pozitif nominal getiri, reel getiri negatif olabilir.","commonMistake":null,"example":null,"relatedTerms":["Reel getiri","TÜFE Endeksi"]},"en":{"title":"Nominal return","shortDescription":"Return calculated without adjusting for inflation.","detailedDescription":"Commonly shown on performance cards and should be read together with real return.","howToInterpret":"Nominal return can be positive while real return is negative.","commonMistake":null,"example":null,"relatedTerms":["Real return","CPI index"]},"de":{"title":"Nominalrendite","shortDescription":"Rendite ohne Bereinigung um Inflation.","detailedDescription":"Häufig auf Performancekarten angezeigt und sollte zusammen mit der Realrendite gelesen werden.","howToInterpret":"Nominalrendite kann positiv sein, während die Realrendite negativ ist.","commonMistake":null,"example":null,"relatedTerms":["Realrendite","VPI-Index"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Ons altın',
    'ons-altin',
    'ACTIVE',
    'ASSET',
    'INTERMEDIATE',
    'MARKET_DATA',
    'Uluslararası piyasalarda troy ons cinsinden işlem gören altın referans fiyatıdır.',
    'Küresel emtia piyasasının temel altın göstergesidir.',
    'Ons fiyatı küresel risk iştahı ve dolar endeksiyle ilişkilidir.',
    NULL,
    NULL,
    FALSE,
    '["Ons altın","altin","ons"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Gram altın","Emtia vadeli işlem"]'::jsonb,
    '{"tr":{"title":"Ons altın","shortDescription":"Uluslararası piyasalarda troy ons cinsinden işlem gören altın referans fiyatıdır.","detailedDescription":"Küresel emtia piyasasının temel altın göstergesidir.","howToInterpret":"Ons fiyatı küresel risk iştahı ve dolar endeksiyle ilişkilidir.","commonMistake":null,"example":null,"relatedTerms":["Gram altın","Emtia vadeli işlem"]},"en":{"title":"Ounce gold","shortDescription":"The international benchmark gold price quoted in troy ounces.","detailedDescription":"The primary global indicator for the gold commodity market.","howToInterpret":"Ounce prices are linked to global risk appetite and the U.S. dollar index.","commonMistake":null,"example":null,"relatedTerms":["Gram gold","Commodity futures"]},"de":{"title":"Unzen-Gold","shortDescription":"Der internationale Referenzpreis für Gold, notiert in Feinunzen.","detailedDescription":"Der wichtigste globale Indikator für den Gold-Rohstoffmarkt.","howToInterpret":"Unzenpreise hängen mit globaler Risikobereitschaft und dem US-Dollar-Index zusammen.","commonMistake":null,"example":null,"relatedTerms":["Gramm-Gold","Rohstoffterminkontrakt"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'P&L',
    'pnl',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'PORTFOLIO_ANALYSIS',
    'Profit & Loss; kâr ve zarar tutarını ifade eden performans göstergesidir.',
    'Hem pozisyon hem portföy düzeyinde izlenir.',
    'Gerçekleşmiş ve gerçekleşmemiş P&L ayrı okunmalıdır.',
    NULL,
    NULL,
    FALSE,
    '["P&L","pnl","kar zarar"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Gerçekleşmiş kar/zarar","Gerçekleşmemiş kar/zarar"]'::jsonb,
    '{"tr":{"title":"P&L","shortDescription":"Profit & Loss; kâr ve zarar tutarını ifade eden performans göstergesidir.","detailedDescription":"Hem pozisyon hem portföy düzeyinde izlenir.","howToInterpret":"Gerçekleşmiş ve gerçekleşmemiş P&L ayrı okunmalıdır.","commonMistake":null,"example":null,"relatedTerms":["Gerçekleşmiş kar/zarar","Gerçekleşmemiş kar/zarar"]},"en":{"title":"P&L","shortDescription":"Profit & Loss; a performance measure showing gain or loss amounts.","detailedDescription":"Tracked at both position and portfolio level.","howToInterpret":"Realized and unrealized P&L should be read separately.","commonMistake":null,"example":null,"relatedTerms":["Realized gain/loss","Unrealized gain/loss"]},"de":{"title":"G&V","shortDescription":"Gewinn und Verlust; eine Kennzahl für Gewinn- oder Verlustbeträge.","detailedDescription":"Wird sowohl auf Positions- als auch auf Portfolioebene verfolgt.","howToInterpret":"Realisierte und unrealisierte G&V sollten getrennt gelesen werden.","commonMistake":null,"example":null,"relatedTerms":["Realisierter Gewinn/Verlust","Unrealisierter Gewinn/Verlust"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Pasta grafik',
    'pasta-grafik',
    'ACTIVE',
    'CHART',
    'BEGINNER',
    'CHARTS',
    'Bütünün parçalara ayrıldığı dairesel dağılım grafiğidir.',
    'Portföy varlık dağılımında kullanılır.',
    'Küçük dilimler pasta grafikte zor okunur; birkaç büyük dilim için uygundur.',
    NULL,
    NULL,
    FALSE,
    '["Pasta grafik"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Varlık dağılımı"]'::jsonb,
    '{"tr":{"title":"Pasta grafik","shortDescription":"Bütünün parçalara ayrıldığı dairesel dağılım grafiğidir.","detailedDescription":"Portföy varlık dağılımında kullanılır.","howToInterpret":"Küçük dilimler pasta grafikte zor okunur; birkaç büyük dilim için uygundur.","commonMistake":null,"example":null,"relatedTerms":["Varlık dağılımı"]},"en":{"title":"Pie chart","shortDescription":"A circular chart that shows how a whole is divided into parts.","detailedDescription":"Used for portfolio asset allocation.","howToInterpret":"Small slices are hard to read; pie charts work best with a few large segments.","commonMistake":null,"example":null,"relatedTerms":["Asset allocation"]},"de":{"title":"Kreisdiagramm","shortDescription":"Ein Kreisdiagramm, das zeigt, wie sich ein Ganzes in Teile aufteilt.","detailedDescription":"Wird für die Portfolio-Vermögensallokation verwendet.","howToInterpret":"Kleine Segmente sind schwer ablesbar; Kreisdiagramme eignen sich für wenige große Anteile.","commonMistake":null,"example":null,"relatedTerms":["Vermögensallokation"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Piyasa değeri',
    'piyasa-degeri',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'MARKET_DATA',
    'Bir şirketin veya varlığın piyasadaki toplam fiyatlandırılmış değeridir.',
    'Hisse için genelde fiyat × dolaşımdaki pay; fon ve emtia için farklı tanımlar kullanılır. Büyüklük, likidite ve risk algısını yansıtır.',
    'Piyasa değeri büyüklüğü tek başına “kaliteli yatırım” demek değildir; büyüme, kârlılık ve borç yapısıyla birlikte okunmalıdır.',
    NULL,
    NULL,
    FALSE,
    '["Piyasa değeri","market cap"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Hisse senedi","Likidite","Getiri"]'::jsonb,
    '{"tr":{"title":"Piyasa değeri","shortDescription":"Bir şirketin veya varlığın piyasadaki toplam fiyatlandırılmış değeridir.","detailedDescription":"Hisse için genelde fiyat × dolaşımdaki pay; fon ve emtia için farklı tanımlar kullanılır. Büyüklük, likidite ve risk algısını yansıtır.","howToInterpret":"Piyasa değeri büyüklüğü tek başına “kaliteli yatırım” demek değildir; büyüme, kârlılık ve borç yapısıyla birlikte okunmalıdır.","commonMistake":null,"example":null,"relatedTerms":["Hisse senedi","Likidite","Getiri"]},"en":{"title":"Market capitalization","shortDescription":"The total market price of a company or asset.","detailedDescription":"For equities, typically share price times shares outstanding. Size affects liquidity, index weight, and risk perception.","howToInterpret":"Large cap does not automatically mean lower risk; combine with fundamentals and volatility.","commonMistake":null,"example":null,"relatedTerms":["Stock","Liquidity","Return"]},"de":{"title":"Marktkapitalisierung","shortDescription":"Der gesamte Marktwert eines Unternehmens oder Vermögenswerts.","detailedDescription":"Bei Aktien meist Kurs mal ausstehende Aktien. Größe beeinflusst Liquidität und Risikowahrnehmung.","howToInterpret":"Große Kapitalisierung allein bedeutet nicht automatisch geringeres Risiko.","commonMistake":null,"example":null,"relatedTerms":["Aktie","Liquidität","Rendite"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Politika faizi',
    'politika-faizi',
    'ACTIVE',
    'MACRO_INDICATOR',
    'BEGINNER',
    'TURKEY_ECONOMY',
    'Merkez bankasının para politikasını yönlendirdiği ana faiz oranıdır.',
    'TCMB’nin enflasyon ve büyüme hedefleri doğrultusunda belirlediği referans orandır.',
    'Politika faizi artışı genelde enflasyonla mücadele; düşüş ise büyümeyi destekleme amacı taşır.',
    NULL,
    NULL,
    FALSE,
    '["Politika faizi","tcmb"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Mevduat faizi","Reel faiz","Faiz"]'::jsonb,
    '{"tr":{"title":"Politika faizi","shortDescription":"Merkez bankasının para politikasını yönlendirdiği ana faiz oranıdır.","detailedDescription":"TCMB’nin enflasyon ve büyüme hedefleri doğrultusunda belirlediği referans orandır.","howToInterpret":"Politika faizi artışı genelde enflasyonla mücadele; düşüş ise büyümeyi destekleme amacı taşır.","commonMistake":null,"example":null,"relatedTerms":["Mevduat faizi","Reel faiz","Faiz"]},"en":{"title":"Policy rate","shortDescription":"The central bank’s main interest rate used to steer monetary policy.","detailedDescription":"The reference rate set by the CBRT in line with inflation and growth objectives.","howToInterpret":"A rate increase usually targets inflation; a cut generally supports growth.","commonMistake":null,"example":null,"relatedTerms":["Deposit rate","Real interest rate","Interest"]},"de":{"title":"Leitzins","shortDescription":"Der zentrale Zinssatz der Zentralbank zur Steuerung der Geldpolitik.","detailedDescription":"Der Referenzzinssatz der TCMB im Einklang mit Inflations- und Wachstumszielen.","howToInterpret":"Eine Zinserhöhung zielt meist auf Inflationsbekämpfung; eine Senkung unterstützt in der Regel das Wachstum.","commonMistake":null,"example":null,"relatedTerms":["Einlagenzins","Realzins","Zins"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Portföy',
    'portfoy',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'PORTFOLIO_ANALYSIS',
    'Bir yatırımcının sahip olduğu varlıkların bütünüdür.',
    'Risk ve getiri hedeflerine göre varlık sınıfları bir arada tutulur.',
    'Portföy performansı tek varlıktan çok, dağılım ve risk yönetimiyle şekillenir.',
    NULL,
    NULL,
    FALSE,
    '["Portföy","portfoy"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Pozisyon","Varlık dağılımı","Çeşitlendirme"]'::jsonb,
    '{"tr":{"title":"Portföy","shortDescription":"Bir yatırımcının sahip olduğu varlıkların bütünüdür.","detailedDescription":"Risk ve getiri hedeflerine göre varlık sınıfları bir arada tutulur.","howToInterpret":"Portföy performansı tek varlıktan çok, dağılım ve risk yönetimiyle şekillenir.","commonMistake":null,"example":null,"relatedTerms":["Pozisyon","Varlık dağılımı","Çeşitlendirme"]},"en":{"title":"Portfolio","shortDescription":"The full set of assets held by an investor.","detailedDescription":"Asset classes are held together according to risk and return goals.","howToInterpret":"Portfolio performance is shaped more by allocation and risk management than by any single asset.","commonMistake":null,"example":null,"relatedTerms":["Position","Asset allocation","Diversification"]},"de":{"title":"Portfolio","shortDescription":"Die Gesamtheit der Vermögenswerte eines Anlegers.","detailedDescription":"Anlageklassen werden entsprechend Risiko- und Renditezielen zusammengehalten.","howToInterpret":"Portfolioleistung wird stärker durch Allokation und Risikomanagement als durch einzelne Werte geprägt.","commonMistake":null,"example":null,"relatedTerms":["Position","Vermögensallokation","Diversifikation"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Portföy reel getiri analizi',
    'portfoy-reel-getiri-analizi',
    'ACTIVE',
    'ANALYSIS_TOOL',
    'INTERMEDIATE',
    'PORTFOLIO_ANALYSIS',
    'Portföy getirisini enflasyona göre düzelterek alım gücü performansını gösterir.',
    'Yüksek enflasyon dönemlerinde portföy değerlendirmesinin merkezinde yer alır.',
    'Nominal pozitif, reel negatif senaryosu yüksek enflasyon dönemlerinde sık görülür.',
    NULL,
    NULL,
    FALSE,
    '["Portföy reel getiri"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Reel getiri","Nominal getiri","TÜFE Endeksi"]'::jsonb,
    '{"tr":{"title":"Portföy reel getiri analizi","shortDescription":"Portföy getirisini enflasyona göre düzelterek alım gücü performansını gösterir.","detailedDescription":"Yüksek enflasyon dönemlerinde portföy değerlendirmesinin merkezinde yer alır.","howToInterpret":"Nominal pozitif, reel negatif senaryosu yüksek enflasyon dönemlerinde sık görülür.","commonMistake":null,"example":null,"relatedTerms":["Reel getiri","Nominal getiri","TÜFE Endeksi"]},"en":{"title":"Portfolio real return analysis","shortDescription":"Shows portfolio performance in purchasing-power terms after inflation adjustment.","detailedDescription":"Central to portfolio review in high-inflation periods.","howToInterpret":"Positive nominal return with negative real return is common in high-inflation environments.","commonMistake":null,"example":null,"relatedTerms":["Real return","Nominal return","CPI index"]},"de":{"title":"Portfolio-Realrendite-Analyse","shortDescription":"Zeigt Portfolioleistung in Kaufkraft nach Inflationsbereinigung.","detailedDescription":"Steht in Hochinflationsphasen im Zentrum der Portfolio-Bewertung.","howToInterpret":"Positive Nominalrendite bei negativer Realrendite ist in Hochinflationsumfeldern häufig.","commonMistake":null,"example":null,"relatedTerms":["Realrendite","Nominalrendite","VPI-Index"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Portföy uyarısı',
    'portfoy-uyarisi',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'NOTIFICATIONS',
    'Portföy değeri veya pozisyon koşullarına bağlı tetiklenen bildirimdir.',
    'Kişisel eşiklerle yapılandırılır.',
    'Eşik değerleri kişisel risk toleransına göre ayarlanmalıdır.',
    NULL,
    NULL,
    FALSE,
    '["Portföy uyarısı"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Fiyat uyarısı","Risk bildirimi"]'::jsonb,
    '{"tr":{"title":"Portföy uyarısı","shortDescription":"Portföy değeri veya pozisyon koşullarına bağlı tetiklenen bildirimdir.","detailedDescription":"Kişisel eşiklerle yapılandırılır.","howToInterpret":"Eşik değerleri kişisel risk toleransına göre ayarlanmalıdır.","commonMistake":null,"example":null,"relatedTerms":["Fiyat uyarısı","Risk bildirimi"]},"en":{"title":"Portfolio alert","shortDescription":"A notification triggered by portfolio value or position conditions.","detailedDescription":"Configured with personal thresholds.","howToInterpret":"Thresholds should match your personal risk tolerance.","commonMistake":null,"example":null,"relatedTerms":["Price alert","Risk notification"]},"de":{"title":"Portfolio-Alarm","shortDescription":"Eine Benachrichtigung, die durch Portfolio- oder Positionsbedingungen ausgelöst wird.","detailedDescription":"Mit persönlichen Schwellenwerten konfigurierbar.","howToInterpret":"Schwellenwerte sollten zur persönlichen Risikotoleranz passen.","commonMistake":null,"example":null,"relatedTerms":["Kursalarm","Risiko-Benachrichtigung"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Pozisyon',
    'pozisyon',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'PORTFOLIO_ANALYSIS',
    'Portföyde belirli bir varlıkta tutulan miktar veya değerdir.',
    'Açık pozisyon piyasa fiyatına bağlı olarak anlık değerlenir.',
    'Pozisyon büyüklüğü portföy riskini doğrudan etkiler.',
    NULL,
    NULL,
    FALSE,
    '["Pozisyon","pozisyon"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Maliyet fiyatı","Güncel değer","P&L"]'::jsonb,
    '{"tr":{"title":"Pozisyon","shortDescription":"Portföyde belirli bir varlıkta tutulan miktar veya değerdir.","detailedDescription":"Açık pozisyon piyasa fiyatına bağlı olarak anlık değerlenir.","howToInterpret":"Pozisyon büyüklüğü portföy riskini doğrudan etkiler.","commonMistake":null,"example":null,"relatedTerms":["Maliyet fiyatı","Güncel değer","P&L"]},"en":{"title":"Position","shortDescription":"The amount or value held in a specific asset within a portfolio.","detailedDescription":"An open position is marked to market based on current prices.","howToInterpret":"Position size directly affects portfolio risk.","commonMistake":null,"example":null,"relatedTerms":["Cost basis","Current value","P&L"]},"de":{"title":"Position","shortDescription":"Die Menge oder der Wert eines bestimmten Vermögenswerts im Portfolio.","detailedDescription":"Eine offene Position wird anhand aktueller Marktpreise bewertet.","howToInterpret":"Die Positionsgröße beeinflusst das Portfoliorisiko unmittelbar.","commonMistake":null,"example":null,"relatedTerms":["Einstandspreis","Aktueller Wert","G&V"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Reel değer',
    'reel-deger',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'BASIC_FINANCE',
    'Enflasyon etkisinden arındırılmış, alım gücü cinsinden ifade edilen tutardır.',
    'Reel değer, paranın gerçekte ne kadar mal ve hizmet satın alabildiğini gösterir.',
    'Reel değer düşüyorsa nominal tutar artsa bile alım gücü zayıflıyor olabilir.',
    'Nominal kazancı doğrudan “gerçek kazanç” sanmak.',
    NULL,
    FALSE,
    '["Reel değer","reel"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Reel getiri","TÜFE Endeksi","Alım gücü"]'::jsonb,
    '{"tr":{"title":"Reel değer","shortDescription":"Enflasyon etkisinden arındırılmış, alım gücü cinsinden ifade edilen tutardır.","detailedDescription":"Reel değer, paranın gerçekte ne kadar mal ve hizmet satın alabildiğini gösterir.","howToInterpret":"Reel değer düşüyorsa nominal tutar artsa bile alım gücü zayıflıyor olabilir.","commonMistake":"Nominal kazancı doğrudan “gerçek kazanç” sanmak.","example":null,"relatedTerms":["Reel getiri","TÜFE Endeksi","Alım gücü"]},"en":{"title":"Real value","shortDescription":"An amount expressed in purchasing-power terms after removing inflation effects.","detailedDescription":"Real value shows how much goods and services money can actually buy.","howToInterpret":"If real value is falling, purchasing power may be weakening even when the nominal amount rises.","commonMistake":"Treating nominal gains as true gains in wealth.","example":null,"relatedTerms":["Real return","CPI index","Purchasing power"]},"de":{"title":"Realwert","shortDescription":"Ein Betrag in Kaufkraft ausgedrückt, bereinigt um Inflationseffekte.","detailedDescription":"Der Realwert zeigt, wie viele Güter und Dienstleistungen Geld tatsächlich kaufen kann.","howToInterpret":"Sinkt der Realwert, kann die Kaufkraft schwächer werden, auch wenn der Nominalbetrag steigt.","commonMistake":"Nominalgewinne direkt als echten Vermögenszuwachs zu betrachten.","example":null,"relatedTerms":["Realrendite","VPI-Index","Kaufkraft"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Reel faiz',
    'reel-faiz',
    'ACTIVE',
    'MACRO_INDICATOR',
    'INTERMEDIATE',
    'TURKEY_ECONOMY',
    'Nominal faiz oranından enflasyon oranının çıkarılmasıyla elde edilen faizdir.',
    'Nakit ve mevduatın alım gücü açısından gerçek getirisini gösterir.',
    'Reel faiz negatifse nakit ve mevduat alım gücü kaybediyor olabilir.',
    NULL,
    NULL,
    FALSE,
    '["Reel faiz","reel"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Politika faizi","TÜFE Yıllık Oran"]'::jsonb,
    '{"tr":{"title":"Reel faiz","shortDescription":"Nominal faiz oranından enflasyon oranının çıkarılmasıyla elde edilen faizdir.","detailedDescription":"Nakit ve mevduatın alım gücü açısından gerçek getirisini gösterir.","howToInterpret":"Reel faiz negatifse nakit ve mevduat alım gücü kaybediyor olabilir.","commonMistake":null,"example":null,"relatedTerms":["Politika faizi","TÜFE Yıllık Oran"]},"en":{"title":"Real interest rate","shortDescription":"The nominal interest rate minus the inflation rate.","detailedDescription":"Shows the true return on cash and deposits in purchasing-power terms.","howToInterpret":"A negative real rate may mean cash and deposits are losing purchasing power.","commonMistake":null,"example":null,"relatedTerms":["Policy rate","CPI annual rate"]},"de":{"title":"Realzins","shortDescription":"Der Nominalzinssatz abzüglich der Inflationsrate.","detailedDescription":"Zeigt die tatsächliche Rendite von Bargeld und Einlagen in Kaufkraft.","howToInterpret":"Ein negativer Realzins kann bedeuten, dass Bargeld und Einlagen Kaufkraft verlieren.","commonMistake":null,"example":null,"relatedTerms":["Leitzins","VPI-Jahresrate"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Reel getiri',
    'reel-getiri',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'PORTFOLIO_ANALYSIS',
    'Nominal getirinin enflasyon etkisinden arındırılmış halidir.',
    'Yatırımın gerçek alım gücü açısından performansını gösterir. Yüksek enflasyon dönemlerinde kritik ölçüdür.',
    'Reel getiri negatifse portföy değeri artmış olsa bile alım gücü düşmüş olabilir.',
    'Nominal kazancı doğrudan başarı sanmak.',
    NULL,
    FALSE,
    '["Reel getiri","reel"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["TÜFE Endeksi","Nominal getiri","Alım gücü"]'::jsonb,
    '{"tr":{"title":"Reel getiri","shortDescription":"Nominal getirinin enflasyon etkisinden arındırılmış halidir.","detailedDescription":"Yatırımın gerçek alım gücü açısından performansını gösterir. Yüksek enflasyon dönemlerinde kritik ölçüdür.","howToInterpret":"Reel getiri negatifse portföy değeri artmış olsa bile alım gücü düşmüş olabilir.","commonMistake":"Nominal kazancı doğrudan başarı sanmak.","example":null,"relatedTerms":["TÜFE Endeksi","Nominal getiri","Alım gücü"]},"en":{"title":"Real return","shortDescription":"Nominal return adjusted for inflation.","detailedDescription":"Shows investment performance in terms of actual purchasing power. Critical in high-inflation periods.","howToInterpret":"If real return is negative, purchasing power may have fallen even when portfolio value rose.","commonMistake":"Treating nominal gains as success without checking inflation.","example":null,"relatedTerms":["CPI index","Nominal return","Purchasing power"]},"de":{"title":"Realrendite","shortDescription":"Nominalrendite bereinigt um Inflationseffekte.","detailedDescription":"Zeigt die Anlageperformance in tatsächlicher Kaufkraft. In Hochinflationsphasen besonders wichtig.","howToInterpret":"Ist die Realrendite negativ, kann die Kaufkraft gesunken sein, auch wenn der Portfoliowert stieg.","commonMistake":"Nominalgewinne direkt als Erfolg zu werten, ohne Inflation zu prüfen.","example":null,"relatedTerms":["VPI-Index","Nominalrendite","Kaufkraft"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Referans çizgisi',
    'referans-cizgisi',
    'ACTIVE',
    'CHART',
    'INTERMEDIATE',
    'CHARTS',
    'Grafikte karşılaştırma için eklenen yatay veya eğimli yardımcı çizgidir.',
    'Analiz ekranında çizim araçlarıyla eklenir.',
    'Destek/direnç veya hedef seviye olarak kullanılır; subjektif yorum içerebilir.',
    NULL,
    NULL,
    FALSE,
    '["Referans çizgisi"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Destek ve direnç","Mum grafik"]'::jsonb,
    '{"tr":{"title":"Referans çizgisi","shortDescription":"Grafikte karşılaştırma için eklenen yatay veya eğimli yardımcı çizgidir.","detailedDescription":"Analiz ekranında çizim araçlarıyla eklenir.","howToInterpret":"Destek/direnç veya hedef seviye olarak kullanılır; subjektif yorum içerebilir.","commonMistake":null,"example":null,"relatedTerms":["Destek ve direnç","Mum grafik"]},"en":{"title":"Reference line","shortDescription":"A horizontal or sloped helper line added to a chart for comparison.","detailedDescription":"Added with drawing tools on analysis screens.","howToInterpret":"Used as support/resistance or target levels; interpretation can be subjective.","commonMistake":null,"example":null,"relatedTerms":["Support and resistance","Candlestick chart"]},"de":{"title":"Referenzlinie","shortDescription":"Eine horizontale oder geneigte Hilfslinie im Chart zum Vergleich.","detailedDescription":"Wird in Analyseansichten mit Zeichenwerkzeugen ergänzt.","howToInterpret":"Wird als Unterstützung/Widerstand oder Zielniveau genutzt; die Deutung kann subjektiv sein.","commonMistake":null,"example":null,"relatedTerms":["Unterstützung und Widerstand","Kerzenchart"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Repo',
    'repo',
    'ACTIVE',
    'TERM',
    'ADVANCED',
    'BASIC_FINANCE',
    'Menkul kıymet teminatlı kısa vadeli borçlanma işlemi; merkez bankası likidite aracıdır.',
    'Politika faizi ile ilişkili piyasa faizlerini etkiler; bankalar ve fonlar için önemli para piyasası aracıdır.',
    'Repo faizi yükseliyorsa likidite sıkılaşıyor olabilir; ters repo yatırımcı tarafındadır.',
    NULL,
    NULL,
    FALSE,
    '["Repo","ters repo"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Politika faizi","Faiz","Likidite"]'::jsonb,
    '{"tr":{"title":"Repo","shortDescription":"Menkul kıymet teminatlı kısa vadeli borçlanma işlemi; merkez bankası likidite aracıdır.","detailedDescription":"Politika faizi ile ilişkili piyasa faizlerini etkiler; bankalar ve fonlar için önemli para piyasası aracıdır.","howToInterpret":"Repo faizi yükseliyorsa likidite sıkılaşıyor olabilir; ters repo yatırımcı tarafındadır.","commonMistake":null,"example":null,"relatedTerms":["Politika faizi","Faiz","Likidite"]},"en":{"title":"Repo","shortDescription":"Short-term collateralized borrowing in money markets; used for liquidity management.","detailedDescription":"Repo rates interact with policy rates and bank funding conditions.","howToInterpret":"Rising repo rates can signal tighter liquidity; reverse repo is the investor side.","commonMistake":null,"example":null,"relatedTerms":["Policy rate","Interest rate","Liquidity"]},"de":{"title":"Repo","shortDescription":"Kurzfristige besicherte Geldmarktfinanzierung; Liquiditätsinstrument.","detailedDescription":"Repo-Sätze hängen mit Leitzins und Bankliquidität zusammen.","howToInterpret":"Steigende Repo-Sätze können Liquiditätsengpässe signalisieren.","commonMistake":null,"example":null,"relatedTerms":["Leitzins","Zins","Liquidität"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Risk',
    'risk',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'BASIC_FINANCE',
    'Beklenen getirinin gerçekleşmeme veya kayıpla sonuçlanma olasılığıdır.',
    'Risk; volatilite, likidite ve kredi riski gibi boyutlarda ölçülür. Portföy dağılımı riski yönetmenin temel aracıdır.',
    'Yüksek risk potansiyel olarak daha yüksek getiri sunabilir; ancak dalgalanma ve kayıp olasılığı da artar.',
    'Riski yalnızca “kaybetme” olarak görmek; likidite ve kur riskini ihmal etmek.',
    NULL,
    FALSE,
    '["Risk","risk"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Volatilite","Likidite","Portföy"]'::jsonb,
    '{"tr":{"title":"Risk","shortDescription":"Beklenen getirinin gerçekleşmeme veya kayıpla sonuçlanma olasılığıdır.","detailedDescription":"Risk; volatilite, likidite ve kredi riski gibi boyutlarda ölçülür. Portföy dağılımı riski yönetmenin temel aracıdır.","howToInterpret":"Yüksek risk potansiyel olarak daha yüksek getiri sunabilir; ancak dalgalanma ve kayıp olasılığı da artar.","commonMistake":"Riski yalnızca “kaybetme” olarak görmek; likidite ve kur riskini ihmal etmek.","example":null,"relatedTerms":["Volatilite","Likidite","Portföy"]},"en":{"title":"Risk","shortDescription":"The likelihood that expected returns will not materialize or will result in a loss.","detailedDescription":"Risk is measured across dimensions such as volatility, liquidity, and credit risk. Portfolio allocation is the primary tool for managing it.","howToInterpret":"Higher risk may offer higher potential return, but it also increases the chance of swings and losses.","commonMistake":"Seeing risk only as “losing money” and overlooking liquidity and currency risk.","example":null,"relatedTerms":["Volatility","Liquidity","Portfolio"]},"de":{"title":"Risiko","shortDescription":"Die Wahrscheinlichkeit, dass erwartete Renditen ausbleiben oder zu Verlusten führen.","detailedDescription":"Risiko wird in Dimensionen wie Volatilität, Liquidität und Kreditrisiko gemessen. Die Portfolioallokation ist das wichtigste Steuerungsinstrument.","howToInterpret":"Höheres Risiko kann höhere Renditechancen bieten, erhöht aber auch Schwankungen und Verlustwahrscheinlichkeit.","commonMistake":"Risiko nur als „Geldverlust“ zu sehen und Liquiditäts- sowie Währungsrisiko zu ignorieren.","example":null,"relatedTerms":["Volatilität","Liquidität","Portfolio"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Risk bildirimi',
    'risk-bildirimi',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'NOTIFICATIONS',
    'Olağandışı volatilite veya risk eşiği aşıldığında gönderilen uyarıdır.',
    'Risk yönetimi hatırlatıcısıdır.',
    'Bildirim risk yönetimi için hatırlatıcıdır; otomatik işlem yapmaz.',
    NULL,
    NULL,
    FALSE,
    '["Risk bildirimi"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Volatilite","Risk"]'::jsonb,
    '{"tr":{"title":"Risk bildirimi","shortDescription":"Olağandışı volatilite veya risk eşiği aşıldığında gönderilen uyarıdır.","detailedDescription":"Risk yönetimi hatırlatıcısıdır.","howToInterpret":"Bildirim risk yönetimi için hatırlatıcıdır; otomatik işlem yapmaz.","commonMistake":null,"example":null,"relatedTerms":["Volatilite","Risk"]},"en":{"title":"Risk notification","shortDescription":"An alert sent when unusual volatility or a risk threshold is exceeded.","detailedDescription":"A reminder for risk management.","howToInterpret":"The notification is a risk-management reminder; it does not execute trades.","commonMistake":null,"example":null,"relatedTerms":["Volatility","Risk"]},"de":{"title":"Risiko-Benachrichtigung","shortDescription":"Ein Alarm bei ungewöhnlicher Volatilität oder überschrittenen Risikoschwellen.","detailedDescription":"Eine Erinnerung für Risikomanagement.","howToInterpret":"Die Benachrichtigung erinnert an Risikomanagement; sie führt keine Trades aus.","commonMistake":null,"example":null,"relatedTerms":["Volatilität","Risiko"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'RSI (14)',
    'rsi-14',
    'ACTIVE',
    'ANALYSIS_TOOL',
    'INTERMEDIATE',
    'CHARTS',
    'Göreceli güç endeksi; fiyat momentumunun aşırı alım/satım bölgelerini özetler.',
    'Analiz grafiğinde 14 periyot RSI, kapanış fiyatlarından türetilir. 0–100 skalasında; 70 üzeri aşırı alım, 30 altı aşırı satım bölgesi olarak yorumlanır (tek başına sinyal değildir).',
    'Trend güçlüyken RSI uzun süre aşırı alımda kalabilir; tek göstergeyle işlem kararı vermeyin, fiyat yapısıyla teyit edin.',
    'RSI 70’i otomatik “sat” sinyali sanmak.',
    NULL,
    FALSE,
    '["RSI","teknik analiz"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Teknik gösterge","Mum grafik","Volatilite"]'::jsonb,
    '{"tr":{"title":"RSI (14)","shortDescription":"Göreceli güç endeksi; fiyat momentumunun aşırı alım/satım bölgelerini özetler.","detailedDescription":"Analiz grafiğinde 14 periyot RSI, kapanış fiyatlarından türetilir. 0–100 skalasında; 70 üzeri aşırı alım, 30 altı aşırı satım bölgesi olarak yorumlanır (tek başına sinyal değildir).","howToInterpret":"Trend güçlüyken RSI uzun süre aşırı alımda kalabilir; tek göstergeyle işlem kararı vermeyin, fiyat yapısıyla teyit edin.","commonMistake":"RSI 70’i otomatik “sat” sinyali sanmak.","example":null,"relatedTerms":["Teknik gösterge","Mum grafik","Volatilite"]},"en":{"title":"RSI (14)","shortDescription":"Relative Strength Index summarizing momentum and overbought/oversold zones.","detailedDescription":"14-period RSI on the analysis chart is derived from closes. Above 70 often labeled overbought, below 30 oversold—not standalone signals.","howToInterpret":"In strong trends RSI can stay overbought; confirm with price structure, not RSI alone.","commonMistake":"Treating RSI 70 as an automatic sell signal.","example":null,"relatedTerms":["Technical indicator","Candlestick chart","Volatility"]},"de":{"title":"RSI (14)","shortDescription":"Relative-Strength-Index: fasst Momentum und überkauft/überverkauft zusammen.","detailedDescription":"14-Perioden-RSI auf dem Analysechart basiert auf Schlusskursen. Über 70/ unter 30 sind Zonen, keine alleinigen Signale.","howToInterpret":"In starken Trends kann RSI lange überkauft bleiben; mit Kursstruktur bestätigen.","commonMistake":"RSI 70 automatisch als Verkaufssignal interpretieren.","example":null,"relatedTerms":["Technischer Indikator","Kerzenchart","Volatilität"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Sparkline',
    'sparkline',
    'ACTIVE',
    'CHART',
    'BEGINNER',
    'CHARTS',
    'Tablo hücresi içindeki küçük mini grafik; kısa dönem fiyat yönünü özetler.',
    'Piyasalar listesindeki trend çizgisi tam geçmiş yerine özet momentum gösterir; ayrıntı için tam grafiğe gidin.',
    'Yön ve eğim önemlidir; eksen ölçeği olmadığı için mutlak fiyat seviyesi okunmaz.',
    NULL,
    NULL,
    FALSE,
    '["Sparkline","mini grafik"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Trend skoru","Çizgi grafik","Dönem getirisi"]'::jsonb,
    '{"tr":{"title":"Sparkline","shortDescription":"Tablo hücresi içindeki küçük mini grafik; kısa dönem fiyat yönünü özetler.","detailedDescription":"Piyasalar listesindeki trend çizgisi tam geçmiş yerine özet momentum gösterir; ayrıntı için tam grafiğe gidin.","howToInterpret":"Yön ve eğim önemlidir; eksen ölçeği olmadığı için mutlak fiyat seviyesi okunmaz.","commonMistake":null,"example":null,"relatedTerms":["Trend skoru","Çizgi grafik","Dönem getirisi"]},"en":{"title":"Sparkline","shortDescription":"Small in-cell chart summarizing recent price direction.","detailedDescription":"Trend lines in market tables compress history; open the full chart for detail.","howToInterpret":"Read slope and direction; absolute levels are not shown without axes.","commonMistake":null,"example":null,"relatedTerms":["Trend score","Line chart","Period return"]},"de":{"title":"Sparkline","shortDescription":"Kleines Mini-Diagramm in Tabellenzellen für Kurzzeit-Trend.","detailedDescription":"Komprimiert Verlauf; für Details vollständigen Chart öffnen.","howToInterpret":"Richtung und Steigung lesen; keine absoluten Preisniveaus.","commonMistake":null,"example":null,"relatedTerms":["Trend-Score","Liniendiagramm","Periodenrendite"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Spread',
    'spread',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'BASIC_FINANCE',
    'Alış ve satış fiyatı arasındaki farktır.',
    'Spread, işlem maliyetinin önemli parçasıdır; banka kurları ve borsa emirlerinde görülür.',
    'Spread genişledikçe işlem maliyeti artar; dar spread daha likit piyasa işaretidir.',
    'Yalnızca orta fiyata bakıp spread maliyetini hesaba katmamak.',
    NULL,
    FALSE,
    '["Spread","spread"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Likidite","Döviz","Efektif kur"]'::jsonb,
    '{"tr":{"title":"Spread","shortDescription":"Alış ve satış fiyatı arasındaki farktır.","detailedDescription":"Spread, işlem maliyetinin önemli parçasıdır; banka kurları ve borsa emirlerinde görülür.","howToInterpret":"Spread genişledikçe işlem maliyeti artar; dar spread daha likit piyasa işaretidir.","commonMistake":"Yalnızca orta fiyata bakıp spread maliyetini hesaba katmamak.","example":null,"relatedTerms":["Likidite","Döviz","Efektif kur"]},"en":{"title":"Spread","shortDescription":"The difference between the bid and ask price.","detailedDescription":"The spread is a major part of transaction cost and appears in bank FX rates and exchange order books.","howToInterpret":"A wider spread raises transaction cost; a tighter spread usually signals a more liquid market.","commonMistake":"Looking only at the mid price and ignoring spread cost.","example":null,"relatedTerms":["Liquidity","Foreign exchange","Effective rate"]},"de":{"title":"Spread","shortDescription":"Die Differenz zwischen Geld- und Briefkurs.","detailedDescription":"Der Spread ist ein wesentlicher Teil der Transaktionskosten und erscheint bei Bankkursen und Börsenorderbüchern.","howToInterpret":"Ein breiterer Spread erhöht die Transaktionskosten; ein enger Spread deutet meist auf einen liquideren Markt hin.","commonMistake":"Nur den Mittelkurs zu betrachten und Spread-Kosten zu ignorieren.","example":null,"relatedTerms":["Liquidität","Devisen","Effektiver Kurs"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Stablecoin',
    'stablecoin',
    'ACTIVE',
    'ASSET',
    'INTERMEDIATE',
    'MARKET_DATA',
    'Değeri genelde bir fiat para birimine (ör. USD) sabitlenmek üzere tasarlanmış kripto varlıktır.',
    'USDT gibi stablecoin’ler işlem çiftlerinde likidite sağlar; ancak rezerv ve düzenleyici riskleri vardır.',
    '“1 USD’ye sabit” hedefi her zaman korunmayabilir; issuer ve denetim kalitesini ayırt edin.',
    NULL,
    NULL,
    FALSE,
    '["Stablecoin","USDT"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Kripto varlık","Likidite","Risk"]'::jsonb,
    '{"tr":{"title":"Stablecoin","shortDescription":"Değeri genelde bir fiat para birimine (ör. USD) sabitlenmek üzere tasarlanmış kripto varlıktır.","detailedDescription":"USDT gibi stablecoin’ler işlem çiftlerinde likidite sağlar; ancak rezerv ve düzenleyici riskleri vardır.","howToInterpret":"“1 USD’ye sabit” hedefi her zaman korunmayabilir; issuer ve denetim kalitesini ayırt edin.","commonMistake":null,"example":null,"relatedTerms":["Kripto varlık","Likidite","Risk"]},"en":{"title":"Stablecoin","shortDescription":"Crypto asset designed to track a fiat currency such as USD.","detailedDescription":"Pairs like USDT/USDT provide liquidity in crypto markets; reserve and regulatory risks remain.","howToInterpret":"The peg can break; assess issuer transparency and audits.","commonMistake":null,"example":null,"relatedTerms":["Crypto asset","Liquidity","Risk"]},"de":{"title":"Stablecoin","shortDescription":"Krypto-Asset, das an eine Fiatwährung wie USD gekoppelt sein soll.","detailedDescription":"USDT u. a. liefern Liquidität; Reserve- und Regulierungsrisiken bleiben.","howToInterpret":"Peg kann brechen; Emittent und Transparenz prüfen.","commonMistake":null,"example":null,"relatedTerms":["Krypto-Asset","Liquidität","Risiko"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Tahvil',
    'tahvil',
    'ACTIVE',
    'ASSET',
    'INTERMEDIATE',
    'MARKET_DATA',
    'Devlet veya şirketlerin borçlanmak için ihraç ettiği sabit getirili menkul kıymettir.',
    'Tahvil yatırımcısı ihraççıya borç verir; kupon ve vade yapısı getiriyi belirler.',
    'Tahvil fiyatı ile getirisi ters yönlü hareket eder; faizler yükselirse fiyat düşebilir.',
    NULL,
    NULL,
    FALSE,
    '["Tahvil","tahvil"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Hazine bonosu","Kupon","Tahvil getirisi"]'::jsonb,
    '{"tr":{"title":"Tahvil","shortDescription":"Devlet veya şirketlerin borçlanmak için ihraç ettiği sabit getirili menkul kıymettir.","detailedDescription":"Tahvil yatırımcısı ihraççıya borç verir; kupon ve vade yapısı getiriyi belirler.","howToInterpret":"Tahvil fiyatı ile getirisi ters yönlü hareket eder; faizler yükselirse fiyat düşebilir.","commonMistake":null,"example":null,"relatedTerms":["Hazine bonosu","Kupon","Tahvil getirisi"]},"en":{"title":"Bond","shortDescription":"A fixed-income security issued by governments or companies to raise debt.","detailedDescription":"A bond investor lends to the issuer; coupon structure and maturity determine the return profile.","howToInterpret":"Bond price and yield move in opposite directions; when rates rise, prices may fall.","commonMistake":null,"example":null,"relatedTerms":["Treasury bill","Coupon","Bond yield"]},"de":{"title":"Anleihe","shortDescription":"Ein festverzinsliches Wertpapier, das von Staaten oder Unternehmen zur Schuldenaufnahme emittiert wird.","detailedDescription":"Ein Anleiheinvestor leiht dem Emittenten Geld; Kuponstruktur und Laufzeit bestimmen das Renditeprofil.","howToInterpret":"Anleihepreis und -rendite bewegen sich gegenläufig; steigen die Zinsen, können die Preise fallen.","commonMistake":null,"example":null,"relatedTerms":["Schatzanweisung","Kupon","Anleiherendite"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Tahvil getirisi',
    'tahvil-getirisi',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'MARKET_DATA',
    'Tahvil yatırımcısının elde ettiği getiri; fiyat hareketi ve kupon ödemelerinden oluşur.',
    'Faizler yükseldiğinde tahvil fiyatı düşer, getiri yükselir (ters ilişki). Eurobond ve devlet tahvillerinde kur riski ayrı boyuttur.',
    'Getiriyi yıllıklandırılmış (yield) gösterimle okuyun; fiyat dalgalanması kısa vadede P&L’i etkiler.',
    NULL,
    NULL,
    FALSE,
    '["Tahvil getirisi","yield"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Tahvil","Kupon","Faiz","Eurobond"]'::jsonb,
    '{"tr":{"title":"Tahvil getirisi","shortDescription":"Tahvil yatırımcısının elde ettiği getiri; fiyat hareketi ve kupon ödemelerinden oluşur.","detailedDescription":"Faizler yükseldiğinde tahvil fiyatı düşer, getiri yükselir (ters ilişki). Eurobond ve devlet tahvillerinde kur riski ayrı boyuttur.","howToInterpret":"Getiriyi yıllıklandırılmış (yield) gösterimle okuyun; fiyat dalgalanması kısa vadede P&L’i etkiler.","commonMistake":null,"example":null,"relatedTerms":["Tahvil","Kupon","Faiz","Eurobond"]},"en":{"title":"Bond yield","shortDescription":"Return to a bond investor from price moves and coupon payments.","detailedDescription":"When rates rise, bond prices usually fall and yields rise (inverse link). FX adds a layer for USD bonds.","howToInterpret":"Focus on annualized yield; short-term price swings affect mark-to-market P&L.","commonMistake":null,"example":null,"relatedTerms":["Bond","Coupon","Interest rate"]},"de":{"title":"Anleiherendite","shortDescription":"Rendite für Anleiheinvestoren aus Kursbewegung und Kuponzahlungen.","detailedDescription":"Steigende Zinsen drücken meist Kurse und erhöhen Renditen. FX-Risiko bei USD-Anleihen.","howToInterpret":"Annualisierte Rendite betrachten; kurzfristige Kursschwankungen beeinflussen P&L.","commonMistake":null,"example":null,"relatedTerms":["Anleihe","Kupon","Zins"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'TCMB kuru ve serbest piyasa',
    'tcmb-vs-serbest-piyasa',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'MARKET_DATA',
    'Resmi referans kurlar ile banka/serbest piyasa efektif kurları arasındaki farktır.',
    'TCMB kurları politika ve resmi işlemlerde referans olabilir; günlük işlemde efektif alış-satış kurları geçerlidir.',
    'Makas genişlediğinde arbitraj veya likidite stresi olabilir; tek kur satırına güvenmeyin.',
    NULL,
    NULL,
    FALSE,
    '["TCMB","serbest piyasa","kur"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Döviz","Efektif kur","Spread"]'::jsonb,
    '{"tr":{"title":"TCMB kuru ve serbest piyasa","shortDescription":"Resmi referans kurlar ile banka/serbest piyasa efektif kurları arasındaki farktır.","detailedDescription":"TCMB kurları politika ve resmi işlemlerde referans olabilir; günlük işlemde efektif alış-satış kurları geçerlidir.","howToInterpret":"Makas genişlediğinde arbitraj veya likidite stresi olabilir; tek kur satırına güvenmeyin.","commonMistake":null,"example":null,"relatedTerms":["Döviz","Efektif kur","Spread"]},"en":{"title":"CBRT vs market FX rates","shortDescription":"Difference between official central bank rates and market/bank effective rates.","detailedDescription":"CBRT rates are references; everyday trading uses bank effective bid/ask.","howToInterpret":"Wide gaps may signal stress; do not rely on a single quote.","commonMistake":null,"example":null,"relatedTerms":["Foreign exchange","Effective rate","Spread"]},"de":{"title":"ZBTR-Kurs vs Marktkurs","shortDescription":"Unterschied zwischen offiziellen Zentralbankkursen und Markt-/Bankenkursen.","detailedDescription":"ZBTR-Kurse sind Referenz; Handel nutzt effektive Geld-/Briefkurse.","howToInterpret":"Große Spreads können Stress signalisieren.","commonMistake":null,"example":null,"relatedTerms":["Wechselkurs","Effektiver Kurs","Spread"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'TEFAS',
    'tefas',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'MARKET_DATA',
    'Türkiye’de yatırım fonlarının fiyat, performans ve bilgilerinin merkezi takip platformudur.',
    'Fon kodları ve günlük NAV verileri fon karşılaştırmalarının temelidir; yönetim ücreti ve risk profili fon izahnamesinde yer alır.',
    'Aynı kategorideki fonları NAV değişimi ve volatilite ile kıyaslayın; geçmiş performans geleceği garanti etmez.',
    NULL,
    NULL,
    FALSE,
    '["TEFAS","fon","NAV"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Fon","Getiri","Risk"]'::jsonb,
    '{"tr":{"title":"TEFAS","shortDescription":"Türkiye’de yatırım fonlarının fiyat, performans ve bilgilerinin merkezi takip platformudur.","detailedDescription":"Fon kodları ve günlük NAV verileri fon karşılaştırmalarının temelidir; yönetim ücreti ve risk profili fon izahnamesinde yer alır.","howToInterpret":"Aynı kategorideki fonları NAV değişimi ve volatilite ile kıyaslayın; geçmiş performans geleceği garanti etmez.","commonMistake":null,"example":null,"relatedTerms":["Fon","Getiri","Risk"]},"en":{"title":"TEFAS","shortDescription":"Turkey’s central platform for mutual fund prices, performance, and disclosures.","detailedDescription":"Fund codes and daily NAV are core to comparisons; fees and risk profile are in the fund prospectus.","howToInterpret":"Compare peers by NAV change and volatility; past performance is not a guarantee.","commonMistake":null,"example":null,"relatedTerms":["Fund","Return","Risk"]},"de":{"title":"TEFAS","shortDescription":"Zentrale türkische Plattform für Fondspreise, Performance und Fondsdaten.","detailedDescription":"Fondscodes und täglicher NAV sind Grundlage für Vergleiche; Gebühren stehen im Prospekt.","howToInterpret":"Peer-Fonds über NAV-Entwicklung und Volatilität vergleichen.","commonMistake":null,"example":null,"relatedTerms":["Fonds","Rendite","Risiko"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Teknik gösterge',
    'teknik-gosterge',
    'ACTIVE',
    'ANALYSIS_TOOL',
    'INTERMEDIATE',
    'CHARTS',
    'Fiyat ve hacim verisinden türetilen, grafik üzerinde okunan analiz araçlarıdır.',
    'RSI, hareketli ortalama ve benzeri göstergeler geçmiş veriye dayanır; geleceği garanti etmez, karar destek sağlar.',
    'Birden fazla gösterge ve zaman dilimi birlikte kullanılmalı; tek göstergeye aşırı güvenmeyin.',
    NULL,
    NULL,
    FALSE,
    '["Teknik gösterge","indicator"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["RSI (14)","Mum grafik","Destek ve direnç"]'::jsonb,
    '{"tr":{"title":"Teknik gösterge","shortDescription":"Fiyat ve hacim verisinden türetilen, grafik üzerinde okunan analiz araçlarıdır.","detailedDescription":"RSI, hareketli ortalama ve benzeri göstergeler geçmiş veriye dayanır; geleceği garanti etmez, karar destek sağlar.","howToInterpret":"Birden fazla gösterge ve zaman dilimi birlikte kullanılmalı; tek göstergeye aşırı güvenmeyin.","commonMistake":null,"example":null,"relatedTerms":["RSI (14)","Mum grafik","Destek ve direnç"]},"en":{"title":"Technical indicator","shortDescription":"Analytics derived from price and volume, plotted on charts.","detailedDescription":"Tools like RSI and moving averages use past data; they support decisions but do not guarantee outcomes.","howToInterpret":"Combine multiple indicators and time frames; avoid single-indicator trading.","commonMistake":null,"example":null,"relatedTerms":["RSI (14)","Candlestick chart","Support and resistance"]},"de":{"title":"Technischer Indikator","shortDescription":"Aus Preis und Volumen abgeleitete Analysewerkzeuge im Chart.","detailedDescription":"RSI und gleitende Durchschnitte nutzen Vergangenheitsdaten; sie unterstützen, garantieren nichts.","howToInterpret":"Mehrere Indikatoren und Zeitebenen kombinieren.","commonMistake":null,"example":null,"relatedTerms":["RSI (14)","Kerzenchart","Unterstützung und Widerstand"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Temettü',
    'temettu',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'MARKET_DATA',
    'Şirketin kârından hissedarlara nakit veya bedelsiz pay olarak dağıttığı paydır.',
    'Temettü verimi = temettü / hisse fiyatı; büyüme hisseleri düşük temettü ödeyebilir. Ex-date sonrası fiyat düzeltmesi normaldir.',
    'Sürdürülebilir temettü için nakit akışı ve borç seviyesine bakın; tek seferlik yüksek temettüye kanmayın.',
    NULL,
    NULL,
    FALSE,
    '["Temettü","dividend"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Hisse senedi","Getiri","Piyasa değeri"]'::jsonb,
    '{"tr":{"title":"Temettü","shortDescription":"Şirketin kârından hissedarlara nakit veya bedelsiz pay olarak dağıttığı paydır.","detailedDescription":"Temettü verimi = temettü / hisse fiyatı; büyüme hisseleri düşük temettü ödeyebilir. Ex-date sonrası fiyat düzeltmesi normaldir.","howToInterpret":"Sürdürülebilir temettü için nakit akışı ve borç seviyesine bakın; tek seferlik yüksek temettüye kanmayın.","commonMistake":null,"example":null,"relatedTerms":["Hisse senedi","Getiri","Piyasa değeri"]},"en":{"title":"Dividend","shortDescription":"Cash or stock distributions of company profits to shareholders.","detailedDescription":"Dividend yield = dividend / price; growth names may pay little. Price often adjusts on ex-date.","howToInterpret":"Check cash flow and debt for sustainability; beware one-off specials.","commonMistake":null,"example":null,"relatedTerms":["Stock","Return","Market cap"]},"de":{"title":"Dividende","shortDescription":"Ausschüttung von Gewinnen an Aktionäre in Cash oder Aktien.","detailedDescription":"Dividendenrendite = Dividende / Kurs; Wachstumswerte zahlen oft wenig.","howToInterpret":"Cashflow und Verschuldung für Nachhaltigkeit prüfen.","commonMistake":null,"example":null,"relatedTerms":["Aktie","Rendite","Marktkapitalisierung"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Tooltip',
    'tooltip',
    'ACTIVE',
    'CHART',
    'BEGINNER',
    'CHARTS',
    'Grafik üzerinde imleçle görünen anlık veri ipucu kutusudur.',
    'Kesin tarih ve fiyat okuması sağlar.',
    'Tarih, fiyat ve hacim gibi kesin değerleri nokta bazında okumayı sağlar.',
    NULL,
    NULL,
    FALSE,
    '["Tooltip","tooltip"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Çizgi grafik"]'::jsonb,
    '{"tr":{"title":"Tooltip","shortDescription":"Grafik üzerinde imleçle görünen anlık veri ipucu kutusudur.","detailedDescription":"Kesin tarih ve fiyat okuması sağlar.","howToInterpret":"Tarih, fiyat ve hacim gibi kesin değerleri nokta bazında okumayı sağlar.","commonMistake":null,"example":null,"relatedTerms":["Çizgi grafik"]},"en":{"title":"Tooltip","shortDescription":"An on-chart data hint that appears when you hover over a point.","detailedDescription":"Provides precise date and price readings.","howToInterpret":"Lets you read exact values such as date, price, and volume at a specific point.","commonMistake":null,"example":null,"relatedTerms":["Line chart"]},"de":{"title":"Tooltip","shortDescription":"Ein Datenhinweis im Chart, der beim Darüberfahren erscheint.","detailedDescription":"Ermöglicht präzises Ablesen von Datum und Preis.","howToInterpret":"Ermöglicht das exakte Ablesen von Werten wie Datum, Preis und Volumen an einem Punkt.","commonMistake":null,"example":null,"relatedTerms":["Liniendiagramm"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Trace',
    'trace',
    'ACTIVE',
    'SYSTEM_TERM',
    'ADVANCED',
    'SYSTEM_OBSERVABILITY',
    'Bir isteğin servisler arası yolculuğunu uçtan uca izleyen dağıtık iz kaydıdır.',
    'Gözlemlenebilirlik altyapısının parçasıdır.',
    'Gecikme ve hata kök neden analizinde kullanılır.',
    NULL,
    NULL,
    TRUE,
    '["Trace"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Correlation ID"]'::jsonb,
    '{"tr":{"title":"Trace","shortDescription":"Bir isteğin servisler arası yolculuğunu uçtan uca izleyen dağıtık iz kaydıdır.","detailedDescription":"Gözlemlenebilirlik altyapısının parçasıdır.","howToInterpret":"Gecikme ve hata kök neden analizinde kullanılır.","commonMistake":null,"example":null,"relatedTerms":["Correlation ID"]},"en":{"title":"Trace","shortDescription":"A distributed trace record that follows a request end to end across services.","detailedDescription":"Part of the observability stack.","howToInterpret":"Used for latency and root-cause analysis of errors.","commonMistake":null,"example":null,"relatedTerms":["Correlation ID"]},"de":{"title":"Trace","shortDescription":"Ein verteilter Trace-Eintrag, der eine Anfrage end-to-end über Services verfolgt.","detailedDescription":"Teil der Observability-Infrastruktur.","howToInterpret":"Wird für Latenz- und Root-Cause-Analyse bei Fehlern genutzt.","commonMistake":null,"example":null,"relatedTerms":["Correlation ID"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Trend skoru',
    'trend-skoru',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'MARKET_DATA',
    'Kısa vadeli fiyat momentumunu segment içindeki diğer enstrümanlarla kıyaslayan özet skordur.',
    'Piyasalar tablosundaki mini grafik ve WEAK–STRONG etiketleri, göreli performansı hızlı okumak için tasarlanmıştır; mutlak getiri değildir.',
    'Yüksek skor “güçlü göreli hareket” demektir; düşük skor zayıf momentum. Mutlaka 1G ve 1A getirisiyle birlikte bakın.',
    NULL,
    NULL,
    FALSE,
    '["Trend","trend skoru","momentum"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Volatilite","Dönem getirisi","Isı haritası"]'::jsonb,
    '{"tr":{"title":"Trend skoru","shortDescription":"Kısa vadeli fiyat momentumunu segment içindeki diğer enstrümanlarla kıyaslayan özet skordur.","detailedDescription":"Piyasalar tablosundaki mini grafik ve WEAK–STRONG etiketleri, göreli performansı hızlı okumak için tasarlanmıştır; mutlak getiri değildir.","howToInterpret":"Yüksek skor “güçlü göreli hareket” demektir; düşük skor zayıf momentum. Mutlaka 1G ve 1A getirisiyle birlikte bakın.","commonMistake":null,"example":null,"relatedTerms":["Volatilite","Dönem getirisi","Isı haritası"]},"en":{"title":"Trend score","shortDescription":"A summary score comparing short-term price momentum to peers in the same segment.","detailedDescription":"Sparklines and WEAK–STRONG labels in the Markets table show relative strength, not absolute return.","howToInterpret":"High score means strong relative momentum; always cross-check 1D and 1M returns.","commonMistake":null,"example":null,"relatedTerms":["Volatility","Period return","Heat map"]},"de":{"title":"Trend-Score","shortDescription":"Ein Kennzahlvergleich kurzfristiger Momentum relativ zu anderen Instrumenten im Segment.","detailedDescription":"Sparklines und WEAK–STRONG Kennzeichnen relative Stärke, nicht absolute Rendite.","howToInterpret":"Hoher Score = starkes relatives Momentum; mit 1T- und 1M-Rendite abgleichen.","commonMistake":null,"example":null,"relatedTerms":["Volatilität","Periodenrendite","Heatmap"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'TÜFE Aylık Oran',
    'tufe-aylik',
    'ACTIVE',
    'MACRO_INDICATOR',
    'BEGINNER',
    'TURKEY_ECONOMY',
    'TÜFE endeksinin bir önceki aya göre yüzde değişimidir.',
    'Kısa vadeli fiyat baskısının en güncel göstergelerinden biridir.',
    'Kısa vadeli fiyat baskısını gösterir; mevsimsellik etkisine dikkat edilmelidir.',
    NULL,
    NULL,
    FALSE,
    '["TÜFE aylık","tufe"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["TÜFE Endeksi","TÜFE Yıllık Oran"]'::jsonb,
    '{"tr":{"title":"TÜFE Aylık Oran","shortDescription":"TÜFE endeksinin bir önceki aya göre yüzde değişimidir.","detailedDescription":"Kısa vadeli fiyat baskısının en güncel göstergelerinden biridir.","howToInterpret":"Kısa vadeli fiyat baskısını gösterir; mevsimsellik etkisine dikkat edilmelidir.","commonMistake":null,"example":null,"relatedTerms":["TÜFE Endeksi","TÜFE Yıllık Oran"]},"en":{"title":"CPI monthly rate","shortDescription":"The percentage change in the CPI index compared with the previous month.","detailedDescription":"One of the most up-to-date indicators of short-term price pressure.","howToInterpret":"It reflects near-term price pressure; seasonal effects should be considered.","commonMistake":null,"example":null,"relatedTerms":["CPI index","CPI annual rate"]},"de":{"title":"VPI-Monatsrate","shortDescription":"Die prozentuale Veränderung des VPI-Index gegenüber dem Vormonat.","detailedDescription":"Einer der aktuellsten Indikatoren für kurzfristigen Preisdruck.","howToInterpret":"Sie zeigt kurzfristigen Preisdruck; saisonale Effekte sollten berücksichtigt werden.","commonMistake":null,"example":null,"relatedTerms":["VPI-Index","VPI-Jahresrate"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'TÜFE Endeksi',
    'tufe-endeksi',
    'ACTIVE',
    'MACRO_INDICATOR',
    'BEGINNER',
    'TURKEY_ECONOMY',
    'Tüketici fiyat seviyesindeki değişimi gösteren endekstir.',
    'Fiyat seviyesinin zaman içinde nasıl değiştiğini gösterir; reel getiri hesaplarında kullanılır.',
    'Endeksin yükselmesi fiyat seviyesinin arttığını gösterir. Endeks seviyesi tek başına enflasyon oranı değildir.',
    'TÜFE endeks değerini doğrudan yüzde enflasyon oranı sanmak.',
    '2022’de 100.000 TL olan bir tutarın bugünkü alım gücü TÜFE değişimiyle hesaplanabilir.',
    FALSE,
    '["TÜFE","tufe","enflasyon"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["TÜFE Aylık Oran","TÜFE Yıllık Oran","Reel getiri","Enflasyon"]'::jsonb,
    '{"tr":{"title":"TÜFE Endeksi","shortDescription":"Tüketici fiyat seviyesindeki değişimi gösteren endekstir.","detailedDescription":"Fiyat seviyesinin zaman içinde nasıl değiştiğini gösterir; reel getiri hesaplarında kullanılır.","howToInterpret":"Endeksin yükselmesi fiyat seviyesinin arttığını gösterir. Endeks seviyesi tek başına enflasyon oranı değildir.","commonMistake":"TÜFE endeks değerini doğrudan yüzde enflasyon oranı sanmak.","example":"2022’de 100.000 TL olan bir tutarın bugünkü alım gücü TÜFE değişimiyle hesaplanabilir.","relatedTerms":["TÜFE Aylık Oran","TÜFE Yıllık Oran","Reel getiri","Enflasyon"]},"en":{"title":"CPI index","shortDescription":"An index that tracks changes in consumer price levels.","detailedDescription":"It shows how the price level evolves over time and is used in real-return calculations.","howToInterpret":"A rising index means prices are higher, but the index level itself is not the inflation rate.","commonMistake":"Treating the CPI index level as the inflation percentage directly.","example":"The purchasing power of TRY 100,000 in 2022 can be estimated today using CPI changes.","relatedTerms":["CPI monthly rate","CPI annual rate","Real return","Inflation"]},"de":{"title":"VPI-Index","shortDescription":"Ein Index, der Veränderungen des Verbraucherpreisniveaus abbildet.","detailedDescription":"Er zeigt, wie sich das Preisniveau im Zeitverlauf entwickelt, und wird bei Realrenditeberechnungen verwendet.","howToInterpret":"Ein steigender Index bedeutet höhere Preise, aber der Indexstand selbst ist nicht die Inflationsrate.","commonMistake":"Den VPI-Indexstand direkt als Inflationsprozentsatz zu interpretieren.","example":"Die Kaufkraft von 100.000 TRY im Jahr 2022 lässt sich heute anhand der VPI-Entwicklung abschätzen.","relatedTerms":["VPI-Monatsrate","VPI-Jahresrate","Realrendite","Inflation"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'TÜFE Yıllık Oran',
    'tufe-yillik',
    'ACTIVE',
    'MACRO_INDICATOR',
    'BEGINNER',
    'TURKEY_ECONOMY',
    'TÜFE endeksinin aynı ay bir önceki yıla göre yüzde değişimidir.',
    'En yaygın kullanılan yıllık enflasyon tanımıdır.',
    'Yıllık oran, uzun dönem fiyat trendini özetler; politika ve piyasa beklentilerini etkiler.',
    NULL,
    NULL,
    FALSE,
    '["TÜFE yıllık","enflasyon"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["TÜFE Endeksi","Reel faiz","Reel getiri"]'::jsonb,
    '{"tr":{"title":"TÜFE Yıllık Oran","shortDescription":"TÜFE endeksinin aynı ay bir önceki yıla göre yüzde değişimidir.","detailedDescription":"En yaygın kullanılan yıllık enflasyon tanımıdır.","howToInterpret":"Yıllık oran, uzun dönem fiyat trendini özetler; politika ve piyasa beklentilerini etkiler.","commonMistake":null,"example":null,"relatedTerms":["TÜFE Endeksi","Reel faiz","Reel getiri"]},"en":{"title":"CPI annual rate","shortDescription":"The percentage change in the CPI index compared with the same month a year earlier.","detailedDescription":"The most widely used definition of annual inflation.","howToInterpret":"The annual rate summarizes longer-term price trends and shapes policy and market expectations.","commonMistake":null,"example":null,"relatedTerms":["CPI index","Real interest rate","Real return"]},"de":{"title":"VPI-Jahresrate","shortDescription":"Die prozentuale Veränderung des VPI-Index gegenüber demselben Monat im Vorjahr.","detailedDescription":"Die am häufigsten verwendete Definition der jährlichen Inflation.","howToInterpret":"Die Jahresrate fasst längerfristige Preistrends zusammen und prägt Politik- und Markterwartungen.","commonMistake":null,"example":null,"relatedTerms":["VPI-Index","Realzins","Realrendite"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Tüketici Güven Endeksi',
    'tuketici-guven',
    'ACTIVE',
    'MACRO_INDICATOR',
    'BEGINNER',
    'TURKEY_ECONOMY',
    'Hanehalkının ekonomik görünüme ve harcama eğilimine dair algısını ölçer.',
    'Talep ve harcama eğilimleri hakkında öncü gösterge olarak izlenir.',
    'Endeks yükseldikçe harcama ve talep beklentisi güçleniyor olabilir.',
    NULL,
    NULL,
    FALSE,
    '["Tüketici güven","guven"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Ekonomik Güven Endeksi"]'::jsonb,
    '{"tr":{"title":"Tüketici Güven Endeksi","shortDescription":"Hanehalkının ekonomik görünüme ve harcama eğilimine dair algısını ölçer.","detailedDescription":"Talep ve harcama eğilimleri hakkında öncü gösterge olarak izlenir.","howToInterpret":"Endeks yükseldikçe harcama ve talep beklentisi güçleniyor olabilir.","commonMistake":null,"example":null,"relatedTerms":["Ekonomik Güven Endeksi"]},"en":{"title":"Consumer confidence index","shortDescription":"Measures households’ perception of the economic outlook and spending intentions.","detailedDescription":"Watched as a leading indicator of demand and consumption trends.","howToInterpret":"A rising index may signal stronger spending and demand expectations.","commonMistake":null,"example":null,"relatedTerms":["Economic confidence index"]},"de":{"title":"Verbrauchervertrauensindex","shortDescription":"Misst die Wahrnehmung privater Haushalte zur Wirtschaftslage und zu Ausgabeabsichten.","detailedDescription":"Wird als Frühindikator für Nachfrage und Konsumtrends beobachtet.","howToInterpret":"Ein steigender Index kann stärkere Ausgabe- und Nachfrageerwartungen signalisieren.","commonMistake":null,"example":null,"relatedTerms":["Wirtschaftsvertrauensindex"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Uygulama içi bildirim',
    'uygulama-ici-bildirim',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'NOTIFICATIONS',
    'Portal oturumu veya mobil uygulama üzerinden anlık gösterilen bildirimdir.',
    'Bildirim merkezinde listelenir.',
    'Okunmamış bildirimler üst çan simgesinde listelenir.',
    NULL,
    NULL,
    FALSE,
    '["Uygulama içi bildirim"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Fiyat uyarısı","Bildirim tercihi"]'::jsonb,
    '{"tr":{"title":"Uygulama içi bildirim","shortDescription":"Portal oturumu veya mobil uygulama üzerinden anlık gösterilen bildirimdir.","detailedDescription":"Bildirim merkezinde listelenir.","howToInterpret":"Okunmamış bildirimler üst çan simgesinde listelenir.","commonMistake":null,"example":null,"relatedTerms":["Fiyat uyarısı","Bildirim tercihi"]},"en":{"title":"In-app notification","shortDescription":"A notification shown instantly in the portal session or mobile app.","detailedDescription":"Listed in the notification center.","howToInterpret":"Unread notifications appear under the bell icon.","commonMistake":null,"example":null,"relatedTerms":["Price alert","Notification preference"]},"de":{"title":"In-App-Benachrichtigung","shortDescription":"Eine Benachrichtigung, die sofort in der Portal-Sitzung oder mobilen App angezeigt wird.","detailedDescription":"Wird im Benachrichtigungscenter aufgelistet.","howToInterpret":"Ungelesene Benachrichtigungen erscheinen unter dem Glocken-Symbol.","commonMistake":null,"example":null,"relatedTerms":["Kursalarm","Benachrichtigungseinstellung"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'ÜFE',
    'ufe',
    'ACTIVE',
    'MACRO_INDICATOR',
    'INTERMEDIATE',
    'TURKEY_ECONOMY',
    'Üretici fiyat endeksi; üretim ve toptan aşamadaki fiyat değişimini ölçer.',
    'Yİ-ÜFE ile ilişkilidir; üretici maliyet baskısı tüketici fiyatlarına gecikmeli yansıyabilir.',
    'ÜFE-TÜFE makası arz zinciri baskısı hakkında ipucu verir; tek veri noktasına dayanmayın.',
    NULL,
    NULL,
    FALSE,
    '["ÜFE","PPI"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Yİ-ÜFE","TÜFE Endeksi","Enflasyon"]'::jsonb,
    '{"tr":{"title":"ÜFE","shortDescription":"Üretici fiyat endeksi; üretim ve toptan aşamadaki fiyat değişimini ölçer.","detailedDescription":"Yİ-ÜFE ile ilişkilidir; üretici maliyet baskısı tüketici fiyatlarına gecikmeli yansıyabilir.","howToInterpret":"ÜFE-TÜFE makası arz zinciri baskısı hakkında ipucu verir; tek veri noktasına dayanmayın.","commonMistake":null,"example":null,"relatedTerms":["Yİ-ÜFE","TÜFE Endeksi","Enflasyon"]},"en":{"title":"PPI","shortDescription":"Producer Price Index measuring price changes at production/wholesale stage.","detailedDescription":"Related to WPI in Turkey; producer costs may pass through to consumer prices with a lag.","howToInterpret":"PPI–CPI gap hints at pipeline pressure; read trends, not one print.","commonMistake":null,"example":null,"relatedTerms":["WPI","CPI index","Inflation"]},"de":{"title":"Erzeugerpreisindex","shortDescription":"Misst Preisänderungen auf Produktions- bzw. Großhandelsstufe.","detailedDescription":"Verzögerte Übertragung auf Verbraucherpreise möglich.","howToInterpret":"PPI-VPI-Spread zeigt Pipeline-Druck; Trends lesen.","commonMistake":null,"example":null,"relatedTerms":["Erzeugerpreise","VPI-Index","Inflation"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Vade',
    'vade',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'BASIC_FINANCE',
    'Borçlanma aracının anaparasının geri ödeneceği tarih veya süredir.',
    'Kısa vade (bono) ile uzun vade (tahvil) farklı faiz ve fiyat hassasiyeti taşır; vade uzadıkça faiz riski genelde artar.',
    'Vade yaklaştıkça fiyat genelde paraya (itfa değerine) yaklaşır; erken satışta piyasa fiyatı geçerlidir.',
    NULL,
    NULL,
    FALSE,
    '["Vade","maturity"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Hazine bonosu","Tahvil","Tahvil getirisi"]'::jsonb,
    '{"tr":{"title":"Vade","shortDescription":"Borçlanma aracının anaparasının geri ödeneceği tarih veya süredir.","detailedDescription":"Kısa vade (bono) ile uzun vade (tahvil) farklı faiz ve fiyat hassasiyeti taşır; vade uzadıkça faiz riski genelde artar.","howToInterpret":"Vade yaklaştıkça fiyat genelde paraya (itfa değerine) yaklaşır; erken satışta piyasa fiyatı geçerlidir.","commonMistake":null,"example":null,"relatedTerms":["Hazine bonosu","Tahvil","Tahvil getirisi"]},"en":{"title":"Maturity","shortDescription":"The date or horizon when principal of a debt instrument is repaid.","detailedDescription":"Short maturity (bill) vs long (bond) implies different rate sensitivity; longer duration usually means more rate risk.","howToInterpret":"Near maturity, price tends toward redemption value; early sale uses market price.","commonMistake":null,"example":null,"relatedTerms":["Treasury bill","Bond","Bond yield"]},"de":{"title":"Laufzeit","shortDescription":"Zeitpunkt bzw. Horizont der Rückzahlung einer Schuldverschreibung.","detailedDescription":"Kurz vs. lang bedeutet unterschiedliche Zinssensitivität; längere Laufzeit oft mehr Zinsrisiko.","howToInterpret":"Gegen Laufzeitende nähert sich der Kurs oft dem Rückzahlungswert.","commonMistake":null,"example":null,"relatedTerms":["Schatzanweisung","Anleihe","Anleiherendite"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Varlık dağılımı',
    'varlik-dagilimi',
    'ACTIVE',
    'TERM',
    'BEGINNER',
    'PORTFOLIO_ANALYSIS',
    'Portföydeki varlık sınıflarının veya enstrümanların ağırlık paylarını gösterir.',
    'Risk ve getiri profilini şekillendiren yapısal görünümdür.',
    'Tek varlığa aşırı yoğunlaşma riski artırır; çeşitlendirme denge sağlar.',
    NULL,
    NULL,
    FALSE,
    '["Varlık dağılımı","dagilim"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Portföy","Risk","Pasta grafik","Çeşitlendirme"]'::jsonb,
    '{"tr":{"title":"Varlık dağılımı","shortDescription":"Portföydeki varlık sınıflarının veya enstrümanların ağırlık paylarını gösterir.","detailedDescription":"Risk ve getiri profilini şekillendiren yapısal görünümdür.","howToInterpret":"Tek varlığa aşırı yoğunlaşma riski artırır; çeşitlendirme denge sağlar.","commonMistake":null,"example":null,"relatedTerms":["Portföy","Risk","Pasta grafik","Çeşitlendirme"]},"en":{"title":"Asset allocation","shortDescription":"Shows the weight of asset classes or instruments in a portfolio.","detailedDescription":"A structural view that shapes risk and return profile.","howToInterpret":"Heavy concentration in one asset raises risk; diversification helps balance exposure.","commonMistake":null,"example":null,"relatedTerms":["Portfolio","Risk","Pie chart","Diversification"]},"de":{"title":"Vermögensallokation","shortDescription":"Zeigt die Gewichtung von Anlageklassen oder Instrumenten im Portfolio.","detailedDescription":"Eine strukturelle Sicht, die Risiko- und Renditeprofil prägt.","howToInterpret":"Starke Konzentration auf einen Vermögenswert erhöht das Risiko; Diversifikation schafft Balance.","commonMistake":null,"example":null,"relatedTerms":["Portfolio","Risiko","Kreisdiagramm","Diversifikation"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'VİOP',
    'viop',
    'ACTIVE',
    'TERM',
    'ADVANCED',
    'MARKET_DATA',
    'Vadeli işlem ve opsiyon piyasasında türev ürünlerin işlem gördüğü piyasadır.',
    'Kaldıraçlı ürünler profesyonel ve risk toleransı yüksek yatırımcılara yöneliktir.',
    'Kaldıraç etkisi nedeniyle küçük fiyat hareketleri büyük sonuçlar doğurabilir.',
    'VİOP ürünlerini spot hisse gibi düşük riskli araç sanmak.',
    NULL,
    FALSE,
    '["VİOP","viop","turev"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Risk","Volatilite","Emtia vadeli işlem"]'::jsonb,
    '{"tr":{"title":"VİOP","shortDescription":"Vadeli işlem ve opsiyon piyasasında türev ürünlerin işlem gördüğü piyasadır.","detailedDescription":"Kaldıraçlı ürünler profesyonel ve risk toleransı yüksek yatırımcılara yöneliktir.","howToInterpret":"Kaldıraç etkisi nedeniyle küçük fiyat hareketleri büyük sonuçlar doğurabilir.","commonMistake":"VİOP ürünlerini spot hisse gibi düşük riskli araç sanmak.","example":null,"relatedTerms":["Risk","Volatilite","Emtia vadeli işlem"]},"en":{"title":"VİOP","shortDescription":"The derivatives market where futures and options are traded in Turkey.","detailedDescription":"Leveraged products are aimed at professional investors and those with higher risk tolerance.","howToInterpret":"Because of leverage, small price moves can produce large outcomes.","commonMistake":"Treating VİOP products like spot equities with low risk.","example":null,"relatedTerms":["Risk","Volatility","Commodity futures"]},"de":{"title":"VİOP","shortDescription":"Der türkische Derivatemarkt, an dem Termin- und Optionskontrakte gehandelt werden.","detailedDescription":"Hebelprodukte richten sich an professionelle Anleger und solche mit höherer Risikotoleranz.","howToInterpret":"Durch den Hebeleffekt können kleine Kursbewegungen große Auswirkungen haben.","commonMistake":"VİOP-Produkte wie risikoarme Spotaktien zu behandeln.","example":null,"relatedTerms":["Risiko","Volatilität","Rohstoffterminkontrakt"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Volatilite',
    'volatilite',
    'ACTIVE',
    'TERM',
    'INTERMEDIATE',
    'BASIC_FINANCE',
    'Fiyatların zaman içindeki dalgalanma derecesidir.',
    'Volatilite hem risk hem fırsat taşır; portföy ve piyasa analizinde temel kavramdır.',
    'Yüksek volatilite hem yükseliş hem düşüş fırsatı taşır; risk yönetimi önem kazanır.',
    NULL,
    NULL,
    FALSE,
    '["Volatilite","volatilite"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["Risk","Mum grafik"]'::jsonb,
    '{"tr":{"title":"Volatilite","shortDescription":"Fiyatların zaman içindeki dalgalanma derecesidir.","detailedDescription":"Volatilite hem risk hem fırsat taşır; portföy ve piyasa analizinde temel kavramdır.","howToInterpret":"Yüksek volatilite hem yükseliş hem düşüş fırsatı taşır; risk yönetimi önem kazanır.","commonMistake":null,"example":null,"relatedTerms":["Risk","Mum grafik"]},"en":{"title":"Volatility","shortDescription":"The degree to which prices fluctuate over time.","detailedDescription":"Volatility carries both risk and opportunity and is a core concept in portfolio and market analysis.","howToInterpret":"High volatility can create upside and downside moves alike, so risk management becomes more important.","commonMistake":null,"example":null,"relatedTerms":["Risk","Candlestick chart"]},"de":{"title":"Volatilität","shortDescription":"Das Ausmaß, in dem Preise im Zeitverlauf schwanken.","detailedDescription":"Volatilität birgt sowohl Risiko als auch Chancen und ist ein Grundbegriff in Portfolio- und Marktanalyse.","howToInterpret":"Hohe Volatilität kann Auf- und Abwärtsbewegungen begünstigen; Risikomanagement wird wichtiger.","commonMistake":null,"example":null,"relatedTerms":["Risiko","Kerzenchart"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;

INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    'Yİ-ÜFE',
    'yi-ufe',
    'ACTIVE',
    'MACRO_INDICATOR',
    'INTERMEDIATE',
    'TURKEY_ECONOMY',
    'Üretici fiyatları endeksi; üretim aşamasındaki maliyet baskısını ölçer.',
    'TÜFE’ye gecikmeli yansıyabilir; arz taraflı enflasyon sinyali verir.',
    'Yİ-ÜFE artışı gelecekte TÜFE üzerinde baskı oluşturabilir; gecikmeli etki görülebilir.',
    NULL,
    NULL,
    FALSE,
    '["Yİ-ÜFE","yi-ufe","üfe"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '["FINANCIAL_LITERACY"]'::jsonb,
    '["TÜFE Endeksi","Enflasyon","ÜFE"]'::jsonb,
    '{"tr":{"title":"Yİ-ÜFE","shortDescription":"Üretici fiyatları endeksi; üretim aşamasındaki maliyet baskısını ölçer.","detailedDescription":"TÜFE’ye gecikmeli yansıyabilir; arz taraflı enflasyon sinyali verir.","howToInterpret":"Yİ-ÜFE artışı gelecekte TÜFE üzerinde baskı oluşturabilir; gecikmeli etki görülebilir.","commonMistake":null,"example":null,"relatedTerms":["TÜFE Endeksi","Enflasyon","ÜFE"]},"en":{"title":"PPI","shortDescription":"The producer price index; it measures cost pressure at the production stage.","detailedDescription":"It may feed into CPI with a lag and signals supply-side inflation pressure.","howToInterpret":"Rising PPI can create future CPI pressure; the effect may appear with a delay.","commonMistake":null,"example":null,"relatedTerms":["CPI index","Inflation","Wholesale price index"]},"de":{"title":"Erzeugerpreisindex","shortDescription":"Der Erzeugerpreisindex; er misst Kosten- bzw. Preisdruck in der Produktionsstufe.","detailedDescription":"Er kann mit Verzögerung in den VPI durchschlagen und signalisiert angebotsseitigen Inflationsdruck.","howToInterpret":"Steigende Erzeugerpreise können künftigen VPI-Druck erzeugen; die Wirkung kann verzögert eintreten.","commonMistake":null,"example":null,"relatedTerms":["VPI-Index","Inflation","Großhandelspreisindex"]}}'::jsonb
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;
