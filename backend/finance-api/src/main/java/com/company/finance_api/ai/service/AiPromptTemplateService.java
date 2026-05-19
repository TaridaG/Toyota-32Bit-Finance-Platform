package com.company.finance_api.ai.service;

import com.company.finance_api.ai.dto.CompleteInfoCardAiRequest;
import com.company.finance_api.ai.dto.InfoCardAiSourceContent;
import com.company.finance_api.ai.dto.TranslateInfoCardAiRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiPromptTemplateService {

    private final ObjectMapper objectMapper;

    public AiPromptTemplateService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildCompleteSystemPrompt() {
        return """
                Sen NRS Finance Portal için finansal okuryazarlık bilgi kartı içeriği üreten bir asistansın.
                Kurallar:
                - Sadece JSON döndür.
                - Markdown kullanma.
                - Başlık ve kısa açıklamayı değiştirme.
                - Sadece istenen eksik alanları üret.
                - Yatırım tavsiyesi verme.
                - Al, sat, tut, hedef fiyat, kesin kazanç veya kişiye özel yatırım tavsiyesi verme.
                - Cümleler sade, kısa ve öğretici olsun.
                - Başlangıç seviyesi kullanıcı anlayabilecek şekilde yaz.
                - Finansal olarak yanlış veya garanti ifade eden cümle kurma.
                - Kavram risk içeriyorsa riskleri sade dille belirt.
                - relatedTerms 3-7 adet olsun.
                - example alanı kısa ve zararsız örnek olsun; yatırım önerisi gibi görünmesin.
                """;
    }

    public String buildCompleteUserPrompt(CompleteInfoCardAiRequest request, List<String> fieldsToGenerate) {
        StringBuilder builder = new StringBuilder();
        builder.append("language: ").append(request.language()).append('\n');
        builder.append("title: ").append(request.title()).append('\n');
        builder.append("shortDescription: ").append(request.shortDescription()).append('\n');
        if (request.category() != null) {
            builder.append("category: ").append(request.category()).append('\n');
        }
        if (request.type() != null) {
            builder.append("type: ").append(request.type()).append('\n');
        }
        if (request.difficulty() != null) {
            builder.append("difficulty: ").append(request.difficulty()).append('\n');
        }
        builder.append("fieldsToGenerate: ").append(String.join(", ", fieldsToGenerate)).append('\n');
        builder.append("Tüm metinleri language değerindeki dilde yaz.");
        return builder.toString();
    }

    public String buildTranslateSystemPrompt() {
        return """
                Sen NRS Finance Portal için onaylı finansal okuryazarlık içeriğini çeviren bir asistansın.
                Kurallar:
                - Sadece JSON döndür.
                - Markdown kullanma.
                - Yeni finansal bilgi ekleme.
                - Kaynak metindeki anlamı koru.
                - Hedef dile doğal ve anlaşılır çevir.
                - Teknik finans terimlerini doğru çevir.
                - Özel isimleri, sembolleri ve marka/varlık adlarını koru.
                - Yatırım tavsiyesi ekleme.
                - JSON alan yapısını koru.
                """;
    }

    public String buildTranslateUserPrompt(TranslateInfoCardAiRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("sourceLanguage: ").append(request.sourceLanguage()).append('\n');
        builder.append("targetLanguage: ").append(request.targetLanguage()).append('\n');
        builder.append("sourceContent:\n");
        builder.append(toJson(request.sourceContent()));
        builder.append("\nCevabı targetLanguage dilinde ver.");
        return builder.toString();
    }

    private String toJson(InfoCardAiSourceContent content) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize source content", ex);
        }
    }
}
