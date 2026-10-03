package br.com.socialconnect.api.produtos.controller;

import br.com.socialconnect.api.produtos.dto.*;
import br.com.socialconnect.api.produtos.model.CategoriaProduto;
import br.com.socialconnect.api.produtos.service.ProdutoService;
import br.com.socialconnect.api.exception.ProblemDetail;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/produtos")
@Tag(name = "Produtos", description = "Cadastro e controle do estoque de doações físicas")
public class ProdutoController {
    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar produtos", description = "Filtros opcionais combináveis; nome parcial sem diferenciar maiúsculas. Paginação começa em zero.")
    @ApiResponse(responseCode = "200", description = "Página de produtos")
    @ApiResponse(responseCode = "400", description = "Filtro ou ordenação inválidos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Page<ProdutoResponseDTO>> listar(
            @Parameter(description = "Parte do nome", example = "arroz") @RequestParam(required = false) String nome,
            @RequestParam(required = false) CategoriaProduto categoria,
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(service.listar(nome, categoria, pageable));
    }

    @GetMapping("/{id_produto}")
    @Operation(summary = "Buscar produto por ID")
    @ApiResponse(responseCode = "200", description = "Produto encontrado")
    @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "400", description = "ID inválido",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(
            @Parameter(example = "1") @PathVariable("id_produto") Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar produto", description = "A data de cadastro é definida pelo servidor.")
    @ApiResponse(responseCode = "201", description = "Produto criado",
            headers = @Header(name = "Location", description = "URL do produto criado", schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "400", description = "Campos inválidos ou ausentes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Nome já cadastrado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Estoque atual negativo",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ProdutoResponseDTO> criar(@Valid @RequestBody ProdutoRequestDTO dto) {
        ProdutoResponseDTO produto = service.criar(dto);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(produto.idProduto()).toUri();
        return ResponseEntity.created(location).body(produto);
    }

    @PutMapping("/{id_produto}")
    @Operation(summary = "Atualizar produto", description = "Substitui todos os campos editáveis; preserva ID e data de cadastro.")
    @ApiResponse(responseCode = "200", description = "Produto atualizado")
    @ApiResponse(responseCode = "400", description = "Campos inválidos ou ausentes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Nome usado por outro produto",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Estoque atual negativo",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ProdutoResponseDTO> atualizar(
            @Parameter(example = "1") @PathVariable("id_produto") Long id,
            @Valid @RequestBody ProdutoRequestDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id_produto}")
    @Operation(summary = "Excluir produto")
    @ApiResponse(responseCode = "204", description = "Produto removido", content = @Content)
    @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "400", description = "ID inválido",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> deletar(@Parameter(example = "1") @PathVariable("id_produto") Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
