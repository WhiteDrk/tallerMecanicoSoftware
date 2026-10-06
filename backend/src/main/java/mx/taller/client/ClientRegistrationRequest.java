package mx.taller.client;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record ClientRegistrationRequest(
  @NotBlank @Size(max=150) String fullName,
  @NotBlank @Size(max=150) String alternateContactName,
  @NotNull @Past @JsonFormat(pattern="yyyy-MM-dd") LocalDate birthDate,
  @NotBlank @Pattern(regexp="^[0-9+() .-]{7,20}$") String phone,
  @NotBlank @Pattern(regexp="^[0-9+() .-]{7,20}$") String workPhone,
  @NotBlank @Email @Size(max=254) String email,
  @Email @Size(max=254) String workEmail,
  @NotBlank @Size(max=160) String street,
  @NotBlank @Size(max=120) String neighborhood,
  @NotBlank @Size(max=120) String municipality,
  @NotBlank @Size(max=120) String state,
  @NotBlank @Pattern(regexp="^[0-9]{5}$") String postalCode
  , @NotNull UUID workshopId
) { }
