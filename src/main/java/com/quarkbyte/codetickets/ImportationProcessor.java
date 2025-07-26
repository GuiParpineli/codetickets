package com.quarkbyte.codetickets;

import org.springframework.batch.item.ItemProcessor;

public class ImportationProcessor implements ItemProcessor<Importation, Importation> {
    @Override
    public Importation process(Importation item) throws Exception {
        if (item.getTipoIngresso().equalsIgnoreCase("vip")) {
            item.setTaxaAdm(130.0);
        } else if (item.getTipoIngresso().equalsIgnoreCase("camarote")) {
            item.setTaxaAdm(80.0);
        } else {
            item.setTaxaAdm(50.0);
        }
        return item;
    }
}
