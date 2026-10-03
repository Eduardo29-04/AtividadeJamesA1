package br.com.socialconnect.api.produtos.exception;

import br.com.socialconnect.api.exception.ProblemDetail;
import br.com.socialconnect.api.exception.RecursoNaoEncontradoException;
import br.com.socialconnect.api.produtos.controller.ProdutoController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice(assignableTypes = ProdutoController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProdutoExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validar(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ProblemDetail.FieldError(erro.getField(), erro.getDefaultMessage())).toList();
        boolean estoqueNegativo = ex.getBindingResult().getFieldErrors().stream()
                .anyMatch(erro -> "EstoqueNaoNegativo".equals(erro.getCode()));
        int status = estoqueNegativo ? 422 : 400;
        return resposta(status, estoqueNegativo ? "Estoque inválido" : "Erro de validação",
                "Um ou mais campos são inválidos.", request, errors);
    }

    @ExceptionHandler(EstoqueNegativoException.class)
    public ResponseEntity<ProblemDetail> estoqueNegativo(EstoqueNegativoException ex, HttpServletRequest request) {
        return resposta(422, "Estoque inválido", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(NomeProdutoDuplicadoException.class)
    public ResponseEntity<ProblemDetail> nomeDuplicado(NomeProdutoDuplicadoException ex, HttpServletRequest request) {
        return resposta(409, "Nome já cadastrado", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return resposta(404, "Produto não encontrado", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return resposta(400, "Requisição inválida",
                "Informe um JSON válido, com categoria permitida e estoques inteiros.", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return resposta(400, "Parâmetro inválido", "Valor inválido para o parâmetro " + ex.getName() + ".", request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> argumentoInvalido(IllegalArgumentException ex, HttpServletRequest request) {
        return resposta(400, "Requisição inválida", ex.getMessage(), request, List.of());
    }

    private ResponseEntity<ProblemDetail> resposta(int status, String title, String detail,
            HttpServletRequest request, List<ProblemDetail.FieldError> errors) {
        var problem = new ProblemDetail("https://socialconnect.api/errors/produtos/" + status,
                title, status, detail, request.getRequestURI(), LocalDateTime.now(), errors);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }
}
