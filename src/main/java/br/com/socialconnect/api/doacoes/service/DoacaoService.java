package br.com.socialconnect.api.doacoes.service;

import br.com.socialconnect.api.doacoes.dto.DoacaoRequestDTO;
import br.com.socialconnect.api.doacoes.dto.DoacaoResponseDTO;
import br.com.socialconnect.api.doacoes.model.Doacao;
import br.com.socialconnect.api.doacoes.model.TipoDoacao;
import br.com.socialconnect.api.doacoes.repository.DoacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class DoacaoService {

    private final DoacaoRepository repository;

    public DoacaoService(DoacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DoacaoResponseDTO criar(DoacaoRequestDTO dto) {
        Doacao doacao = toEntity(dto);
        Doacao salva = repository.save(doacao);
        return toDTO(salva);
    }

    public Page<DoacaoResponseDTO> listar(
            LocalDate dataInicio, LocalDate dataFim, TipoDoacao tipo, Pageable pageable) {

        Specification<Doacao> filtros = (root, query, cb) -> cb.conjunction();
        if (dataInicio != null) {
            filtros = filtros.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dataDoacao"), dataInicio));
        }
        if (dataFim != null) {
            filtros = filtros.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("dataDoacao"), dataFim));
        }
        if (tipo != null) {
            filtros = filtros.and((root, query, cb) -> cb.equal(root.get("tipo"), tipo));
        }
        return repository.findAll(filtros, pageable).map(this::toDTO);
    }

    private Doacao toEntity(DoacaoRequestDTO dto) {
        Doacao doacao = new Doacao();
        // Ajuste os getters/setters conforme os campos reais da sua entidade Doacao
        doacao.setIdDoador(dto.idDoador()); // ou dto.getIdDoador() se for classe comum
        doacao.setDataDoacao(dto.dataDoacao());
        doacao.setValor(dto.valor());
        doacao.setTipo(dto.tipo());
        return doacao;
    }

    private DoacaoResponseDTO toDTO(Doacao entity) {
        return new DoacaoResponseDTO(
                entity.getIdDoacao(),
                entity.getIdDoador(),
                entity.getDataDoacao(),
                entity.getValor(),
                entity.getTipo()
        );
    }
}
