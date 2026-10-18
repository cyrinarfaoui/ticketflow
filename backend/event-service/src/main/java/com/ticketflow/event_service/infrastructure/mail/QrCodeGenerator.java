package com.ticketflow.event_service.infrastructure.mail;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/** Dessine un QR code en image PNG (ZXing calcule la grille de carrés, on la peint en noir et blanc). */
@Component
public class QrCodeGenerator {

    private static final int SIZE = 240;   // pixels : lisible par un téléphone, léger dans un e-mail
    private static final int BLACK = 0x000000;
    private static final int WHITE = 0xFFFFFF;

    public byte[] png(String content) {
        try {
            // Correction d'erreur "M" : le code reste lisible même abîmé à ~15 % (écran rayé, impression pâle)
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, SIZE, SIZE,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < SIZE; x++) {
                for (int y = 0; y < SIZE; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
                }
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(image, "png", png);
            return png.toByteArray();
        } catch (WriterException e) {
            throw new IllegalArgumentException("Impossible de créer le QR code", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
