package com.tersesystems.dynamicspans;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

class FileConditionSource implements ConditionSource {
    private final Supplier<Reader> reader;
    private final Supplier<Boolean> validator;

    public FileConditionSource(Path path) throws IOException {
        if (! Files.exists(path)) {
            throw new FileNotFoundException(path.toAbsolutePath().toString());
        }
        AtomicReference<FileTime> lastModified = new AtomicReference<>(Files.getLastModifiedTime(path));
        this.validator = () -> {
            try {
                final FileTime newTime = Files.getLastModifiedTime(path);
                if (newTime.compareTo(lastModified.get()) > 0) {
                    lastModified.set(newTime);
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                return true;
            }
        };

        this.reader = () -> {
            try {
                return Files.newBufferedReader(path);
            } catch (IOException e) {
                e.printStackTrace();
                return new StringReader("def evaluate() { false }");
            }
        };
    }

    @Override
    public boolean isInvalid() {
        return validator.get();
    }

    @Override
    public Reader getReader() {
        return reader.get();
    }
}
