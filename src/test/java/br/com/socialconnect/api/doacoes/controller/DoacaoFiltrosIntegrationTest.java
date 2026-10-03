package br.com.socialconnect.api.doacoes.controller;

import br.com.socialconnect.api.doacoes.model.Doacao;
import br.com.socialconnect.api.doacoes.model.TipoDoacao;
import br.com.socialconnect.api.doacoes.repository.DoacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:doacoes-filtros;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class DoacaoFiltrosIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private DoacaoRepository repository;

    @BeforeEach
    void preparar() {
        repository.deleteAll();
        repository.saveAll(List.of(
                doacao("2026-01-10", TipoDoacao.ALIMENTO),
                doacao("2026-02-10", TipoDoacao.ROUPA),
                doacao("2026-03-10", TipoDoacao.ALIMENTO)));
    }

    @Test
    void deveListarSemFiltros() throws Exception {
        mvc.perform(get("/api/v1/doacoes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void deveFiltrarSomentePorTipo() throws Exception {
        mvc.perform(get("/api/v1/doacoes").param("tipo", "ROUPA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("ROUPA"));
    }

    @Test
    void deveFiltrarSomentePorDataInicialInclusiva() throws Exception {
        mvc.perform(get("/api/v1/doacoes").param("dataInicio", "2026-02-10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void deveFiltrarSomentePorDataFinalInclusiva() throws Exception {
        mvc.perform(get("/api/v1/doacoes").param("dataFim", "2026-02-10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void deveFiltrarIntervaloSemTipo() throws Exception {
        mvc.perform(get("/api/v1/doacoes").param("dataInicio", "2026-02-01").param("dataFim", "2026-02-28"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].dataDoacao").value("2026-02-10"));
    }

    @Test
    void deveCombinarFiltros() throws Exception {
        mvc.perform(get("/api/v1/doacoes").param("dataInicio", "2026-02-01")
                        .param("dataFim", "2026-03-31").param("tipo", "ALIMENTO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].dataDoacao").value("2026-03-10"));
    }

    @Test
    void devePaginarEOrdenarResultadosFiltrados() throws Exception {
        mvc.perform(get("/api/v1/doacoes").param("tipo", "ALIMENTO")
                        .param("page", "1").param("size", "1").param("sort", "dataDoacao,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].dataDoacao").value("2026-01-10"));
    }

    private Doacao doacao(String data, TipoDoacao tipo) {
        return Doacao.builder().idDoador(1L).dataDoacao(LocalDate.parse(data)).tipo(tipo).build();
    }
}
