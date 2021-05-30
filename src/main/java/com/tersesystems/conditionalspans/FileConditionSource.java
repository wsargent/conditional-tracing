package com.tersesystems.conditionalspans;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

class FileConditionSource implements ConditionSource {
    private final Supplier<Reader> reader;
    private final Supplier<Boolean> validator;
    private final Path path;

    public FileConditionSource(Path path) throws IOException {
        this.path = path;
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
                // this probably can't be caught by anything, so
                // print AND throw
                e.printStackTrace();
                return false;
            }
        };

        this.reader = () -> {
            try {
                return Files.newBufferedReader(path);
            } catch (IOException e) {
                // this probably can't be caught by anything, so
                // print AND throw
                e.printStackTrace();
                throw new EvaluationException(e);
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

    @Override
    public String toString() {
        return "FileConditionSource{" +
                "path=" + path + '}';
    }
}
