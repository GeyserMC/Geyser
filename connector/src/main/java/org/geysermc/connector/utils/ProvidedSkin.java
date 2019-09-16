package org.geysermc.connector.utils;

import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProvidedSkin {
    @Getter private byte[] skin;

    public ProvidedSkin(String internalUrl) {
        try {
            BufferedImage image = ImageIO.read(ProvidedSkin.class.getClassLoader().getResource(internalUrl));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(image.getWidth() * 4 + image.getHeight() * 4);
            try {
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int rgba = image.getRGB(x, y);
                        outputStream.write((rgba >> 16) & 0xFF);
                        outputStream.write((rgba >> 8) & 0xFF);
                        outputStream.write(rgba & 0xFF);
                        outputStream.write((rgba >> 24) & 0xFF);
                    }
                }
                image.flush();
                skin = outputStream.toByteArray();
            } finally {
                try {
                    outputStream.close();
                } catch (IOException ignored) {}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
