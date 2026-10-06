package mx.taller.client;
public class DuplicateClientException extends RuntimeException { public DuplicateClientException() { super("Ya existe un cliente con el correo o teléfono proporcionado"); } }
