package mx.taller.vehicle;

import jakarta.validation.constraints.*;

public record VehicleDeactivationRequest(
  @NotBlank @Size(max = 17) String confirmationVin,
  @NotNull @AssertTrue Boolean confirmed,
  @NotNull @PositiveOrZero Long revision
) { }
