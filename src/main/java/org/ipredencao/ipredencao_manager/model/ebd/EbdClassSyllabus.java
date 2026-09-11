package org.ipredencao.ipredencao_manager.model.ebd;

// Usado só internamente pelo download (EbdService -> EbdController); nunca
// serializado como JSON — o controller monta um ResponseEntity<byte[]> com os
// headers certos a partir daqui. Mesmo papel que EbdMaterialContent tinha
// antes de EbdMaterial/EbdMaterialContent serem unificados — não dá pra
// repetir esse truque aqui porque a ementa não é uma linha própria (fica
// embutida em ebd_class), então não tem um "EbdClassSyllabus" de listagem
// pra unificar com.
public record EbdClassSyllabus(
        String fileName,
        String contentType,
        byte[] data
) {}
