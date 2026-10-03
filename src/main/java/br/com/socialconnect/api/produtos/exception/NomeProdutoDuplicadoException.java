package br.com.socialconnect.api.produtos.exception;

public class NomeProdutoDuplicadoException extends RuntimeException {
    public NomeProdutoDuplicadoException() {
        super("Já existe um produto com esse nome.");
    }
}
