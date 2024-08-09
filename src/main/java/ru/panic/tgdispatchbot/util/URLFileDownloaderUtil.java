package ru.panic.tgdispatchbot.util;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class URLFileDownloaderUtil {
    public File downloadFileAndPackInTemp(String URL, String prefix, String suffix) throws IOException {
        URL fileUrl = new URL(URL);
        InputStream in = fileUrl.openStream();

        File tempFile = File.createTempFile(prefix, suffix);

        Path tempFilePath = tempFile.toPath();
        Files.copy(in, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        in.close();

        return tempFile;
    }
}
