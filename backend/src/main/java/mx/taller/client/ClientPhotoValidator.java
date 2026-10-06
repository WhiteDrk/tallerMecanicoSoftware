package mx.taller.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ClientPhotoValidator {
  static final long MAX_SIZE = 7L * 1024 * 1024;
  public ValidatedPhoto validate(MultipartFile photo) {
    if (photo == null || photo.isEmpty()) return new ValidatedPhoto(null, null);
    if (photo.getSize() > MAX_SIZE) throw new InvalidClientPhotoException("La fotografía no debe superar 7 MB");
    String contentType = photo.getContentType();
    if (contentType == null || !isAllowed(contentType)) throw new InvalidClientPhotoException("El archivo debe ser una imagen JPEG o PNG");
    try {
      byte[] bytes = photo.getBytes(); BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
      if (image == null || image.getWidth() < 1 || image.getHeight() < 1) throw new InvalidClientPhotoException("El archivo no es una imagen válida");
      return new ValidatedPhoto(bytes, contentType);
    } catch (IOException error) { throw new InvalidClientPhotoException("No fue posible leer la fotografía"); }
  }
  private boolean isAllowed(String type) { return MediaType.IMAGE_JPEG_VALUE.equals(type) || MediaType.IMAGE_PNG_VALUE.equals(type); }
  public record ValidatedPhoto(byte[] bytes, String contentType) { }
}
