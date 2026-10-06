package mx.taller.config;

import java.time.Instant;
import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import mx.taller.auth.*;
import mx.taller.client.DuplicateClientException;
import mx.taller.client.InvalidClientPhotoException;
import mx.taller.address.AddressCatalogNotFoundException;
import mx.taller.workshop.DuplicateWorkshopException;
import mx.taller.workshop.InvalidWorkshopPhotoException;
import mx.taller.workshop.WorkshopAuthorizationException;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice public class ApiExceptionHandler {
  @ExceptionHandler(AuthenticationFailedException.class) @ResponseStatus(HttpStatus.UNAUTHORIZED) Map<String,Object> loginFailure() { return error("AUTHENTICATION_FAILED", "Credenciales no válidas"); }
  @ExceptionHandler(InvalidResetTokenException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,Object> resetFailure() { return error("INVALID_RESET_TOKEN", "El enlace de recuperación no es válido o expiró"); }
  @ExceptionHandler(ConflictException.class) @ResponseStatus(HttpStatus.CONFLICT) Map<String,Object> conflict(ConflictException error) { return error("REQUEST_CONFLICT", error.getMessage()); }
  @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,Object> invalidInput() { return error("INVALID_REQUEST", "La solicitud no es válida"); }
  @ExceptionHandler(ConstraintViolationException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,Object> invalidParameter() { return error("INVALID_REQUEST", "Los parámetros de la solicitud no son válidos"); }
  @ExceptionHandler(DuplicateClientException.class) @ResponseStatus(HttpStatus.CONFLICT) Map<String,Object> duplicateClient(DuplicateClientException error) { return error("DUPLICATE_CLIENT", error.getMessage()); }
  @ExceptionHandler(DataIntegrityViolationException.class) @ResponseStatus(HttpStatus.CONFLICT) Map<String,Object> duplicateConstraint() { return error("REQUEST_CONFLICT", "No se pudo completar la solicitud por una restricción de datos"); }
  @ExceptionHandler(InvalidClientPhotoException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,Object> invalidPhoto(InvalidClientPhotoException error) { return error("INVALID_CLIENT_PHOTO", error.getMessage()); }
  @ExceptionHandler(AddressCatalogNotFoundException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,Object> invalidAddressCatalog(AddressCatalogNotFoundException error) { return error("INVALID_ADDRESS_CATALOG", error.getMessage()); }
  @ExceptionHandler(DuplicateWorkshopException.class) @ResponseStatus(HttpStatus.CONFLICT) Map<String,Object> duplicateWorkshop(DuplicateWorkshopException error) { return error("DUPLICATE_WORKSHOP", error.getMessage()); }
  @ExceptionHandler(InvalidWorkshopPhotoException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,Object> invalidWorkshopPhoto(InvalidWorkshopPhotoException error) { return error("INVALID_WORKSHOP_PHOTO", error.getMessage()); }
  @ExceptionHandler(WorkshopAuthorizationException.class) @ResponseStatus(HttpStatus.FORBIDDEN) Map<String,Object> workshopForbidden(WorkshopAuthorizationException error) { return error("WORKSHOP_FORBIDDEN", error.getMessage()); }
  private Map<String,Object> error(String code, String message) { return Map.of("timestamp", Instant.now().toString(), "code", code, "message", message); }
}
