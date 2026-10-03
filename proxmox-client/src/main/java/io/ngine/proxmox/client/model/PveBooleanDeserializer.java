package io.ngine.proxmox.client.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Proxmox encodes booleans as {@code 0}/{@code 1}, sometimes as strings.
 */
final class PveBooleanDeserializer extends StdDeserializer<Boolean> {

    PveBooleanDeserializer() {
        super(Boolean.class);
    }

    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return switch (parser.currentToken()) {
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NUMBER_INT -> parser.getIntValue() != 0;
            case VALUE_STRING -> parse(parser.getText().strip(), parser, context);
            default -> (Boolean) context.handleUnexpectedToken(Boolean.class, parser);
        };
    }

    private Boolean parse(String text, JsonParser parser, DeserializationContext context) throws IOException {
        return switch (text) {
            case "1", "true", "yes", "on" -> true;
            case "0", "false", "no", "off", "" -> false;
            default -> (Boolean) context.handleWeirdStringValue(Boolean.class, text, "not a Proxmox boolean");
        };
    }
}
