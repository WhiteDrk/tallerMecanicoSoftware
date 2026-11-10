package mx.taller.vehicle;

public class VehicleNotFoundException extends RuntimeException {
  public VehicleNotFoundException() { super("No se encontró un vehículo disponible para esta consulta"); }
}
