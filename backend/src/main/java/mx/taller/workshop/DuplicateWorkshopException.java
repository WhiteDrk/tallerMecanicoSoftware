package mx.taller.workshop;
public class DuplicateWorkshopException extends RuntimeException { public DuplicateWorkshopException() { super("Ya existe un taller con ese RFC"); } }
