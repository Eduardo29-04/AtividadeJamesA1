package br.com.socialconnect.api.produtos.service;

import br.com.socialconnect.api.exception.RecursoNaoEncontradoException;
import br.com.socialconnect.api.produtos.dto.*;
import br.com.socialconnect.api.produtos.exception.*;
import br.com.socialconnect.api.produtos.model.*;
import br.com.socialconnect.api.produtos.repository.ProdutoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ProdutoServiceImpl implements ProdutoService {
    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
            "idProduto", "nome", "categoria", "estoqueAtual", "estoqueMinimo", "unidadeMedida", "dataCadastro");
    private final ProdutoRepository repository;

    public ProdutoServiceImpl(ProdutoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<ProdutoResponseDTO> listar(String nome, CategoriaProduto categoria, Pageable pageable) {
        for (Sort.Order ordem : pageable.getSort()) {
            if (!CAMPOS_ORDENACAO.contains(ordem.getProperty())) {
                throw new IllegalArgumentException("Campo de ordenação inválido: " + ordem.getProperty());
            }
        }
        Specification<Produto> filtros = (root, query, cb) -> cb.conjunction();
        if (nome != null && !nome.isBlank()) {
            String trecho = nome.strip().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            filtros = filtros.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + trecho + "%", '\\'));
        }
        if (categoria != null) {
            filtros = filtros.and((root, query, cb) -> cb.equal(root.get("categoria"), categoria));
        }
        return repository.findAll(filtros, pageable).map(this::toResponse);
    }

    @Override
    public ProdutoResponseDTO buscarPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Override
    @Transactional
    public ProdutoResponseDTO criar(ProdutoRequestDTO dto) {
        validarEstoques(dto);
        if (repository.existsByNome(dto.nome().strip())) {
            throw new NomeProdutoDuplicadoException();
        }
        Produto produto = new Produto();
        preencher(produto, dto);
        produto.setDataCadastro(LocalDate.now());
        return toResponse(salvar(produto));
    }

    @Override
    @Transactional
    public ProdutoResponseDTO atualizar(Long id, ProdutoRequestDTO dto) {
        Produto produto = buscarEntidade(id);
        validarEstoques(dto);
        if (repository.existsByNomeAndIdProdutoNot(dto.nome().strip(), id)) {
            throw new NomeProdutoDuplicadoException();
        }
        preencher(produto, dto);
        return toResponse(salvar(produto));
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        repository.delete(buscarEntidade(id));
    }

    private Produto buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + id));
    }

    private void validarEstoques(ProdutoRequestDTO dto) {
        // Também protege chamadas ao service que não passam pelo controller.
        if (dto.estoqueAtual() != null && dto.estoqueAtual() < 0) {
            throw new EstoqueNegativoException();
        }
        if (dto.estoqueAtual() == null || dto.estoqueMinimo() == null || dto.estoqueMinimo() < 0) {
            throw new IllegalArgumentException("Informe os estoques atual e mínimo com valores não negativos.");
        }
    }

    private Produto salvar(Produto produto) {
        try {
            // Flush permite tratar a restrição UNIQUE inclusive em cadastros simultâneos.
            return repository.saveAndFlush(produto);
        } catch (DataIntegrityViolationException ex) {
            Throwable causa = ex;
            while (causa != null) {
                if (causa instanceof org.hibernate.exception.ConstraintViolationException violacao
                        && violacao.getConstraintName() != null
                        && violacao.getConstraintName().toLowerCase(Locale.ROOT).contains("uk_produtos_nome")) {
                    throw new NomeProdutoDuplicadoException();
                }
                causa = causa.getCause();
            }
            throw ex;
        }
    }

    private void preencher(Produto produto, ProdutoRequestDTO dto) {
        produto.setNome(dto.nome().strip());
        produto.setCategoria(dto.categoria());
        produto.setEstoqueAtual(dto.estoqueAtual());
        produto.setEstoqueMinimo(dto.estoqueMinimo());
        produto.setUnidadeMedida(dto.unidadeMedida().strip());
    }

    private ProdutoResponseDTO toResponse(Produto produto) {
        return new ProdutoResponseDTO(produto.getIdProduto(), produto.getNome(), produto.getCategoria(),
                produto.getEstoqueAtual(), produto.getEstoqueMinimo(), produto.getUnidadeMedida(),
                produto.getDataCadastro(), produto.getEstoqueAtual() < produto.getEstoqueMinimo());
    }
}
