-- A tabela doacoes já foi criada e populada pela V2.
-- O módulo base representa id_doador como Long e não possui tabela doadores.

CREATE INDEX idx_doacoes_data ON doacoes(data_doacao);
CREATE INDEX idx_doacoes_tipo ON doacoes(tipo);
