package br.com.socialconnect.api.produtos.controller;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:produtos-test;DB_CLOSE_DELAY=-1")
class ProdutoControllerH2Test extends ProdutoHttpContract {
}
