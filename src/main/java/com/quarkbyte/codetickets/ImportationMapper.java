package com.quarkbyte.codetickets;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ImportationMapper implements FieldSetMapper<Importation> {
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatterHour = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Importation mapFieldSet(FieldSet fieldSet) {
        Importation importation = new Importation();
        importation.setCpf(fieldSet.readString("cpf"));
        importation.setCliente(fieldSet.readString("cliente"));
        importation.setNascimento(LocalDate.parse(fieldSet.readString("nascimento"), dateTimeFormatter));
        importation.setEvento(fieldSet.readString("evento"));
        importation.setData(LocalDate.parse(fieldSet.readString("data"), dateTimeFormatter));
        importation.setTipoIngresso(fieldSet.readString("tipoIngresso"));
        importation.setValor(fieldSet.readDouble("valor"));
        importation.setHoraImportacao(LocalDateTime.now());
        return importation;
    }
}
