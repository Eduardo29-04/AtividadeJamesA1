package br.com.socialconnect.api.produtos.controller;

import br.com.socialconnect.api.produtos.repository.ProdutoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;

abstract class ProdutoHttpContract {
    @Autowired
    private Environment environment;
    @Autowired
    private ProdutoRepository repository;
    @Autowired
    private Flyway flyway;
    @Autowired
    private JdbcTemplate jdbc;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String ROTA = "/api/v1/produtos";

    @BeforeEach
    void limparProdutos() {
        repository.deleteAll();
    }

    @Test
    void deveCriarProdutoQuandoDadosValidos() throws Exception {
        // Arrange
        String dados = dados("Arroz 5kg", "ALIMENTO", 3, 10);
        // Act
        var response = enviar("POST", ROTA, dados);
        // Assert
        assertThat(response.statusCode()).isEqualTo(201);
        var produto = json(response);
        assertThat(produto.get("idProduto").asLong()).isPositive();
        assertThat(produto.get("estoqueBaixo").asBoolean()).isTrue();
        assertThat(produto.get("dataCadastro").asText()).matches("\\d{4}-\\d{2}-\\d{2}");
        String location = response.headers().firstValue("Location").orElseThrow();
        assertThat(location).endsWith(ROTA + "/" + produto.get("idProduto").asLong());
        assertThat(enviar("GET", URI.create(location).getPath(), null).statusCode()).isEqualTo(200);
    }

