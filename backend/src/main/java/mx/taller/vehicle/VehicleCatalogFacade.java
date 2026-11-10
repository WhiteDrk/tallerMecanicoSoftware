package mx.taller.vehicle;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleCatalogFacade {
  private final VehicleBrandRepository brands;
  private final VehicleModelRepository models;
  public VehicleCatalogFacade(VehicleBrandRepository brands, VehicleModelRepository models) { this.brands = brands; this.models = models; }
  @Transactional(readOnly = true) public List<BrandOption> brands() { return brands.findByEnabledTrueOrderByNameAsc().stream().map(brand -> new BrandOption(brand.getId(), brand.getName())).toList(); }
  @Transactional(readOnly = true) public List<ModelOption> models(Long brandId) { return models.findByBrandIdAndEnabledTrueOrderByNameAsc(brandId).stream().map(model -> new ModelOption(model.getId(), model.getName())).toList(); }
  @Transactional(readOnly = true) public VehicleModel requiredModel(Long brandId, Long modelId) {
    VehicleBrand brand = brands.findByIdAndEnabledTrue(brandId).orElseThrow(() -> new VehicleValidationException("La marca no existe o está inhabilitada"));
    VehicleModel model = models.findByIdAndEnabledTrue(modelId).orElseThrow(() -> new VehicleValidationException("El modelo no existe o está inhabilitado"));
    if (!model.getBrand().getId().equals(brand.getId())) throw new VehicleValidationException("El modelo no pertenece a la marca seleccionada");
    return model;
  }
  @Transactional public VehicleModel resolveModel(Long brandId, Long modelId, String manualBrand, String manualModel) {
    boolean selectedCatalog = brandId != null || modelId != null;
    boolean suppliedManual = hasText(manualBrand) || hasText(manualModel);
    if (selectedCatalog == suppliedManual) throw new VehicleValidationException("Seleccione marca y modelo del catálogo o capture ambos datos manuales");
    if (selectedCatalog) {
      if (brandId == null || modelId == null) throw new VehicleValidationException("Debe seleccionar una marca y un modelo del catálogo");
      return requiredModel(brandId, modelId);
    }
    String normalizedBrand = normalizeManualName(manualBrand, "marca");
    String normalizedModel = normalizeManualName(manualModel, "modelo");
    VehicleBrand brand = brands.findByNameIgnoreCase(normalizedBrand).orElseGet(() -> brands.save(VehicleBrand.manualPending(normalizedBrand)));
    if (!brand.isEnabled() && !brand.isPendingReview()) throw new VehicleValidationException("La marca manual coincide con una marca inhabilitada");
    VehicleModel model = models.findByBrandIdAndNameIgnoreCase(brand.getId(), normalizedModel).orElseGet(() -> models.save(VehicleModel.manualPending(brand, normalizedModel)));
    if (!model.isEnabled() && !model.isPendingReview()) throw new VehicleValidationException("El modelo manual coincide con un modelo inhabilitado");
    return model;
  }
  private boolean hasText(String value) { return value != null && !value.isBlank(); }
  private String normalizeManualName(String value, String field) {
    String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    if (!normalized.matches("[A-ZÁÉÍÓÚÜÑ0-9][A-ZÁÉÍÓÚÜÑ0-9 &().,'/+-]{1,99}")) throw new VehicleValidationException("La " + field + " manual debe tener de 2 a 100 caracteres válidos");
    return normalized;
  }
  public record BrandOption(Long id, String name) { }
  public record ModelOption(Long id, String name) { }
}
