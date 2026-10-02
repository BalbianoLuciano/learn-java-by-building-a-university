package io.github.balbianoluciano.ljbu.api.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** The JSON Schemas of packages/contracts, to check what the api answers. */
public final class Contracts {

  private static final Path DIRECTORY = Path.of("../../packages/contracts");

  private Contracts() {}

  public static void assertMatches(String schemaFile, String json) {
    try {
      List<Error> errors =
          SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
              .getSchema(Files.readString(DIRECTORY.resolve(schemaFile)), InputFormat.JSON)
              .validate(json, InputFormat.JSON);
      assertThat(errors).as("violations of %s in %s", schemaFile, json).isEmpty();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
