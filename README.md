# SocialConnect API — Avaliação A1

Módulo de produtos para controlar o estoque de doações físicas de uma ONG.
Base: [repositório da disciplina](https://github.com/Prof-JamesCampos/socialconnect-api).

**Disciplina:** ITE005 — Tópicos Especiais em Sistemas para Internet III
**Professor:** James Campos
**Branch:** `avalicao1`
**Tecnologias:** Java 21, Spring Boot 4.1.1, JPA, Bean Validation, Flyway, H2, PostgreSQL 17, Swagger, JUnit e Mockito.

## Executar no Windows

Instale o JDK 21. O Maven Wrapper incluído baixa o Maven automaticamente.
A primeira execução precisa de internet para baixar dependências.

```powershell
git clone --branch avalicao1 https://github.com/Eduardo29-04/AtividadeJamesA1.git
cd AtividadeJamesA1
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

Se necessário, configure `JAVA_HOME` com a pasta do JDK 21.
Com Maven instalado, também pode usar `mvn clean compile` e `mvn spring-boot:run`.
No Linux/macOS, use `bash ./mvnw` no lugar de `.\mvnw.cmd`.

- [Swagger UI](http://localhost:8080/swagger-ui.html)
- [OpenAPI JSON](http://localhost:8080/api-docs)

O padrão usa **H2 em memória**: os dados são perdidos ao encerrar a aplicação.
O Flyway cria as tabelas; o Hibernate apenas valida a estrutura (`ddl-auto=validate`).
Não é necessário instalar banco de dados nesse modo.

## Testar no Swagger

1. Expanda **Produtos** e abra `POST /api/v1/produtos`.
2. Clique em **Try it out**, envie o JSON abaixo e confira o retorno 201.
3. Use o ID retornado nas rotas de consulta, atualização e exclusão.
4. Repita o nome para obter 409; use estoque atual -1 para obter 422.

```json
{
  "nome": "Arroz 5kg",
  "categoria": "ALIMENTO",
  "estoqueAtual": 3,
  "estoqueMinimo": 10,
  "unidadeMedida": "unidade"
}
```

O exemplo retorna `estoqueBaixo: true`, pois `3 < 10`.
Categorias: `ALIMENTO`, `ROUPA`, `HIGIENE` e `OUTROS`.

## Endpoints

| Método | Rota | Sucesso | Erros |
|---|---|---|---|
| GET | `/api/v1/produtos` | 200, página | 400 |
| GET | `/api/v1/produtos/{id_produto}` | 200 | 400, 404 |
| POST | `/api/v1/produtos` | 201 + Location | 400, 409, 422 |
| PUT | `/api/v1/produtos/{id_produto}` | 200 | 400, 404, 409, 422 |
| DELETE | `/api/v1/produtos/{id_produto}` | 204, sem corpo | 400, 404 |

Exemplo: `/api/v1/produtos?nome=arroz&categoria=ALIMENTO&page=0&size=10&sort=nome,asc`.
Os filtros funcionam separadamente ou juntos. Nome é parcial e ignora maiúsculas;
`%` e `_` são texto literal. A primeira página é 0; o padrão é 10 itens por nome.
É possível ordenar pelos campos persistidos, como `nome`, `categoria` e `estoqueAtual`.

## Regras

- Todos os campos de entrada são obrigatórios.
- Nome: até 150 caracteres. Unidade de medida: até 20.
- Estoque atual negativo: **422**, inclusive na atualização.
- Estoque mínimo negativo e demais falhas de validação: **400**.
- Nome repetido: **409**, inclusive ao atualizar para o nome de outro produto.
- Espaços externos são removidos. A unicidade diferencia maiúsculas/minúsculas;
  a pesquisa parcial não diferencia.
- `estoqueBaixo` é `estoqueAtual < estoqueMinimo`; se iguais, é `false`.
- O servidor define `dataCadastro`. PUT substitui todos os campos editáveis e preserva data e ID.
- Erros usam `application/problem+json`, com `type`, `title`, `status`, `detail`,
  `instance` e a lista `errors` quando houver falhas de validação.
- Mensagens de validação em português e inglês (`Accept-Language: en`).
- A restrição UNIQUE do banco também protege contra cadastros simultâneos.

## Testes

```powershell
# Suíte completa e empacotamento
.\mvnw.cmd clean verify

# Unitários
.\mvnw.cmd "-Dtest=ProdutoServiceTest" test

# HTTP com H2, sem Docker
.\mvnw.cmd "-Dtest=ProdutoControllerH2Test" test

# HTTP com PostgreSQL 17, requer Docker em execução
.\mvnw.cmd "-Dtest=ProdutoControllerIntegrationTest" test
```

`ProdutoServiceTest` usa Mockito e comentários Arrange, Act e Assert.
`ProdutoControllerIntegrationTest` usa `@SpringBootTest(RANDOM_PORT)` e
`@Testcontainers` com `postgres:17-alpine`. Herda os cenários de `ProdutoHttpContract`,
também executados em H2 por `ProdutoControllerH2Test`.

Cobertura: CRUD, estoque negativo, nome duplicado, alerta nos limites, filtros,
paginação, validações, tradução, migrations, restrições do banco e Swagger/OpenAPI.

**Sem Docker, os testes PostgreSQL são ignorados (skipped); isso não significa que passaram.**
Os testes unitários e H2 continuam executando. O GitHub Actions verifica Docker e roda a suíte.
Relatórios: `target/surefire-reports` e artefatos do workflow.

## PostgreSQL local (opcional)

Com Docker em execução:

```powershell
docker run --name socialconnect-postgres -e POSTGRES_DB=socialconnect -e POSTGRES_USER=socialconnect -e POSTGRES_PASSWORD=socialconnect -p 5432:5432 -d postgres:17-alpine
.\mvnw.cmd "-Dspring-boot.run.profiles=postgres" spring-boot:run
```

O perfil aceita `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.
As credenciais do exemplo são apenas para desenvolvimento local.

## Organização e estudo

O pacote `produtos` contém `controller`, `dto`, `exception`, `model`, `repository`,
`service` e `validation`. Veja [o guia do módulo](docs/GUIA_PRODUTOS.md).

## Correções na base

A V2 já criava `doacoes`, mas a V3 repetia a criação e referenciava `doadores`,
que não existe na base. A V3 agora acrescenta somente índices. A V4 cria `produtos`.
Foi incluído o starter Flyway do Spring Boot 4 e o suporte PostgreSQL; as dependências
Testcontainers foram alinhadas ao BOM do Spring Boot.

Na verificação dos endpoints, também foi corrigida a listagem de doações:
os filtros de tipo, data inicial e data final agora funcionam isoladamente ou
combinados, com cobertura em `DoacaoFiltrosIntegrationTest`.

Esta entrega parte de um banco novo. Não aplique esta revisão da V3 diretamente a um banco
antigo que já registre outro checksum dessa migration.

## Entrega

- Branch: [avalicao1](https://github.com/Eduardo29-04/AtividadeJamesA1/tree/avalicao1).
- Preencher pessoalmente [AI_USAGE.md](AI_USAGE.md).
- Revisar e compreender o código antes de entregar.
- Enviar o link da branch na atividade do Teams.
