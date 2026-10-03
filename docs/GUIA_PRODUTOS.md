# Guia do módulo de produtos

## Fluxo de uma requisição

O JSON chega ao `ProdutoController`, é convertido em `ProdutoRequestDTO` e validado
por causa de `@Valid`. O controller chama `ProdutoService`, que aplica as regras.
O repository salva a entidade no banco. A resposta expõe um DTO, não a entidade JPA.

## Entidade e migration

`Produto` representa a tabela `produtos`. `@Id` identifica a chave primária;
`@GeneratedValue(IDENTITY)` deixa o banco gerar o ID. `@Column` define nomes e limites.
`@Enumerated(STRING)` salva o nome da categoria, não sua posição no enum.

A V4 cria a tabela. `NOT NULL` exige valores; `CHECK` impede estoques negativos;
`UNIQUE` impede nomes repetidos. Isso protege o banco também fora das rotas HTTP.

## DTOs e validação

Records separam os dados de entrada e saída. ID, data e alerta são definidos pelo servidor.
`@NotBlank` exige texto não vazio; `@Size` limita comprimento; `@NotNull` exige valor;
`@Min(0)` exige número não negativo.

`@EstoqueNaoNegativo` usa um validador próprio. Ele deixa `null` passar, pois a ausência
é tratada por `@NotNull`. O handler distingue essa validação para devolver 422.
As outras validações devolvem 400.

## Regras do service

O service verifica estoque negativo mesmo se chamado diretamente, sem HTTP.
A criação consulta se o nome existe. A atualização exclui o próprio ID dessa consulta,
permitindo manter o nome. A data original é preservada.

`saveAndFlush` envia a gravação ao banco dentro do método. Assim, o service consegue
traduzir a violação UNIQUE em 409 mesmo quando duas requisições tentam cadastrar o mesmo nome.
Outras falhas de integridade não são tratadas indevidamente como duplicação de nome.

`@Transactional` desfaz as alterações se houver exceção. Leituras usam `readOnly = true`.
O alerta é calculado a cada resposta como `estoqueAtual < estoqueMinimo`;
isso evita guardar um alerta que fique desatualizado.

## Filtros e paginação

Specifications combinam filtros opcionais. Nome usa LIKE e letras minúsculas;
categoria usa igualdade. Quando ambos são informados, ambos precisam ser atendidos.
`Pageable` contém página, tamanho e ordenação; `Page.map` converte entidades em DTOs
preservando o total de elementos e páginas.

## HTTP e Swagger

201 indica criação e inclui Location; 200 indica consulta ou atualização;
204 indica exclusão sem corpo. 400 é pedido inválido; 404 é produto inexistente;
409 é nome duplicado; 422 é estoque atual negativo.

`ProdutoExceptionHandler` centraliza os erros. `@Tag`, `@Operation`, `@ApiResponse`
e `@Schema` documentam as rotas no Swagger e permitem testá-las pelo navegador.

## Exercícios para acompanhar o código

1. Cadastre estoque 3 e mínimo 10: o alerta deve ser verdadeiro.
2. Atualize estoque para 10: o alerta deve ser falso.
3. Repita o nome em outro cadastro: deve ocorrer 409.
4. Atualize para -1: deve ocorrer 422 e o estoque anterior deve ser preservado.
5. Combine os filtros de nome e categoria e use página de tamanho 1.
6. Exclua e consulte o mesmo ID: deve ocorrer 404.

Os testes unitários simulam o repository com Mockito. Os testes HTTP passam por todas
as camadas da aplicação e repetem o mesmo contrato nos bancos H2 e PostgreSQL 17.
