package br.com.socialconnect.api.produtos.repository;

import br.com.socialconnect.api.produtos.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {
    boolean existsByNome(String nome);
    boolean existsByNomeAndIdProdutoNot(String nome, Long idProduto);
}
