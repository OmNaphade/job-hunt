package org.omnaphade.job_service.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.List;

/**
 * {@link PageImpl} has no no-arg constructor or Jackson-visible creator, so plain bean deserialization
 * fails when a cached {@code Page<T>} is read back from Redis — this reconstructs it from the same fields
 * its normal getters produce. Implements {@link ContextualDeserializer} to recover the content element
 * type (e.g. {@code JobResponseDTO}) from the surrounding {@code PageImpl<JobResponseDTO>} JavaType the
 * cache's {@code Jackson2JsonRedisSerializer} is bound to — without it, {@code content} would deserialize
 * as a list of {@code LinkedHashMap} instead of the real DTO type.
 */
final class PageImplDeserializer extends StdDeserializer<PageImpl<?>> implements ContextualDeserializer {

    private final JavaType contentType;

    PageImplDeserializer() {
        this(null);
    }

    private PageImplDeserializer(JavaType contentType) {
        super(PageImpl.class);
        this.contentType = contentType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType containerType = property != null ? property.getType() : ctxt.getContextualType();
        JavaType resolvedContentType = (containerType != null && containerType.containedTypeCount() > 0)
                ? containerType.containedType(0)
                : ctxt.getTypeFactory().constructType(Object.class);
        return new PageImplDeserializer(resolvedContentType);
    }

    @Override
    public PageImpl<?> deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        JavaType listType = ctxt.getTypeFactory().constructCollectionType(List.class,
                contentType != null ? contentType : ctxt.getTypeFactory().constructType(Object.class));
        List<?> content = ctxt.readTreeAsValue(node.get("content"), listType);
        int number = node.get("number").asInt();
        int size = node.get("size").asInt();
        long totalElements = node.get("totalElements").asLong();
        return new PageImpl<>(content, PageRequest.of(number, size), totalElements);
    }

}
