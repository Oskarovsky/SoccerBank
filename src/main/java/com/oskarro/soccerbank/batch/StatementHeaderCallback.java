package com.oskarro.soccerbank.batch;

import org.springframework.batch.item.file.FlatFileHeaderCallback;

import java.io.IOException;
import java.io.Writer;

public class StatementHeaderCallback implements FlatFileHeaderCallback {

    @Override
    public void writeHeader(Writer writer) throws IOException {
        writer.write(String.format("%120s\n", "Club Service Number"));
        writer.write(String.format("%120s\n", "(48) 333-8401"));
        writer.write(String.format("%120s\n", "Available 24/7"));
        writer.write("\n");
    }
}
