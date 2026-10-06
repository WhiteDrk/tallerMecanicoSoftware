package mx.taller.auth;
public class InvalidResetTokenException extends RuntimeException { public InvalidResetTokenException() { super("El enlace de recuperación no es válido o expiró"); } }
