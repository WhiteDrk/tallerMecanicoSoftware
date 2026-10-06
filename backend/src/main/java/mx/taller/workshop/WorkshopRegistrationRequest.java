package mx.taller.workshop;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record WorkshopRegistrationRequest(
  @NotBlank @Size(max = 150) String name,
  @NotBlank @Size(max = 200) String legalName,
  @NotBlank @Pattern(regexp = "^[0-9+() .-]{7,20}$") String phone,
  @NotNull UUID managerId,
  @NotBlank @Pattern(regexp = "^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$") String rfc,
  @NotBlank @Email @Size(max = 254) String email,
  @NotBlank @Size(max = 160) String street,
  @NotBlank @Size(max = 120) String neighborhood,
  @NotBlank @Size(max = 120) String municipality,
  @NotBlank @Size(max = 120) String state,
  @NotBlank @Pattern(regexp = "^[0-9]{5}$") String postalCode
) { }
