package br.com.socialconnect.api.produtos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "produtos", uniqueConstraints = @UniqueConstraint(name = "uk_produtos_nome", columnNames = "nome"))
@Getter
@Setter
@NoArgsConstructor
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produto")
    private Long idProduto;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaProduto categoria;

    @Column(name = "estoque_atual", nullable = false)
    private Integer estoqueAtual;

    @Column(name = "estoque_minimo", nullable = false)
    private Integer estoqueMinimo;

    @Column(name = "unidade_medida", nullable = false, length = 20)
    private String unidadeMedida;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDate dataCadastro;
}
