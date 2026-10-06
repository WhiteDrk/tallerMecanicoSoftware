package mx.taller.auth;
public class AuthenticationFailedException extends RuntimeException { public AuthenticationFailedException() { super("Credenciales no válidas"); } }
