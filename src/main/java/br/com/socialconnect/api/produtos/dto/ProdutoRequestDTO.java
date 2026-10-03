package br.com.socialconnect.api.produtos.dto;

import br.com.socialconnect.api.produtos.model.CategoriaProduto;
import br.com.socialconnect.api.produtos.validation.EstoqueNaoNegativo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Dados obrigatórios para criação ou substituição total de um produto")
public record ProdutoRequestDTO(
        @NotBlank(message = "{produto.nome.obrigatorio}")
        @Size(max = 150, message = "{produto.nome.tamanho}")
        @Schema(example = "Arroz 5kg", maxLength = 150)
        String nome,

        @NotNull(message = "{produto.categoria.obrigatoria}")
        @Schema(example = "ALIMENTO")
        CategoriaProduto categoria,

        @NotNull(message = "{produto.estoqueAtual.obrigatorio}")
        @EstoqueNaoNegativo
        @Schema(example = "3", minimum = "0")
        Integer estoqueAtual,

        @NotNull(message = "{produto.estoqueMinimo.obrigatorio}")
        @Min(value = 0, message = "{produto.estoqueMinimo.naoNegativo}")
        @Schema(example = "10", minimum = "0")
        Integer estoqueMinimo,

        @NotBlank(message = "{produto.unidade.obrigatoria}")
        @Size(max = 20, message = "{produto.unidade.tamanho}")
        @Schema(example = "unidade", maxLength = 20)
        String unidadeMedida
) {}
