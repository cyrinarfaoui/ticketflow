package com.ticketflow.event_service.infrastructure.mail;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeGeneratorTest {

    @Test
    void produit_une_image_png_carree() throws Exception {
        byte[] png = new QrCodeGenerator().png("TICKETFLOW-123-1");

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image).isNotNull();   // ImageIO sait la relire : c'est bien une image PNG valide
        assertThat(image.getWidth()).isEqualTo(240).isEqualTo(image.getHeight());
    }

    @Test
    void deux_codes_differents_donnent_deux_images_differentes() {
        QrCodeGenerator generator = new QrCodeGenerator();

        assertThat(generator.png("TICKETFLOW-123-1")).isNotEqualTo(generator.png("TICKETFLOW-123-2"));
    }
}
