package br.com.socialconnect.api.produtos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EstoqueNaoNegativoValidator implements ConstraintValidator<EstoqueNaoNegativo, Integer> {
    @Override
    public boolean isValid(Integer estoque, ConstraintValidatorContext context) {
        // A ausência de valor é tratada separadamente por @NotNull.
        return estoque == null || estoque >= 0;
    }
}
