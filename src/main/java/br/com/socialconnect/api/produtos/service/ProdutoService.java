package br.com.socialconnect.api.produtos.service;

import br.com.socialconnect.api.produtos.dto.ProdutoRequestDTO;
import br.com.socialconnect.api.produtos.dto.ProdutoResponseDTO;
import br.com.socialconnect.api.produtos.model.CategoriaProduto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProdutoService {
    Page<ProdutoResponseDTO> listar(String nome, CategoriaProduto categoria, Pageable pageable);
    ProdutoResponseDTO buscarPorId(Long id);
    ProdutoResponseDTO criar(ProdutoRequestDTO dto);
    ProdutoResponseDTO atualizar(Long id, ProdutoRequestDTO dto);
    void deletar(Long id);
}
