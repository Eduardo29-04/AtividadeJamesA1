package br.com.socialconnect.api.produtos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EstoqueNaoNegativoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EstoqueNaoNegativo {
    String message() default "{produto.estoque.naoNegativo}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
