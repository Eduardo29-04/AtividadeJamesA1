package br.com.socialconnect.api.produtos.service;

import br.com.socialconnect.api.exception.RecursoNaoEncontradoException;
import br.com.socialconnect.api.produtos.dto.ProdutoRequestDTO;
import br.com.socialconnect.api.produtos.exception.*;
import br.com.socialconnect.api.produtos.model.*;
import br.com.socialconnect.api.produtos.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {
    @Mock
    private ProdutoRepository repository;
    @InjectMocks
    private ProdutoServiceImpl service;

    @Test
    void deveCriarProdutoQuandoDadosValidos() {
        // Arrange
        var dto = dados("Arroz", 3, 10);
        when(repository.saveAndFlush(any(Produto.class))).thenAnswer(invocation -> {
            Produto produto = invocation.getArgument(0);
            produto.setIdProduto(1L);
            return produto;
        });

        // Act
        var resposta = service.criar(dto);

        // Assert
        assertThat(resposta.idProduto()).isEqualTo(1L);
        assertThat(resposta.nome()).isEqualTo("Arroz");
        assertThat(resposta.dataCadastro()).isEqualTo(LocalDate.now());
        assertThat(resposta.estoqueBaixo()).isTrue();
    }

    @Test
    void deveLancarExcecaoQuandoEstoqueNegativo() {
        // Arrange
        var dto = dados("Arroz", -1, 10);

        // Act
        var erro = catchThrowable(() -> service.criar(dto));

        // Assert
        assertThat(erro).isInstanceOf(EstoqueNegativoException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void deveLancarExcecaoQuandoNomeDuplicado() {
        // Arrange
        when(repository.existsByNome("Arroz")).thenReturn(true);

        // Act
        var erro = catchThrowable(() -> service.criar(dados("Arroz", 5, 10)));

        // Assert
        assertThat(erro).isInstanceOf(NomeProdutoDuplicadoException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void devePreservarDataCadastroAoAtualizar() {
        // Arrange
        Produto produto = produto();
        when(repository.findById(1L)).thenReturn(Optional.of(produto));
        when(repository.saveAndFlush(produto)).thenReturn(produto);

        // Act
        var resposta = service.atualizar(1L, dados("Arroz", 10, 10));

        // Assert
        assertThat(resposta.dataCadastro()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(resposta.estoqueBaixo()).isFalse();
        verify(repository).existsByNomeAndIdProdutoNot("Arroz", 1L);
    }

    @Test
    void deveRejeitarEstoqueNegativoNaAtualizacao() {
        // Arrange
        Produto produto = produto();
        when(repository.findById(1L)).thenReturn(Optional.of(produto));

        // Act
        var erro = catchThrowable(() -> service.atualizar(1L, dados("Arroz", -1, 10)));

        // Assert
        assertThat(erro).isInstanceOf(EstoqueNegativoException.class);
        assertThat(produto.getEstoqueAtual()).isEqualTo(3);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarNomeDeOutroProdutoNaAtualizacao() {
        // Arrange
        Produto produto = produto();
        when(repository.findById(1L)).thenReturn(Optional.of(produto));
        when(repository.existsByNomeAndIdProdutoNot("Feijao", 1L)).thenReturn(true);

        // Act
        var erro = catchThrowable(() -> service.atualizar(1L, dados("Feijao", 10, 10)));

        // Assert
        assertThat(erro).isInstanceOf(NomeProdutoDuplicadoException.class);
        assertThat(produto.getNome()).isEqualTo("Arroz");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRetornarProdutoPorIdComAlerta() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(produto()));

        // Act
        var resposta = service.buscarPorId(1L);

        // Assert
        assertThat(resposta.estoqueBaixo()).isTrue();
        assertThat(resposta.idProduto()).isEqualTo(1L);
    }

    @Test
    void deveRejeitarBuscaInexistente() {
        // Arrange
        when(repository.findById(99L)).thenReturn(Optional.empty());

        // Act
        var erro = catchThrowable(() -> service.buscarPorId(99L));

        // Assert
        assertThat(erro).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveDeletarProdutoExistente() {
        // Arrange
        Produto produto = produto();
        when(repository.findById(1L)).thenReturn(Optional.of(produto));

        // Act
        service.deletar(1L);

        // Assert
        verify(repository).delete(produto);
    }

    @Test
    void deveRejeitarExclusaoInexistente() {
        // Arrange
        when(repository.findById(99L)).thenReturn(Optional.empty());

        // Act
        var erro = catchThrowable(() -> service.deletar(99L));

        // Assert
        assertThat(erro).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repository, never()).delete(any(Produto.class));
    }

    @Test
    void deveAceitarEstoqueZeroSemAlertaQuandoMinimoZero() {
        // Arrange
        when(repository.saveAndFlush(any(Produto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var resposta = service.criar(dados("Arroz", 0, 0));

        // Assert
        assertThat(resposta.estoqueAtual()).isZero();
        assertThat(resposta.estoqueBaixo()).isFalse();
    }

    @Test
    void deveRemoverEspacosExternosDoNome() {
        // Arrange
        when(repository.existsByNome("Arroz")).thenReturn(true);

        // Act
        var erro = catchThrowable(() -> service.criar(dados("  Arroz  ", 1, 0)));

        // Assert
        assertThat(erro).isInstanceOf(NomeProdutoDuplicadoException.class);
    }

    private ProdutoRequestDTO dados(String nome, int atual, int minimo) {
        return new ProdutoRequestDTO(nome, CategoriaProduto.ALIMENTO, atual, minimo, "unidade");
    }

    private Produto produto() {
        Produto produto = new Produto();
        produto.setIdProduto(1L);
        produto.setNome("Arroz");
        produto.setCategoria(CategoriaProduto.ALIMENTO);
        produto.setEstoqueAtual(3);
        produto.setEstoqueMinimo(10);
        produto.setUnidadeMedida("unidade");
        produto.setDataCadastro(LocalDate.of(2026, 1, 1));
        return produto;
    }
}
