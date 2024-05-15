package com.redhat.hacbs.recipes;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.MINIMIZE_QUOTES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.SPLIT_LINES;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public interface RecipeManager<T> {

    ObjectMapper MAPPER = new ObjectMapper(
            new YAMLFactory().disable(SPLIT_LINES).enable(MINIMIZE_QUOTES))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

    default T parse(Path file) throws IOException {
        try (var in = Files.newInputStream(file)) {
            return parse(in);
        }
    }

    T parse(InputStream in) throws IOException;

    void write(T data, OutputStream out) throws IOException;

    default void write(T data, Path file) throws IOException {
        try (var out = Files.newOutputStream(file)) {
            write(data, out);
        }
    }
}