    @Test
    void deveRetornar409QuandoNomeDuplicado() throws Exception {
        // Arrange
        criar("Arroz", "ALIMENTO", 3, 10);
        // Act
        var response = enviar("POST", ROTA, dados("Arroz", "ALIMENTO", 3, 10));
        // Assert
        verificarProblema(response, 409);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void deveRetornar422QuandoEstoqueNegativo() throws Exception {
        // Arrange
        String dados = dados("Arroz", "ALIMENTO", -1, 10);
        // Act
        var response = enviar("POST", ROTA, dados);
        // Assert
        verificarProblema(response, 422);
        assertThat(json(response).get("errors").get(0).get("field").asText()).isEqualTo("estoqueAtual");
        assertThat(json(response).get("errors").get(0).get("message").asText()).doesNotContain("{produto.");
        assertThat(repository.count()).isZero();
    }

    @Test
    void deveRejeitarAtualizacaoNegativaSemAlterarProduto() throws Exception {
        // Arrange
        long id = criar("Arroz", "ALIMENTO", 3, 10).get("idProduto").asLong();
        // Act
        var response = enviar("PUT", ROTA + "/" + id, dados("Arroz", "ALIMENTO", -1, 10));
        // Assert
        verificarProblema(response, 422);
        assertThat(json(enviar("GET", ROTA + "/" + id, null)).get("estoqueAtual").asInt()).isEqualTo(3);
    }

    @Test
    void deveAtualizarTodosOsCamposPreservandoData() throws Exception {
        // Arrange
        var original = criar("Arroz", "ALIMENTO", 3, 10);
        long id = original.get("idProduto").asLong();
        // Act
        var response = enviar("PUT", ROTA + "/" + id, dados("Casaco", "ROUPA", 10, 10).replace("\"unidade\"", "\"peca\""));
        // Assert
        assertThat(response.statusCode()).isEqualTo(200);
        var atualizado = json(response);
        assertThat(atualizado.get("nome").asText()).isEqualTo("Casaco");
        assertThat(atualizado.get("categoria").asText()).isEqualTo("ROUPA");
        assertThat(atualizado.get("unidadeMedida").asText()).isEqualTo("peca");
        assertThat(atualizado.get("dataCadastro")).isEqualTo(original.get("dataCadastro"));
        assertThat(atualizado.get("estoqueBaixo").asBoolean()).isFalse();
    }

    @Test
    void devePermitirAtualizarSemTrocarNome() throws Exception {
        // Arrange
        long id = criar("Arroz", "ALIMENTO", 3, 10).get("idProduto").asLong();
        // Act
        var response = enviar("PUT", ROTA + "/" + id, dados("Arroz", "ALIMENTO", 20, 10));
        // Assert
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response).get("estoqueBaixo").asBoolean()).isFalse();
    }

    @Test
    void deveRejeitarNomeDuplicadoNaAtualizacao() throws Exception {
        // Arrange
        long id = criar("Arroz", "ALIMENTO", 3, 10).get("idProduto").asLong();
        criar("Feijao", "ALIMENTO", 8, 10);
        // Act
        var response = enviar("PUT", ROTA + "/" + id, dados("Feijao", "ALIMENTO", 5, 10));
        // Assert
        verificarProblema(response, 409);
        assertThat(json(enviar("GET", ROTA + "/" + id, null)).get("nome").asText()).isEqualTo("Arroz");
    }

    @Test
    void deveListarComFiltrosIndependentesECombinadosEPaginacao() throws Exception {
        // Arrange
        criar("Arroz branco", "ALIMENTO", 1, 0);
        criar("Arroz integral", "ALIMENTO", 1, 0);
        criar("Arroz brinquedo", "OUTROS", 1, 0);
        criar("Sabonete", "HIGIENE", 1, 0);
        // Act
        var todos = json(enviar("GET", ROTA, null));
        var nome = json(enviar("GET", ROTA + "?nome=ARROZ", null));
        var categoria = json(enviar("GET", ROTA + "?categoria=HIGIENE", null));
        var pagina = json(enviar("GET", ROTA + "?nome=arroz&categoria=ALIMENTO&page=1&size=1&sort=nome,asc", null));
        // Assert
        assertThat(todos.get("totalElements").asInt()).isEqualTo(4);
        assertThat(nome.get("totalElements").asInt()).isEqualTo(3);
        assertThat(categoria.get("content").get(0).get("nome").asText()).isEqualTo("Sabonete");
        assertThat(pagina.get("totalElements").asInt()).isEqualTo(2);
        assertThat(pagina.get("number").asInt()).isEqualTo(1);
        assertThat(pagina.get("content").get(0).get("nome").asText()).isEqualTo("Arroz integral");
    }

    @Test
    void deveTratarCuringasDoFiltroComoTextoLiteral() throws Exception {
        // Arrange
        criar("Leite 100%", "ALIMENTO", 1, 0);
        criar("Leite comum", "ALIMENTO", 1, 0);
        // Act
        var response = enviar("GET", ROTA + "?nome=%25", null);
        // Assert
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response).get("totalElements").asInt()).isEqualTo(1);
    }

    @Test
    void deveExcluirERetornar404Depois() throws Exception {
        // Arrange
        long id = criar("Arroz", "ALIMENTO", 3, 10).get("idProduto").asLong();
        // Act
        var response = enviar("DELETE", ROTA + "/" + id, null);
        // Assert
        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
        verificarProblema(enviar("GET", ROTA + "/" + id, null), 404);
        verificarProblema(enviar("DELETE", ROTA + "/" + id, null), 404);
        verificarProblema(enviar("PUT", ROTA + "/" + id, dados("Arroz", "ALIMENTO", 1, 0)), 404);
    }

    @Test
    void deveRetornar400QuandoCamposAusentes() throws Exception {
        // Arrange / Act
        var response = enviar("POST", ROTA, "{}");
        // Assert
        verificarProblema(response, 400);
        assertThat(json(response).get("errors").size()).isEqualTo(5);
    }

    @Test
    void deveValidarLimitesDosCampos() throws Exception {
        // Arrange
        String[] invalidos = {
                dados("", "ALIMENTO", 1, 0),
                dados(" ", "ALIMENTO", 1, 0),
                dados("A".repeat(151), "ALIMENTO", 1, 0),
                dados("Arroz", "ALIMENTO", 1, -1),
                dados("Arroz", "ALIMENTO", 1, 0).replace("\"unidade\"", "\"" + "x".repeat(21) + "\""),
                dados("Arroz", "ALIMENTO", 1, 0).replace("\"unidade\"", "\"\"")
        };
        // Act / Assert
        for (String invalido : invalidos) {
            verificarProblema(enviar("POST", ROTA, invalido), 400);
        }
        assertThat(repository.count()).isZero();
    }

    @Test
    void deveRejeitarJsonCategoriaIdEOrdenacaoInvalidos() throws Exception {
        // Arrange / Act / Assert
        verificarProblema(enviar("POST", ROTA, "{"), 400);
        verificarProblema(enviar("POST", ROTA, dados("Arroz", "INVALIDA", 1, 0)), 400);
        verificarProblema(enviar("GET", ROTA + "?categoria=INVALIDA", null), 400);
        verificarProblema(enviar("GET", ROTA + "/abc", null), 400);
        verificarProblema(enviar("GET", ROTA + "?sort=inexistente,asc", null), 400);
    }

    @Test
    void deveValidarPutCompleto() throws Exception {
        // Arrange
        long id = criar("Arroz", "ALIMENTO", 3, 10).get("idProduto").asLong();
        // Act
        var response = enviar("PUT", ROTA + "/" + id, "{\"nome\":\"Feijao\"}");
        // Assert
        verificarProblema(response, 400);
        assertThat(json(enviar("GET", ROTA + "/" + id, null)).get("nome").asText()).isEqualTo("Arroz");
    }

    @Test
    void deveAceitarZeroECalcularAlertaNosLimites() throws Exception {
        // Arrange / Act
        var zero = criar("Zero", "OUTROS", 0, 0);
        var igual = criar("Igual", "OUTROS", 10, 10);
        var acima = criar("Acima", "OUTROS", 11, 10);
        var abaixo = criar("Abaixo", "OUTROS", 9, 10);
        // Assert
        assertThat(zero.get("estoqueBaixo").asBoolean()).isFalse();
        assertThat(igual.get("estoqueBaixo").asBoolean()).isFalse();
        assertThat(acima.get("estoqueBaixo").asBoolean()).isFalse();
        assertThat(abaixo.get("estoqueBaixo").asBoolean()).isTrue();
    }

    @Test
    void deveTraduzirMensagemDeValidacaoParaIngles() throws Exception {
        // Arrange
        var request = HttpRequest.newBuilder(uri(ROTA)).header("Content-Type", "application/json")
                .header("Accept-Language", "en").POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        // Act
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        // Assert
        verificarProblema(response, 400);
        assertThat(response.body()).contains("Product name is required.");
    }

    @Test
    void deveAplicarFlywayERestricoesDoBanco() {
        // Arrange / Act
        var migrations = flyway.info().applied();
        // Assert
        assertThat(migrations).hasSize(4);
        assertThat(migrations[3].getVersion().toString()).isEqualTo("4");
        String sql = "INSERT INTO produtos (nome,categoria,estoque_atual,estoque_minimo,unidade_medida,data_cadastro) VALUES (?,?,?,?,?,CURRENT_DATE)";
        assertThatThrownBy(() -> jdbc.update(sql, "Invalido", "ALIMENTO", -1, 0, "kg"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update(sql, "Unico", "ALIMENTO", 1, 0, "kg");
        assertThatThrownBy(() -> jdbc.update(sql, "Unico", "ALIMENTO", 1, 0, "kg"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveExporSwaggerComTodasAsOperacoesEExemplos() throws Exception {
        // Arrange / Act
        var swagger = enviar("GET", "/swagger-ui.html", null);
        var docs = enviar("GET", "/api-docs", null);
        // Assert
        assertThat(swagger.statusCode()).isBetween(200, 399);
        assertThat(enviar("GET", "/swagger-ui/index.html", null).statusCode()).isEqualTo(200);
        assertThat(docs.statusCode()).isEqualTo(200);
        var documento = json(docs);
        var paths = documento.get("paths");
        assertThat(paths.get(ROTA).has("get")).isTrue();
        assertThat(paths.get(ROTA).has("post")).isTrue();
        assertThat(paths.get(ROTA + "/{id_produto}").has("get")).isTrue();
        assertThat(paths.get(ROTA + "/{id_produto}").has("put")).isTrue();
        assertThat(paths.get(ROTA + "/{id_produto}").has("delete")).isTrue();
        assertThat(paths.get(ROTA).get("post").get("responses").has("422")).isTrue();
        assertThat(paths.get(ROTA).get("post").get("responses").get("201").get("headers").has("Location")).isTrue();
        assertThat(documento.get("components").get("schemas").get("ProdutoRequestDTO")
                .get("properties").get("nome").get("example").asText()).isEqualTo("Arroz 5kg");
    }

    private JsonNode criar(String nome, String categoria, int atual, int minimo) throws Exception {
        var response = enviar("POST", ROTA, dados(nome, categoria, atual, minimo));
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return json(response);
    }

    private String dados(String nome, String categoria, int atual, int minimo) {
        return """
                {"nome":"%s","categoria":"%s","estoqueAtual":%d,"estoqueMinimo":%d,"unidadeMedida":"unidade"}
                """.formatted(nome, categoria, atual, minimo);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + environment.getRequiredProperty("local.server.port") + path);
    }

    private HttpResponse<String> enviar(String method, String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) throws Exception {
        return mapper.readTree(response.body());
    }

    private void verificarProblema(HttpResponse<String> response, int status) throws Exception {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        var problem = json(response);
        assertThat(problem.get("status").asInt()).isEqualTo(status);
        assertThat(problem.get("title").asText()).isNotBlank();
        assertThat(problem.get("type").asText()).startsWith("https://");
        assertThat(problem.get("detail").asText()).isNotBlank();
        assertThat(problem.get("instance").asText()).startsWith(ROTA);
    }
}
