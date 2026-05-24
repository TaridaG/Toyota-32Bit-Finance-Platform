ALTER TABLE info_cards
    ADD COLUMN IF NOT EXISTS translations JSONB NOT NULL DEFAULT '{}'::jsonb;

UPDATE info_cards
SET translations = jsonb_build_object(
    'tr', jsonb_build_object(
        'title', title,
        'shortDescription', short_description,
        'detailedDescription', COALESCE(detailed_description, ''),
        'howToInterpret', COALESCE(how_to_interpret, ''),
        'commonMistake', COALESCE(common_mistake, ''),
        'example', COALESCE(example_text, ''),
        'relatedTerms', COALESCE(related_terms, '[]'::jsonb)
    ),
    'en', jsonb_build_object(
        'title', title,
        'shortDescription', short_description,
        'detailedDescription', COALESCE(detailed_description, ''),
        'howToInterpret', COALESCE(how_to_interpret, ''),
        'commonMistake', COALESCE(common_mistake, ''),
        'example', COALESCE(example_text, ''),
        'relatedTerms', COALESCE(related_terms, '[]'::jsonb)
    ),
    'de', jsonb_build_object(
        'title', title,
        'shortDescription', short_description,
        'detailedDescription', COALESCE(detailed_description, ''),
        'howToInterpret', COALESCE(how_to_interpret, ''),
        'commonMistake', COALESCE(common_mistake, ''),
        'example', COALESCE(example_text, ''),
        'relatedTerms', COALESCE(related_terms, '[]'::jsonb)
    )
)
WHERE translations = '{}'::jsonb;
