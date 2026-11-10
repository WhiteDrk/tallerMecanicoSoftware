package mx.taller.vehicle;

import jakarta.validation.constraints.*;

public record VehicleRegistrationRequest(
  @NotBlank @Size(max = 17) String vin,
  @NotBlank @Size(max = 10) String plate,
  Long brandId,
  Long modelId,
  @Size(max = 100) String manualBrand,
  @Size(max = 100) String manualModel,
  @NotNull @Min(1886) @Max(3000) Integer year,
  @NotBlank @Size(max = 100) String version,
  @PositiveOrZero Long mileage,
  @NotBlank @Size(max = 40) String serialNumber
) { }
