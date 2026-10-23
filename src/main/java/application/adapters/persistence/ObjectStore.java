package application.adapters.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public final class ObjectStore {
    private static final ObjectInputFilter DOMAIN_FILTER = ObjectStore::filterSerializedClass;

    private final JdbcTemplate jdbc;

    public ObjectStore(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "JdbcTemplate es obligatorio");
        jdbc.execute("CREATE TABLE IF NOT EXISTS nexus_domain_object ("
                + "bucket VARCHAR(60) NOT NULL, entity_key VARCHAR(255) NOT NULL, payload BLOB NOT NULL, "
                + "PRIMARY KEY (bucket, entity_key))");
    }

    public <T> T save(String bucket, String key, T value) {
        Objects.requireNonNull(value, "El objeto a persistir es obligatorio");
        jdbc.update("MERGE INTO nexus_domain_object (bucket, entity_key, payload) "
                        + "KEY (bucket, entity_key) VALUES (?, ?, ?)",
                requireText(bucket, "El tipo de objeto es obligatorio"),
                requireText(key, "La clave del objeto es obligatoria"), serialize(value));
        return value;
    }

    public <T> Optional<T> find(String bucket, String key, Class<T> type) {
        Objects.requireNonNull(type, "El tipo de objeto es obligatorio");
        List<T> rows = jdbc.query("SELECT payload FROM nexus_domain_object "
                        + "WHERE bucket = ? AND entity_key = ?",
                (result, rowNumber) -> deserialize(result.getBytes(1), type), bucket, key);
        return rows.stream().findFirst();
    }

    public <T> List<T> findAll(String bucket, Class<T> type) {
        Objects.requireNonNull(type, "El tipo de objeto es obligatorio");
        List<T> rows = jdbc.query("SELECT payload FROM nexus_domain_object WHERE bucket = ? ORDER BY entity_key",
                (result, rowNumber) -> deserialize(result.getBytes(1), type), bucket);
        return List.copyOf(rows);
    }

    public void delete(String bucket, String key) {
        jdbc.update("DELETE FROM nexus_domain_object WHERE bucket = ? AND entity_key = ?", bucket, key);
    }

    private static byte[] serialize(Object value) {
        try (var bytes = new ByteArrayOutputStream(); var output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
            output.flush();
            return bytes.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo persistir el objeto del dominio", exception);
        }
    }

    private static <T> T deserialize(byte[] payload, Class<T> type) {
        try (var input = new ObjectInputStream(new ByteArrayInputStream(payload))) {
            input.setObjectInputFilter(DOMAIN_FILTER);
            return type.cast(input.readObject());
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo leer el objeto persistido", exception);
        }
    }

    private static ObjectInputFilter.Status filterSerializedClass(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > 64 || info.references() > 100_000 || info.streamBytes() > 20_000_000) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        while (type.isArray()) {
            type = type.getComponentType();
        }
        if (type.isPrimitive()) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        String name = type.getName();
        return name.startsWith("application.domain.models.")
                || name.startsWith("application.domain.valueObjects.")
                || name.startsWith("java.lang.")
                || name.startsWith("java.time.")
                || name.startsWith("java.util.")
                ? ObjectInputFilter.Status.ALLOWED
                : ObjectInputFilter.Status.REJECTED;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
